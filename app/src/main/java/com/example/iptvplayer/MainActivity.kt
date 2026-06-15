package com.example.iptvplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set a simple placeholder UI
        setContentView(android.R.layout.simple_list_item_1)
        // TODO: Implement M3U parser, ExoPlayer UI, etc.
    }
}
