package com.driveplay.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.media.MediaMetadataRetriever
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DrivePlayApp() }
    }
}

private enum class Tab { HOME, LIBRARY, PLAYLISTS, ONLINE }

@Composable
private fun DrivePlayApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UsbMediaRepository(context) }
    val playlistStore = remember { PlaylistStore(context) }

    var tab by remember { mutableStateOf(Tab.HOME) }
    var videos by remember { mutableStateOf(emptyList<VideoFile>()) }
    var playlists by remember { mutableStateOf(playlistStore.load()) }
    var selectedVideo by remember { mutableStateOf<VideoFile?>(null) }
    var usbTreeUri by remember {
        mutableStateOf(context.getSharedPreferences("driveplay_usb", 0).getString("tree", null)?.let(Uri::parse))
    }
    var isScanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Connect a USB drive and select its video folder.") }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var showAddToPlaylist by remember { mutableStateOf<VideoFile?>(null) }

    fun scanUsb(uri: Uri) {
        scope.launch {
            isScanning = true
            status = "Scanning USB folder…"
            val result = repository.scanTree(uri)
            videos = result
            isScanning = false
            status = if (result.isEmpty()) "No supported video files found in this folder."
                     else "${result.size} video(s) found."
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            usbTreeUri = uri
            context.getSharedPreferences("driveplay_usb", 0).edit()
                .putString("tree", uri.toString()).apply()
            scanUsb(uri)
        }
    }

    LaunchedEffect(usbTreeUri) {
        usbTreeUri?.let { scanUsb(it) }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF101820)) {
            if (selectedVideo != null) {
                VideoPlayerV02(video = selectedVideo!!, onBack = { selectedVideo = null })
            } else {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = Color(0xFF17232D),
                        contentColor = Color.White
                    ) {
                        Spacer(Modifier.height(18.dp))
                        NavigationRailItem(
                            selected = tab == Tab.HOME,
                            onClick = { tab = Tab.HOME },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Home") }
                        )
                        NavigationRailItem(
                            selected = tab == Tab.LIBRARY,
                            onClick = { tab = Tab.LIBRARY },
                            icon = { Icon(Icons.Default.VideoLibrary, null) },
                            label = { Text("Library") }
                        )
                        NavigationRailItem(
                            selected = tab == Tab.PLAYLISTS,
                            onClick = { tab = Tab.PLAYLISTS },
                            icon = { Icon(Icons.Default.PlaylistPlay, null) },
                            label = { Text("Playlists") }
                        )
                        NavigationRailItem(
                            selected = tab == Tab.ONLINE,
                            onClick = { tab = Tab.ONLINE },
                            icon = { Icon(Icons.Default.Language, null) },
                            label = { Text("Online") }
                        )
                    }

                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("DRIVEPLAY", style = MaterialTheme.typography.headlineMedium)
                                Text("Your personal media centre", color = Color.LightGray)
                            }
                            if (tab == Tab.LIBRARY || tab == Tab.HOME) {
                                Button(onClick = { folderPicker.launch(usbTreeUri) }) {
                                    Icon(Icons.Default.Usb, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (usbTreeUri == null) "Select USB folder" else "Change USB folder")
                                }
                            }
                        }

                        when (tab) {
                            Tab.HOME -> HomeScreen(
                                videos = videos,
                                status = status,
                                isScanning = isScanning,
                                onOpenLibrary = { tab = Tab.LIBRARY },
                                onOpenVideo = { selectedVideo = it },
                                onSelectUsb = { folderPicker.launch(usbTreeUri) }
                            )
                            Tab.LIBRARY -> LibraryScreen(
                                videos = videos,
                                status = status,
                                isScanning = isScanning,
                                onRefresh = { usbTreeUri?.let(::scanUsb) },
                                onPlay = { selectedVideo = it },
                                onAddPlaylist = { showAddToPlaylist = it }
                            )
                            Tab.PLAYLISTS -> PlaylistScreen(
                                playlists = playlists,
                                videos = videos,
                                onCreate = { showCreatePlaylist = true },
                                onPlay = { uri ->
                                    videos.firstOrNull { it.uri.toString() == uri }?.let { selectedVideo = it }
                                }
                            )
                            Tab.ONLINE -> OnlineScreen()
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("Create playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = playlistName.trim()
                    if (name.isNotEmpty() && playlists.none { it.name.equals(name, true) }) {
                        playlists = playlists + Playlist(name, emptyList())
                        playlistStore.save(playlists)
                    }
                    playlistName = ""
                    showCreatePlaylist = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") }
            }
        )
    }

    showAddToPlaylist?.let { video ->
        AlertDialog(
            onDismissRequest = { showAddToPlaylist = null },
            title = { Text("Add to playlist") },
            text = {
                if (playlists.isEmpty()) Text("Create a playlist first.")
                else Column {
                    playlists.forEach { playlist ->
                        TextButton(onClick = {
                            val updated = playlists.map {
                                if (it.name == playlist.name && video.id !in it.videoUris)
                                    it.copy(videoUris = it.videoUris + video.id) else it
                            }
                            playlists = updated
                            playlistStore.save(updated)
                            showAddToPlaylist = null
                        }) { Text(playlist.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddToPlaylist = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun HomeScreen(
    videos: List<VideoFile>,
    status: String,
    isScanning: Boolean,
    onOpenLibrary: () -> Unit,
    onOpenVideo: (VideoFile) -> Unit,
    onSelectUsb: () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HomeCard("USB Video Library", "$status\n${videos.size} video(s)", Icons.Default.Usb, onOpenLibrary)
            HomeCard("Online Services", "YouTube and Instagram entry points", Icons.Default.Language, {
                // Navigation is handled by the Online tab.
            })
            HomeCard("Playlists", "Organize your favourite videos", Icons.Default.PlaylistPlay, onOpenLibrary)
        }
        Text("Recently available", style = MaterialTheme.typography.titleLarge)
        if (isScanning) CircularProgressIndicator()
        else if (videos.isEmpty()) {
            EmptyState("No USB videos loaded", "Select a USB folder to scan your videos.", onSelectUsb)
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(190.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(videos.take(6), key = { it.id }) { video ->
                    VideoCard(video, onClick = { onOpenVideo(video) })
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Video playback is intended only while safely parked.", color = Color(0xFFFFC66D))
    }
}

@Composable
private fun LibraryScreen(
    videos: List<VideoFile>,
    status: String,
    isScanning: Boolean,
    onRefresh: () -> Unit,
    onPlay: (VideoFile) -> Unit,
    onAddPlaylist: (VideoFile) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$status  •  ${videos.size} items", Modifier.weight(1f), color = Color.LightGray)
            TextButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Text("Refresh") }
        }
        if (isScanning) CircularProgressIndicator()
        else if (videos.isEmpty()) EmptyState("USB library is empty", "Select a USB folder using the button above.") {}
        else LazyVerticalGrid(
            columns = GridCells.Adaptive(210.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoCard(video, onClick = { onPlay(video) }, onAddPlaylist = { onAddPlaylist(video) })
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoFile, onClick: () -> Unit, onAddPlaylist: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF263746))
    ) {
        Column {
            VideoThumbnail(video.uri)
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(video.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatBytes(video.sizeBytes), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
                if (onAddPlaylist != null) {
                    IconButton(onClick = onAddPlaylist) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(uri: Uri) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                frame
            }.onSuccess { bitmap = it }
        }
    }
    if (bitmap != null) {
        AsyncImage(model = bitmap, contentDescription = "Video thumbnail", modifier = Modifier.fillMaxWidth().height(112.dp))
    } else {
        Box(
            Modifier.fillMaxWidth().height(112.dp).background(Color(0xFF17232D)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
    }
}

@Composable
private fun PlaylistScreen(
    playlists: List<Playlist>,
    videos: List<VideoFile>,
    onCreate: () -> Unit,
    onPlay: (String) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${playlists.size} playlist(s)", Modifier.weight(1f), color = Color.LightGray)
            Button(onClick = onCreate) { Icon(Icons.Default.Add, null); Text("New playlist") }
        }
        if (playlists.isEmpty()) {
            Text("No playlists yet. Create one to organize your USB videos.", color = Color.LightGray)
        } else {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(playlists.size) { index ->
                    val playlist = playlists[index]
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF263746))) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(playlist.name, style = MaterialTheme.typography.titleLarge)
                            Text("${playlist.videoUris.size} video(s)", color = Color.LightGray)
                            playlist.videoUris.forEach { uri ->
                                val item = videos.firstOrNull { it.id == uri }
                                if (item != null) {
                                    TextButton(onClick = { onPlay(uri) }) {
                                        Icon(Icons.Default.PlayArrow, null)
                                        Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                } else {
                                    Text("Unavailable USB file", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineScreen() {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Online services", style = MaterialTheme.typography.titleLarge)
        Text(
            "Choose an official service. DrivePlay will hand off to the installed app or supported web experience. Embedded playback can be added only where the provider permits it.",
            color = Color.LightGray
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HomeCard("YouTube", "Open official YouTube", Icons.Default.PlayCircle) {
                openExternal(context, "https://www.youtube.com")
            }
            HomeCard("Instagram", "Open official Instagram", Icons.Default.PhotoCamera) {
                openExternal(context, "https://www.instagram.com")
            }
        }
        Text(
            "Note: YouTube/Instagram playback and sign-in are controlled by their official apps and policies. This is not an offline downloader.",
            color = Color(0xFFFFC66D)
        )
    }
}

private fun openExternal(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.widthIn(min = 190.dp).height(142.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF263746))
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String, action: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(40.dp), tint = Color.LightGray)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, color = Color.LightGray)
        Spacer(Modifier.height(8.dp))
        Button(onClick = action) { Text("Choose USB folder") }
    }
}

@Composable
private fun VideoPlayerV02(video: VideoFile, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Library", color = Color.White) }
            Text(video.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            update = { it.player = player }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    return String.format(Locale.getDefault(), "%.1f MB", kb / 1024.0)
}
