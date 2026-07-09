package com.example.iptvplayer.player

import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.iptvplayer.R
import com.example.iptvplayer.R
import androidx.media3.ui.PlayerView
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
    private var pbLoading: ProgressBar? = null
    private val activityContext: Context = context.applicationContext

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        val httpDataSourceFactory: DefaultHttpDataSource.Factory = try {
            DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        } catch (e: Exception) {
            Log.w(TAG, "Custom network init failed, falling back to default HTTP factory", e)
            DefaultHttpDataSource.Factory()
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(activityContext)

        Log.d(TAG, "Initializing ExoPlayer with media source factory=$mediaSourceFactory context=$activityContext")

        player = ExoPlayer.Builder(activityContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exo ->
                playerView.player = exo
                playerView.useController = true

                pbLoading = (playerView.parent as? android.view.ViewGroup)?.findViewById<ProgressBar>(R.id.pbLoading)
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d("IPTV_PLAYER", "State changed: $playbackState")
                        val isPlaying = exo.isPlaying
                        onPlayerStateChanged(isPlaying)
                        pbLoading?.visibility = if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                            release()
                            onPlayerStateChanged(false)
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("IPTV_PLAYER", "Playback Error: ${error.localizedMessage}", error)
                        android.widget.Toast.makeText(context, "Playback Error: ${error.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                        scheduleFallback()
                    }
                })
            }
    }

    fun playMedia(uri: String) {
        fallbackJob?.cancel()
        try {
            player?.let { exo ->
                val mediaItem = MediaItem.fromUri(uri)
                Log.d("IPTV_PLAYER", "playMedia uri=$uri")
                exo.setMediaItem(mediaItem)
                exo.prepare()
                exo.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "playMedia failed for: $uri", e)
        }
    }

    fun setResizeMode(resizeMode: Int) {
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
        playerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (player?.isPlaying == true) player?.pause() else player?.play()
                return@setOnTouchListener true
            }
            true
        }
    }

    init {
        initPlayer()
    }
}
