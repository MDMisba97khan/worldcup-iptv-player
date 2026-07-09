package com.example.iptvplayer

object NativePlaylistParser {
    init {
        System.loadLibrary("playlist-parser")
    }

    external fun nativeCreateParser(): Long
    external fun nativeDestroyParser(ptr: Long)
    external fun nativeParseM3U(ptr: Long, input: String): List<Channel>
}
