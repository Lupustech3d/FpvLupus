package com.lupustech.fpvlupus.data

import android.content.Context
import android.content.SharedPreferences
import com.lupustech.fpvlupus.model.AppConfig

class SettingsRepository(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppConfig {
        return AppConfig(
            cameraUrl = preferences.getString(KEY_CAMERA_URL, AppConfig.DEFAULT_CAMERA_URL) ?: AppConfig.DEFAULT_CAMERA_URL,
            servoIp = preferences.getString(KEY_SERVO_IP, AppConfig.DEFAULT_SERVO_IP) ?: AppConfig.DEFAULT_SERVO_IP,
            servoPort = preferences.getInt(KEY_SERVO_PORT, AppConfig.DEFAULT_SERVO_PORT),
            endpoint = preferences.getString(KEY_ENDPOINT, AppConfig.DEFAULT_ENDPOINT) ?: AppConfig.DEFAULT_ENDPOINT,
            horizontalSensitivity = preferences.getFloat(KEY_H_SENSITIVITY, AppConfig.DEFAULT_HORIZONTAL_SENSITIVITY),
            verticalSensitivity = preferences.getFloat(KEY_V_SENSITIVITY, AppConfig.DEFAULT_VERTICAL_SENSITIVITY),
            invertHorizontal = preferences.getBoolean(KEY_INVERT_H, false),
            invertVertical = preferences.getBoolean(KEY_INVERT_V, false),
            panMin = preferences.getInt(KEY_PAN_MIN, AppConfig.DEFAULT_PAN_MIN),
            panMax = preferences.getInt(KEY_PAN_MAX, AppConfig.DEFAULT_PAN_MAX),
            tiltMin = preferences.getInt(KEY_TILT_MIN, AppConfig.DEFAULT_TILT_MIN),
            tiltMax = preferences.getInt(KEY_TILT_MAX, AppConfig.DEFAULT_TILT_MAX),
            panCenter = preferences.getInt(KEY_PAN_CENTER, AppConfig.DEFAULT_PAN_CENTER),
            tiltCenter = preferences.getInt(KEY_TILT_CENTER, AppConfig.DEFAULT_TILT_CENTER),
            sendIntervalMs = preferences.getLong(KEY_SEND_INTERVAL, AppConfig.DEFAULT_SEND_INTERVAL_MS),
            deadZone = preferences.getFloat(KEY_DEAD_ZONE, AppConfig.DEFAULT_DEAD_ZONE),
            smoothing = preferences.getFloat(KEY_SMOOTHING, AppConfig.DEFAULT_SMOOTHING)
        )
    }

    fun save(config: AppConfig) {
        preferences.edit()
            .putString(KEY_CAMERA_URL, config.cameraUrl)
            .putString(KEY_SERVO_IP, config.servoIp)
            .putInt(KEY_SERVO_PORT, config.servoPort)
            .putString(KEY_ENDPOINT, config.endpoint)
            .putFloat(KEY_H_SENSITIVITY, config.horizontalSensitivity)
            .putFloat(KEY_V_SENSITIVITY, config.verticalSensitivity)
            .putBoolean(KEY_INVERT_H, config.invertHorizontal)
            .putBoolean(KEY_INVERT_V, config.invertVertical)
            .putInt(KEY_PAN_MIN, config.panMin)
            .putInt(KEY_PAN_MAX, config.panMax)
            .putInt(KEY_TILT_MIN, config.tiltMin)
            .putInt(KEY_TILT_MAX, config.tiltMax)
            .putInt(KEY_PAN_CENTER, config.panCenter)
            .putInt(KEY_TILT_CENTER, config.tiltCenter)
            .putLong(KEY_SEND_INTERVAL, config.sendIntervalMs)
            .putFloat(KEY_DEAD_ZONE, config.deadZone)
            .putFloat(KEY_SMOOTHING, config.smoothing)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "fpv_lupus_prefs"
        private const val KEY_CAMERA_URL = "camera_url"
        private const val KEY_SERVO_IP = "servo_ip"
        private const val KEY_SERVO_PORT = "servo_port"
        private const val KEY_ENDPOINT = "endpoint"
        private const val KEY_H_SENSITIVITY = "h_sensitivity"
        private const val KEY_V_SENSITIVITY = "v_sensitivity"
        private const val KEY_INVERT_H = "invert_h"
        private const val KEY_INVERT_V = "invert_v"
        private const val KEY_PAN_MIN = "pan_min"
        private const val KEY_PAN_MAX = "pan_max"
        private const val KEY_TILT_MIN = "tilt_min"
        private const val KEY_TILT_MAX = "tilt_max"
        private const val KEY_PAN_CENTER = "pan_center"
        private const val KEY_TILT_CENTER = "tilt_center"
        private const val KEY_SEND_INTERVAL = "send_interval"
        private const val KEY_DEAD_ZONE = "dead_zone"
        private const val KEY_SMOOTHING = "smoothing"
    }
}
