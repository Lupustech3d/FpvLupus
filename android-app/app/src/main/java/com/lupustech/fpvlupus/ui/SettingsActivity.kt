package com.lupustech.fpvlupus.ui

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lupustech.fpvlupus.R
import com.lupustech.fpvlupus.data.SettingsRepository
import com.lupustech.fpvlupus.databinding.ActivitySettingsBinding
import com.lupustech.fpvlupus.model.AppConfig
import com.lupustech.fpvlupus.net.ServoController
import com.lupustech.fpvlupus.util.SignalUtils
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: SettingsRepository
    private val servoController = ServoController()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = SettingsRepository(this)
        bindConfig(repository.load())
        setupActions()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        servoController.shutdown()
        super.onDestroy()
    }

    private fun setupActions() {
        binding.saveButton.setOnClickListener {
            val config = collectConfig() ?: return@setOnClickListener
            repository.save(config)
            setResult(Activity.RESULT_OK)
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.restoreDefaultsButton.setOnClickListener {
            bindConfig(AppConfig())
        }

        binding.testCameraButton.setOnClickListener {
            val config = collectConfig() ?: return@setOnClickListener
            Toast.makeText(this, R.string.camera_testing, Toast.LENGTH_SHORT).show()
            executor.execute {
                val success = testCameraConnection(config.cameraUrl)
                mainHandler.post {
                    Toast.makeText(
                        this,
                        if (success) R.string.camera_test_ok else R.string.camera_test_fail,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.testServoButton.setOnClickListener {
            val config = collectConfig() ?: return@setOnClickListener
            Toast.makeText(this, R.string.servo_testing, Toast.LENGTH_SHORT).show()
            servoController.updateConfig(config)
            servoController.testConnection { success ->
                Toast.makeText(
                    this,
                    if (success) R.string.servo_test_ok else R.string.servo_test_fail,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindConfig(config: AppConfig) = with(binding) {
        cameraUrlInput.setText(config.cameraUrl)
        servoIpInput.setText(config.servoIp)
        servoPortInput.setText(config.servoPort.toString())
        endpointInput.setText(config.endpoint)
        hSensitivityInput.setText(config.horizontalSensitivity.toString())
        vSensitivityInput.setText(config.verticalSensitivity.toString())
        invertHorizontalSwitch.isChecked = config.invertHorizontal
        invertVerticalSwitch.isChecked = config.invertVertical
        panMinInput.setText(config.panMin.toString())
        panMaxInput.setText(config.panMax.toString())
        tiltMinInput.setText(config.tiltMin.toString())
        tiltMaxInput.setText(config.tiltMax.toString())
        panCenterInput.setText(config.panCenter.toString())
        tiltCenterInput.setText(config.tiltCenter.toString())
        sendIntervalInput.setText(config.sendIntervalMs.toString())
        deadZoneInput.setText(config.deadZone.toString())
        smoothingInput.setText(config.smoothing.toString())
    }

    private fun collectConfig(): AppConfig? {
        val cameraUrl = binding.cameraUrlInput.text?.toString()?.trim().orEmpty()
        if (Uri.parse(cameraUrl).scheme?.startsWith("http") != true) {
            Toast.makeText(this, R.string.invalid_camera_url, Toast.LENGTH_SHORT).show()
            return null
        }

        val port = binding.servoPortInput.text?.toString()?.toIntOrNull()
        if (port == null || port !in 1..65535) {
            Toast.makeText(this, R.string.invalid_port, Toast.LENGTH_SHORT).show()
            return null
        }

        val panMin = binding.panMinInput.text.safeInt(AppConfig.DEFAULT_PAN_MIN)
        val panMax = binding.panMaxInput.text.safeInt(AppConfig.DEFAULT_PAN_MAX)
        val tiltMin = binding.tiltMinInput.text.safeInt(AppConfig.DEFAULT_TILT_MIN)
        val tiltMax = binding.tiltMaxInput.text.safeInt(AppConfig.DEFAULT_TILT_MAX)
        val normalizedPanMin = minOf(panMin, panMax)
        val normalizedPanMax = maxOf(panMin, panMax)
        val normalizedTiltMin = minOf(tiltMin, tiltMax)
        val normalizedTiltMax = maxOf(tiltMin, tiltMax)

        val panCenter = SignalUtils.clampInt(
            binding.panCenterInput.text.safeInt(AppConfig.DEFAULT_PAN_CENTER),
            normalizedPanMin,
            normalizedPanMax
        )
        val tiltCenter = SignalUtils.clampInt(
            binding.tiltCenterInput.text.safeInt(AppConfig.DEFAULT_TILT_CENTER),
            normalizedTiltMin,
            normalizedTiltMax
        )

        return AppConfig(
            cameraUrl = cameraUrl,
            servoIp = binding.servoIpInput.text?.toString()?.trim().orEmpty(),
            servoPort = port,
            endpoint = binding.endpointInput.text?.toString()?.trim().orEmpty().ifEmpty { AppConfig.DEFAULT_ENDPOINT },
            horizontalSensitivity = binding.hSensitivityInput.text.safeFloat(AppConfig.DEFAULT_HORIZONTAL_SENSITIVITY),
            verticalSensitivity = binding.vSensitivityInput.text.safeFloat(AppConfig.DEFAULT_VERTICAL_SENSITIVITY),
            invertHorizontal = binding.invertHorizontalSwitch.isChecked,
            invertVertical = binding.invertVerticalSwitch.isChecked,
            panMin = normalizedPanMin,
            panMax = normalizedPanMax,
            tiltMin = normalizedTiltMin,
            tiltMax = normalizedTiltMax,
            panCenter = panCenter,
            tiltCenter = tiltCenter,
            sendIntervalMs = binding.sendIntervalInput.text.safeLong(AppConfig.DEFAULT_SEND_INTERVAL_MS).coerceAtLeast(20L),
            deadZone = binding.deadZoneInput.text.safeFloat(AppConfig.DEFAULT_DEAD_ZONE).coerceAtLeast(0f),
            smoothing = binding.smoothingInput.text.safeFloat(AppConfig.DEFAULT_SMOOTHING).coerceIn(0.01f, 1f)
        )
    }

    private fun CharSequence?.safeInt(defaultValue: Int): Int = this?.toString()?.toIntOrNull() ?: defaultValue

    private fun CharSequence?.safeLong(defaultValue: Long): Long = this?.toString()?.toLongOrNull() ?: defaultValue

    private fun CharSequence?.safeFloat(defaultValue: Float): Float = this?.toString()?.toFloatOrNull() ?: defaultValue

    private fun testCameraConnection(url: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.instanceFollowRedirects = true
            connection.responseCode in 200..299
        } catch (_: IOException) {
            false
        } finally {
            connection?.disconnect()
        }
    }
}
