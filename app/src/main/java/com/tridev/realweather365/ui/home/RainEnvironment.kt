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
fun RainEnvironment(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "rain-environment")
    val rainFlow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain-flow"
    )
    val cloudShift = transition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 42000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain-cloud-shift"
    )
    val reflectionShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wet-road-reflection"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRainSky()
        drawRainClouds(cloudShift.value)
        drawRainCity()
        drawWetStreet(reflectionShift.value)
        drawStreetLights()
        drawRainTree()
        drawRainLayers(rainFlow.value)
    }
}

private fun DrawScope.drawRainSky() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF192937),
            0.28f to Color(0xFF304653),
            0.56f to Color(0xFF53636A),
            0.74f to Color(0xFF37484F),
            1f to Color(0xFF10242D)
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFB6D0D5).copy(alpha = 0.11f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.52f, size.height * 0.38f),
            radius = size.width * 0.75f
        )
    )
}

private fun DrawScope.drawRainClouds(progress: Float) {
    val baseY = size.height * 0.13f
    repeat(5) { layer ->
        val width = size.width * (0.42f + layer * 0.07f)
        val x = (((progress + layer * 0.23f) % 1.35f) * (size.width + width)) - width
        val y = baseY + layer * size.height * 0.052f
        val alpha = 0.26f - layer * 0.025f
        val cloudColor = Color(0xFF172934).copy(alpha = alpha.coerceAtLeast(0.10f))

        drawOval(
            color = cloudColor,
            topLeft = Offset(x, y),
            size = Size(width, size.height * (0.075f + layer * 0.006f))
        )
        drawCircle(
            color = cloudColor,
            radius = size.width * (0.08f + layer * 0.008f),
            center = Offset(x + width * 0.33f, y + size.height * 0.015f)
        )
        drawCircle(
            color = cloudColor.copy(alpha = alpha * 0.9f),
            radius = size.width * (0.10f + layer * 0.006f),
            center = Offset(x + width * 0.58f, y - size.height * 0.004f)
        )
    }
}

private fun DrawScope.drawRainCity() {
    val horizon = size.height * 0.47f
    val buildingColor = Color(0xFF13242C)
    val nearBuilding = Color(0xFF0C1B22)

    val skyline = listOf(
        0.00f to 0.13f,
        0.10f to 0.18f,
        0.21f to 0.12f,
        0.31f to 0.22f,
        0.47f to 0.14f,
        0.57f to 0.19f,
        0.72f to 0.15f,
        0.84f to 0.23f,
        0.94f to 0.16f
    )

    skyline.forEachIndexed { index, (xRatio, heightRatio) ->
        val x = size.width * xRatio
        val width = size.width * if (index % 2 == 0) 0.12f else 0.10f
        val height = size.height * heightRatio
        drawRect(
            color = if (index % 3 == 0) nearBuilding else buildingColor,
            topLeft = Offset(x, horizon - height),
            size = Size(width, height + size.height * 0.11f)
        )

        repeat(4) { row ->
            repeat(2) { col ->
                if ((row + col + index) % 3 == 0) {
                    val wx = x + width * (0.20f + col * 0.46f)
                    val wy = horizon - height + size.height * (0.028f + row * 0.032f)
                    drawRect(
                        color = Color(0xFFFFD58A).copy(alpha = 0.48f),
                        topLeft = Offset(wx, wy),
                        size = Size(width * 0.10f, size.height * 0.010f)
                    )
                }
            }
        }
    }

    // Temple-like silhouette to keep the Chandauli monsoon scene distinctive.
    val templeX = size.width * 0.49f
    val templeBase = horizon + size.height * 0.05f
    val temple = Path().apply {
        moveTo(templeX - size.width * 0.055f, templeBase)
        lineTo(templeX - size.width * 0.045f, horizon - size.height * 0.08f)
        lineTo(templeX - size.width * 0.022f, horizon - size.height * 0.08f)
        lineTo(templeX, horizon - size.height * 0.18f)
        lineTo(templeX + size.width * 0.022f, horizon - size.height * 0.08f)
        lineTo(templeX + size.width * 0.045f, horizon - size.height * 0.08f)
        lineTo(templeX + size.width * 0.055f, templeBase)
        close()
    }
    drawPath(temple, Color(0xFF0E2028))
    drawCircle(
        color = Color(0xFFFFC86B).copy(alpha = 0.52f),
        radius = size.width * 0.010f,
        center = Offset(templeX, horizon - size.height * 0.035f)
    )
}

private fun DrawScope.drawWetStreet(reflection: Float) {
    val top = size.height * 0.52f
    val road = Path().apply {
        moveTo(size.width * 0.28f, top)
        lineTo(size.width * 0.72f, top)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(
        road,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF283A40), Color(0xFF10242B), Color(0xFF07171D)),
            startY = top,
            endY = size.height
        )
    )

    repeat(18) { index ->
        val laneY = top + (index / 18f) * (size.height - top)
        val perspective = ((laneY - top) / (size.height - top)).coerceIn(0f, 1f)
        val phase = reflection * 6.28318f + index * 0.74f
        val centerX = size.width * (0.5f + sin(phase) * (0.05f + perspective * 0.05f))
        val half = size.width * (0.025f + perspective * 0.14f)
        drawLine(
            color = Color(0xFF9EDAE4).copy(alpha = 0.06f + perspective * 0.11f),
            start = Offset(centerX - half, laneY),
            end = Offset(centerX + half, laneY),
            strokeWidth = 1.2f + perspective * 2.4f
        )
    }

    // Warm lamp reflections on the wet road.
    listOf(0.28f, 0.72f).forEach { xRatio ->
        val x = size.width * xRatio
        repeat(9) { index ->
            val y = top + size.height * (0.035f + index * 0.038f)
            val width = size.width * (0.018f + index * 0.009f)
            drawLine(
                color = Color(0xFFFFB963).copy(alpha = 0.24f - index * 0.018f),
                start = Offset(x - width, y),
                end = Offset(x + width, y),
                strokeWidth = 2.2f
            )
        }
    }
}

private fun DrawScope.drawStreetLights() {
    val roadTop = size.height * 0.50f
    listOf(0.26f, 0.74f).forEach { xRatio ->
        val x = size.width * xRatio
        val top = roadTop - size.height * 0.08f
        val bottom = roadTop + size.height * 0.12f
        drawLine(
            color = Color(0xFF10191D),
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = size.width * 0.008f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE1A0).copy(alpha = 0.82f),
                    Color(0xFFFFB24A).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(x, top),
                radius = size.width * 0.11f
            ),
            radius = size.width * 0.11f,
            center = Offset(x, top)
        )
        drawCircle(
            color = Color(0xFFFFD17C),
            radius = size.width * 0.012f,
            center = Offset(x, top)
        )
    }
}

private fun DrawScope.drawRainTree() {
    val baseX = size.width * 0.94f
    val baseY = size.height * 0.66f
    drawLine(
        color = Color(0xFF071713),
        start = Offset(baseX, baseY),
        end = Offset(size.width * 0.86f, size.height * 0.26f),
        strokeWidth = size.width * 0.028f
    )

    repeat(18) { index ->
        val angleWave = sin(index * 1.37f)
        val cx = size.width * (0.74f + (index % 5) * 0.055f)
        val cy = size.height * (0.20f + (index / 5) * 0.055f + angleWave * 0.012f)
        drawCircle(
            color = Color(0xFF0B2B22).copy(alpha = 0.95f),
            radius = size.width * (0.060f + (index % 3) * 0.012f),
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawRainLayers(progress: Float) {
    // Far rain: thin, soft and dense.
    repeat(78) { index ->
        val seedX = ((index * 37) % 101) / 101f
        val seedY = ((index * 53) % 113) / 113f
        val y = ((seedY + progress * 0.78f) % 1.08f) * size.height
        val x = seedX * size.width + progress * size.width * 0.035f
        val length = size.height * (0.020f + (index % 4) * 0.004f)
        drawLine(
            color = Color(0xFFC6E8F0).copy(alpha = 0.20f + (index % 3) * 0.035f),
            start = Offset(x, y),
            end = Offset(x - size.width * 0.012f, y + length),
            strokeWidth = 1.0f
        )
    }

    // Near rain: fewer, brighter drops for depth.
    repeat(26) { index ->
        val seedX = ((index * 61 + 17) % 97) / 97f
        val seedY = ((index * 47 + 9) % 109) / 109f
        val y = ((seedY + progress * 1.14f) % 1.10f) * size.height
        val x = seedX * size.width + progress * size.width * 0.05f
        val length = size.height * (0.032f + (index % 5) * 0.004f)
        drawLine(
            color = Color(0xFFE2F7FA).copy(alpha = 0.36f + (index % 2) * 0.08f),
            start = Offset(x, y),
            end = Offset(x - size.width * 0.018f, y + length),
            strokeWidth = 1.6f
        )
    }

    // Small road splashes.
    repeat(14) { index ->
        val x = size.width * (((index * 19) % 89) / 89f)
        val y = size.height * (0.64f + ((index * 11) % 29) / 100f)
        val pulse = ((progress + index * 0.13f) % 1f)
        drawOval(
            color = Color(0xFFBDE8EF).copy(alpha = (0.16f * (1f - pulse)).coerceAtLeast(0f)),
            topLeft = Offset(x - size.width * 0.018f * pulse, y),
            size = Size(size.width * 0.036f * pulse, size.height * 0.005f * pulse)
        )
    }
}
