package com.tridev.realweather365.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
fun ThunderstormEnvironment(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thunderstorm-environment")

    val rainShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "storm-rain"
    )

    val cloudShift = transition.animateFloat(
        initialValue = -0.08f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "storm-clouds"
    )

    val flash = transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5600
                0f at 0
                0f at 3380
                1f at 3440
                0.10f at 3510
                0.82f at 3570
                0f at 3660
                0f at 5600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "lightning-flash"
    )

    val waterPulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "storm-water"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawStormSky(flash.value)
        drawStormCloudDeck(cloudShift.value, flash.value)
        drawStormHorizonGlow(flash.value)
        drawStormCity()
        drawLightningBolt(flash.value)
        drawStormWater(waterPulse.value, flash.value)
        drawHeavyRain(rainShift.value)
        drawForegroundRain(rainShift.value)
        drawFlashWash(flash.value)
    }
}

private fun DrawScope.drawStormSky(flash: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF07131F),
            0.24f to Color(0xFF10283C),
            0.48f to Color(0xFF19364D),
            0.68f to Color(0xFF112B3E),
            1f to Color(0xFF06141E)
        )
    )

    if (flash > 0f) {
        drawRect(Color(0xFF9CC8F5).copy(alpha = flash * 0.16f))
    }
}

private fun DrawScope.drawStormCloudDeck(progress: Float, flash: Float) {
    val layers = listOf(
        Triple(0.04f, 0.13f, Color(0xFF172839)),
        Triple(0.12f, 0.19f, Color(0xFF1C3348)),
        Triple(0.22f, 0.26f, Color(0xFF132536))
    )

    layers.forEachIndexed { index, (yRatio, widthRatio, baseColor) ->
        val shift = ((progress + index * 0.27f) % 1.28f) * size.width
        repeat(4) { cloudIndex ->
            val cloudWidth = size.width * (widthRatio + cloudIndex * 0.035f)
            val x = shift - size.width * 0.34f + cloudIndex * size.width * 0.29f
            val y = size.height * (yRatio + (cloudIndex % 2) * 0.035f)
            val cloudColor = baseColor.copy(alpha = 0.86f + flash * 0.08f)

            drawOval(
                color = cloudColor,
                topLeft = Offset(x, y),
                size = Size(cloudWidth, size.height * (0.075f + index * 0.018f))
            )
            drawCircle(
                color = cloudColor,
                radius = cloudWidth * 0.20f,
                center = Offset(x + cloudWidth * 0.31f, y + size.height * 0.010f)
            )
            drawCircle(
                color = cloudColor,
                radius = cloudWidth * 0.24f,
                center = Offset(x + cloudWidth * 0.58f, y - size.height * 0.008f)
            )
        }
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xAA08131D), Color.Transparent),
            startY = size.height * 0.24f,
            endY = size.height * 0.46f
        )
    )
}

private fun DrawScope.drawStormHorizonGlow(flash: Float) {
    val horizon = size.height * 0.48f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF6A88A0).copy(alpha = 0.10f + flash * 0.18f),
                Color.Transparent
            ),
            startY = horizon - size.height * 0.09f,
            endY = horizon + size.height * 0.10f
        )
    )
}

private fun DrawScope.drawStormCity() {
    val ground = size.height * 0.61f
    val buildings = listOf(
        0.01f to 0.16f,
        0.09f to 0.23f,
        0.18f to 0.14f,
        0.26f to 0.27f,
        0.36f to 0.19f,
        0.45f to 0.31f,
        0.57f to 0.21f,
        0.67f to 0.28f,
        0.78f to 0.18f,
        0.87f to 0.25f,
        0.95f to 0.16f
    )

    buildings.forEachIndexed { index, (xRatio, heightRatio) ->
        val width = size.width * (0.075f + (index % 3) * 0.016f)
        val height = size.height * heightRatio
        val left = size.width * xRatio
        val top = ground - height

        drawRect(
            color = if (index % 2 == 0) Color(0xFF09151D) else Color(0xFF0B1B24),
            topLeft = Offset(left, top),
            size = Size(width, height)
        )

        val cols = 3
        val rows = 7
        repeat(rows) { row ->
            repeat(cols) { col ->
                if ((row + col + index) % 3 == 0) {
                    val wx = left + width * (0.16f + col * 0.27f)
                    val wy = top + height * (0.13f + row * 0.105f)
                    drawRect(
                        color = Color(0xFFFFC96B).copy(alpha = 0.55f),
                        topLeft = Offset(wx, wy),
                        size = Size(width * 0.07f, height * 0.025f)
                    )
                }
            }
        }
    }

    drawRect(
        color = Color(0xFF07141B),
        topLeft = Offset(0f, ground),
        size = Size(size.width, size.height * 0.07f)
    )
}

private fun DrawScope.drawLightningBolt(flash: Float) {
    if (flash <= 0.04f) return

    val start = Offset(size.width * 0.57f, size.height * 0.18f)
    val bolt = Path().apply {
        moveTo(start.x, start.y)
        lineTo(size.width * 0.52f, size.height * 0.30f)
        lineTo(size.width * 0.58f, size.height * 0.29f)
        lineTo(size.width * 0.49f, size.height * 0.44f)
        lineTo(size.width * 0.55f, size.height * 0.36f)
        lineTo(size.width * 0.50f, size.height * 0.37f)
        close()
    }

    drawPath(
        path = bolt,
        color = Color.White.copy(alpha = 0.90f * flash)
    )

    drawLine(
        color = Color(0xFF9CCBFF).copy(alpha = 0.38f * flash),
        start = Offset(size.width * 0.54f, size.height * 0.20f),
        end = Offset(size.width * 0.49f, size.height * 0.43f),
        strokeWidth = 8f
    )
}

private fun DrawScope.drawStormWater(progress: Float, flash: Float) {
    val waterTop = size.height * 0.64f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF0B2939), Color(0xFF071E2A), Color(0xFF031018)),
            startY = waterTop,
            endY = size.height
        ),
        topLeft = Offset(0f, waterTop),
        size = Size(size.width, size.height - waterTop)
    )

    repeat(23) { index ->
        val phase = progress * 6.28318f + index * 0.61f
        val y = waterTop + size.height * (0.018f + index * 0.012f)
        val centerX = size.width * (0.54f + sin(phase) * 0.13f)
        val width = size.width * (0.035f + (index % 6) * 0.022f)
        drawLine(
            color = Color(0xFF92BFD5).copy(alpha = 0.08f + flash * 0.07f),
            start = Offset(centerX - width, y),
            end = Offset(centerX + width, y),
            strokeWidth = 1.4f + (index % 2)
        )
    }

    repeat(9) { index ->
        val x = size.width * (0.08f + index * 0.105f)
        val reflectionHeight = size.height * (0.05f + (index % 4) * 0.018f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFC86A).copy(alpha = 0.18f), Color.Transparent),
                startY = waterTop,
                endY = waterTop + reflectionHeight
            ),
            topLeft = Offset(x, waterTop),
            size = Size(size.width * 0.012f, reflectionHeight)
        )
    }
}

private fun DrawScope.drawHeavyRain(shift: Float) {
    repeat(74) { index ->
        val x = ((index * 0.137f + 0.04f) % 1f) * size.width
        val y = ((index * 0.071f + shift) % 1f) * size.height
        val length = size.height * (0.020f + (index % 4) * 0.005f)
        drawLine(
            color = Color(0xFFB5D8E7).copy(alpha = 0.24f),
            start = Offset(x, y),
            end = Offset(x - size.width * 0.014f, y + length),
            strokeWidth = 1.1f
        )
    }
}

private fun DrawScope.drawForegroundRain(shift: Float) {
    repeat(22) { index ->
        val x = ((index * 0.217f + 0.12f) % 1f) * size.width
        val y = ((index * 0.113f + shift * 1.7f) % 1f) * size.height
        val length = size.height * (0.045f + (index % 3) * 0.012f)
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(x, y),
            end = Offset(x - size.width * 0.026f, y + length),
            strokeWidth = 2.1f
        )
    }
}

private fun DrawScope.drawFlashWash(flash: Float) {
    if (flash > 0f) {
        drawRect(Color(0xFFD7E8FF).copy(alpha = flash * 0.08f))
    }
}
