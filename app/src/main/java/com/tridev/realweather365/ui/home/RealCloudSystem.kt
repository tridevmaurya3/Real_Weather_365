package com.tridev.realweather365.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class CloudAtmosphereStyle {
    FAIR,
    OVERCAST,
    RAIN,
    STORM,
    SNOW,
    NIGHT
}

@Composable
fun RealCloudSystem(
    cloudCover: Int,
    windSpeed: Int,
    windDirection: Int,
    condition: String,
    scene: WeatherScene,
    solar: SolarVisualState,
    moon: MoonVisualState,
    animationLevel: Int,
    modifier: Modifier = Modifier
) {
    val targetCover = cloudCover.coerceIn(0, 100).toFloat()
    val cover = animateFloatAsState(
        targetValue = targetCover,
        animationSpec = tween(durationMillis = 2_400),
        label = "physical-cloud-coverage"
    ).value
    if (cover <= 0.5f && scene !in setOf(WeatherScene.RAIN, WeatherScene.THUNDERSTORM, WeatherScene.SNOW)) return

    val style = cloudStyle(scene, condition)
    val motionFactor = (0.38f + windSpeed.coerceIn(0, 95) / 28f).coerceIn(0.38f, 3.5f)
    val transition = rememberInfiniteTransition(label = "real-cloud-system-2")
    val highProgress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cloudDuration(132_000, motionFactor), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "high-cloud-flow"
    )
    val midProgress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cloudDuration(92_000, motionFactor), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mid-cloud-flow"
    )
    val lowProgress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cloudDuration(58_000, motionFactor), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "low-cloud-flow"
    )

    val staticMode = animationLevel <= 0
    val quality = when {
        animationLevel >= 2 -> 1f
        animationLevel == 1 -> 0.78f
        else -> 0.58f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val destination = ((windDirection + 180) % 360 + 360) % 360
        val radians = destination / 180.0 * PI
        val flowX = sin(radians).toFloat()
        val flowY = (-cos(radians)).toFloat()
        val moonLight = if (moon.aboveHorizon) moon.illuminationFraction.toFloat() else 0f
        val dayLight = solar.daylight
        val warm = solar.warmth

        val high = if (staticMode) 0.23f else highProgress.value
        val mid = if (staticMode) 0.47f else midProgress.value
        val low = if (staticMode) 0.71f else lowProgress.value

        drawHighCloudLayer(
            cover = cover,
            style = style,
            progress = high,
            flowX = flowX,
            flowY = flowY,
            daylight = dayLight,
            warmth = warm,
            moonLight = moonLight,
            quality = quality
        )
        drawMidCloudLayer(
            cover = cover,
            style = style,
            progress = mid,
            flowX = flowX,
            flowY = flowY,
            daylight = dayLight,
            warmth = warm,
            moonLight = moonLight,
            quality = quality
        )
        drawLowCloudLayer(
            cover = cover,
            style = style,
            progress = low,
            flowX = flowX,
            flowY = flowY,
            daylight = dayLight,
            warmth = warm,
            moonLight = moonLight,
            quality = quality
        )
    }
}

private fun cloudDuration(base: Int, motionFactor: Float): Int =
    (base / motionFactor).toInt().coerceIn(12_000, 180_000)

private fun cloudStyle(scene: WeatherScene, condition: String): CloudAtmosphereStyle = when {
    scene == WeatherScene.THUNDERSTORM -> CloudAtmosphereStyle.STORM
    scene == WeatherScene.RAIN -> CloudAtmosphereStyle.RAIN
    scene == WeatherScene.SNOW -> CloudAtmosphereStyle.SNOW
    scene == WeatherScene.NIGHT -> CloudAtmosphereStyle.NIGHT
    condition.contains("overcast", true) || condition.contains("cloud", true) || condition.contains("बादल", true) -> CloudAtmosphereStyle.OVERCAST
    else -> CloudAtmosphereStyle.FAIR
}

private fun DrawScope.drawHighCloudLayer(
    cover: Float,
    style: CloudAtmosphereStyle,
    progress: Float,
    flowX: Float,
    flowY: Float,
    daylight: Float,
    warmth: Float,
    moonLight: Float,
    quality: Float
) {
    val count = (((cover + 12f) / 18f) * quality).toInt().coerceIn(0, 6)
    if (count == 0) return
    val baseAlpha = (0.05f + cover / 100f * 0.12f) * when (style) {
        CloudAtmosphereStyle.STORM, CloudAtmosphereStyle.RAIN -> 0.72f
        CloudAtmosphereStyle.SNOW -> 0.88f
        else -> 1f
    }
    val dayColor = mixCloud(Color(0xFFC9D6E0), Color.White, daylight * 0.78f)
    val warmColor = if (warmth > 0f) mixCloud(dayColor, Color(0xFFFFC7A1), warmth * 0.38f) else dayColor
    val color = if (daylight < 0.12f) mixCloud(Color(0xFF72869A), warmColor, moonLight * 0.25f) else warmColor

    repeat(count) { index ->
        val width = size.width * (0.24f + (index % 3) * 0.055f)
        val height = size.height * (0.010f + (index % 2) * 0.004f)
        val baseX = size.width * ((index * 0.293f + 0.08f) % 1f)
        val travel = size.width * 1.35f
        val x = wrapPosition(baseX + progress * travel * flowX, size.width, width)
        val yBase = size.height * (0.075f + (index % 4) * 0.048f)
        val y = yBase + progress * size.height * 0.045f * flowY
        val alpha = (baseAlpha * (0.76f + (index % 3) * 0.10f)).coerceIn(0f, 0.26f)
        drawOval(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, color.copy(alpha = alpha), color.copy(alpha = alpha * 0.82f), Color.Transparent),
                startX = x,
                endX = x + width
            ),
            topLeft = Offset(x, y),
            size = Size(width, height)
        )
        drawOval(
            color = color.copy(alpha = alpha * 0.48f),
            topLeft = Offset(x + width * 0.18f, y + height * 0.74f),
            size = Size(width * 0.62f, height * 0.65f)
        )
    }
}

private fun DrawScope.drawMidCloudLayer(
    cover: Float,
    style: CloudAtmosphereStyle,
    progress: Float,
    flowX: Float,
    flowY: Float,
    daylight: Float,
    warmth: Float,
    moonLight: Float,
    quality: Float
) {
    val styleBoost = when (style) {
        CloudAtmosphereStyle.OVERCAST, CloudAtmosphereStyle.RAIN, CloudAtmosphereStyle.SNOW -> 1.18f
        CloudAtmosphereStyle.STORM -> 1.28f
        else -> 0.92f
    }
    val count = (((cover + 6f) / 16f) * styleBoost * quality).toInt().coerceIn(0, 8)
    if (count == 0) return
    val palette = cloudPalette(style, daylight, warmth, moonLight)

    repeat(count) { index ->
        val scale = 0.72f + (index % 4) * 0.09f
        val width = size.width * 0.24f * scale
        val height = size.height * 0.036f * scale
        val baseX = size.width * ((index * 0.217f + 0.04f) % 1f)
        val x = wrapPosition(baseX + progress * size.width * 1.55f * flowX, size.width, width)
        val yBase = size.height * (0.16f + (index % 5) * 0.047f)
        val y = yBase + progress * size.height * 0.065f * flowY
        val alpha = (0.09f + cover / 100f * 0.22f).coerceAtMost(0.34f)
        drawVolumetricCloud(
            x = x,
            y = y,
            width = width,
            height = height,
            top = palette.first.copy(alpha = alpha),
            bottom = palette.second.copy(alpha = alpha * 0.92f),
            density = 0.78f,
            lightFromLeft = flowX >= -0.15f
        )
    }
}

private fun DrawScope.drawLowCloudLayer(
    cover: Float,
    style: CloudAtmosphereStyle,
    progress: Float,
    flowX: Float,
    flowY: Float,
    daylight: Float,
    warmth: Float,
    moonLight: Float,
    quality: Float
) {
    val weatherBoost = when (style) {
        CloudAtmosphereStyle.STORM -> 1.45f
        CloudAtmosphereStyle.RAIN -> 1.34f
        CloudAtmosphereStyle.SNOW -> 1.20f
        CloudAtmosphereStyle.OVERCAST -> 1.15f
        else -> 0.86f
    }
    val count = (((cover + 4f) / 14f) * weatherBoost * quality).toInt().coerceIn(0, 10)
    if (count == 0) return
    val palette = cloudPalette(style, daylight, warmth, moonLight)

    if (cover >= 82 || style == CloudAtmosphereStyle.STORM) {
        val deckAlpha = when (style) {
            CloudAtmosphereStyle.STORM -> 0.30f
            CloudAtmosphereStyle.RAIN -> 0.20f
            else -> 0.12f
        }
        drawRect(
            brush = Brush.verticalGradient(
                0f to palette.first.copy(alpha = deckAlpha * 0.58f),
                0.31f to palette.second.copy(alpha = deckAlpha),
                0.48f to Color.Transparent
            ),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height * 0.49f)
        )
    }

    repeat(count) { index ->
        val depth = 0.72f + (index % 5) * 0.08f
        val weatherScale = when (style) {
            CloudAtmosphereStyle.STORM -> 1.34f
            CloudAtmosphereStyle.RAIN -> 1.18f
            else -> 1f
        }
        val width = size.width * 0.31f * depth * weatherScale
        val height = size.height * 0.055f * depth * weatherScale
        val baseX = size.width * ((index * 0.173f + 0.02f) % 1f)
        val x = wrapPosition(baseX + progress * size.width * 1.78f * flowX, size.width, width)
        val yBase = size.height * (0.245f + (index % 5) * 0.044f)
        val y = yBase + progress * size.height * 0.082f * flowY
        val alphaBase = when (style) {
            CloudAtmosphereStyle.STORM -> 0.64f
            CloudAtmosphereStyle.RAIN -> 0.48f
            CloudAtmosphereStyle.SNOW -> 0.38f
            CloudAtmosphereStyle.OVERCAST -> 0.34f
            CloudAtmosphereStyle.NIGHT -> 0.31f
            else -> 0.22f
        }
        val alpha = (alphaBase * (0.58f + cover / 100f * 0.62f)).coerceIn(0.10f, 0.78f)
        drawVolumetricCloud(
            x = x,
            y = y,
            width = width,
            height = height,
            top = palette.first.copy(alpha = alpha),
            bottom = palette.second.copy(alpha = (alpha * 1.08f).coerceAtMost(0.84f)),
            density = if (style == CloudAtmosphereStyle.STORM) 1.12f else 0.94f,
            lightFromLeft = flowX >= -0.15f
        )

        if (style == CloudAtmosphereStyle.RAIN || style == CloudAtmosphereStyle.STORM) {
            drawOval(
                brush = Brush.verticalGradient(
                    listOf(palette.second.copy(alpha = alpha * 0.42f), Color.Transparent),
                    startY = y + height * 0.65f,
                    endY = y + height * 2.15f
                ),
                topLeft = Offset(x + width * 0.10f, y + height * 0.58f),
                size = Size(width * 0.80f, height * 1.65f)
            )
        }
    }
}

private fun DrawScope.drawVolumetricCloud(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    top: Color,
    bottom: Color,
    density: Float,
    lightFromLeft: Boolean
) {
    // A wide translucent envelope removes the hard cut-out edge typical of
    // stacked circles and gives each formation a humid, atmospheric falloff.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                top.copy(alpha = top.alpha * 0.20f),
                bottom.copy(alpha = bottom.alpha * 0.16f),
                Color.Transparent
            ),
            center = Offset(x + width * 0.50f, y + height * 0.52f),
            radius = width * 0.58f
        ),
        topLeft = Offset(x - width * 0.08f, y - height * 0.55f),
        size = Size(width * 1.16f, height * 2.05f)
    )

    val silhouette = Path().apply {
        moveTo(x + width * 0.03f, y + height * 0.72f)
        cubicTo(x + width * 0.04f, y + height * 0.39f, x + width * 0.13f, y + height * 0.31f, x + width * 0.23f, y + height * 0.38f)
        cubicTo(x + width * 0.27f, y + height * 0.02f, x + width * 0.42f, y - height * 0.18f, x + width * 0.53f, y + height * 0.16f)
        cubicTo(x + width * 0.65f, y - height * 0.04f, x + width * 0.79f, y + height * 0.10f, x + width * 0.79f, y + height * 0.36f)
        cubicTo(x + width * 0.94f, y + height * 0.29f, x + width * 1.01f, y + height * 0.48f, x + width * 0.96f, y + height * 0.72f)
        cubicTo(x + width * 0.74f, y + height * 0.92f, x + width * 0.24f, y + height * 0.94f, x + width * 0.03f, y + height * 0.72f)
        close()
    }
    drawPath(
        path = silhouette,
        brush = Brush.verticalGradient(
            0f to top,
            0.48f to mixCloud(top, bottom, 0.44f),
            1f to bottom,
            startY = y - height * 0.18f,
            endY = y + height
        )
    )

    drawOval(
        brush = Brush.verticalGradient(
            listOf(top, bottom),
            startY = y - height * 0.65f,
            endY = y + height
        ),
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    val volumes = listOf(
        Triple(0.15f, 0.34f, 0.17f),
        Triple(0.29f, 0.13f, 0.23f),
        Triple(0.45f, -0.02f, 0.29f),
        Triple(0.61f, 0.08f, 0.25f),
        Triple(0.77f, 0.25f, 0.20f),
        Triple(0.89f, 0.42f, 0.13f)
    )
    volumes.forEachIndexed { index, (px, py, pr) ->
        val radius = width * pr * 0.48f * density
        val litSide = if (lightFromLeft) 1f - px else px
        val highlight = mixCloud(bottom, top, 0.42f + litSide * 0.40f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    highlight.copy(alpha = highlight.alpha * (0.82f + litSide * 0.14f)),
                    mixCloud(highlight, bottom, 0.40f).copy(alpha = highlight.alpha * 0.78f),
                    Color.Transparent
                ),
                center = Offset(x + width * px, y + height * (0.44f + py)),
                radius = radius
            ),
            radius = radius,
            center = Offset(x + width * px, y + height * (0.44f + py))
        )
    }

    // Moisture-rich bases are flatter and darker than the illuminated crown.
    drawOval(
        brush = Brush.verticalGradient(
            listOf(bottom.copy(alpha = bottom.alpha * 0.18f), bottom.copy(alpha = bottom.alpha * 0.72f), Color.Transparent),
            startY = y + height * 0.54f,
            endY = y + height * 1.12f
        ),
        topLeft = Offset(x + width * 0.04f, y + height * 0.55f),
        size = Size(width * 0.92f, height * 0.58f)
    )
}

private fun cloudPalette(
    style: CloudAtmosphereStyle,
    daylight: Float,
    warmth: Float,
    moonLight: Float
): Pair<Color, Color> {
    if (style == CloudAtmosphereStyle.STORM) {
        return Color(0xFF3A4A59) to Color(0xFF101C28)
    }
    if (style == CloudAtmosphereStyle.RAIN) {
        return Color(0xFF70808B) to Color(0xFF263843)
    }
    if (style == CloudAtmosphereStyle.SNOW) {
        return Color(0xFFE4EDF2) to Color(0xFF99AAB5)
    }

    val nightTop = mixCloud(Color(0xFF74879A), Color(0xFFAABBC9), moonLight * 0.32f)
    val nightBottom = mixCloud(Color(0xFF344759), Color(0xFF607486), moonLight * 0.22f)
    if (daylight < 0.14f || style == CloudAtmosphereStyle.NIGHT) {
        return nightTop to nightBottom
    }

    var top = mixCloud(Color(0xFFB8C8D3), Color.White, daylight * 0.78f)
    var bottom = mixCloud(Color(0xFF708594), Color(0xFFB8C7CE), daylight * 0.66f)
    if (warmth > 0.04f) {
        top = mixCloud(top, Color(0xFFFFD3B1), warmth * 0.36f)
        bottom = mixCloud(bottom, Color(0xFFCF8F83), warmth * 0.20f)
    }
    if (style == CloudAtmosphereStyle.OVERCAST) {
        top = mixCloud(top, Color(0xFF9BA9B1), 0.36f)
        bottom = mixCloud(bottom, Color(0xFF66747E), 0.42f)
    }
    return top to bottom
}

private fun wrapPosition(value: Float, screenWidth: Float, objectWidth: Float): Float {
    val span = screenWidth + objectWidth * 1.8f
    var wrapped = (value + objectWidth * 0.9f) % span
    if (wrapped < 0f) wrapped += span
    return wrapped - objectWidth * 0.9f
}

private fun mixCloud(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t
    )
}
