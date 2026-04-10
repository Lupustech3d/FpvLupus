package com.appcamera.vr

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import com.appcamera.vr.databinding.ActivityMainBinding
import com.appcamera.vr.databinding.DialogConfigBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var configStorage: ConfigStorage
    private lateinit var gestureDetector: GestureDetector

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoTextureView: TextureView? = null
    private var currentConfig = CameraConfig(
        ConfigStorage.DEFAULT_IP,
        ConfigStorage.DEFAULT_USER,
        ConfigStorage.DEFAULT_PASSWORD
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var reconnectRunnable: Runnable? = null
    private var isConfigButtonVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configStorage = ConfigStorage(this)
        currentConfig = configStorage.load()

        setupFullscreen()
        setupVideoSurface()
        setupGestures()
        setupUi()
        initializePlayer()
    }

    private fun setupFullscreen() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupVideoSurface() {
        videoTextureView = TextureView(this).apply {
            isOpaque = false
            keepScreenOn = true
        }
        binding.vrContainer.setVideoView(videoTextureView!!)
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    isConfigButtonVisible = !isConfigButtonVisible
                    binding.buttonConfig.animate()
                        .alpha(if (isConfigButtonVisible) 1f else 0f)
                        .setDuration(180L)
                        .withStartAction {
                            if (isConfigButtonVisible) {
                                binding.buttonConfig.visibility = View.VISIBLE
                            }
                        }
                        .withEndAction {
                            if (!isConfigButtonVisible) {
                                binding.buttonConfig.visibility = View.GONE
                            }
                        }
                        .start()
                    return true
                }
            }
        )
    }

    private fun setupUi() {
        binding.buttonConfig.setOnClickListener {
            showConfigDialog()
        }
        binding.buttonOpenConfig.setOnClickListener {
            showConfigDialog()
        }
    }

    private fun showConnectionWarning(show: Boolean) {
        binding.layoutConnectionWarning.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateStatus(textResId: Int, color: Int = 0xFFFFFFFF.toInt()) {
        binding.textStatus.text = getString(textResId)
        binding.textStatus.setTextColor(color)
        binding.textStatus.visibility = View.VISIBLE
    }

    private fun hideStatus() {
        binding.textStatus.visibility = View.GONE
    }

    private fun initializePlayer() {
        if (libVLC != null || mediaPlayer != null) return

        val options = arrayListOf(
            "--network-caching=150",
            "--live-caching=150",
            "--file-caching=0",
            "--sout-mux-caching=0",
            "--clock-jitter=0",
            "--clock-synchro=0",
            "--rtsp-tcp",
            "--avcodec-fast",
            "--avcodec-threads=0"
        )

        val libVlcInstance = LibVLC(this, options)
        libVLC = libVlcInstance
        mediaPlayer = MediaPlayer(libVlcInstance).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening -> {
                        runOnUiThread {
                            showConnectionWarning(false)
                            updateStatus(R.string.status_connecting, 0xFFFFFF00.toInt())
                        }
                    }

                    MediaPlayer.Event.Buffering -> {
                        runOnUiThread {
                            showConnectionWarning(false)
                        }
                    }

                    MediaPlayer.Event.Playing -> {
                        cancelReconnect()
                        runOnUiThread {
                            showConnectionWarning(false)
                            updateStatus(R.string.status_connected, 0xFF00FF00.toInt())
                            mainHandler.postDelayed({ hideStatus() }, 2000)
                        }
                    }

                    MediaPlayer.Event.EncounteredError,
                    MediaPlayer.Event.Stopped,
                    MediaPlayer.Event.EndReached -> {
                        runOnUiThread {
                            showConnectionWarning(true)
                            updateStatus(R.string.status_error, 0xFFFF4444.toInt())
                        }
                        scheduleReconnect()
                    }
                }
            }
        }

        attachVideoOutput()
    }

    private fun attachVideoOutput() {
        val player = mediaPlayer ?: return
        val texture = videoTextureView ?: return
        val vout = player.vlcVout
        vout.detachViews()
        vout.setVideoView(texture)
        vout.attachViews()
        player.setScale(2f)
    }

    private fun playCurrentStream() {
        val player = mediaPlayer ?: return
        val libVlcInstance = libVLC ?: return
        cancelReconnect()
        showConnectionWarning(false)

        val media = Media(libVlcInstance, Uri.parse(currentConfig.toRtspUrl())).apply {
            setHWDecoderEnabled(true, false)
            addOption(":network-caching=150")
            addOption(":live-caching=150")
            addOption(":clock-jitter=0")
            addOption(":clock-synchro=0")
            addOption(":rtsp-tcp")
            addOption(":no-video-title-show")
        }

        player.stop()
        player.media = media
        media.release()
        player.play()
    }

    private fun showConfigDialog() {
        val dialogBinding = DialogConfigBinding.inflate(LayoutInflater.from(this))
        dialogBinding.editIp.setText(currentConfig.ip)
        dialogBinding.editUser.setText(currentConfig.user)
        dialogBinding.editPassword.setText(currentConfig.password)

        fun updateUrlPreview() {
            val ip = dialogBinding.editIp.text?.toString()?.trim().orEmpty()
            val user = dialogBinding.editUser.text?.toString()?.trim().orEmpty()
            val pass = dialogBinding.editPassword.text?.toString()?.trim().orEmpty()
            val cred = if (user.isNotBlank()) {
                if (pass.isNotBlank()) "$user:***@" else "$user@"
            } else ""
            dialogBinding.textUrlPreview.text =
                "rtsp://$cred$ip:${ConfigStorage.PORT}/${ConfigStorage.PATH}"
        }

        dialogBinding.editIp.doAfterTextChanged { updateUrlPreview() }
        dialogBinding.editUser.doAfterTextChanged { updateUrlPreview() }
        dialogBinding.editPassword.doAfterTextChanged { updateUrlPreview() }
        updateUrlPreview()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.buttonSave.setOnClickListener {
            val ip = dialogBinding.editIp.text?.toString()?.trim().orEmpty()
            val user = dialogBinding.editUser.text?.toString()?.trim().orEmpty()
            val pass = dialogBinding.editPassword.text?.toString()?.trim().orEmpty()

            if (ip.isBlank()) {
                dialogBinding.editIp.error = getString(R.string.invalid_config)
                return@setOnClickListener
            }

            currentConfig = CameraConfig(ip = ip, user = user, password = pass)
            configStorage.save(currentConfig)
            dialog.dismiss()
            restartPlayback()
        }

        dialog.show()
    }

    private fun restartPlayback() {
        playCurrentStream()
    }

    private fun scheduleReconnect() {
        if (reconnectRunnable != null) return
        reconnectRunnable = Runnable {
            reconnectRunnable = null
            restartPlayback()
        }.also {
            mainHandler.postDelayed(it, RECONNECT_DELAY_MS)
        }
    }

    private fun cancelReconnect() {
        reconnectRunnable?.let(mainHandler::removeCallbacks)
        reconnectRunnable = null
    }

    override fun onStart() {
        super.onStart()
        setupFullscreen()
        if (mediaPlayer == null) {
            initializePlayer()
        }
        playCurrentStream()
    }

    override fun onResume() {
        super.onResume()
        setupFullscreen()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupFullscreen()
        }
    }

    override fun onStop() {
        cancelReconnect()
        mediaPlayer?.stop()
        super.onStop()
    }

    override fun onDestroy() {
        cancelReconnect()
        mediaPlayer?.vlcVout?.detachViews()
        mediaPlayer?.setEventListener(null)
        mediaPlayer?.release()
        mediaPlayer = null
        libVLC?.release()
        libVLC = null
        videoTextureView = null
        super.onDestroy()
    }

    companion object {
        private const val RECONNECT_DELAY_MS = 2500L
    }
}
