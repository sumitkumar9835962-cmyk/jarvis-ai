package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcGlow
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    isListening: Boolean,
    isProcessing: Boolean,
    statusText: String,
    onCoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 500 else if (isProcessing) 800 else 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val activeColor = when {
        isListening -> ArcGlow
        isProcessing -> ElectricPurple
        else -> CyberCyan
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(200.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCoreClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f * 0.9f * pulseScale

            // Outer Glow aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activeColor.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = maxRadius * 1.2f
                ),
                radius = maxRadius * 1.1f,
                center = center
            )

            // Outer Dashed Rotating Ring
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.7f),
                    radius = maxRadius * 0.85f,
                    center = center,
                    style = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                    )
                )

                // Notch indicators on outer ring
                for (i in 0 until 8) {
                    val angle = (i * 45) * (Math.PI / 180.0)
                    val startR = maxRadius * 0.85f
                    val endR = maxRadius * 0.95f
                    val startX = center.x + startR * cos(angle).toFloat()
                    val startY = center.y + startR * sin(angle).toFloat()
                    val endX = center.x + endR * cos(angle).toFloat()
                    val endY = center.y + endR * sin(angle).toFloat()
                    drawLine(
                        color = activeColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 5f
                    )
                }
            }

            // Inner Rotating Core Ring
            rotate(innerRotation, pivot = center) {
                drawCircle(
                    color = NeonBlue.copy(alpha = 0.8f),
                    radius = maxRadius * 0.65f,
                    center = center,
                    style = Stroke(
                        width = 6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(35f, 20f), 0f)
                    )
                )

                // 3 Segment Triangles / Arc Nodes
                for (i in 0 until 3) {
                    val angle = (i * 120) * (Math.PI / 180.0)
                    val r = maxRadius * 0.65f
                    val x = center.x + r * cos(angle).toFloat()
                    val y = center.y + r * sin(angle).toFloat()
                    drawCircle(
                        color = activeColor,
                        radius = 8f,
                        center = Offset(x, y)
                    )
                }
            }

            // Central Glowing Core Disk
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        activeColor,
                        activeColor.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.45f
                ),
                radius = maxRadius * 0.42f,
                center = center
            )

            // Inner Ring Boundary
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = maxRadius * 0.42f,
                center = center,
                style = Stroke(width = 3f)
            )
        }

        // Center Text Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "JARVIS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isListening) "LISTENING" else if (isProcessing) "THINKING" else "ONLINE",
                color = activeColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
