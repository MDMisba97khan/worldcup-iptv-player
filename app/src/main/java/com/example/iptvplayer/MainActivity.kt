package com.example.iptvplayer

import android.app.PictureInPictureParams
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
    private lateinit var tvStatus: TextView

    private lateinit var playerController: PlayerController

    // Background executor
    private val ioDispatcher = Dispatchers.IO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()

        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnLoad = findViewById(R.id.btnLoad)
        btnPrimary = findViewById(R.id.btnPrimary)
        btnSecondary = findViewById(R.id.btnSecondary)
        rvChannels = findViewById(R.id.rvChannels)
        playerViewWrapper = findViewById(R.id.playerViewWrapper)
        resizeModeSpinner = findViewById(R.id.resizeModeSpinner)
        btnPip = findViewById(R.id.btnPip)
        tvStatus = findViewById(R.id.tvStatus)

        playerController = PlayerController(this, findViewById(R.id.playerView), lifecycleScope) { playing ->
            Log.d(TAG, "Player isPlaying=$playing")
        }
        playerController.setupGestures()

        rvChannels.layoutManager = LinearLayoutManager(this)
        var channelsAdapter = ChannelsAdapter { pos ->
            if (pos in channelList.indices) {
                playChannel(channelList[pos])
            }
        }
        rvChannels.adapter = channelsAdapter

        setupResizeModes()

        // Notification permission toggle helper
        btnPip.setOnClickListener {
            startForegroundService(Intent(this, com.example.iptvplayer.service.PlaybackForegroundService::class.java))
            playerController.togglePip()
        }

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
        etM3uUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadPlaylistFromUrl()
                true
            } else false
        }
    }

    private fun setupResizeModes() {
        val modes = listOf("FIT", "FILL", "ZOOM", "FIXED_WIDTH", "FIXED_HEIGHT")
        resizeModeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        resizeModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val mode = when (pos) {
                    0 -> 0 // PlayerView.RESIZE_MODE_FIT
                    1 -> 1 // PlayerView.RESIZE_MODE_FILL
                    2 -> 2 // PlayerView.RESIZE_MODE_ZOOM
                    3 -> 3 // PlayerView.RESIZE_MODE_FIXED_WIDTH
                    else -> 4 // PlayerView.RESIZE_MODE_FIXED_HEIGHT
                }
                playerController.setResizeMode(mode)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
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

        tvStatus.text = "Loading playlist..."
        lifecycleScope.launch(ioDispatcher) {
            try {
                val primaryRaw = PlaylistRepository.fetchPrimaryRaw(this@MainActivity)
                val secondaryRaw = PlaylistRepository.fetchSecondaryRaw(this@MainActivity)

                val chosen = primaryRaw ?: secondaryRaw
                if (chosen == null) {
                    runOnMain("Failed to load both playlists")
                    return@launch
                }

                val channels = PlaylistRepository.parseWithNativeEngine(chosen)
                runOnMain {
                    channelList.clear()
                    channelList.addAll(channels)
                    (rvChannels.adapter as ChannelsAdapter).submitList(channelList)
                    tvStatus.text = "Loaded ${channelList.size} channels"
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Playlist load failed", t)
                runOnMain("Error: ${t.message}")
            }
        }
    }

    private fun playChannel(channel: Channel) {
        lifecycleScope.launch(ioDispatcher) {
            try {
                // In full implementation, iterate channel alternatives if URL format supports variants
                val chosenUrl = channel.url
                playerController.playMedia(chosenUrl)
                    playerViewWrapper.visibility = View.VISIBLE
                    tvStatus.text = "Playing: ${channel.name}"
            } catch (t: Throwable) {
                Log.e(TAG, "playChannel failed", t)
                runOnMain("Playback error: ${t.message}")
            }
        }
    }

    private fun runOnMain(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            tvStatus.text = message
        }
    }

    private val channelList = mutableListOf<Channel>()

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
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ChannelVH(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            fun bind(channel: Channel) {
                (itemView as TextView).text = channel.name
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClick(pos)
                    }
                }
            }
        }
    }
}
