package com.example.iptvplayer.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.iptvplayer.NativePlaylistParser
import com.example.iptvplayer.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

object PlaylistRepository {

    private const val TAG = "PlaylistRepository"

    private val dohDns = DnsOverHttps(
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build(),
        platform = Dns.SYSTEM,
        url = HttpUrl.get("https://cloudflare-dns.com/dns-query")
    )

    private val okHttpClient = OkHttpClient.Builder()
        .dns(dohDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun fetchPrimaryRaw(context: Context): String? =
        fetchRaw(PlaylistPreferences.primaryUrl(context).first())

    suspend fun fetchSecondaryRaw(context: Context): String? =
        fetchRaw(PlaylistPreferences.secondaryUrl(context).first())

    private suspend fun fetchRaw(url: String): String? {
        return withContext(Dispatchers.IO) {
            if (!url.startsWith("http", ignoreCase = true)) return@withContext null
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WorldCupIPTV/2.0")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    resp.body?.string()
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchRaw failed: $url", e)
                null
            }
        }
    }

    suspend fun parseWithNativeEngine(raw: String): List<Channel> {
        return withContext(Dispatchers.IO) {
            try {
                val ptr = NativePlaylistParser.nativeCreateParser()
                try {
                    NativePlaylistParser.nativeParseM3U(ptr, raw)
                } finally {
                    NativePlaylistParser.nativeDestroyParser(ptr)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Native parse failed, falling back to Kotlin parser", e)
                com.example.iptvplayer.M3UParser().parse(raw)
            }
        }
    }
}
