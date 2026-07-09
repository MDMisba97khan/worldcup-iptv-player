package com.example.iptvplayer.player

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.core.view.GestureDetectorCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.iptvplayer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerController(
    private val context: Context,
    private val playerView: PlayerView,
    private val lifecycleScope: CoroutineScope,
    private val onPlayerStateChanged: (Boolean) -> Unit = {}
) {

    companion object {
        private const val TAG = "PlayerController"
    }

    private var player: ExoPlayer? = null
    private var fallbackJob: Job? = null
    private val activityContext = context.applicationContext

    init {
        initPlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        player = ExoPlayer.Builder(activityContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exo ->
                playerView.player = exo
                playerView.useController = true
                playerView.setShowSubtitleButton(false)
                playerView.setShowFastForwardButton(true)
                playerView.setShowRewindButton(true)

                var lastResizeMode = playerView.resizeMode
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val isPlaying = exo.isPlaying
                        onPlayerStateChanged(isPlaying)
                        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                            // Auto-release to prevent leaks
                            release()
                            onPlayerStateChanged(false)
                        }
                        if (playbackState == Player.STATE_READY && lastResizeMode != playerView.resizeMode) {
                            lastResizeMode = playerView.resizeMode
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}")
                        scheduleFallback()
                    }
                })
            }
    }

    fun playMedia(uri: String) {
        fallbackJob?.cancel()
        try {
            player?.let { exo ->
                exo.setMediaItem(MediaItem.fromUri(uri))
                exo.prepare()
                exo.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "playMedia failed for: $uri", e)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun setResizeMode(@PlayerView.ResizeMode resizeMode: Int) {
        playerView.resizeMode = resizeMode
    }

    fun togglePip() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                (context as? android.app.Activity)?.enterPictureInPictureMode(params)
                    ?: Log.w(TAG, "Context is not an Activity; cannot enter PiP")
            }
        } catch (e: Exception) {
            Log.e(TAG, "PiP failed", e)
        }
    }

    fun isPlaying(): Boolean = player?.isPlaying ?: false
    fun release() {
        fallbackJob?.cancel()
        try {
            player?.release()
        } catch (e: Exception) { /* ignore */ }
        player = null
    }

    private fun scheduleFallback() {
        fallbackJob?.cancel()
        fallbackJob = lifecycleScope.launch(Dispatchers.Main) {
            delay(800)
            onPlayerStateChanged(false)
        }
    }

    fun setupGestures() {
        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (e.x > playerView.width / 2) {
                    player?.let { if (it.isPlaying) it.pause() else it.play() }
                }
                return super.onDoubleTap(e)
            }
        }
        val gestureDetector = GestureDetectorCompat(context, gestureListener)

        val brightnessDetector = object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent?,
                distanceX: Float, distanceY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false
                val window = (context as? android.app.Activity)?.window ?: return false
                val layoutParams = window.attributes
                val newBrightness = (layoutParams.screenBrightness - distanceY / 1000f).coerceIn(0f, 1f)
                layoutParams.screenBrightness = newBrightness
                window.attributes = layoutParams
                return true
            }
        }

        playerView.setOnTouchListener { v, event ->
            val halfW = v.width / 2.0f
            val x = event.x
            if (x < halfW) {
                val det = GestureDetector(context, brightnessDetector)
                det.onTouchEvent(event)
            } else {
                val det = GestureDetector(context, gestureListener)
                det.onTouchEvent(event)
            }
            true
        }
    }
}
