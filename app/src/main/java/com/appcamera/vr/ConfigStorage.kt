package com.appcamera.vr

import android.content.Context

class ConfigStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): CameraConfig {
        return CameraConfig(
            ip = preferences.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP,
            user = preferences.getString(KEY_USER, DEFAULT_USER) ?: DEFAULT_USER,
            password = preferences.getString(KEY_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        )
    }

    fun save(config: CameraConfig) {
        preferences.edit()
            .putString(KEY_IP, config.ip)
            .putString(KEY_USER, config.user)
            .putString(KEY_PASSWORD, config.password)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "camera_config"
        private const val KEY_IP = "ip"
        private const val KEY_USER = "user"
        private const val KEY_PASSWORD = "password"

        const val DEFAULT_IP = "192.168.1.1"
        const val DEFAULT_USER = ""
        const val DEFAULT_PASSWORD = ""
        const val PORT = 554
        const val PATH = "cam/realmonitor?channel=1&subtype=1"
    }
}

data class CameraConfig(
    val ip: String,
    val user: String,
    val password: String
) {
    fun toRtspUrl(): String {
        val credentials = if (user.isNotBlank()) {
            if (password.isNotBlank()) "$user:$password@" else "$user@"
        } else ""
        return "rtsp://$credentials$ip:${ConfigStorage.PORT}/${ConfigStorage.PATH}"
    }
}
