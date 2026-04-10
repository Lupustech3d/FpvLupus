package com.lupustech.fpvlupus.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.lupustech.fpvlupus.model.AppConfig
import com.lupustech.fpvlupus.util.SignalUtils
import kotlin.math.roundToInt

class HeadTrackingManager(context: Context) : SensorEventListener {
    interface Listener {
        fun onAnglesComputed(pan: Int, tilt: Int)
        fun onTrackingAvailabilityChanged(available: Boolean)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    private var baselineYaw = 0f
    private var baselinePitch = 0f
    private var filteredYawDelta = 0f
    private var filteredPitchDelta = 0f
    private var smoothedPan = 0f
    private var smoothedTilt = 0f
    private var lastDeliveredPan: Int? = null
    private var lastDeliveredTilt: Int? = null
    private var active = false
    private var hasBaseline = false
    private var config = AppConfig()

    var listener: Listener? = null

    fun updateConfig(newConfig: AppConfig) {
        config = newConfig
    }

    fun start(): Boolean {
        val sensor = rotationSensor ?: run {
            listener?.onTrackingAvailabilityChanged(false)
            return false
        }
        active = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        listener?.onTrackingAvailabilityChanged(active)
        return active
    }

    fun stop() {
        active = false
        hasBaseline = false
        filteredYawDelta = 0f
        filteredPitchDelta = 0f
        lastDeliveredPan = null
        lastDeliveredTilt = null
        sensorManager.unregisterListener(this)
        listener?.onTrackingAvailabilityChanged(rotationSensor != null)
    }

    fun recalibrate() {
        hasBaseline = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!active || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) {
            return
        }

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            remappedMatrix
        )
        SensorManager.getOrientation(remappedMatrix, orientationValues)

        val yawDegrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        val pitchDegrees = Math.toDegrees(orientationValues[1].toDouble()).toFloat()

        if (!hasBaseline) {
            baselineYaw = yawDegrees
            baselinePitch = pitchDegrees
            filteredYawDelta = 0f
            filteredPitchDelta = 0f
            smoothedPan = config.panCenter.toFloat()
            smoothedTilt = config.tiltCenter.toFloat()
            lastDeliveredPan = config.panCenter
            lastDeliveredTilt = config.tiltCenter
            hasBaseline = true
            return
        }

        var deltaYaw = SignalUtils.normalizeAngleDelta(yawDegrees - baselineYaw)
        var deltaPitch = SignalUtils.normalizeAngleDelta(pitchDegrees - baselinePitch)

        deltaYaw *= config.horizontalSensitivity
        deltaPitch *= config.verticalSensitivity

        if (config.invertHorizontal) deltaYaw *= -1f
        if (config.invertVertical) deltaPitch *= -1f

        deltaYaw = SignalUtils.applyDeadZone(deltaYaw, config.deadZone)
        deltaPitch = SignalUtils.applyDeadZone(deltaPitch, config.deadZone)

        val deltaSmoothing = (config.smoothing * 0.75f).coerceIn(0.05f, 0.25f)
        val outputSmoothing = (config.smoothing * 1.35f).coerceIn(0.08f, 0.45f)

        filteredYawDelta = SignalUtils.smooth(filteredYawDelta, deltaYaw, deltaSmoothing)
        filteredPitchDelta = SignalUtils.smooth(filteredPitchDelta, deltaPitch, deltaSmoothing)

        filteredYawDelta = SignalUtils.applyDeadZone(filteredYawDelta, config.deadZone * 0.65f)
        filteredPitchDelta = SignalUtils.applyDeadZone(filteredPitchDelta, config.deadZone * 0.65f)

        val targetPan = SignalUtils.clamp(
            config.panCenter + filteredYawDelta,
            config.panMin.toFloat(),
            config.panMax.toFloat()
        )
        val targetTilt = SignalUtils.clamp(
            config.tiltCenter - filteredPitchDelta,
            config.tiltMin.toFloat(),
            config.tiltMax.toFloat()
        )

        smoothedPan = SignalUtils.smooth(smoothedPan, targetPan, outputSmoothing)
        smoothedTilt = SignalUtils.smooth(smoothedTilt, targetTilt, outputSmoothing)

        val pan = smoothedPan.roundToInt()
        val tilt = smoothedTilt.roundToInt()
        if (pan != lastDeliveredPan || tilt != lastDeliveredTilt) {
            lastDeliveredPan = pan
            lastDeliveredTilt = tilt
            listener?.onAnglesComputed(pan, tilt)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
