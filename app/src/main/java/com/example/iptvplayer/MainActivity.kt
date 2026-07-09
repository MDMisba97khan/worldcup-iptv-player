package com.example.iptvplayer

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var etM3uUrl: EditText
    private lateinit var btnLoad: Button
    private lateinit var rvChannels: RecyclerView
    private lateinit var playerView: PlayerView
    private lateinit var channelsAdapter: ChannelsAdapter

    // ExoPlayer
    private lateinit var player: ExoPlayer

    // Parser
    private val m3uParser = M3UParser()
    private var channelList = mutableListOf<M3UParser.Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AdMob (uses empty placeholder IDs; user replaces later)
        AdMobHelper.initialize(this)

        // Bind views
        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnLoad = findViewById(R.id.btnLoad)
        rvChannels = findViewById(R.id.rvChannels)
        playerView = findViewById(R.id.playerView)

        // Setup RecyclerView
        rvChannels.layoutManager = LinearLayoutManager(this)
        channelsAdapter = ChannelsAdapter { pos ->
            playChannel(channelList[pos])
        }
        rvChannels.adapter = channelsAdapter

        // Setup ExoPlayer
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Load button click
        btnLoad.setOnClickListener { loadPlaylistFromUrl() }
        // Allow "Go" on keyboard
        etM3uUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadPlaylistFromUrl()
                true
            } else false
        }
    }

    /** Fetch the .m3u content from a URL (simple GET). */
    private fun fetchM3uFromUrl(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            if (connection.responseCode == 200) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Called when the user presses Load. */
    private fun loadPlaylistFromUrl() {
        val url = etM3uUrl.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a .m3u URL", Toast.LENGTH_SHORT).show()
            return
        }
        showLoading(true)
        // Network work on a background thread (simple Thread – fine for demo)
        Thread {
            val content = fetchM3uFromUrl(url)
            runOnUiThread {
                showLoading(false)
                if (content == null || content.isEmpty()) {
                    Toast.makeText(this, "Failed to load playlist", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                channelList.clear()
                channelList.addAll(m3uParser.parse(content))
                if (channelList.isEmpty()) {
                    Toast.makeText(this, "No channels found in playlist", Toast.LENGTH_SHORT).show()
                } else {
                    channelsAdapter.submitList(channelList)
                    Toast.makeText(this, "Loaded ${channelList.size} channels", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /** Play a selected channel. */
    private fun playChannel(channel: M3UParser.Channel) {
        playerView.visibility = View.VISIBLE
        player.seekToDefaultPosition()
        player.setMediaItem(MediaItem.fromUri(channel.url))
        player.prepare()
        player.play()
    }

    /** Show/hide the loading indicator (simple disabled UI). */
    private fun showLoading(loading: Boolean) {
        etM3uUrl.isEnabled = !loading
        btnLoad.isEnabled = !loading
        if (loading) {
            etM3uUrl.setHint("Loading…")
        } else {
            etM3uUrl.setHint("Enter .m3u URL")
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    /** Simple adapter for the channel list. */
    private inner class ChannelsAdapter(
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ChannelsAdapter.ChannelViewHolder>() {

        private var items = listOf<M3UParser.Channel>()

        fun submitList(newList: List<M3UParser.Channel>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ChannelViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ChannelViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ChannelViewHolder(itemView: android.view.View) :
            RecyclerView.ViewHolder(itemView) {
            fun bind(channel: M3UParser.Channel) {
                (itemView as TextView).text = channel.name
                itemView.setOnClickListener { onItemClick(bindingAdapterPosition) }
            }
        }
    }
}
