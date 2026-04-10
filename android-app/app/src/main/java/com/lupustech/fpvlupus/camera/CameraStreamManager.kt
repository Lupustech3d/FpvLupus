package com.lupustech.fpvlupus.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.lupustech.fpvlupus.ui.VrDualFrameView
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraStreamManager(
    private val vrDualFrameView: VrDualFrameView
) {
    interface Listener {
        fun onStreamStatusChanged(connected: Boolean)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    private val renderPosted = AtomicBoolean(false)
    private val minRenderIntervalMs = 33L

    @Volatile
    private var currentUrl: String? = null

    @Volatile
    private var released = false

    private val decodeOptions = BitmapFactory.Options().apply {
        // Faster decode and lower memory bandwidth for MJPEG while keeping acceptable quality.
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    @Volatile
    private var lastRenderAt = 0L

    @Volatile
    private var latestBitmap: Bitmap? = null

    private val renderRunnable = Runnable {
        renderPosted.set(false)
        if (released) return@Runnable
        latestBitmap?.let { vrDualFrameView.setFrame(it) }
    }

    var listener: Listener? = null

    fun configure() {
        vrDualFrameView.keepScreenOn = true
    }

    fun load(url: String) {
        if (released) return
        currentUrl = url
        if (running.compareAndSet(false, true)) {
            executor.execute { streamLoop() }
        }
    }

    fun reload(url: String) {
        currentUrl = url
        if (running.compareAndSet(false, true)) {
            executor.execute { streamLoop() }
        }
    }

    fun release() {
        released = true
        running.set(false)
        mainHandler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        latestBitmap = null
        mainHandler.post {
            vrDualFrameView.clearFrame()
        }
    }

    private fun streamLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY)
        while (!released && running.get()) {
            val url = currentUrl
            if (url.isNullOrBlank()) {
                running.set(false)
                return
            }

            var connection: HttpURLConnection? = null
            var input: BufferedInputStream? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 8000
                    useCaches = false
                    instanceFollowRedirects = true
                    setRequestProperty("Connection", "Keep-Alive")
                }
                connection.connect()
                val connected = connection.responseCode in 200..299
                publishStatus(connected)
                if (!connected) {
                    sleepBeforeRetry()
                    continue
                }

                input = BufferedInputStream(connection.inputStream, 16 * 1024)
                while (!released && currentUrl == url) {
                    val frameBytes = readNextJpegFrame(input) ?: break
                    skipBufferedJunk(input)
                    val now = System.currentTimeMillis()
                    if (now - lastRenderAt < minRenderIntervalMs) {
                        continue
                    }

                    val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size, decodeOptions)
                    if (bitmap != null) {
                        lastRenderAt = now
                        publishFrame(bitmap)
                        publishStatus(true)
                    }
                }
            } catch (_: IOException) {
                publishStatus(false)
                sleepBeforeRetry()
            } finally {
                try {
                    input?.close()
                } catch (_: IOException) {
                }
                connection?.disconnect()
            }
        }
        running.set(false)
    }

    private fun publishFrame(bitmap: Bitmap) {
        latestBitmap = bitmap
        if (renderPosted.compareAndSet(false, true)) {
            mainHandler.postAtFrontOfQueue(renderRunnable)
        }
    }

    private fun publishStatus(connected: Boolean) {
        mainHandler.post {
            if (!released) {
                listener?.onStreamStatusChanged(connected)
            }
        }
    }

    private fun sleepBeforeRetry() {
        try {
            Thread.sleep(1500L)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun readNextJpegFrame(input: BufferedInputStream): ByteArray? {
        var previous = -1
        while (true) {
            val current = input.read()
            if (current == -1) return null
            if (previous == 0xFF && current == 0xD8) {
                break
            }
            previous = current
        }

        val output = ByteArrayOutputStream(32 * 1024)
        output.write(0xFF)
        output.write(0xD8)
        previous = 0xD8

        while (true) {
            val current = input.read()
            if (current == -1) return null
            output.write(current)
            if (previous == 0xFF && current == 0xD9) {
                return output.toByteArray()
            }
            previous = current
        }
    }

    private fun skipBufferedJunk(input: BufferedInputStream) {
        try {
            val available = input.available()
            if (available > 64 * 1024) {
                input.skip((available - 16 * 1024).toLong())
            }
        } catch (_: IOException) {
        }
    }
}
