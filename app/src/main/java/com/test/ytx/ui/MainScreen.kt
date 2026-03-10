package com.test.ytx.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.test.ytx.model.VideoMetadata
import com.test.ytx.viewmodel.MainViewModel
import com.test.ytx.viewmodel.UiState
import com.test.ytx.viewmodel.DownloadState
import com.test.ytx.viewmodel.DownloadProgressInfo
import com.test.ytx.ui.components.MetadataLoadingIndicator
import com.test.ytx.ui.components.ExpressiveProgressRing
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val downloadInfo by viewModel.downloadInfo.collectAsStateWithLifecycle()
    val isServerOnline by viewModel.isServerOnline.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    
    var urlText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "YTX", 
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        ServerStatusBadge(isOnline = isServerOnline)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Rounded.Settings, 
                            contentDescription = "Settings", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.checkServerStatus() },
            state = pullToRefreshState,
            indicator = {
                if (pullToRefreshState.distanceFraction > 0f || isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MetadataLoadingIndicator(
                            modifier = Modifier
                                .size(50.dp)
                                .graphicsLayer {
                                    val progress = pullToRefreshState.distanceFraction.coerceIn(0f, 1f)
                                    alpha = if (isRefreshing) 1f else progress
                                    scaleX = if (isRefreshing) 1f else progress
                                    scaleY = if (isRefreshing) 1f else progress
                                }
                        )
                    }
                }
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                GreetingHeader()
                Spacer(modifier = Modifier.height(32.dp))

                LinkInputField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    onFetchClick = { viewModel.fetchMetadata(urlText) },
                    onClearClick = { urlText = "" },
                    onPasteClick = {
                        clipboardManager.getText()?.let { urlText = it.text }
                    },
                    enabled = downloadState == DownloadState.Idle
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState is UiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MetadataLoadingIndicator(
                            modifier = Modifier.size(60.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                metadata?.let { data ->
                    MetadataCard(
                        metadata = data,
                        downloadState = downloadState,
                        downloadInfo = downloadInfo,
                        onDownloadClick = { format, quality -> viewModel.startDownload(urlText, format, quality) },
                        onResetClick = { 
                            viewModel.resetForNewDownload()
                            urlText = ""
                        }
                    )
                }

                if (uiState is UiState.Error) {
                    ErrorDisplay(message = (uiState as UiState.Error).message)
                }
                
                if (uiState is UiState.Idle && metadata == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enter a video link to begin",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ServerStatusBadge(isOnline: Boolean) {
    Surface(
        color = (if (isOnline) Color.Green else Color.Red).copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(1.dp, (if (isOnline) Color.Green else Color.Red).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isOnline) Color.Green else Color.Red
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOnline) "ONLINE" else "OFFLINE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isOnline) Color.Green else Color.Red
            )
        }
    }
}

@Composable
fun GreetingHeader() {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = "Ready for high-speed downloads",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun LinkInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onFetchClick: () -> Unit,
    onClearClick: () -> Unit,
    onPasteClick: () -> Unit,
    enabled: Boolean
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text("Paste Link") },
            placeholder = { Text("https://youtube.com/...") },
            leadingIcon = {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClearClick, enabled = enabled) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    IconButton(onClick = onPasteClick, enabled = enabled) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Search
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFetchClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = enabled && value.isNotBlank(),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyze URL", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetadataCard(
    metadata: VideoMetadata,
    downloadState: DownloadState,
    downloadInfo: DownloadProgressInfo,
    onDownloadClick: (String, String) -> Unit,
    onResetClick: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("MP4") }
    var selectedQuality by remember { mutableStateOf("best") }
    val cardCornerRadius = 32.dp
    val isDownloading = downloadState !is DownloadState.Idle

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cardCornerRadius)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column {
            AnimatedContent(
                targetState = isDownloading,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.8f))
                        .togetherWith(fadeOut(animationSpec = tween(600)) + scaleOut(targetScale = 1.2f))
                },
                label = "ThumbnailState"
            ) { downloading ->
                if (downloading) {
                    Box(
                        modifier = Modifier
                            .height(240.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Smoothly animate the progress to match Google Play's feel
                        val progressValue by animateFloatAsState(
                            targetValue = if (downloadState is DownloadState.Completed) 1f else downloadInfo.progress,
                            animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
                            label = "Progress"
                        )

                        // Circular Thumbnail
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(metadata.thumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Thumbnail",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        // The wiggly progress ring
                        ExpressiveProgressRing(
                            progress = progressValue,
                            isConverting = downloadInfo.isConverting,
                            modifier = Modifier.size(190.dp)
                        )

                        // Status Overlay (Checkmark or Progress)
                        AnimatedContent(
                            targetState = downloadState,
                            transitionSpec = {
                                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                            },
                            label = "StatusOverlay"
                        ) { state ->
                            if (state is DownloadState.Completed) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SuccessCheckmarkAnimation(color = Color.Black)
                                }
                            } else if (downloadInfo.isConverting) {
                                MetadataLoadingIndicator(modifier = Modifier.size(60.dp))
                            } else {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${(progressValue * 100).toInt()}%",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(220.dp).fillMaxWidth()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(metadata.thumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                        startY = 300f
                                    )
                                )
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = metadata.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = metadata.author ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape) {
                        Text(
                            text = metadata.duration,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (downloadState == DownloadState.Idle) {
                    // Quality Selector
                    AnimatedVisibility(
                        visible = selectedFormat != "MP3",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Text("Select Quality", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Best", "1080p", "720p").forEach { quality ->
                                    FilterChip(
                                        selected = selectedQuality == quality.lowercase(),
                                        onClick = { selectedQuality = quality.lowercase() },
                                        label = { Text(quality) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DownloadTypeButton(
                            text = "MP4 Video",
                            isSelected = selectedFormat == "MP4",
                            onClick = { selectedFormat = "MP4" },
                            modifier = Modifier.weight(1f)
                        )
                        DownloadTypeButton(
                            text = "MP3 Audio",
                            isSelected = selectedFormat == "MP3",
                            onClick = { selectedFormat = "MP3" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onDownloadClick(selectedFormat, selectedQuality) },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Start Download", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 18.sp)
                    }
                } else {
                    DownloadStatusInfo(downloadState, downloadInfo, onResetClick)
                }
            }
        }
    }
}

@Composable
fun DownloadStatusInfo(
    state: DownloadState,
    info: DownloadProgressInfo,
    onResetClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = info.speed,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.Red
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(visible = state is DownloadState.Completed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            (state as? DownloadState.Completed)?.fileUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share File"))
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share")
                    }

                    FilledTonalButton(
                        onClick = {
                            (state as? DownloadState.Completed)?.fileUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, context.contentResolver.getType(uri))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onResetClick) {
                    Text("Download Another", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun DownloadTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(50.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) Color.Black else Color.Gray
            )
        }
    }
}

@Composable
fun ErrorDisplay(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SuccessCheckmarkAnimation(color: Color) {
    val composition by remember { mutableStateOf(0) } // Placeholder for actual animation
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(48.dp)
    )
}
