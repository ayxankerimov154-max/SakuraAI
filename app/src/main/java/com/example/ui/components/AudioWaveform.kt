package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FridayCyan
import com.example.ui.theme.FridayNeonBlue
import kotlin.math.sin

@Composable
fun AudioWaveform(
    isActive: Boolean,
    amplitude: Float = 0.5f,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    color: Color = FridayCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val totalWidth = size.width
        val height = size.height
        val barWidth = (totalWidth / (barCount * 1.5f)).coerceAtLeast(3f)
        val spacing = barWidth * 0.5f

        for (i in 0 until barCount) {
            val x = i * (barWidth + spacing) + spacing
            val normalizedIndex = i.toFloat() / barCount
            val wave = if (isActive) {
                val sinVal = (sin((normalizedIndex * 4 * Math.PI + phase).toDouble()).toFloat() + 1f) / 2f
                (sinVal * amplitude * 0.7f + 0.15f).coerceIn(0.1f, 1f)
            } else {
                0.08f
            }

            val barHeight = height * wave
            val y = (height - barHeight) / 2f

            drawRoundRect(
                color = if (i % 2 == 0) color else FridayNeonBlue,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
