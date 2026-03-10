package com.test.ytx.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.ytx.ui.components.ColorWheel
import com.test.ytx.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val accentColorInt by viewModel.accentColor.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val isServerOnline by viewModel.isServerOnline.collectAsState()
    val downloadPath by viewModel.downloadPath.collectAsState()

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val accentColor = accentColorInt?.let { Color(it) } ?: Color(0xFFE53935)

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
            viewModel.updateDownloadPath(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack, 
                            contentDescription = "Back",
                            tint = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Global Accent", icon = Icons.Rounded.ColorLens, color = accentColor)
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ColorWheel(
                    modifier = Modifier.size(250.dp),
                    initialColor = accentColor,
                    onColorChanged = { color ->
                        viewModel.updateAccentColor(color.toArgb())
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            HorizontalDivider(color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader(title = "Server Connection", icon = Icons.Rounded.Dns, color = accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            
            ServerUrlInput(
                url = serverUrl,
                isOnline = isServerOnline,
                accentColor = accentColor,
                onUrlChange = { viewModel.updateServerUrl(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader(title = "Download Location", icon = Icons.Rounded.Folder, color = accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            
            DownloadPathPicker(
                path = downloadPath,
                accentColor = accentColor,
                onPickClick = { directoryPickerLauncher.launch(null) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader(title = "App Information", icon = Icons.Rounded.Info, color = accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            
            AppInfoCard(accentColor = accentColor)

            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "MADE BY TAQI",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = accentColor.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun AppInfoCard(accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF080808)),
        border = BorderStroke(1.dp, Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            InfoRow("Built with", "Kotlin & Jetpack Compose", accentColor)
            InfoRow("Backend", "Python (yt-dlp)", accentColor)
            InfoRow("UI Design", "Material 3 Expressive", accentColor)
            InfoRow("Version", "1.0.0 Stable", accentColor)
            InfoRow("Developer", "TAQI", accentColor)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = accentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = color, 
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        )
    }
}

@Composable
fun ServerUrlInput(
    url: String,
    isOnline: Boolean,
    accentColor: Color,
    onUrlChange: (String) -> Unit
) {
    var text by remember(url) { mutableStateOf(url) }

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                onUrlChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Server URL") },
            placeholder = { Text("http://192.168.1.x:8000") },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFF1A1A1A),
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF080808))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor by animateColorAsState(
                targetValue = if (isOnline) Color(0xFF43A047) else Color(0xFFE53935),
                label = "statusColor"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isOnline) "Server is Online" else "Server is Offline",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            )
        }
    }
}

@Composable
fun DownloadPathPicker(
    path: String?,
    accentColor: Color,
    onPickClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPickClick),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF080808),
        border = BorderStroke(1.dp, Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = "Phone Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = path ?: "Tap to select folder...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
