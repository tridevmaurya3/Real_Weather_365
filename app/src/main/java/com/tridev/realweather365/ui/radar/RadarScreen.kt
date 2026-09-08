package com.tridev.realweather365.ui.radar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.data.radar.RadarMapBundle
import com.tridev.realweather365.data.radar.RadarRenderableFrame
import com.tridev.realweather365.data.radar.RainViewerRadarRepository
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private enum class RadarLayer {
    RAIN,
    TEMPERATURE
}

private sealed interface RadarLoadState {
    data object Loading : RadarLoadState
    data class Ready(val bundle: RadarMapBundle) : RadarLoadState
}

@Composable
fun RadarScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { RainViewerRadarRepository(context) }
    val location = state.selectedLocation

    var selectedLayer by rememberSaveable { mutableStateOf(RadarLayer.RAIN) }
    var zoom by rememberSaveable { mutableStateOf(6) }
    var refreshToken by remember { mutableStateOf(0) }
    var radarState by remember(location.id, zoom) { mutableStateOf<RadarLoadState>(RadarLoadState.Loading) }
    var frameIndex by rememberSaveable(location.id, zoom) { mutableStateOf(0) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(location.id, zoom, refreshToken) {
        radarState = RadarLoadState.Loading
        radarState = RadarLoadState.Ready(
            repository.load(
                location = location,
                zoom = zoom,
                maxFrames = 7
            )
        )
    }

    val bundle = (radarState as? RadarLoadState.Ready)?.bundle
    val frames = bundle?.frames.orEmpty()

    LaunchedEffect(location.id, zoom, frames.size) {
        frameIndex = if (frames.isEmpty()) 0 else 0
    }

    LaunchedEffect(isPlaying, frames.size, selectedLayer) {
        if (selectedLayer != RadarLayer.RAIN || !isPlaying || frames.size < 2) return@LaunchedEffect
        while (true) {
            delay(760)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    val currentFrame = frames.getOrNull(frameIndex.coerceIn(0, max(0, frames.lastIndex)))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06131B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RadarHeader(
                location = state.location,
                onBack = onBack,
                onRefresh = { refreshToken++ }
            )

            Box(modifier = Modifier.weight(1f)) {
                RadarMapViewport(
                    state = state,
                    bundle = bundle,
                    frame = currentFrame,
                    layer = selectedLayer,
                    loading = radarState is RadarLoadState.Loading,
                    zoom = zoom,
                    onZoomIn = { if (zoom < 7) zoom++ },
                    onZoomOut = { if (zoom > 4) zoom-- },
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
                    bundle = bundle,
                    frame = currentFrame,
                    loading = radarState is RadarLoadState.Loading,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                )
            }

            RadarTimeline(
                frames = frames,
                frameIndex = frameIndex,
                onFrameIndexChanged = { frameIndex = it },
                isPlaying = isPlaying,
                onTogglePlay = { isPlaying = !isPlaying },
                layer = selectedLayer,
                timeZoneId = location.timeZoneId,
                languageCode = state.languageCode,
                bundle = bundle
            )
        }
    }
}

@Composable
private fun RadarHeader(
    location: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        color = Color(0xF2071720),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFF75E5F0), modifier = Modifier.size(13.dp))
                    Text(location, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Live Radar • real provider frames",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 8.sp,
                    letterSpacing = 0.3.sp
                )
            }

            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Refresh, "Refresh radar", tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RadarMapViewport(
    state: WeatherHomeUiState,
    bundle: RadarMapBundle?,
    frame: RadarRenderableFrame?,
    layer: RadarLayer,
    loading: Boolean,
    zoom: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val location = state.selectedLocation

    Box(modifier = modifier.background(Color(0xFF0C252E))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF102A32), Color(0xFF12343A), Color(0xFF0A222B))
                )
            )

            val verticalLines = 7
            val horizontalLines = 10
            repeat(verticalLines) { index ->
                val x = size.width * index / (verticalLines - 1f)
                drawLine(
                    Color.White.copy(alpha = 0.055f),
                    Offset(x, 0f),
                    Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
            repeat(horizontalLines) { index ->
                val y = size.height * index / (horizontalLines - 1f)
                drawLine(
                    Color.White.copy(alpha = 0.045f),
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            val center = Offset(size.width / 2f, size.height / 2f)
            val ringMax = size.minDimension * 0.43f
            listOf(0.28f, 0.52f, 0.76f, 1f).forEach { fraction ->
                drawCircle(
                    color = Color(0xFF54DDEA).copy(alpha = 0.10f),
                    radius = ringMax * fraction,
                    center = center,
                    style = Stroke(width = 1.2f)
                )
            }

            drawLine(
                Color.White.copy(alpha = 0.07f),
                Offset(center.x, 0f),
                Offset(center.x, size.height),
                strokeWidth = 1f
            )
            drawLine(
                Color.White.copy(alpha = 0.07f),
                Offset(0f, center.y),
                Offset(size.width, center.y),
                strokeWidth = 1f
            )
        }

        if (layer == RadarLayer.RAIN && frame != null) {
            val imageBitmap = remember(frame.bitmap) { frame.bitmap.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = "RainViewer radar precipitation",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.88f),
                contentScale = ContentScale.Crop
            )
        }

        if (layer == RadarLayer.TEMPERATURE) {
            val temperatureTint = when {
                state.temperature <= 5 -> listOf(Color(0x88386FD4), Color(0x5536B7DD), Color.Transparent)
                state.temperature <= 20 -> listOf(Color(0x7756CBE2), Color(0x5538D4A4), Color.Transparent)
                state.temperature <= 32 -> listOf(Color(0x66F2D357), Color(0x55F2A84C), Color.Transparent)
                else -> listOf(Color(0x77F28B42), Color(0x66E74D3A), Color.Transparent)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(temperatureTint))
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color(0x553DE9F3), radius = 18f, center = center)
            drawCircle(Color.White, radius = 8f, center = center)
            drawCircle(Color(0xFF21D7E7), radius = 5f, center = center)
            drawCircle(Color.White.copy(alpha = 0.65f), radius = 25f, center = center, style = Stroke(width = 1.2f))
        }

        ZoomControls(
            zoom = zoom,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 78.dp)
        )

        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xB9071922),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f))
        ) {
            Column(Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                Text(
                    "${location.name} • ${String.format(Locale.US, "%.3f", location.latitude)}, ${String.format(Locale.US, "%.3f", location.longitude)}",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 8.sp
                )
                Text(
                    if (layer == RadarLayer.RAIN) "Radar centered on selected coordinates" else "Temperature tint uses current weather value",
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 7.sp
                )
            }
        }

        RadarAttribution(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp)
        )

        if (loading) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xD5071922),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF63DFEA))
                    Text("Loading real radar frames…", color = Color.White, fontSize = 9.sp)
                }
            }
        } else if (layer == RadarLayer.RAIN && bundle?.coverageAvailable == false) {
            RadarMessage(
                title = "Radar coverage unavailable",
                detail = "This selected location is outside the provider's current composite radar coverage.",
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (layer == RadarLayer.RAIN && bundle?.errorMessage != null) {
            RadarMessage(
                title = "Radar temporarily unavailable",
                detail = bundle.errorMessage,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun RadarMessage(title: String, detail: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 28.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xE0081B25),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(detail, color = Color.White.copy(alpha = 0.58f), fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun ZoomControls(
    zoom: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = Color(0xCF071922),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(34.dp).clickable(onClick = onZoomIn), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Add, "Zoom in", tint = if (zoom < 7) Color.White else Color.White.copy(alpha = 0.28f), modifier = Modifier.size(17.dp))
            }
            Text("Z$zoom", color = Color(0xFF6FE1EB), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.size(34.dp).clickable(onClick = onZoomOut), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Remove, "Zoom out", tint = if (zoom > 4) Color.White else Color.White.copy(alpha = 0.28f), modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun RadarAttribution(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = Color(0xA5071821),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Text(
            text = "Weather radar: RainViewer",
            modifier = Modifier
                .clickable { runCatching { uriHandler.openUri("https://www.rainviewer.com/") } }
                .padding(horizontal = 7.dp, vertical = 5.dp),
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 7.sp
        )
    }
}

@Composable
private fun RadarStatusBadge(
    layer: RadarLayer,
    bundle: RadarMapBundle?,
    frame: RadarRenderableFrame?,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val text = when {
        layer == RadarLayer.TEMPERATURE -> "TEMPERATURE • CURRENT VALUE"
        loading -> "PRECIPITATION • CONNECTING"
        bundle?.coverageAvailable == false -> "PRECIPITATION • NO COVERAGE"
        frame == null -> "PRECIPITATION • UNAVAILABLE"
        frame.meta.isNowcast -> "PRECIPITATION • NOWCAST"
        !frame.hasPrecipitationEcho -> "PRECIPITATION • NO ECHO"
        else -> "PRECIPITATION • OBSERVED"
    }
    val dot = when {
        bundle?.coverageAvailable == false -> Color(0xFFFFB15C)
        frame == null && !loading -> Color(0xFFFF7C7C)
        else -> Color(0xFF55EAA5)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xC9081B25),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(7.dp).background(dot, CircleShape))
            Text(text, color = Color.White.copy(alpha = 0.86f), fontSize = 8.sp, fontWeight = FontWeight.Medium)
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
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LayerControlItem(Icons.Outlined.Layers, "Layers", false) { }
            LayerControlItem(Icons.Outlined.WaterDrop, "Rain", selectedLayer == RadarLayer.RAIN) { onLayerChange(RadarLayer.RAIN) }
            LayerControlItem(Icons.Outlined.Thermostat, "Temp", selectedLayer == RadarLayer.TEMPERATURE) { onLayerChange(RadarLayer.TEMPERATURE) }
        }
    }
}

@Composable
private fun LayerControlItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Icon(icon, label, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, color = tint, fontSize = 7.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RadarTimeline(
    frames: List<RadarRenderableFrame>,
    frameIndex: Int,
    onFrameIndexChanged: (Int) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    layer: RadarLayer,
    timeZoneId: String,
    languageCode: String,
    bundle: RadarMapBundle?
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = Color(0xF4081821),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            if (layer == RadarLayer.TEMPERATURE) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Thermostat, null, tint = Color(0xFFFFB65F), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Current temperature context", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Spatial temperature grid is not presented as live radar data.", color = Color.White.copy(alpha = 0.50f), fontSize = 8.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                RadarLegend(RadarLayer.TEMPERATURE)
                return@Column
            }

            val current = frames.getOrNull(frameIndex.coerceIn(0, max(0, frames.lastIndex)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.size(36.dp).clickable(enabled = frames.size > 1, onClick = onTogglePlay),
                    shape = CircleShape,
                    color = Color(0xFF143442),
                    border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            if (isPlaying) "Pause radar" else "Play radar",
                            tint = if (frames.size > 1) Color.White else Color.White.copy(alpha = 0.30f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            current?.let { formatRadarTime(it.meta.timeEpochSeconds, timeZoneId, languageCode) } ?: "No radar frame",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            current?.let { if (it.meta.isNowcast) "NOWCAST" else "OBSERVED" } ?: "",
                            color = if (current?.meta?.isNowcast == true) Color(0xFFFFCF62) else Color(0xFF63DFEA),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (frames.size > 1) {
                        Slider(
                            value = frameIndex.toFloat().coerceIn(0f, frames.lastIndex.toFloat()),
                            onValueChange = { onFrameIndexChanged(it.toInt().coerceIn(0, frames.lastIndex)) },
                            valueRange = 0f..frames.lastIndex.toFloat(),
                            steps = (frames.size - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF58E1ED),
                                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    } else {
                        Spacer(Modifier.height(5.dp))
                    }
                }
            }

            if (frames.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 46.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(0, frames.lastIndex / 2, frames.lastIndex).distinct().forEach { index ->
                        Text(
                            formatRadarClock(frames[index].meta.timeEpochSeconds, timeZoneId, languageCode),
                            color = Color.White.copy(alpha = if (index == frameIndex) 0.90f else 0.45f),
                            fontSize = 7.sp
                        )
                    }
                }
            } else {
                Text(
                    when {
                        bundle?.coverageAvailable == false -> "No composite radar coverage for this location."
                        bundle?.errorMessage != null -> bundle.errorMessage
                        else -> "Waiting for radar timeline…"
                    },
                    modifier = Modifier.padding(start = 46.dp, top = 4.dp),
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 8.sp
                )
            }

            Spacer(Modifier.height(7.dp))
            RadarLegend(RadarLayer.RAIN)
        }
    }
}

@Composable
private fun RadarLegend(layer: RadarLayer) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (layer == RadarLayer.RAIN) "Light" else "Cool", color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
        Canvas(Modifier.weight(1f).padding(horizontal = 8.dp).height(4.dp)) {
            val colors = if (layer == RadarLayer.RAIN) {
                listOf(Color(0xFF1CBDF2), Color(0xFF20E096), Color(0xFFE8E44A), Color(0xFFFF8A33), Color(0xFFF33449))
            } else {
                listOf(Color(0xFF3979D9), Color(0xFF39CBE4), Color(0xFFF6DB55), Color(0xFFFF9A45), Color(0xFFE64A3C))
            }
            drawRoundRect(
                brush = Brush.horizontalGradient(colors),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                size = Size(size.width, size.height)
            )
        }
        Text(if (layer == RadarLayer.RAIN) "Heavy" else "Hot", color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
    }
}

private fun formatRadarTime(epochSeconds: Long, timeZoneId: String, languageCode: String): String {
    val locale = if (languageCode == "hi") Locale("hi", "IN") else Locale.ENGLISH
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    return Instant.ofEpochSecond(epochSeconds)
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("EEE • h:mm a", locale))
}

private fun formatRadarClock(epochSeconds: Long, timeZoneId: String, languageCode: String): String {
    val locale = if (languageCode == "hi") Locale("hi", "IN") else Locale.ENGLISH
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    return Instant.ofEpochSecond(epochSeconds)
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("h:mm a", locale))
}
