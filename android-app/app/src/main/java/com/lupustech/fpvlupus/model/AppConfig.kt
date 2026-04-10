package com.lupustech.fpvlupus.model

data class AppConfig(
    val cameraUrl: String = DEFAULT_CAMERA_URL,
    val servoIp: String = DEFAULT_SERVO_IP,
    val servoPort: Int = DEFAULT_SERVO_PORT,
    val endpoint: String = DEFAULT_ENDPOINT,
    val horizontalSensitivity: Float = DEFAULT_HORIZONTAL_SENSITIVITY,
    val verticalSensitivity: Float = DEFAULT_VERTICAL_SENSITIVITY,
    val invertHorizontal: Boolean = false,
    val invertVertical: Boolean = false,
    val panMin: Int = DEFAULT_PAN_MIN,
    val panMax: Int = DEFAULT_PAN_MAX,
    val tiltMin: Int = DEFAULT_TILT_MIN,
    val tiltMax: Int = DEFAULT_TILT_MAX,
    val panCenter: Int = DEFAULT_PAN_CENTER,
    val tiltCenter: Int = DEFAULT_TILT_CENTER,
    val sendIntervalMs: Long = DEFAULT_SEND_INTERVAL_MS,
    val deadZone: Float = DEFAULT_DEAD_ZONE,
    val smoothing: Float = DEFAULT_SMOOTHING
) {
    companion object {
        const val DEFAULT_CAMERA_URL = "http://192.168.1.1/stream"
        const val DEFAULT_SERVO_IP = "192.168.1.2"
        const val DEFAULT_SERVO_PORT = 80
        const val DEFAULT_ENDPOINT = "control"
        const val DEFAULT_HORIZONTAL_SENSITIVITY = 2.3f
        const val DEFAULT_VERTICAL_SENSITIVITY = 2.0f
        const val DEFAULT_PAN_MIN = 20
        const val DEFAULT_PAN_MAX = 160
        const val DEFAULT_TILT_MIN = 40
        const val DEFAULT_TILT_MAX = 140
        const val DEFAULT_PAN_CENTER = 90
        const val DEFAULT_TILT_CENTER = 90
        const val DEFAULT_SEND_INTERVAL_MS = 80L
        const val DEFAULT_DEAD_ZONE = 2.0f
        const val DEFAULT_SMOOTHING = 0.15f
    }
}
