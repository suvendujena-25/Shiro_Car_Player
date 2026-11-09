package com.driveplay.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences("driveplay_playlists", Context.MODE_PRIVATE)

    fun load(): List<Playlist> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                val uris = item.getJSONArray("videos")
                Playlist(
                    name = item.getString("name"),
                    videoUris = (0 until uris.length()).map { uris.getString(it) }
                )
            }.getOrNull()
        }
    }

    fun save(playlists: List<Playlist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val videos = JSONArray()
            playlist.videoUris.forEach(videos::put)
            array.put(JSONObject().put("name", playlist.name).put("videos", videos))
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}
