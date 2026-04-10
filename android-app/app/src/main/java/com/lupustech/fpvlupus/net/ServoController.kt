package com.lupustech.fpvlupus.net

import android.os.Handler
import android.os.Looper
import com.lupustech.fpvlupus.model.AppConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class ServoController {
    interface Listener {
        fun onServoRequestResult(success: Boolean)
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestInFlight = AtomicBoolean(false)
    private val dispatchRunnable = Runnable { dispatchPendingCommand() }

    @Volatile
    private var config: AppConfig = AppConfig()

    @Volatile
    private var lastSentAt: Long = 0L

    @Volatile
    private var lastPan: Int? = null

    @Volatile
    private var lastTilt: Int? = null

    @Volatile
    private var pendingPan: Int? = null

    @Volatile
    private var pendingTilt: Int? = null

    @Volatile
    private var forcePending = false

    @Volatile
    private var lastRequestSucceeded = true

    var listener: Listener? = null

    fun updateConfig(newConfig: AppConfig) {
        config = newConfig
    }

    fun center() {
        sendAngles(config.panCenter, config.tiltCenter, force = true)
    }

    fun testConnection(callback: (Boolean) -> Unit) {
        executor.execute {
            val success = performRequest(config.panCenter, config.tiltCenter)
            lastRequestSucceeded = success
            mainHandler.post { callback(success) }
        }
    }

    fun sendAngles(pan: Int, tilt: Int, force: Boolean = false) {
        pendingPan = pan
        pendingTilt = tilt
        forcePending = forcePending || force
        scheduleDispatch(if (force) 0L else remainingIntervalDelay())
    }

    fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
    }

    private fun remainingIntervalDelay(): Long {
        val elapsed = System.currentTimeMillis() - lastSentAt
        return (config.sendIntervalMs - elapsed).coerceAtLeast(0L)
    }

    private fun scheduleDispatch(delayMs: Long) {
        mainHandler.removeCallbacks(dispatchRunnable)
        mainHandler.postDelayed(dispatchRunnable, delayMs)
    }

    private fun dispatchPendingCommand() {
        val pan = pendingPan ?: return
        val tilt = pendingTilt ?: return

        if (requestInFlight.get()) {
            scheduleDispatch(config.sendIntervalMs)
            return
        }

        val remainingDelay = remainingIntervalDelay()
        if (!forcePending && remainingDelay > 0L) {
            scheduleDispatch(remainingDelay)
            return
        }

        val changedEnough = !lastRequestSucceeded ||
            lastPan == null || lastTilt == null ||
            abs(lastPan!! - pan) >= 1 || abs(lastTilt!! - tilt) >= 1

        if (!forcePending && !changedEnough) {
            pendingPan = null
            pendingTilt = null
            return
        }

        if (!requestInFlight.compareAndSet(false, true)) {
            scheduleDispatch(config.sendIntervalMs)
            return
        }

        pendingPan = null
        pendingTilt = null
        val forced = forcePending
        forcePending = false
        lastSentAt = System.currentTimeMillis()

        executor.execute {
            val success = performRequest(pan, tilt)
            lastRequestSucceeded = success
            if (success || forced) {
                lastPan = pan
                lastTilt = tilt
            }
            requestInFlight.set(false)
            mainHandler.post {
                listener?.onServoRequestResult(success)
                if (pendingPan != null && pendingTilt != null) {
                    scheduleDispatch(remainingIntervalDelay())
                }
            }
        }
    }

    private fun performRequest(pan: Int, tilt: Int): Boolean {
        val urlString = buildUrl(config, pan, tilt)
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1500
            connection.readTimeout = 1500
            connection.instanceFollowRedirects = true
            connection.responseCode in 200..299
        } catch (_: IOException) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildUrl(config: AppConfig, pan: Int, tilt: Int): String {
        val endpoint = config.endpoint.trim().trimStart('/')
        return "http://${config.servoIp}:${config.servoPort}/$endpoint?pan=$pan&tilt=$tilt"
    }
}
