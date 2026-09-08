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
import kotlin.math.sin

@Composable
fun SnowEnvironment(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "snow-environment")

    val farSnow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "far-snow"
    )

    val nearSnow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "near-snow"
    )

    val mistShift = transition.animateFloat(
        initialValue = -0.18f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 38000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "winter-mist"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawWinterSky()
        drawWinterClouds(mistShift.value)
        drawDistantSnowMountains()
        drawMidSnowRange()
        drawWinterMist(mistShift.value)
        drawSnowPineForest()
        drawSnowGround()
        drawFarSnow(farSnow.value)
        drawNearSnow(nearSnow.value)
        drawColdVignette()
    }
}

private fun DrawScope.drawWinterSky() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF6F91AE),
            0.20f to Color(0xFF8DA9BE),
            0.43f to Color(0xFFB9CBD8),
            0.63f to Color(0xFFD9E5EC),
            1f to Color(0xFF718B9C)
        )
    )
}

private fun DrawScope.drawWinterClouds(progress: Float) {
    repeat(4) { index ->
        val width = size.width * (0.32f + index * 0.05f)
        val x = ((progress + index * 0.29f) % 1.36f) * size.width - width * 0.72f
        val y = size.height * (0.08f + (index % 2) * 0.07f)
        val color = Color(0xFFE8F0F5).copy(alpha = 0.24f - index * 0.025f)

        drawOval(
            color = color,
            topLeft = Offset(x, y),
            size = Size(width, size.height * (0.045f + index * 0.006f))
        )
        drawCircle(
            color = color,
            radius = width * 0.13f,
            center = Offset(x + width * 0.34f, y + size.height * 0.003f)
        )
        drawCircle(
            color = color.copy(alpha = color.alpha * 0.92f),
            radius = width * 0.16f,
            center = Offset(x + width * 0.58f, y - size.height * 0.008f)
        )
    }
}

private fun DrawScope.drawDistantSnowMountains() {
    val base = size.height * 0.55f
    val mountain = Path().apply {
        moveTo(0f, base + size.height * 0.04f)
        lineTo(size.width * 0.11f, base - size.height * 0.03f)
        lineTo(size.width * 0.20f, base + size.height * 0.01f)
        lineTo(size.width * 0.34f, base - size.height * 0.17f)
        lineTo(size.width * 0.43f, base - size.height * 0.05f)
        lineTo(size.width * 0.54f, base - size.height * 0.22f)
        lineTo(size.width * 0.65f, base - size.height * 0.07f)
        lineTo(size.width * 0.78f, base - size.height * 0.18f)
        lineTo(size.width * 0.90f, base - size.height * 0.02f)
        lineTo(size.width, base - size.height * 0.10f)
        lineTo(size.width, base + size.height * 0.16f)
        lineTo(0f, base + size.height * 0.16f)
        close()
    }

    drawPath(
        path = mountain,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFF2F7FA), Color(0xFFCAD7E0), Color(0xFF8AA0AF)),
            startY = base - size.height * 0.23f,
            endY = base + size.height * 0.15f
        )
    )

    val shadowFaces = listOf(
        listOf(0.34f to -0.17f, 0.43f to -0.05f, 0.39f to 0.05f),
        listOf(0.54f to -0.22f, 0.65f to -0.07f, 0.59f to 0.04f),
        listOf(0.78f to -0.18f, 0.90f to -0.02f, 0.83f to 0.05f)
    )

    shadowFaces.forEach { pts ->
        val face = Path().apply {
            moveTo(size.width * pts[0].first, base + size.height * pts[0].second)
            lineTo(size.width * pts[1].first, base + size.height * pts[1].second)
            lineTo(size.width * pts[2].first, base + size.height * pts[2].second)
            close()
        }
        drawPath(face, Color(0xFF7993A5).copy(alpha = 0.34f))
    }
}

private fun DrawScope.drawMidSnowRange() {
    val top = size.height * 0.53f
    val ridge = Path().apply {
        moveTo(0f, top + size.height * 0.09f)
        lineTo(size.width * 0.14f, top + size.height * 0.015f)
        lineTo(size.width * 0.28f, top + size.height * 0.065f)
        lineTo(size.width * 0.42f, top - size.height * 0.015f)
        lineTo(size.width * 0.55f, top + size.height * 0.075f)
        lineTo(size.width * 0.70f, top + size.height * 0.005f)
        lineTo(size.width * 0.84f, top + size.height * 0.07f)
        lineTo(size.width, top + size.height * 0.015f)
        lineTo(size.width, top + size.height * 0.18f)
        lineTo(0f, top + size.height * 0.18f)
        close()
    }

    drawPath(
        ridge,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFC9D6DE), Color(0xFF879EAB), Color(0xFF607988)),
            startY = top - size.height * 0.02f,
            endY = top + size.height * 0.18f
        )
    )
}

private fun DrawScope.drawWinterMist(progress: Float) {
    val mistY = size.height * 0.57f
    repeat(5) { index ->
        val width = size.width * (0.35f + index * 0.08f)
        val x = ((progress + index * 0.22f) % 1.40f) * size.width - width * 0.65f
        val y = mistY + index * size.height * 0.016f
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFF1F5F8).copy(alpha = 0.18f - index * 0.018f),
                    Color.Transparent
                ),
                startX = x,
                endX = x + width
            ),
            topLeft = Offset(x, y),
            size = Size(width, size.height * 0.034f)
        )
    }
}

private fun DrawScope.drawSnowPineForest() {
    val ground = size.height * 0.72f
    repeat(24) { index ->
        val x = size.width * (index / 23f)
        val depth = when (index % 3) {
            0 -> 0.14f
            1 -> 0.10f
            else -> 0.075f
        }
        val h = size.height * depth
        val y = ground + sin(index * 1.23f) * size.height * 0.016f
        val trunkColor = Color(0xFF344B55)
        val pineColor = when (index % 4) {
            0 -> Color(0xFF345461)
            1 -> Color(0xFF2D4954)
            else -> Color(0xFF3B5C67)
        }

        drawLine(
            color = trunkColor,
            start = Offset(x, y),
            end = Offset(x, y - h * 0.92f),
            strokeWidth = 2f
        )

        repeat(4) { tier ->
            val tierY = y - h * (0.22f + tier * 0.18f)
            val half = h * (0.23f - tier * 0.035f)
            val crown = Path().apply {
                moveTo(x, tierY - h * 0.18f)
                lineTo(x - half, tierY + h * 0.12f)
                lineTo(x + half, tierY + h * 0.12f)
                close()
            }
            drawPath(crown, pineColor)
            drawLine(
                color = Color.White.copy(alpha = 0.72f),
                start = Offset(x - half * 0.70f, tierY + h * 0.07f),
                end = Offset(x + half * 0.18f, tierY + h * 0.01f),
                strokeWidth = 2.2f
            )
        }
    }
}

private fun DrawScope.drawSnowGround() {
    val top = size.height * 0.70f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE6EEF3), Color(0xFFBCCDD7), Color(0xFF8DA4B1)),
            startY = top,
            endY = size.height
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top)
    )

    repeat(11) { index ->
        val y = top + size.height * (0.018f + index * 0.024f)
        val center = size.width * (0.47f + sin(index * 0.83f) * 0.12f)
        val half = size.width * (0.10f + index * 0.014f)
        drawLine(
            color = Color.White.copy(alpha = 0.16f),
            start = Offset(center - half, y),
            end = Offset(center + half, y),
            strokeWidth = 1.5f
        )
    }
}

private fun DrawScope.drawFarSnow(progress: Float) {
    repeat(75) { index ->
        val x = ((index * 0.173f + 0.07f + sin(index * 0.91f) * 0.03f) % 1f) * size.width
        val y = ((index * 0.091f + progress) % 1f) * size.height
        val radius = 1.2f + (index % 3) * 0.55f
        drawCircle(
            color = Color.White.copy(alpha = 0.42f + (index % 4) * 0.07f),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawNearSnow(progress: Float) {
    repeat(30) { index ->
        val sway = sin(progress * 6.28318f + index * 0.77f) * size.width * 0.025f
        val x = ((index * 0.237f + 0.11f) % 1f) * size.width + sway
        val y = ((index * 0.137f + progress * 1.15f) % 1f) * size.height
        val radius = 2.6f + (index % 4) * 1.05f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.08f)),
                center = Offset(x, y),
                radius = radius * 2.2f
            ),
            radius = radius * 2.2f,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.82f),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawColdVignette() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x1607192A), Color.Transparent, Color(0x56041722)),
            startY = 0f,
            endY = size.height
        )
    )
}
