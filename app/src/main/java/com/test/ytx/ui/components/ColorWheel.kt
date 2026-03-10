package com.test.ytx.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    initialColor: Color = Color.Red,
    onColorChanged: (Color) -> Unit
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableStateOf(0f) }
    
    val selectorX = remember { Animatable(0f) }
    val selectorY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Initialize position from initial color once we have the layout size
    LaunchedEffect(center, radius) {
        if (radius > 0 && selectorX.value == 0f && selectorY.value == 0f) {
            val point = getPointForColor(initialColor, center, radius)
            selectorX.snapTo(point.x)
            selectorY.snapTo(point.y)
        }
    }

    // Cache gradients to prevent per-frame allocations and lag
    val sweepGradient = remember(center) {
        if (center != Offset.Zero) {
            Brush.sweepGradient(
                colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                    Color.Blue, Color.Magenta, Color.Red
                ),
                center = center
            )
        } else null
    }

    val radialGradient = remember(center, radius) {
        if (radius > 0) {
            Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            )
        } else null
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            // Use drawWithCache to minimize CPU work during swiping
            .drawWithCache {
                onDrawBehind {
                    val canvasCenter = Offset(size.width / 2, size.height / 2)
                    val canvasRadius = size.width / 2
                    
                    if (center != canvasCenter || radius != canvasRadius) {
                        center = canvasCenter
                        radius = canvasRadius
                    }

                    if (radius > 0 && sweepGradient != null && radialGradient != null) {
                        drawCircle(brush = sweepGradient, radius = radius, center = center)
                        drawCircle(brush = radialGradient, radius = radius, center = center)
                    }
                }
            }
            .pointerInput(center, radius) {
                if (radius <= 0) return@pointerInput
                detectTapGestures { offset ->
                    val distance = (offset - center).getDistance()
                    if (distance <= radius) {
                        scope.launch {
                            selectorX.animateTo(offset.x, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            selectorY.animateTo(offset.y, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                        }
                        onColorChanged(getColorAtPoint(offset, center, radius))
                    }
                }
            }
            .pointerInput(center, radius) {
                if (radius <= 0) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        val distance = (offset - center).getDistance()
                        if (distance <= radius) {
                            scope.launch {
                                selectorX.snapTo(offset.x)
                                selectorY.snapTo(offset.y)
                            }
                            onColorChanged(getColorAtPoint(offset, center, radius))
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val currentPoint = Offset(selectorX.value, selectorY.value)
                    val newPoint = currentPoint + dragAmount
                    val distance = (newPoint - center).getDistance()
                    
                    val finalPoint = if (distance <= radius) {
                        newPoint
                    } else {
                        val angle = atan2(newPoint.y - center.y, newPoint.x - center.x)
                        Offset(
                            x = center.x + radius * cos(angle),
                            y = center.y + radius * sin(angle)
                        )
                    }
                    
                    scope.launch {
                        selectorX.snapTo(finalPoint.x)
                        selectorY.snapTo(finalPoint.y)
                    }
                    onColorChanged(getColorAtPoint(finalPoint, center, radius))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val selectorOffset = Offset(selectorX.value, selectorY.value)
            if (selectorOffset != Offset.Zero) {
                // Outer ring for contrast
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = selectorOffset,
                    style = Stroke(width = 3.dp.toPx())
                )
                // Inner ring for depth
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = 13.dp.toPx(),
                    center = selectorOffset,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

private fun getColorAtPoint(point: Offset, center: Offset, radius: Float): Color {
    if (radius <= 0) return Color.Red
    val relativeX = point.x - center.x
    val relativeY = point.y - center.y
    val distance = sqrt(relativeX * relativeX + relativeY * relativeY)
    val saturation = (distance / radius).coerceIn(0f, 1f)
    
    // atan2 gives angle in range (-PI, PI]. 0 is 3 o'clock (Right).
    // This perfectly matches the sweepGradient start angle.
    var angle = Math.toDegrees(atan2(relativeY.toDouble(), relativeX.toDouble())).toFloat()
    if (angle < 0) angle += 360f
    
    // HSV: Red=0, Yellow=60, Green=120, Blue=240
    // The screen coordinates (Y down) and sweepGradient (CW) are already synced.
    return Color.hsv(hue = angle, saturation = saturation, value = 1f)
}

private fun getPointForColor(color: Color, center: Offset, radius: Float): Offset {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    
    val angle = Math.toRadians(hsv[0].toDouble())
    val saturation = hsv[1]
    val dist = saturation * radius

    return Offset(
        x = center.x + (dist * cos(angle)).toFloat(),
        y = center.y + (dist * sin(angle)).toFloat()
    )
}
