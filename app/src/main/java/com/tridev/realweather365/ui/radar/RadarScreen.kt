package com.tridev.realweather365.ui.radar

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.theme.RealWeather365Theme
import kotlin.math.cos
import kotlin.math.sin

private enum class RadarLayer {
    RAIN,
    TEMPERATURE
}

@Composable
fun RadarScreen(
    location: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLayer by rememberSaveable { mutableStateOf(RadarLayer.RAIN) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }

    val transition = rememberInfiniteTransition(label = "radar-animation")
    val radarProgress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar-progress"
    )
    val cloudDrift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar-drift"
    )

    val animatedProgress = if (isPlaying) radarProgress.value else 0.38f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06131B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RadarHeader(location = location, onBack = onBack)

            Box(modifier = Modifier.weight(1f)) {
                RadarMapCanvas(
                    progress = animatedProgress,
                    drift = cloudDrift.value,
                    layer = selectedLayer,
                    modifier = Modifier.fillMaxSize()
                )

                RadarLayerControls(
                    selectedLayer = selectedLayer,
                    onLayerChange = { selectedLayer = it },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                )

                RadarStatusBadge(
                    layer = selectedLayer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                )
            }

            RadarTimeline(
                isPlaying = isPlaying,
                onTogglePlay = { isPlaying = !isPlaying },
                progress = animatedProgress,
                layer = selectedLayer
            )
        }
    }
}

@Composable
private fun RadarHeader(location: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xF2071720),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF75E5F0),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = location,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Live Radar",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    tint = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun RadarStatusBadge(layer: RadarLayer, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xC9081B25),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Color(0xFF55EAA5), CircleShape)
            )
            Text(
                text = if (layer == RadarLayer.RAIN) "PRECIPITATION • LIVE" else "TEMPERATURE • LIVE",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RadarLayerControls(
    selectedLayer: RadarLayer,
    onLayerChange: (RadarLayer) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xD0071922),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LayerControlItem(
                icon = Icons.Outlined.Layers,
                label = "Layers",
                selected = false,
                onClick = { }
            )
            LayerControlItem(
                icon = Icons.Outlined.WaterDrop,
                label = "Rain",
                selected = selectedLayer == RadarLayer.RAIN,
                onClick = { onLayerChange(RadarLayer.RAIN) }
            )
            LayerControlItem(
                icon = Icons.Outlined.Thermostat,
                label = "Temp",
                selected = selectedLayer == RadarLayer.TEMPERATURE,
                onClick = { onLayerChange(RadarLayer.TEMPERATURE) }
            )
        }
    }
}

@Composable
private fun LayerControlItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) Color(0xFF64E8F2) else Color.White.copy(alpha = 0.72f)
    val background = if (selected) Color(0x263CE7F3) else Color.Transparent

    Column(
        modifier = Modifier
            .width(54.dp)
            .background(background, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = tint,
            fontSize = 7.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun RadarTimeline(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    progress: Float,
    layer: RadarLayer
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xF4081821),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onTogglePlay),
                    shape = CircleShape,
                    color = Color(0xFF143442),
                    border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (isPlaying) "Pause radar" else "Play radar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Now",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (layer == RadarLayer.RAIN) "Thu 9:41 AM" else "Surface temperature",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    RadarProgressBar(progress = progress, layer = layer)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 46.dp, top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Now", "10:00", "11:00", "12:00", "13:00").forEach { time ->
                    Text(
                        text = time,
                        color = Color.White.copy(alpha = if (time == "Now") 0.90f else 0.45f),
                        fontSize = 7.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))
            RadarLegend(layer = layer)
        }
    }
}

@Composable
private fun RadarProgressBar(progress: Float, layer: RadarLayer) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val centerY = size.height / 2f
        drawLine(
            color = Color.White.copy(alpha = 0.16f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 2.2f
        )

        val activeColor = if (layer == RadarLayer.RAIN) Color(0xFF58E1ED) else Color(0xFFFFB65F)
        val x = size.width * progress.coerceIn(0f, 1f)
        drawLine(
            color = activeColor.copy(alpha = 0.55f),
            start = Offset(0f, centerY),
            end = Offset(x, centerY),
            strokeWidth = 2.6f
        )
        drawCircle(
            color = Color.White,
            radius = 4.3f,
            center = Offset(x, centerY)
        )
        drawCircle(
            color = activeColor.copy(alpha = 0.40f),
            radius = 8.5f,
            center = Offset(x, centerY)
        )
    }
}

@Composable
private fun RadarLegend(layer: RadarLayer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (layer == RadarLayer.RAIN) "Light" else "Cool",
            color = Color.White.copy(alpha = 0.48f),
            fontSize = 7.sp
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(4.dp)
        ) {
            val colors = if (layer == RadarLayer.RAIN) {
                listOf(
                    Color(0xFF1CBDF2),
                    Color(0xFF20E096),
                    Color(0xFFE8E44A),
                    Color(0xFFFF8A33),
                    Color(0xFFF33449)
                )
            } else {
                listOf(
                    Color(0xFF3979D9),
                    Color(0xFF39CBE4),
                    Color(0xFFF6DB55),
                    Color(0xFFFF9A45),
                    Color(0xFFE64A3C)
                )
            }
            drawRoundRect(
                brush = Brush.horizontalGradient(colors),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
        Text(
            text = if (layer == RadarLayer.RAIN) "Heavy" else "Hot",
            color = Color.White.copy(alpha = 0.48f),
            fontSize = 7.sp
        )
    }
}

@Composable
private fun RadarMapCanvas(
    progress: Float,
    drift: Float,
    layer: RadarLayer,
    modifier: Modifier = Modifier
) {
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(205, 232, 244, 248)
            textSize = 22f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
    }
    val secondaryLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(155, 210, 228, 234)
            textSize = 17f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
    }

    Canvas(modifier = modifier) {
        drawMapBase()
        drawDistrictShapes()
        drawRiverNetwork()
        drawRoadNetwork()
        drawRadarWeather(progress = progress, drift = drift, layer = layer)
        drawRadarSweep(progress)
        drawLocationMarker()
        drawMapLabels(labelPaint, secondaryLabelPaint)
        drawMapVignette()
    }
}

private fun DrawScope.drawMapBase() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF102A32),
            0.48f to Color(0xFF14343B),
            1f to Color(0xFF0B222B)
        )
    )

    repeat(9) { index ->
        val y = size.height * (0.06f + index * 0.115f)
        drawLine(
            color = Color.White.copy(alpha = 0.025f),
            start = Offset(0f, y),
            end = Offset(size.width, y - size.height * 0.05f),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawDistrictShapes() {
    val regions = listOf(
        listOf(0.00f to 0.05f, 0.32f to 0.01f, 0.42f to 0.22f, 0.24f to 0.36f, 0.00f to 0.28f),
        listOf(0.42f to 0.02f, 0.82f to 0.03f, 0.96f to 0.25f, 0.65f to 0.34f, 0.43f to 0.22f),
        listOf(0.00f to 0.30f, 0.24f to 0.37f, 0.36f to 0.62f, 0.08f to 0.69f, 0.00f to 0.58f),
        listOf(0.25f to 0.37f, 0.65f to 0.34f, 0.71f to 0.64f, 0.37f to 0.63f),
        listOf(0.66f to 0.35f, 0.98f to 0.27f, 1.00f to 0.70f, 0.72f to 0.64f),
        listOf(0.08f to 0.70f, 0.37f to 0.64f, 0.55f to 1.00f, 0.00f to 1.00f),
        listOf(0.38f to 0.65f, 0.72f to 0.65f, 1.00f to 0.72f, 1.00f to 1.00f, 0.55f to 1.00f)
    )

    regions.forEachIndexed { index, points ->
        val path = Path().apply {
            points.forEachIndexed { pointIndex, point ->
                val p = Offset(size.width * point.first, size.height * point.second)
                if (pointIndex == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(
            path = path,
            color = if (index % 2 == 0) Color(0xFF173B3C) else Color(0xFF123536)
        )
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.07f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
        )
    }
}

private fun DrawScope.drawRiverNetwork() {
    val mainRiver = Path().apply {
        moveTo(size.width * 0.02f, size.height * 0.42f)
        cubicTo(
            size.width * 0.25f, size.height * 0.36f,
            size.width * 0.38f, size.height * 0.56f,
            size.width * 0.57f, size.height * 0.50f
        )
        cubicTo(
            size.width * 0.73f, size.height * 0.45f,
            size.width * 0.78f, size.height * 0.57f,
            size.width * 1.02f, size.height * 0.53f
        )
    }
    drawPath(
        path = mainRiver,
        color = Color(0xFF56BBD0).copy(alpha = 0.40f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.5f)
    )
    drawPath(
        path = mainRiver,
        color = Color(0xFF9CDDEA).copy(alpha = 0.32f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
    )

    repeat(3) { index ->
        val tributary = Path().apply {
            moveTo(size.width * (0.18f + index * 0.27f), 0f)
            cubicTo(
                size.width * (0.17f + index * 0.25f), size.height * 0.22f,
                size.width * (0.28f + index * 0.18f), size.height * 0.35f,
                size.width * (0.31f + index * 0.18f), size.height * 0.48f
            )
        }
        drawPath(
            tributary,
            color = Color(0xFF4AA9BE).copy(alpha = 0.22f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f)
        )
    }
}

private fun DrawScope.drawRoadNetwork() {
    val roads = listOf(
        listOf(0.05f to 0.82f, 0.28f to 0.67f, 0.50f to 0.55f, 0.78f to 0.41f, 1.0f to 0.31f),
        listOf(0.10f to 0.18f, 0.30f to 0.31f, 0.52f to 0.52f, 0.70f to 0.79f, 0.84f to 1.0f),
        listOf(0.00f to 0.59f, 0.25f to 0.55f, 0.50f to 0.52f, 0.78f to 0.57f, 1.0f to 0.64f),
        listOf(0.35f to 0.00f, 0.40f to 0.25f, 0.51f to 0.52f, 0.48f to 0.79f, 0.43f to 1.0f)
    )

    roads.forEachIndexed { index, points ->
        val path = Path().apply {
            points.forEachIndexed { pointIndex, point ->
                val x = size.width * point.first
                val y = size.height * point.second
                if (pointIndex == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path,
            color = if (index == 0) Color(0xFFF3B45B).copy(alpha = 0.32f) else Color.White.copy(alpha = 0.16f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (index == 0) 2.4f else 1.3f)
        )
    }
}

private fun DrawScope.drawRadarWeather(progress: Float, drift: Float, layer: RadarLayer) {
    if (layer == RadarLayer.TEMPERATURE) {
        drawTemperatureField(progress)
        return
    }

    val shiftX = sin(drift * 6.28318f) * size.width * 0.035f
    val shiftY = cos(drift * 6.28318f) * size.height * 0.018f

    drawRainCell(
        center = Offset(size.width * 0.20f + shiftX, size.height * 0.28f + shiftY),
        radius = size.width * 0.28f,
        strength = 0.72f
    )
    drawRainCell(
        center = Offset(size.width * 0.72f + shiftX * 0.5f, size.height * 0.34f - shiftY),
        radius = size.width * 0.32f,
        strength = 1f
    )
    drawRainCell(
        center = Offset(size.width * 0.54f - shiftX, size.height * 0.73f + shiftY * 0.4f),
        radius = size.width * 0.30f,
        strength = 0.82f
    )

    repeat(7) { index ->
        val phase = progress * 6.28318f + index * 0.9f
        val center = Offset(
            size.width * (0.14f + ((index * 0.137f) % 0.78f)) + sin(phase) * 12f,
            size.height * (0.16f + ((index * 0.193f) % 0.70f)) + cos(phase) * 9f
        )
        drawCircle(
            color = Color(0xFF28D995).copy(alpha = 0.16f),
            radius = size.width * (0.055f + (index % 3) * 0.018f),
            center = center
        )
    }
}

private fun DrawScope.drawRainCell(center: Offset, radius: Float, strength: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF03A44).copy(alpha = 0.66f * strength),
                Color(0xFFFF9D32).copy(alpha = 0.60f * strength),
                Color(0xFFE7DF43).copy(alpha = 0.52f * strength),
                Color(0xFF21D88D).copy(alpha = 0.46f * strength),
                Color(0xFF28A8EC).copy(alpha = 0.28f * strength),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawTemperatureField(progress: Float) {
    val wobble = sin(progress * 6.28318f) * size.width * 0.02f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE95042).copy(alpha = 0.62f), Color(0xFFF7A647).copy(alpha = 0.32f), Color.Transparent),
            center = Offset(size.width * 0.68f + wobble, size.height * 0.36f),
            radius = size.width * 0.52f
        ),
        radius = size.width * 0.52f,
        center = Offset(size.width * 0.68f + wobble, size.height * 0.36f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3A85DB).copy(alpha = 0.56f), Color(0xFF38C7D8).copy(alpha = 0.26f), Color.Transparent),
            center = Offset(size.width * 0.18f - wobble, size.height * 0.68f),
            radius = size.width * 0.48f
        ),
        radius = size.width * 0.48f,
        center = Offset(size.width * 0.18f - wobble, size.height * 0.68f)
    )
}

private fun DrawScope.drawRadarSweep(progress: Float) {
    val center = Offset(size.width * 0.50f, size.height * 0.52f)
    val maxRadius = size.width * 0.58f
    repeat(3) { index ->
        val local = (progress + index * 0.32f) % 1f
        drawCircle(
            color = Color(0xFF8EEAF2).copy(alpha = (1f - local) * 0.12f),
            radius = maxRadius * local,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
        )
    }
}

private fun DrawScope.drawLocationMarker() {
    val center = Offset(size.width * 0.50f, size.height * 0.52f)
    drawCircle(Color(0xFF5DEAF3).copy(alpha = 0.22f), radius = 18f, center = center)
    drawCircle(Color(0xFF5DEAF3), radius = 6.5f, center = center)
    drawCircle(Color.White, radius = 2.5f, center = center)
}

private fun DrawScope.drawMapLabels(primary: Paint, secondary: Paint) {
    val canvas = drawContext.canvas.nativeCanvas
    canvas.drawText("Chandauli", size.width * 0.52f, size.height * 0.50f, primary)
    canvas.drawText("Varanasi", size.width * 0.23f, size.height * 0.33f, secondary)
    canvas.drawText("Mughalsarai", size.width * 0.61f, size.height * 0.45f, secondary)
    canvas.drawText("Mirzapur", size.width * 0.18f, size.height * 0.72f, secondary)
    canvas.drawText("Ghazipur", size.width * 0.72f, size.height * 0.18f, secondary)
    canvas.drawText("Sonbhadra", size.width * 0.56f, size.height * 0.83f, secondary)
}

private fun DrawScope.drawMapVignette() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x4A031018), Color.Transparent, Color.Transparent, Color(0x79020B10))
        )
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0x52020D13), Color.Transparent, Color.Transparent, Color(0x3D020D13))
        )
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RadarScreenPreview() {
    RealWeather365Theme {
        RadarScreen(location = "Chandauli", onBack = {})
    }
}
