package com.tridev.realweather365.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

@Composable
fun SunnyEnvironment(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sunny-reference-environment")
    val cloudShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 82000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud-drift"
    )
    val shimmer = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lake-shimmer"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawReferenceSky()
        drawReferenceSun()
        drawAtmosphericClouds(cloudShift.value)
        drawMountainWorld()
        drawReferenceLake(shimmer.value)
        drawShoreForest()
        drawForegroundPines()
    }
}

private fun DrawScope.drawReferenceSky() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF0D80D3),
            0.27f to Color(0xFF45AEEB),
            0.52f to Color(0xFF8FD2F1),
            0.69f to Color(0xFFD5ECF3),
            1f to Color(0xFF4D8DA7)
        )
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0x44FFF1CD), Color.Transparent),
            startY = size.height * 0.28f,
            endY = size.height * 0.62f
        )
    )
}

private fun DrawScope.drawReferenceSun() {
    val center = Offset(size.width * 0.18f, size.height * 0.19f)
    val glowRadius = size.width * 0.34f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.86f),
                Color(0xFFFFF4A8).copy(alpha = 0.52f),
                Color(0xFFFFDD72).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    repeat(18) { index ->
        rotate(index * 20f, pivot = center) {
            drawRoundRect(
                color = Color.White.copy(alpha = if (index % 2 == 0) 0.48f else 0.24f),
                topLeft = Offset(center.x - 1.1f, center.y - size.width * 0.17f),
                size = Size(2.2f, size.width * if (index % 2 == 0) 0.10f else 0.065f),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }

    drawCircle(Color(0xFFFFF7CF), radius = size.width * 0.038f, center = center)
    drawCircle(Color.White.copy(alpha = 0.75f), radius = size.width * 0.018f, center = center)
}

private fun DrawScope.drawAtmosphericClouds(progress: Float) {
    fun cloud(x: Float, y: Float, scale: Float, alpha: Float) {
        val c = Color.White.copy(alpha = alpha)
        drawOval(c, Offset(x, y), Size(size.width * 0.24f * scale, size.height * 0.028f * scale))
        drawCircle(c, size.width * 0.035f * scale, Offset(x + size.width * 0.055f * scale, y))
        drawCircle(c, size.width * 0.046f * scale, Offset(x + size.width * 0.115f * scale, y - size.height * 0.007f * scale))
        drawCircle(c, size.width * 0.032f * scale, Offset(x + size.width * 0.17f * scale, y))
    }

    val driftA = (progress * size.width * 0.34f) - size.width * 0.13f
    val driftB = (progress * size.width * 0.22f) - size.width * 0.08f
    cloud(size.width * 0.56f + driftA, size.height * 0.17f, 0.75f, 0.35f)
    cloud(size.width * 0.18f + driftB, size.height * 0.30f, 0.55f, 0.22f)
    cloud(size.width * 0.72f - driftB, size.height * 0.29f, 0.46f, 0.18f)
}

private fun DrawScope.drawMountainWorld() {
    val horizon = size.height * 0.50f

    val distant = Path().apply {
        moveTo(0f, horizon + size.height * 0.045f)
        lineTo(size.width * 0.12f, horizon - size.height * 0.035f)
        lineTo(size.width * 0.24f, horizon + size.height * 0.012f)
        lineTo(size.width * 0.37f, horizon - size.height * 0.105f)
        lineTo(size.width * 0.48f, horizon - size.height * 0.018f)
        lineTo(size.width * 0.61f, horizon - size.height * 0.155f)
        lineTo(size.width * 0.73f, horizon - size.height * 0.035f)
        lineTo(size.width * 0.86f, horizon - size.height * 0.11f)
        lineTo(size.width, horizon + size.height * 0.018f)
        lineTo(size.width, horizon + size.height * 0.14f)
        lineTo(0f, horizon + size.height * 0.14f)
        close()
    }
    drawPath(
        distant,
        brush = Brush.verticalGradient(
            listOf(Color(0xFFEAF2F4), Color(0xFF7897A3), Color(0xFF476973)),
            startY = horizon - size.height * 0.17f,
            endY = horizon + size.height * 0.14f
        )
    )

    val snowPeakLeft = Path().apply {
        moveTo(size.width * 0.30f, horizon - size.height * 0.04f)
        lineTo(size.width * 0.37f, horizon - size.height * 0.105f)
        lineTo(size.width * 0.44f, horizon - size.height * 0.035f)
        lineTo(size.width * 0.40f, horizon - size.height * 0.056f)
        lineTo(size.width * 0.37f, horizon - size.height * 0.037f)
        lineTo(size.width * 0.34f, horizon - size.height * 0.061f)
        close()
    }
    drawPath(snowPeakLeft, Color.White.copy(alpha = 0.88f))

    val snowPeakCenter = Path().apply {
        moveTo(size.width * 0.51f, horizon - size.height * 0.055f)
        lineTo(size.width * 0.61f, horizon - size.height * 0.155f)
        lineTo(size.width * 0.70f, horizon - size.height * 0.052f)
        lineTo(size.width * 0.65f, horizon - size.height * 0.082f)
        lineTo(size.width * 0.61f, horizon - size.height * 0.058f)
        lineTo(size.width * 0.57f, horizon - size.height * 0.086f)
        close()
    }
    drawPath(snowPeakCenter, Color.White.copy(alpha = 0.94f))

    val middle = Path().apply {
        moveTo(0f, horizon + size.height * 0.08f)
        lineTo(size.width * 0.15f, horizon + size.height * 0.015f)
        lineTo(size.width * 0.30f, horizon + size.height * 0.072f)
        lineTo(size.width * 0.47f, horizon + size.height * 0.008f)
        lineTo(size.width * 0.63f, horizon + size.height * 0.075f)
        lineTo(size.width * 0.80f, horizon + size.height * 0.012f)
        lineTo(size.width, horizon + size.height * 0.075f)
        lineTo(size.width, horizon + size.height * 0.17f)
        lineTo(0f, horizon + size.height * 0.17f)
        close()
    }
    drawPath(
        middle,
        brush = Brush.verticalGradient(
            listOf(Color(0xFF315B54), Color(0xFF143D35)),
            startY = horizon,
            endY = horizon + size.height * 0.18f
        )
    )

    val near = Path().apply {
        moveTo(0f, horizon + size.height * 0.105f)
        lineTo(size.width * 0.17f, horizon + size.height * 0.06f)
        lineTo(size.width * 0.34f, horizon + size.height * 0.10f)
        lineTo(size.width * 0.52f, horizon + size.height * 0.055f)
        lineTo(size.width * 0.72f, horizon + size.height * 0.11f)
        lineTo(size.width, horizon + size.height * 0.055f)
        lineTo(size.width, horizon + size.height * 0.19f)
        lineTo(0f, horizon + size.height * 0.19f)
        close()
    }
    drawPath(near, Color(0xFF0E332A))
}

private fun DrawScope.drawReferenceLake(shimmer: Float) {
    val lakeTop = size.height * 0.585f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF4B9DB6),
                Color(0xFF167595),
                Color(0xFF0A506F),
                Color(0xFF063548)
            ),
            startY = lakeTop,
            endY = size.height
        ),
        topLeft = Offset(0f, lakeTop),
        size = Size(size.width, size.height - lakeTop)
    )

    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent)
        ),
        topLeft = Offset(0f, lakeTop),
        size = Size(size.width, size.height * 0.18f)
    )

    repeat(25) { index ->
        val phase = shimmer * 6.28318f + index * 0.63f
        val y = lakeTop + size.height * 0.018f + index * size.height * 0.0125f
        val centerX = size.width * (0.49f + sin(phase) * 0.11f)
        val lineWidth = size.width * (0.045f + (index % 6) * 0.022f)
        drawLine(
            color = Color(0xFFE0F7FB).copy(alpha = 0.055f + (index % 4) * 0.027f),
            start = Offset(centerX - lineWidth, y),
            end = Offset(centerX + lineWidth, y),
            strokeWidth = 1.1f + (index % 3) * 0.45f
        )
    }

    repeat(8) { index ->
        val x = size.width * (0.11f + index * 0.11f)
        val reflectionHeight = size.height * (0.045f + (index % 3) * 0.018f)
        drawLine(
            color = Color(0xFF0B463D).copy(alpha = 0.30f),
            start = Offset(x, lakeTop + size.height * 0.006f),
            end = Offset(x + size.width * 0.012f, lakeTop + reflectionHeight),
            strokeWidth = size.width * 0.012f
        )
    }
}

private fun DrawScope.drawShoreForest() {
    val baseY = size.height * 0.605f

    fun tinyPine(x: Float, height: Float, color: Color) {
        val path = Path().apply {
            moveTo(x, baseY - height)
            lineTo(x - height * 0.18f, baseY)
            lineTo(x + height * 0.18f, baseY)
            close()
        }
        drawPath(path, color)
    }

    repeat(24) { index ->
        val x = size.width * (index / 23f)
        val height = size.height * (0.026f + (index % 5) * 0.006f)
        tinyPine(x, height, if (index % 2 == 0) Color(0xFF174A38) else Color(0xFF103C30))
    }
}

private fun DrawScope.drawForegroundPines() {
    fun pine(x: Float, baseY: Float, height: Float, color: Color) {
        drawRect(
            color = Color(0xFF122B22),
            topLeft = Offset(x - height * 0.012f, baseY - height * 0.18f),
            size = Size(height * 0.024f, height * 0.18f)
        )
        repeat(6) { layer ->
            val top = baseY - height + layer * height * 0.13f
            val half = height * (0.075f + layer * 0.020f)
            val path = Path().apply {
                moveTo(x, top)
                lineTo(x - half, top + height * 0.28f)
                lineTo(x + half, top + height * 0.28f)
                close()
            }
            drawPath(path, color)
        }
    }

    val base = size.height * 0.70f
    pine(size.width * 0.035f, base, size.height * 0.21f, Color(0xFF0B392B))
    pine(size.width * 0.105f, base + size.height * 0.012f, size.height * 0.155f, Color(0xFF104331))
    pine(size.width * 0.165f, base + size.height * 0.018f, size.height * 0.12f, Color(0xFF0D382B))
    pine(size.width * 0.965f, base + size.height * 0.008f, size.height * 0.22f, Color(0xFF0A3328))
    pine(size.width * 0.89f, base + size.height * 0.020f, size.height * 0.16f, Color(0xFF10402F))
    pine(size.width * 0.82f, base + size.height * 0.024f, size.height * 0.12f, Color(0xFF133D31))
}
