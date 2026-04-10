package com.lupustech.fpvlupus.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lupustech.fpvlupus.R
import com.lupustech.fpvlupus.camera.CameraStreamManager
import com.lupustech.fpvlupus.data.SettingsRepository
import com.lupustech.fpvlupus.databinding.ActivityMainBinding
import com.lupustech.fpvlupus.model.AppConfig
import com.lupustech.fpvlupus.net.ServoController
import com.lupustech.fpvlupus.sensor.HeadTrackingManager

class MainActivity : AppCompatActivity(), CameraStreamManager.Listener,
    ServoController.Listener, HeadTrackingManager.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var cameraStreamManager: CameraStreamManager
    private lateinit var headTrackingManager: HeadTrackingManager
    private val servoController = ServoController()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var config = AppConfig()
    private var trackingEnabled = false
    private var cameraConnected = false
    private var servoConnected = false
    private var trackingAvailable = false

    private val hideOverlayRunnable = Runnable { setOverlayVisible(false) }
    private val hideHintRunnable = Runnable { binding.hintText.visibility = View.GONE }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                reloadConfig()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        settingsRepository = SettingsRepository(this)
        config = settingsRepository.load()

        cameraStreamManager = CameraStreamManager(binding.vrStreamView).also {
            it.listener = this
            it.configure()
        }
        headTrackingManager = HeadTrackingManager(this).also {
            it.listener = this
            it.updateConfig(config)
        }
        servoController.listener = this
        servoController.updateConfig(config)

        setupGestureHandling()
        setupControls()
        applyConfig()
        showOverlayTemporarily()
    }

    override fun onResume() {
        super.onResume()
        WindowInsetsControllerCompat(window, binding.root).hide(WindowInsetsCompat.Type.systemBars())
        cameraStreamManager.reload(config.cameraUrl)
        if (trackingEnabled) {
            headTrackingManager.start()
            headTrackingManager.recalibrate()
        }
    }

    override fun onPause() {
        if (trackingEnabled) {
            headTrackingManager.stop()
        }
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        headTrackingManager.stop()
        servoController.shutdown()
        cameraStreamManager.release()
        super.onDestroy()
    }

    private fun setupGestureHandling() {
        binding.vrStreamView.setOnClickListener {
            val visible = binding.topBar.visibility == View.VISIBLE
            if (visible) {
                setOverlayVisible(false)
            } else {
                showOverlayTemporarily()
            }
        }
    }

    private fun setupControls() = with(binding) {
        settingsButton.setOnClickListener {
            showOverlayTemporarily()
            settingsLauncher.launch(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        recenterButton.setOnClickListener {
            headTrackingManager.recalibrate()
            Toast.makeText(this@MainActivity, R.string.recenter, Toast.LENGTH_SHORT).show()
            showOverlayTemporarily()
        }

        centerServoButton.setOnClickListener {
            servoController.center()
            showOverlayTemporarily()
        }

        trackingToggle.setOnCheckedChangeListener { _, isChecked ->
            trackingEnabled = isChecked
            if (isChecked) {
                val started = headTrackingManager.start()
                if (!started) {
                    trackingToggle.isChecked = false
                    trackingEnabled = false
                    Toast.makeText(this@MainActivity, R.string.sensor_unavailable, Toast.LENGTH_SHORT).show()
                }
            } else {
                headTrackingManager.stop()
            }
            updateStatusViews()
            showOverlayTemporarily()
        }
    }

    private fun reloadConfig() {
        config = settingsRepository.load()
        applyConfig()
        if (trackingEnabled) {
            headTrackingManager.recalibrate()
        }
        servoController.center()
    }

    private fun applyConfig() {
        headTrackingManager.updateConfig(config)
        servoController.updateConfig(config)
        cameraStreamManager.reload(config.cameraUrl)
        updateStatusViews()
    }

    private fun showOverlayTemporarily() {
        setOverlayVisible(true)
        binding.hintText.visibility = View.GONE
        mainHandler.removeCallbacks(hideOverlayRunnable)
        mainHandler.postDelayed(hideOverlayRunnable, 3500L)
    }

    private fun scheduleOverlayHide() {
        mainHandler.removeCallbacks(hideOverlayRunnable)
        mainHandler.postDelayed(hideOverlayRunnable, 3500L)
    }

    private fun setOverlayVisible(visible: Boolean) {
        val overlayVisibility = if (visible) View.VISIBLE else View.GONE
        binding.topBar.visibility = overlayVisibility
        binding.controlScroller.visibility = overlayVisibility
        if (!visible) {
            binding.hintText.visibility = View.VISIBLE
            mainHandler.removeCallbacks(hideHintRunnable)
            mainHandler.postDelayed(hideHintRunnable, 2200L)
        }
    }

    override fun onStreamStatusChanged(connected: Boolean) {
        cameraConnected = connected
        updateStatusViews()
    }

    override fun onServoRequestResult(success: Boolean) {
        servoConnected = success
        updateStatusViews()
    }

    override fun onAnglesComputed(pan: Int, tilt: Int) {
        if (trackingEnabled) {
            servoController.sendAngles(pan, tilt)
        }
    }

    override fun onTrackingAvailabilityChanged(available: Boolean) {
        trackingAvailable = available
        updateStatusViews()
    }

    private fun updateStatusViews() = with(binding) {
        cameraStatusText.text = formatStatus(getString(R.string.camera_status), cameraConnected)
        cameraStatusText.setTextColor(if (cameraConnected) getColor(R.color.status_ok) else getColor(R.color.status_error))

        servoStatusText.text = formatStatus(getString(R.string.servo_status), servoConnected)
        servoStatusText.setTextColor(if (servoConnected) getColor(R.color.status_ok) else getColor(R.color.status_error))

        val trackingActive = trackingEnabled && trackingAvailable
        trackingStatusText.text = "${getString(R.string.tracking_status)}: ${if (trackingActive) getString(R.string.active) else getString(R.string.inactive)}"
        trackingStatusText.setTextColor(if (trackingActive) getColor(R.color.status_ok) else getColor(R.color.status_idle))
    }

    private fun formatStatus(label: String, connected: Boolean): String {
        val state = if (connected) getString(R.string.connected) else getString(R.string.disconnected)
        return "$label: $state"
    }
}
