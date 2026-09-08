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
    val transition = rememberInfiniteTransition(label = "sunny-environment")
    val cloudShift = transition.animateFloat(
        initialValue = -0.08f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 65000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud-shift"
    )
    val shimmer = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "water-shimmer"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawSky()
        drawSun()
        drawClouds(cloudShift.value)
        drawMountains()
        drawLake(shimmer.value)
        drawPines()
    }
}

private fun DrawScope.drawSky() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF168FE2),
            0.42f to Color(0xFF72C8F5),
            0.72f to Color(0xFFC7EAF8),
            1f to Color(0xFF4D91B9)
        )
    )
}

private fun DrawScope.drawSun() {
    val center = Offset(size.width * 0.20f, size.height * 0.18f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFBE8),
                Color(0xFFFFF2A5).copy(alpha = 0.86f),
                Color.Transparent
            ),
            center = center,
            radius = size.width * 0.28f
        ),
        radius = size.width * 0.28f,
        center = center
    )
    drawCircle(Color(0xFFFFF8D3), radius = size.width * 0.042f, center = center)
    repeat(12) { index ->
        rotate(index * 30f, pivot = center) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.42f),
                topLeft = Offset(center.x - 1.5f, center.y - size.width * 0.11f),
                size = Size(3f, size.width * 0.055f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
            )
        }
    }
}

private fun DrawScope.drawClouds(progress: Float) {
    val x = (progress * size.width) - size.width * 0.16f
    val y = size.height * 0.15f
    val cloud = Color.White.copy(alpha = 0.34f)
    drawOval(cloud, topLeft = Offset(x, y), size = Size(size.width * 0.23f, size.height * 0.035f))
    drawCircle(cloud, radius = size.width * 0.045f, center = Offset(x + size.width * 0.07f, y))
    drawCircle(cloud, radius = size.width * 0.055f, center = Offset(x + size.width * 0.13f, y - size.height * 0.006f))
}

private fun DrawScope.drawMountains() {
    val horizon = size.height * 0.48f

    val far = Path().apply {
        moveTo(0f, horizon + size.height * 0.05f)
        lineTo(size.width * 0.16f, horizon - size.height * 0.055f)
        lineTo(size.width * 0.29f, horizon + size.height * 0.01f)
        lineTo(size.width * 0.46f, horizon - size.height * 0.13f)
        lineTo(size.width * 0.61f, horizon + size.height * 0.015f)
        lineTo(size.width * 0.78f, horizon - size.height * 0.095f)
        lineTo(size.width, horizon + size.height * 0.03f)
        lineTo(size.width, horizon + size.height * 0.14f)
        lineTo(0f, horizon + size.height * 0.14f)
        close()
    }
    drawPath(
        far,
        brush = Brush.verticalGradient(
            listOf(Color(0xFFDDE8EC), Color(0xFF6F8C97)),
            startY = horizon - size.height * 0.15f,
            endY = horizon + size.height * 0.14f
        )
    )

    val snow = Path().apply {
        moveTo(size.width * 0.37f, horizon - size.height * 0.06f)
        lineTo(size.width * 0.46f, horizon - size.height * 0.13f)
        lineTo(size.width * 0.54f, horizon - size.height * 0.055f)
        lineTo(size.width * 0.49f, horizon - size.height * 0.075f)
        lineTo(size.width * 0.46f, horizon - size.height * 0.045f)
        lineTo(size.width * 0.42f, horizon - size.height * 0.078f)
        close()
    }
    drawPath(snow, Color.White.copy(alpha = 0.92f))

    val near = Path().apply {
        moveTo(0f, horizon + size.height * 0.07f)
        lineTo(size.width * 0.18f, horizon + size.height * 0.01f)
        lineTo(size.width * 0.35f, horizon + size.height * 0.07f)
        lineTo(size.width * 0.58f, horizon + size.height * 0.025f)
        lineTo(size.width * 0.76f, horizon + size.height * 0.075f)
        lineTo(size.width, horizon + size.height * 0.01f)
        lineTo(size.width, horizon + size.height * 0.18f)
        lineTo(0f, horizon + size.height * 0.18f)
        close()
    }
    drawPath(near, Color(0xFF214F45))
}

private fun DrawScope.drawLake(shimmer: Float) {
    val lakeTop = size.height * 0.57f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF2C83A8), Color(0xFF0C587B), Color(0xFF073B56)),
            startY = lakeTop,
            endY = size.height
        ),
        topLeft = Offset(0f, lakeTop),
        size = Size(size.width, size.height - lakeTop)
    )

    val baseY = lakeTop + size.height * 0.05f
    repeat(18) { index ->
        val phase = shimmer * 6.28318f + index * 0.72f
        val y = baseY + index * size.height * 0.019f
        val centerX = size.width * (0.48f + sin(phase) * 0.07f)
        val lineWidth = size.width * (0.06f + (index % 5) * 0.025f)
        drawLine(
            color = Color(0xFFBDE9F5).copy(alpha = 0.08f + (index % 3) * 0.035f),
            start = Offset(centerX - lineWidth, y),
            end = Offset(centerX + lineWidth, y),
            strokeWidth = 1.5f + (index % 2)
        )
    }
}

private fun DrawScope.drawPines() {
    fun pine(x: Float, baseY: Float, height: Float, color: Color) {
        drawRect(
            color = Color(0xFF173226),
            topLeft = Offset(x - height * 0.018f, baseY - height * 0.18f),
            size = Size(height * 0.036f, height * 0.18f)
        )
        repeat(4) { layer ->
            val top = baseY - height + layer * height * 0.18f
            val half = height * (0.12f + layer * 0.035f)
            val path = Path().apply {
                moveTo(x, top)
                lineTo(x - half, top + height * 0.34f)
                lineTo(x + half, top + height * 0.34f)
                close()
            }
            drawPath(path, color)
        }
    }

    val base = size.height * 0.67f
    pine(size.width * 0.06f, base, size.height * 0.18f, Color(0xFF133F31))
    pine(size.width * 0.13f, base + size.height * 0.015f, size.height * 0.14f, Color(0xFF0F382B))
    pine(size.width * 0.91f, base + size.height * 0.02f, size.height * 0.17f, Color(0xFF12372C))
    pine(size.width * 0.82f, base + size.height * 0.025f, size.height * 0.13f, Color(0xFF173E32))
}
