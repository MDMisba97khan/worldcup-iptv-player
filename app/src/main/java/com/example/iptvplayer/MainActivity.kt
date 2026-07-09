package com.example.iptvplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var etM3uUrl: EditText
    private lateinit var btnLoad: Button
    private lateinit var rvChannels: RecyclerView
    private lateinit var playerView: PlayerView
    private lateinit var channelsAdapter: ChannelsAdapter

    private var player: ExoPlayer? = null
    private val m3uParser = M3UParser()
    private var channelList = mutableListOf<M3UParser.Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // Skip problematic initialization steps for now
            // TODO: Re-enable AdMob after valid `ca-app-pub-xxxx` app ID is configured

            etM3uUrl = findViewById(R.id.etM3uUrl)
            btnLoad = findViewById(R.id.btnLoad)
            rvChannels = findViewById(R.id.rvChannels)
            playerView = findViewById(R.id.playerView)

            rvChannels.layoutManager = LinearLayoutManager(this)
            channelsAdapter = ChannelsAdapter { pos ->
                if (pos in channelList.indices) {
                    playChannel(channelList[pos])
                }
            }
            rvChannels.adapter = channelsAdapter

            player = try {
                ExoPlayer.Builder(this).build()
            } catch (e: Exception) {
                Toast.makeText(this, "Player unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                null
            }

            if (player != null) {
                playerView.player = player
                findViewById<androidx.media3.ui.PlayerView>(R.id.playerView).useController = true
            } else {
                playerView.visibility = View.GONE
            }

            btnLoad.setOnClickListener { loadPlaylistFromUrl() }
            etM3uUrl.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    loadPlaylistFromUrl()
                    true
                } else false
            }
        } catch (t: Throwable) {
            try {
                Toast.makeText(this, "Startup error: ${t.message}", Toast.LENGTH_LONG).show()
            } catch (_: Exception) { }
        }
    }

    private fun fetchM3uFromUrl(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadPlaylistFromUrl() {
        val url = etM3uUrl.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a .m3u URL", Toast.LENGTH_SHORT).show()
            return
        }
        showLoading(true)
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
                    Toast.makeText(this, "No channels found", Toast.LENGTH_SHORT).show()
                } else {
                    channelsAdapter.submitList(channelList)
                    Toast.makeText(this, "Loaded ${channelList.size} channels", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun playChannel(channel: M3UParser.Channel) {
        try {
            player?.let { p ->
                playerView.visibility = View.VISIBLE
                p.seekToDefaultPosition()
                p.setMediaItem(MediaItem.fromUri(channel.url))
                p.prepare()
                p.play()
            } ?: run {
                Toast.makeText(this, "Player not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Playback failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(loading: Boolean) {
        etM3uUrl.isEnabled = !loading
        btnLoad.isEnabled = !loading
        etM3uUrl.hint = if (loading) "Loading…" else "Enter .m3u URL"
    }

    override fun onDestroy() {
        try {
            player?.release()
        } catch (_: Exception) { }
        super.onDestroy()
    }

    private inner class ChannelsAdapter(
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ChannelViewHolder>() {

        private var items = listOf<M3UParser.Channel>()

        fun submitList(newList: List<M3UParser.Channel>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ChannelViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ChannelViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            fun bind(channel: M3UParser.Channel) {
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
