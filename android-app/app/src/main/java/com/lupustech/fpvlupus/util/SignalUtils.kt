package com.lupustech.fpvlupus.util

import kotlin.math.abs

object SignalUtils {
    fun clamp(value: Float, min: Float, max: Float): Float = value.coerceIn(min, max)

    fun clampInt(value: Int, min: Int, max: Int): Int = value.coerceIn(min, max)

    fun applyDeadZone(value: Float, deadZone: Float): Float {
        return if (abs(value) < deadZone) 0f else value
    }

    fun smooth(previous: Float, target: Float, factor: Float): Float {
        val clampedFactor = factor.coerceIn(0f, 1f)
        return previous + (target - previous) * clampedFactor
    }

    fun normalizeAngleDelta(angle: Float): Float {
        var delta = angle
        while (delta > 180f) delta -= 360f
        while (delta < -180f) delta += 360f
        return delta
    }
}
