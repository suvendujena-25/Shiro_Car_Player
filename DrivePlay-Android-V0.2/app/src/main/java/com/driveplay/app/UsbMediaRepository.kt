package com.driveplay.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsbMediaRepository(private val context: Context) {
    suspend fun scanTree(treeUri: Uri): List<VideoFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val results = mutableListOf<VideoFile>()

        fun visit(folder: DocumentFile) {
            folder.listFiles().forEach { file ->
                when {
                    file.isDirectory -> visit(file)
                    file.isFile && isVideo(file) -> results += VideoFile(
                        uri = file.uri,
                        name = file.name ?: "Unknown video",
                        sizeBytes = file.length(),
                        modifiedMillis = file.lastModified()
                    )
                }
            }
        }

        visit(root)
        results.sortedBy { it.name.lowercase() }
    }

    private fun isVideo(file: DocumentFile): Boolean {
        val mime = file.type.orEmpty()
        if (mime.startsWith("video/")) return true
        val name = file.name.orEmpty().lowercase()
        return listOf(".mp4", ".mkv", ".avi", ".mov", ".webm", ".3gp", ".m4v")
            .any(name::endsWith)
    }
}
