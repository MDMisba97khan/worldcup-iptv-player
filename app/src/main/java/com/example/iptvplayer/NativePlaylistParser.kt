package com.example.iptvplayer

import com.example.iptvplayer.model.Channel

object NativePlaylistParser {
    init {
        System.loadLibrary("playlist-parser")
    }

    external fun nativeCreateParser(): Long
    external fun nativeDestroyParser(ptr: Long)
    external fun nativeParseM3U(ptr: Long, input: String): List<Channel>

    fun parseWithEngine(raw: String): List<Channel> {
        val parserPtr = nativeCreateParser()
        if (parserPtr == 0L) return emptyList()
        return try {
            nativeParseM3U(parserPtr, raw)
        } finally {
            nativeDestroyParser(parserPtr)
        }
    }
}
