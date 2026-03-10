package com.test.ytx.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.test.ytx.data.SettingsRepository

@Composable
fun YTXTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    
    val accentColorInt by repository.accentColor.collectAsState()
    val targetAccentColor = remember(accentColorInt) {
        accentColorInt?.let { Color(it) } ?: Color(0xFFE53935)
    }

    // Animate the color change to ensure transitions are buttery smooth even during rapid swipes
    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 150), // Fast but smooth
        label = "accentAnimation"
    )

    val colorScheme = remember(animatedAccentColor) {
        darkColorScheme(
            primary = animatedAccentColor,
            onPrimary = Color.White,
            primaryContainer = animatedAccentColor.copy(alpha = 0.15f),
            onPrimaryContainer = Color.White,
            secondary = animatedAccentColor,
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF111111),
            onSecondaryContainer = Color.White,
            tertiary = animatedAccentColor,
            onTertiary = Color.Black,
            background = Color(0xFF000000),
            onBackground = Color.White,
            surface = Color(0xFF000000),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF080808),
            onSurfaceVariant = Color.Gray,
            outline = animatedAccentColor.copy(alpha = 0.5f),
            error = Color(0xFFCF6679)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val argbBackground = Color.Black.toArgb()
            window.statusBarColor = argbBackground
            window.navigationBarColor = argbBackground
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
