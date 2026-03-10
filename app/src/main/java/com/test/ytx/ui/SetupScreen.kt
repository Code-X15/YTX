package com.test.ytx.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SetupScreen(
    onComplete: (String, String) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var downloadPath by remember { mutableStateOf("Downloads/YTX") }
    var serverUrl by remember { mutableStateOf("http://10.0.2.2:8000") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it } togetherWith
                    fadeOut() + slideOutHorizontally { -it }
                },
                label = "setupStep"
            ) { step ->
                when (step) {
                    0 -> SetupStepContent(
                        icon = Icons.Default.Language,
                        title = "Welcome to YTX",
                        description = "High-speed video and audio downloader.\nBuilt with Kotlin & Jetpack Compose.",
                        onNext = { currentStep++ }
                    )
                    1 -> SetupInputStep(
                        icon = Icons.Default.FolderOpen,
                        title = "Download Location",
                        description = "Where should we save your files?",
                        value = downloadPath,
                        onValueChange = { downloadPath = it },
                        onNext = { currentStep++ }
                    )
                    2 -> SetupInputStep(
                        icon = Icons.Default.Link,
                        title = "Server URL",
                        description = "Enter your YTX backend address (optional)",
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        onNext = { currentStep++ },
                        isOptional = true
                    )
                    3 -> SetupStepContent(
                        icon = Icons.Default.DoneAll,
                        title = "All Set!",
                        description = "Ready to start downloading.\nMade by TAQI",
                        buttonText = "Get Started",
                        onNext = { onComplete(downloadPath, serverUrl) }
                    )
                }
            }
        }
    }
}

@Composable
fun SetupStepContent(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String = "Next",
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun SetupInputStep(
    icon: ImageVector,
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    isOptional: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            if (isOptional) {
                TextButton(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Skip")
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Next", fontWeight = FontWeight.Bold)
            }
        }
    }
}
