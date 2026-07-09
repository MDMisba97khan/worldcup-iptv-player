package com.example.iptvplayer.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "playlist_prefs")

object PlaylistPreferences {
    private val KEY_PRIMARY_URL = stringPreferencesKey("primary_playlist_url")
    private val KEY_SECONDARY_URL = stringPreferencesKey("secondary_playlist_url")
    private val KEY_LAST_SYNC = stringPreferencesKey("last_sync_epoch_ms")

    fun primaryUrl(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_PRIMARY_URL] ?: "https://iptv-org.github.io/iptv/index.m3u" }

    suspend fun setPrimaryUrl(context: Context, url: String) {
        context.dataStore.edit { it[KEY_PRIMARY_URL] = url }
    }

    fun secondaryUrl(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_SECONDARY_URL] ?: "https://go.skym3u.top/nszn.m3u" }

    suspend fun setSecondaryUrl(context: Context, url: String) {
        context.dataStore.edit { it[KEY_SECONDARY_URL] = url }
    }

    fun lastSync(context: Context): Flow<Long> =
        context.dataStore.data.map { it[KEY_LAST_SYNC] ?: 0L }

    suspend fun setLastSync(context: Context, epochMs: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC] = epochMs }
    }
}
