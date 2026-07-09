package com.example.iptvplayer

import android.app.PictureInPictureParams
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.iptvplayer.data.PlaylistPreferences
import com.example.iptvplayer.data.PlaylistRepository
import com.example.iptvplayer.model.Channel
import com.example.iptvplayer.player.PlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }

    // UI
    private lateinit var etM3uUrl: EditText
    private lateinit var btnLoad: Button
    private lateinit var btnPrimary: Button
    private lateinit var btnSecondary: Button
    private lateinit var rvChannels: RecyclerView
    private lateinit var playerViewWrapper: View
    private lateinit var resizeModeSpinner: Spinner
    private lateinit var btnPip: Button
    private lateinit var btnFullscreen: Button
    private lateinit var tvStatus: TextView

    private lateinit var playerController: PlayerController

    private val channelList = mutableListOf<Channel>()
    private var channelsAdapter: ChannelsAdapter? = null

    // Background executor
    private val ioDispatcher = Dispatchers.IO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.iptvplayer.R.layout.activity_main)

        requestNotificationPermission()

        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnLoad = findViewById(R.id.btnLoad)
        val btnLoadAll = findViewById<Button>(R.id.btnLoadAll)
        btnPrimary = findViewById(R.id.btnPrimary)
        btnSecondary = findViewById(R.id.btnSecondary)
        rvChannels = findViewById(R.id.rvChannels)
        playerViewWrapper = findViewById(R.id.playerViewWrapper)
        resizeModeSpinner = findViewById(R.id.resizeModeSpinner)
        btnPip = findViewById(R.id.btnPip)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        tvStatus = findViewById(R.id.tvStatus)

        playerController = PlayerController(
            this,
            findViewById(R.id.playerView),
            lifecycleScope,
            onPlayerStateChanged = { playing ->
                Log.d(TAG, "Player isPlaying=$playing")
            },
            onPlaybackError = { errorMsg ->
                Log.w(TAG, "Playback failed: $errorMsg")
                advanceToNextChannel()
            }
        )
        playerController.setupGestures()

        rvChannels.layoutManager = LinearLayoutManager(this)
        channelsAdapter = ChannelsAdapter { pos ->
            if (pos in channelList.indices) {
                playChannel(channelList[pos])
            }
        }
        rvChannels.adapter = channelsAdapter

        setupResizeModes()

        btnPip.setOnClickListener {
            startForegroundService(Intent(this, com.example.iptvplayer.service.PlaybackForegroundService::class.java))
            playerController.togglePip()
        }

        enableAutoRotationAndFullscreen()

        // Prefill with detected cached URLs if available
        lifecycleScope.launch {
            val primary = PlaylistPreferences.primaryUrl(this@MainActivity).first()
            val secondary = PlaylistPreferences.secondaryUrl(this@MainActivity).first()
            etM3uUrl.setText(primary)
            if (primary.isNotBlank()) btnPrimary.text = "Primary ✓"
            if (secondary.isNotBlank()) btnSecondary.text = "Secondary ✓"
        }

        var primaryUrl = ""
        var secondaryUrl = ""
        lifecycleScope.launch {
            primaryUrl = PlaylistPreferences.primaryUrl(this@MainActivity).first()
            secondaryUrl = PlaylistPreferences.secondaryUrl(this@MainActivity).first()
            if (primaryUrl.isNotBlank()) btnPrimary.text = "Primary ✓"
            if (secondaryUrl.isNotBlank()) btnSecondary.text = "Secondary ✓"
        }
        btnPrimary.setOnClickListener {
            lifecycleScope.launch { etM3uUrl.setText(PlaylistPreferences.primaryUrl(this@MainActivity).first()) }
        }
        btnSecondary.setOnClickListener {
            lifecycleScope.launch { etM3uUrl.setText(PlaylistPreferences.secondaryUrl(this@MainActivity).first()) }
        }
        btnLoad.setOnClickListener { loadPlaylistFromUrl() }
        btnLoadAll.setOnClickListener { loadAllSources() }
        etM3uUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadPlaylistFromUrl()
                true
            } else false
        }
    }

    private var isFullscreen = false

    private fun enableAutoRotationAndFullscreen() {
        val listener = object : OrientationEventListener(this) {
            private var lastLandscape = false
            override fun onOrientationChanged(orientation: Int) {
                val isLandscape = orientation in 60..120 || orientation in 240..300
                if (isLandscape == lastLandscape) return
                lastLandscape = isLandscape
                runOnUiThread {
                    playerViewWrapper.requestLayout()
                    playerViewWrapper.visibility = View.VISIBLE
                    window.decorView.systemUiVisibility = if (isLandscape) {
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    } else {
                        View.SYSTEM_UI_FLAG_VISIBLE
                    }
                    isFullscreen = isLandscape
                    if (isLandscape) {
                        playerViewWrapper.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    } else {
                        playerViewWrapper.layoutParams.height = (180 * resources.displayMetrics.density).toInt()
                    }
                    playerViewWrapper.layoutParams = playerViewWrapper.layoutParams
                }
            }
        }
        listener.enable()
    }

    private fun setupResizeModes() {
        val modes = listOf("FIT", "FILL", "ZOOM", "FIXED_WIDTH", "FIXED_HEIGHT")
        resizeModeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        resizeModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val mode = when (pos) {
                    0 -> 0
                    1 -> 1
                    2 -> 2
                    3 -> 3
                    else -> 4
                }
                playerController.setResizeMode(mode)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnFullscreen.setOnClickListener {
            isFullscreen = !isFullscreen
            if (isFullscreen) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                val layoutParams = playerViewWrapper.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                playerViewWrapper.layoutParams = layoutParams
                btnFullscreen.text = "⛷"
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                val layoutParams = playerViewWrapper.layoutParams
                layoutParams.height = (180 * resources.displayMetrics.density).toInt()
                playerViewWrapper.layoutParams = layoutParams
                btnFullscreen.text = "⛶"
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), NOTIFICATION_PERMISSION_REQUEST)
            }
        }
    }

    private fun loadPlaylistFromUrl() {
        val url = etM3uUrl.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter a playlist URL", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                tvStatus.text = "Loading playlist..."

                val raw = withContext(Dispatchers.IO) { PlaylistRepository.fetchRaw(url) }

                if (raw == null) {
                    tvStatus.text = "Failed to load playlist"
                    return@launch
                }

                val channels = PlaylistRepository.parseWithNativeEngine(raw)
                channelList.clear()
                channelList.addAll(channels)
                channelsAdapter?.submitList(channelList.toList())
                tvStatus.text = "Loaded ${channelList.size} channels"
                PlaylistRepository.cacheUrl(this@MainActivity, url)
            } catch (t: Throwable) {
                Log.e(TAG, "Playlist load failed", t)
                tvStatus.text = "Error: ${t.message}"
            }
        }
    }

    private fun loadAllSources() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                tvStatus.text = "Loading all playlists..."
                val allRaw = withContext(Dispatchers.IO) { PlaylistRepository.fetchAll(this@MainActivity) }
                if (allRaw.isEmpty()) {
                    tvStatus.text = "No playlists loaded"
                    return@launch
                }
                val channels = allRaw.flatMap { raw ->
                    PlaylistRepository.parseWithNativeEngine(raw)
                }
                channelList.clear()
                channelList.addAll(channels)
                channelsAdapter?.submitList(channelList.toList())
                tvStatus.text = "Loaded ${channelList.size} channels from ${allRaw.size} sources"
            } catch (t: Throwable) {
                Log.e(TAG, "Load all sources failed", t)
                tvStatus.text = "Error: ${t.message}"
            }
        }
    }

    private fun advanceToNextChannel() {
        val pos = (playerController as? android.view.View)?.let { 0 } ?: 0
        val next = (pos + 1).coerceAtMost((channelList.size - 1).coerceAtLeast(0))
        if (next in channelList.indices && next != pos) {
            playChannel(channelList[next])
        }
    }

    private fun playChannel(channel: Channel) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val chosenUrl = channel.url
                playerController.playMedia(chosenUrl)
                playerViewWrapper.visibility = View.VISIBLE
                tvStatus.text = "Playing: ${channel.name}"
            } catch (t: Throwable) {
                Log.e(TAG, "playChannel failed", t)
                tvStatus.text = "Playback error: ${t.message}"
            }
        }
    }

    override fun onDestroy() {
        playerController.release()
        super.onDestroy()
    }

    private inner class ChannelsAdapter(
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ChannelsAdapter.ChannelVH>() {

        private var items = listOf<Channel>()

        fun submitList(newList: List<Channel>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ChannelVH(view)
        }

        override fun onBindViewHolder(holder: ChannelVH, position: Int) {
            holder.bind(items[position], position)
        }

        override fun getItemCount(): Int = items.size

        inner class ChannelVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(channel: Channel, position: Int) {
                (itemView as TextView).text = channel.name
                itemView.setOnClickListener { onItemClick(position) }
            }
        }
    }
}
