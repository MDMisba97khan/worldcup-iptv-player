package com.example.iptvplayer.model

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val name: String,
    val url: String,
    val logo: String = ""
)
