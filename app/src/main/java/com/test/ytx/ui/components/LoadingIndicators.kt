package com.test.ytx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MetadataLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val polygons = remember {
        listOf(
            RoundedPolygon.star(
                numVerticesPerRadius = 12,
                innerRadius = 0.2f,
                rounding = CornerRounding(radius = 0.4f)
            ),
            RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = 0.7f,
                rounding = CornerRounding(radius = 0.1f)
            ),
            RoundedPolygon(
                numVertices = 10,
                rounding = CornerRounding(radius = 0.5f)
            ),
            RoundedPolygon.star(
                numVerticesPerRadius = 5,
                innerRadius = 0.3f,
                rounding = CornerRounding(radius = 0.5f)
            )
        )
    }

    LoadingIndicator(
        modifier = modifier,
        color = color,
        polygons = polygons
    )
}

/**
 * A production-ready implementation of the Material 3 Expressive "Wavy" progress indicator.
 * It automatically switches between Indeterminate (spinning) and Determinate (progress) states.
 * 
 * Tuned to match the organic "Google Play" look by adjusting amplitude and wavelength.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    isConverting: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // 1. Progress Animation: Prevent jumping between values
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "WavyProgress"
    )

    // 2. Wiggle Controls:
    // Amplitude (Float): Controls the "depth" of the wave.
    // Wavelength (Dp): Controls the "density" or frequency of waves.
    val amplitudeValue = if (isConverting) 3f else 1.5f
    val wavelengthValue = if (isConverting) 15.dp else 25.dp

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (progress <= 0f || isConverting) {
            // Indeterminate State (Spinning Wavy Circle)
            CircularWavyProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = color,
                trackColor = color.copy(alpha = 0.1f),
                amplitude = amplitudeValue,
                wavelength = wavelengthValue
            )
        } else {
            // Determinate State (Progress Fill)
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                trackColor = color.copy(alpha = 0.1f),
                // In determinate mode, amplitude can be a function of progress
                amplitude = { amplitudeValue },
                wavelength = wavelengthValue
            )
        }
    }
}
