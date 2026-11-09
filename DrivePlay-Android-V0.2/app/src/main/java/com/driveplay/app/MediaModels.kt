package com.driveplay.app

import android.net.Uri

data class VideoFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val modifiedMillis: Long
) {
    val id: String get() = uri.toString()
}

data class Playlist(
    val name: String,
    val videoUris: List<String>
)
