package com.example.iptvplayer

/**
 * Simple, dependency‑free M3U parser.
 * Returns a list of Channel objects (name, url).
 */
class M3UParser {

    data class Channel(val name: String, val url: String)

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                // Extract channel name after the first comma
                val commaIndex = line.indexOf(',')
                val name = if (commaIndex != -1) line.substring(commaIndex + 1) else "Unknown"
                // Next non‑empty line should be the URL
                var j = i + 1
                while (j < lines.size && lines[j].trim().isEmpty()) j++
                if (j < lines.size) {
                    val url = lines[j].trim()
                    if (url.isNotEmpty() && !url.startsWith("#")) {
                        channels += Channel(name, url)
                    }
                }
                i = j
            } else {
                i++
            }
        }
        return channels
    }
}
