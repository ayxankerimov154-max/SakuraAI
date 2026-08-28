package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FridayCyan
import com.example.ui.theme.FridayNeonBlue
import com.example.ui.theme.FridayPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val dynamicScale = when {
        isListening -> 1.0f + (rmsLevel * 0.35f)
        isSpeaking -> pulseScale * 1.05f
        else -> pulseScale
    }

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("voice_orb_container")
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Energy Rings
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(dynamicScale)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.3f

            // Outer Hologram Wave Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        FridayCyan.copy(alpha = 0.2f),
                        FridayNeonBlue.copy(alpha = 0.6f),
                        FridayPurple.copy(alpha = 0.4f),
                        FridayCyan.copy(alpha = 0.2f)
                    )
                ),
                radius = radius * 1.15f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Inner Pulsing Core Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isListening) FridayCyan.copy(alpha = 0.5f)
                        else if (isSpeaking) FridayPurple.copy(alpha = 0.5f)
                        else FridayNeonBlue.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.2f
                ),
                radius = radius,
                center = center
            )

            // Particle ring nodes
            val nodeCount = 8
            for (i in 0 until nodeCount) {
                val angle = Math.toRadians((rotationAngle + (i * 360f / nodeCount)).toDouble())
                val x = center.x + (radius * 0.92f * cos(angle)).toFloat()
                val y = center.y + (radius * 0.92f * sin(angle)).toFloat()
                drawCircle(
                    color = if (isListening) FridayCyan else FridayPurple,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Inner Core Surface
        Surface(
            modifier = Modifier
                .size(110.dp)
                .scale(if (isListening) 1.08f else 1f),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isSpeaking) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Friday Speaking",
                        tint = FridayCyan,
                        modifier = Modifier.size(46.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) FridayCyan else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }
    }
}
