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
fun NightEnvironment(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "night-environment")

    val cloudShift = transition.animateFloat(
        initialValue = -0.12f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 52000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "night-cloud-shift"
    )

    val waterShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "moon-reflection"
    )

    val starPulse = transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star-pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawNightSky()
        drawStars(starPulse.value)
        drawMoon()
        drawNightClouds(cloudShift.value)
        drawNightMountains()
        drawNightForest()
        drawNightWater(waterShift.value)
        drawMoonReflection(waterShift.value)
        drawNightVignette()
    }
}

private fun DrawScope.drawNightSky() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF071526),
            0.22f to Color(0xFF0A2340),
            0.48f to Color(0xFF12395A),
            0.68f to Color(0xFF0A2B45),
            1f to Color(0xFF04131E)
        )
    )
}

private fun DrawScope.drawStars(pulse: Float) {
    repeat(58) { index ->
        val x = ((index * 0.173f + 0.037f) % 1f) * size.width
        val y = ((index * 0.091f + 0.028f) % 0.46f) * size.height
        val base = 0.34f + (index % 5) * 0.10f
        val alpha = (base * (0.72f + pulse * 0.28f)).coerceAtMost(0.9f)
        val radius = 0.7f + (index % 3) * 0.45f
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawMoon() {
    val center = Offset(size.width * 0.79f, size.height * 0.145f)
    val radius = size.width * 0.066f
    val glow = radius * 3.1f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFDFF1FF).copy(alpha = 0.28f),
                Color(0xFFA9D6F7).copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = center,
            radius = glow
        ),
        radius = glow,
        center = center
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE7F0F6), Color(0xFFC6D5E1)),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.24f),
            radius = radius * 1.25f
        ),
        radius = radius,
        center = center
    )

    drawCircle(
        color = Color(0xFFB8C8D2).copy(alpha = 0.22f),
        radius = radius * 0.18f,
        center = Offset(center.x - radius * 0.25f, center.y + radius * 0.10f)
    )
    drawCircle(
        color = Color(0xFFB8C8D2).copy(alpha = 0.17f),
        radius = radius * 0.11f,
        center = Offset(center.x + radius * 0.28f, center.y - radius * 0.17f)
    )
}

private fun DrawScope.drawNightClouds(progress: Float) {
    val cloudColor = Color(0xFFB6C7D5)
    repeat(4) { layer ->
        val width = size.width * (0.28f + layer * 0.06f)
        val x = ((progress + layer * 0.31f) % 1.35f) * size.width - width * 0.8f
        val y = size.height * (0.12f + layer * 0.072f)
        val alpha = 0.08f + layer * 0.018f

        drawOval(
            color = cloudColor.copy(alpha = alpha),
            topLeft = Offset(x, y),
            size = Size(width, size.height * (0.042f + layer * 0.006f))
        )
        drawOval(
            color = cloudColor.copy(alpha = alpha * 0.78f),
            topLeft = Offset(x + width * 0.29f, y - size.height * 0.010f),
            size = Size(width * 0.53f, size.height * 0.048f)
        )
    }
}

private fun DrawScope.drawNightMountains() {
    val base = size.height * 0.58f
    val back = Path().apply {
        moveTo(0f, base + size.height * 0.07f)
        lineTo(size.width * 0.12f, base - size.height * 0.02f)
        lineTo(size.width * 0.25f, base + size.height * 0.02f)
        lineTo(size.width * 0.40f, base - size.height * 0.09f)
        lineTo(size.width * 0.54f, base + size.height * 0.01f)
        lineTo(size.width * 0.69f, base - size.height * 0.07f)
        lineTo(size.width * 0.84f, base + size.height * 0.025f)
        lineTo(size.width, base - size.height * 0.035f)
        lineTo(size.width, base + size.height * 0.16f)
        lineTo(0f, base + size.height * 0.16f)
        close()
    }
    drawPath(
        back,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF19334A), Color(0xFF0C2232)),
            startY = base - size.height * 0.10f,
            endY = base + size.height * 0.18f
        )
    )

    val near = Path().apply {
        moveTo(0f, base + size.height * 0.10f)
        lineTo(size.width * 0.14f, base + size.height * 0.02f)
        lineTo(size.width * 0.28f, base + size.height * 0.075f)
        lineTo(size.width * 0.43f, base + size.height * 0.005f)
        lineTo(size.width * 0.57f, base + size.height * 0.08f)
        lineTo(size.width * 0.73f, base + size.height * 0.012f)
        lineTo(size.width * 0.88f, base + size.height * 0.06f)
        lineTo(size.width, base + size.height * 0.015f)
        lineTo(size.width, base + size.height * 0.19f)
        lineTo(0f, base + size.height * 0.19f)
        close()
    }
    drawPath(near, Color(0xFF081D28).copy(alpha = 0.98f))
}

private fun DrawScope.drawNightForest() {
    val ground = size.height * 0.69f
    repeat(31) { index ->
        val x = size.width * (index / 30f)
        val h = size.height * (0.055f + (index % 6) * 0.012f)
        val top = ground - h
        val trunk = Color(0xFF061419)
        val foliage = if (index % 2 == 0) Color(0xFF0A2328) else Color(0xFF0B2930)

        drawLine(
            color = trunk,
            start = Offset(x, ground + size.height * 0.02f),
            end = Offset(x, top + h * 0.22f),
            strokeWidth = 1.6f
        )
        repeat(4) { tier ->
            val tierY = top + h * (0.18f + tier * 0.18f)
            val half = h * (0.10f + tier * 0.045f)
            val tri = Path().apply {
                moveTo(x, tierY - h * 0.18f)
                lineTo(x - half, tierY + h * 0.10f)
                lineTo(x + half, tierY + h * 0.10f)
                close()
            }
            drawPath(tri, foliage.copy(alpha = 0.98f - tier * 0.07f))
        }
    }
}

private fun DrawScope.drawNightWater(progress: Float) {
    val top = size.height * 0.705f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0C3145), Color(0xFF082536), Color(0xFF03131D)),
            startY = top,
            endY = size.height
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top)
    )

    repeat(26) { index ->
        val phase = progress * 6.28318f + index * 0.51f
        val y = top + size.height * (0.012f + index * 0.009f)
        val centerX = size.width * (0.49f + sin(phase) * 0.14f)
        val width = size.width * (0.025f + (index % 7) * 0.016f)
        drawLine(
            color = Color(0xFF6AA5C5).copy(alpha = 0.08f),
            start = Offset(centerX - width, y),
            end = Offset(centerX + width, y),
            strokeWidth = 1.3f
        )
    }
}

private fun DrawScope.drawMoonReflection(progress: Float) {
    val top = size.height * 0.705f
    val moonX = size.width * 0.79f
    repeat(20) { index ->
        val y = top + size.height * (0.010f + index * 0.011f)
        val wobble = sin(progress * 6.28318f + index * 0.72f) * size.width * 0.014f
        val half = size.width * (0.018f + (index % 5) * 0.010f)
        val alpha = (0.24f - index * 0.008f).coerceAtLeast(0.055f)
        drawLine(
            color = Color(0xFFD8EFFF).copy(alpha = alpha),
            start = Offset(moonX + wobble - half, y),
            end = Offset(moonX + wobble + half, y),
            strokeWidth = 1.6f
        )
    }
}

private fun DrawScope.drawNightVignette() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Transparent, Color(0xAA031018), Color(0xE8010A0F)),
            startY = size.height * 0.45f,
            endY = size.height
        )
    )
}
