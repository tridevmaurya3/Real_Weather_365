package com.tridev.realweather365.ui.radar

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tridev.realweather365.data.radar.RadarMapBundle
import com.tridev.realweather365.data.radar.RadarRenderableFrame
import com.tridev.realweather365.data.radar.RainViewerRadarRepository
import com.tridev.realweather365.data.weather.OpenMeteoMapLayerRepository
import com.tridev.realweather365.data.weather.SpatialWeatherField
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val DIRECT_RADAR_HOST = "https://tilecache.rainviewer.com"

private enum class ReliableLayer(val label: String, val icon: ImageVector) {
    RADAR("Radar", Icons.Outlined.WaterDrop),
    TEMPERATURE("Temp", Icons.Outlined.Thermostat),
    WIND("Wind", Icons.Outlined.Air),
    CLOUDS("Cloud", Icons.Outlined.Cloud),
    PRECIPITATION("Precip", Icons.Outlined.WaterDrop)
}

private enum class ReliableMapStyle(val label: String) {
    DARK("Dark"), LIGHT("Light"), SATELLITE("Satellite")
}

private data class ReliableViewport(val latitude: Double, val longitude: Double, val zoom: Int)

private class DirectMapBridge(
    private val onReady: () -> Unit,
    private val onViewport: (ReliableViewport) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun mapReady() {
        handler.post(onReady)
    }

    @JavascriptInterface
    fun viewportChanged(latitude: Double, longitude: Double, zoom: Int) {
        handler.post {
            onViewport(
                ReliableViewport(
                    latitude = latitude.coerceIn(-84.0, 84.0),
                    longitude = normalizeReliableLongitude(longitude),
                    zoom = zoom.coerceIn(3, 12)
                )
            )
        }
    }
}

/**
 * Stage 36 map reliability hotfix.
 *
 * This screen deliberately does not depend on a remote JavaScript mapping library. The WebView
 * renders standard slippy-map tiles directly, so a CDN failure cannot leave the whole map blank.
 * CARTO/Esri tiles fall back to OpenStreetMap per tile, while radar and model fields remain live.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReliableRadarScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val radarRepository = remember(context) { RainViewerRadarRepository(context) }
    val fieldRepository = remember { OpenMeteoMapLayerRepository() }
    val location = state.selectedLocation

    var selectedLayer by rememberSaveable { mutableStateOf(ReliableLayer.RADAR) }
    var mapStyle by rememberSaveable { mutableStateOf(ReliableMapStyle.DARK) }
    var opacity by rememberSaveable { mutableStateOf(0.72f) }
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    var refreshToken by remember { mutableStateOf(0) }
    var bundle by remember(location.id) { mutableStateOf<RadarMapBundle?>(null) }
    var radarLoading by remember(location.id) { mutableStateOf(true) }
    var radarMessage by remember(location.id) { mutableStateOf<String?>(null) }
    var field by remember(location.id) { mutableStateOf<SpatialWeatherField?>(null) }
    var fieldLoading by remember(location.id) { mutableStateOf(false) }
    var fieldMessage by remember(location.id) { mutableStateOf<String?>(null) }
    var frameIndex by rememberSaveable(location.id) { mutableStateOf(0) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var viewport by remember(location.id) {
        mutableStateOf(ReliableViewport(location.latitude, location.longitude, 8))
    }
    var webView by remember(location.id) { mutableStateOf<WebView?>(null) }
    var mapReady by remember(location.id) { mutableStateOf(false) }

    BackHandler(enabled = fullScreen) { fullScreen = false }

    LaunchedEffect(location.id, refreshToken) {
        radarLoading = true
        radarMessage = null
        val result = runCatching { radarRepository.load(location = location, zoom = 6, maxFrames = 9) }
        result.onSuccess {
            bundle = it
            radarMessage = it.errorMessage
        }.onFailure {
            bundle = null
            radarMessage = it.message ?: "Radar provider unavailable"
        }
        radarLoading = false
    }

    LaunchedEffect(selectedLayer, viewport, refreshToken, location.id) {
        if (selectedLayer == ReliableLayer.RADAR) return@LaunchedEffect
        delay(500L)
        fieldLoading = true
        fieldMessage = null
        val span = reliableSpanForZoom(viewport.zoom)
        runCatching {
            fieldRepository.load(
                centerLatitude = viewport.latitude,
                centerLongitude = viewport.longitude,
                gridSize = 7,
                halfSpanLatitudeDegrees = span
            )
        }.onSuccess {
            field = it
        }.onFailure {
            field = null
            fieldMessage = it.message ?: "Weather field unavailable"
        }
        fieldLoading = false
    }

    val frames = bundle?.frames.orEmpty()
    LaunchedEffect(frames.size, location.id) {
        frameIndex = 0
    }
    LaunchedEffect(playing, frames.size, selectedLayer) {
        if (!playing || selectedLayer != ReliableLayer.RADAR || frames.size < 2) return@LaunchedEffect
        while (true) {
            delay(1_050L)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }
    val safeFrameIndex = frameIndex.coerceIn(0, max(0, frames.lastIndex))
    val currentFrame = frames.getOrNull(safeFrameIndex)

    DisposableEffect(location.id) {
        onDispose {
            webView?.apply {
                stopLoading()
                removeJavascriptInterface("RealWeatherBridge")
                loadUrl("about:blank")
                destroy()
            }
            webView = null
            mapReady = false
        }
    }

    LaunchedEffect(webView, mapReady, selectedLayer, currentFrame?.meta?.path, opacity) {
        val view = webView ?: return@LaunchedEffect
        if (!mapReady) return@LaunchedEffect
        if (selectedLayer == ReliableLayer.RADAR && currentFrame != null) {
            view.evaluateJavascript(
                "setRadar(${JSONObject.quote(DIRECT_RADAR_HOST)},${JSONObject.quote(currentFrame.meta.path)},${opacity.coerceIn(0.2f, 1f)});",
                null
            )
        } else {
            view.evaluateJavascript("clearRadar();", null)
        }
    }

    LaunchedEffect(webView, mapReady, selectedLayer, field, opacity, state.temperatureUnit, state.windUnit) {
        val view = webView ?: return@LaunchedEffect
        if (!mapReady) return@LaunchedEffect
        if (selectedLayer == ReliableLayer.RADAR || field == null) {
            view.evaluateJavascript("clearField();", null)
        } else {
            view.evaluateJavascript(
                "setField(${JSONObject.quote(selectedLayer.jsKey())},${reliableFieldToJson(field!!, state)},${JSONObject.quote(state.temperatureUnit)},${JSONObject.quote(state.windUnit)},${opacity.coerceIn(0.2f, 1f)});",
                null
            )
        }
    }

    LaunchedEffect(webView, mapReady, mapStyle) {
        val view = webView ?: return@LaunchedEffect
        if (!mapReady) return@LaunchedEffect
        view.evaluateJavascript("setBaseStyle(${JSONObject.quote(mapStyle.name.lowercase(Locale.US))});", null)
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF06131B))) {
        Column(Modifier.fillMaxSize()) {
            if (!fullScreen) {
                ReliableMapHeader(
                    locationName = state.location,
                    onBack = onBack,
                    onRefresh = { refreshToken++ }
                )
            }

            Box(Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val bridge = DirectMapBridge(
                            onReady = { mapReady = true },
                            onViewport = { viewport = it }
                        )
                        WebView(ctx).apply {
                            setBackgroundColor(AndroidColor.rgb(7, 20, 27))
                            overScrollMode = View.OVER_SCROLL_NEVER
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.blockNetworkImage = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = false
                            settings.setSupportZoom(false)
                            addJavascriptInterface(bridge, "RealWeatherBridge")
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {}
                            loadDataWithBaseURL(
                                "https://appassets.androidplatform.net/",
                                buildDirectMapHtml(location.latitude, location.longitude, location.name),
                                "text/html",
                                "UTF-8",
                                null
                            )
                            webView = this
                        }
                    },
                    update = { webView = it }
                )

                ReliableLayerBar(
                    selected = selectedLayer,
                    onSelected = {
                        selectedLayer = it
                        opacity = if (it == ReliableLayer.RADAR) 0.72f else 0.58f
                    },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = if (fullScreen) 58.dp else 8.dp)
                )

                ReliableMapButtons(
                    fullScreen = fullScreen,
                    style = mapStyle,
                    onFullScreen = { fullScreen = !fullScreen },
                    onStyle = { mapStyle = mapStyle.next() },
                    onRecenter = { webView?.evaluateJavascript("recenter();", null) },
                    onZoomIn = { webView?.evaluateJavascript("zoomBy(1);", null) },
                    onZoomOut = { webView?.evaluateJavascript("zoomBy(-1);", null) },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                )

                ReliableStatus(
                    layer = selectedLayer,
                    radarLoading = radarLoading,
                    fieldLoading = fieldLoading,
                    radarMessage = radarMessage,
                    fieldMessage = fieldMessage,
                    coverage = bundle?.coverageAvailable,
                    mapReady = mapReady,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 9.dp, top = if (fullScreen) 112.dp else 58.dp)
                )

                if (fullScreen) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding(),
                        color = Color(0xC0061720),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.ArrowBack,
                                "Back",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp).clickable(onClick = onBack).padding(8.dp)
                            )
                            Text(
                                state.location,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Outlined.FullscreenExit,
                                "Exit full screen",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp).clickable { fullScreen = false }.padding(8.dp)
                            )
                        }
                    }
                }
            }

            if (!fullScreen) {
                ReliableBottomPanel(
                    layer = selectedLayer,
                    opacity = opacity,
                    onOpacityChanged = { opacity = it },
                    frames = frames,
                    frameIndex = safeFrameIndex,
                    onFrameIndex = { frameIndex = it },
                    playing = playing,
                    onPlayPause = { playing = !playing },
                    currentFrame = currentFrame,
                    timeZoneId = location.timeZoneId,
                    mapStyle = mapStyle,
                    field = field,
                    state = state
                )
            }
        }
    }
}

@Composable
private fun ReliableMapHeader(locationName: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        color = Color(0xF2071720),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(38.dp).clickable(onClick = onBack).padding(8.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFF70E3EE), modifier = Modifier.size(13.dp))
                    Text(locationName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Direct geographic tile engine • no remote map JS dependency", color = Color.White.copy(alpha = 0.52f), fontSize = 7.sp)
            }
            Icon(Icons.Outlined.Refresh, "Refresh", tint = Color.White, modifier = Modifier.size(38.dp).clickable(onClick = onRefresh).padding(9.dp))
        }
    }
}

@Composable
private fun ReliableLayerBar(selected: ReliableLayer, onSelected: (ReliableLayer) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xD9071922),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(Modifier.padding(horizontal = 3.dp, vertical = 3.dp)) {
            ReliableLayer.entries.forEach { item ->
                val active = item == selected
                Column(
                    modifier = Modifier
                        .width(54.dp)
                        .background(if (active) Color(0x253DE2EF) else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { onSelected(item) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(item.icon, item.label, tint = if (active) Color(0xFF6FE8F2) else Color.White.copy(alpha = 0.68f), modifier = Modifier.size(15.dp))
                    Text(item.label, color = if (active) Color(0xFF6FE8F2) else Color.White.copy(alpha = 0.68f), fontSize = 6.5.sp)
                }
            }
        }
    }
}

@Composable
private fun ReliableMapButtons(
    fullScreen: Boolean,
    style: ReliableMapStyle,
    onFullScreen: () -> Unit,
    onStyle: () -> Unit,
    onRecenter: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
        ReliableRoundButton(Icons.Outlined.MyLocation, "Recenter", onRecenter)
        ReliableRoundButton(Icons.Outlined.ZoomIn, "Zoom in", onZoomIn)
        ReliableRoundButton(Icons.Outlined.ZoomOut, "Zoom out", onZoomOut)
        ReliableRoundButton(if (fullScreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen, "Full screen", onFullScreen)
        Surface(
            modifier = Modifier.clickable(onClick = onStyle),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xD9071922),
            border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Layers, null, tint = Color(0xFF6FE4EE), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(style.label, color = Color.White, fontSize = 7.sp)
            }
        }
    }
}

@Composable
private fun ReliableRoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(38.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = Color(0xD9071922),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ReliableStatus(
    layer: ReliableLayer,
    radarLoading: Boolean,
    fieldLoading: Boolean,
    radarMessage: String?,
    fieldMessage: String?,
    coverage: Boolean?,
    mapReady: Boolean,
    modifier: Modifier = Modifier
) {
    val loading = if (layer == ReliableLayer.RADAR) radarLoading else fieldLoading
    val message = if (layer == ReliableLayer.RADAR) radarMessage else fieldMessage
    val label = when {
        !mapReady -> "MAP • STARTING"
        loading -> "${layer.label.uppercase(Locale.US)} • LOADING"
        layer == ReliableLayer.RADAR && coverage == false -> "RADAR • NO COVERAGE"
        message != null -> "${layer.label.uppercase(Locale.US)} • LIMITED"
        else -> "${layer.label.uppercase(Locale.US)} • LIVE"
    }
    Surface(modifier, shape = RoundedCornerShape(10.dp), color = Color(0xD5071922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f))) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (loading || !mapReady) {
                CircularProgressIndicator(Modifier.size(10.dp), strokeWidth = 1.5.dp, color = Color(0xFF6FE4EE))
                Spacer(Modifier.width(5.dp))
            }
            Text(label, color = Color.White.copy(alpha = 0.88f), fontSize = 6.7.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ReliableBottomPanel(
    layer: ReliableLayer,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    frames: List<RadarRenderableFrame>,
    frameIndex: Int,
    onFrameIndex: (Int) -> Unit,
    playing: Boolean,
    onPlayPause: () -> Unit,
    currentFrame: RadarRenderableFrame?,
    timeZoneId: String,
    mapStyle: ReliableMapStyle,
    field: SpatialWeatherField?,
    state: WeatherHomeUiState
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = Color(0xF4081821),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (layer == ReliableLayer.RADAR) {
                    Surface(
                        modifier = Modifier.size(34.dp).clickable(enabled = frames.size > 1, onClick = onPlayPause),
                        shape = CircleShape,
                        color = Color(0xFF143442)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(reliableFrameLabel(currentFrame, timeZoneId), color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
                        if (frames.size > 1) {
                            Slider(
                                value = frameIndex.toFloat(),
                                onValueChange = { onFrameIndex(it.toInt().coerceIn(0, frames.lastIndex)) },
                                valueRange = 0f..frames.lastIndex.toFloat(),
                                steps = (frames.size - 2).coerceAtLeast(0),
                                modifier = Modifier.height(24.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF58DEE9), inactiveTrackColor = Color.White.copy(alpha = 0.14f))
                            )
                        }
                    }
                } else {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (field == null) "Viewport weather field" else "${field.points.size} Open-Meteo samples • viewport aware",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when (layer) {
                                ReliableLayer.TEMPERATURE -> "Temperature • ${state.temperatureUnit}"
                                ReliableLayer.WIND -> "Wind speed/direction • ${state.windUnit}"
                                ReliableLayer.CLOUDS -> "Cloud cover • %"
                                ReliableLayer.PRECIPITATION -> "Model precipitation • mm"
                                ReliableLayer.RADAR -> ""
                            },
                            color = Color.White.copy(alpha = 0.52f),
                            fontSize = 7.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Opacity", color = Color.White.copy(alpha = 0.52f), fontSize = 7.sp, modifier = Modifier.width(48.dp))
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0.2f..1f,
                    modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF58DEE9), inactiveTrackColor = Color.White.copy(alpha = 0.14f))
                )
                Text("${(opacity * 100).toInt()}%", color = Color.White.copy(alpha = 0.66f), fontSize = 7.sp, modifier = Modifier.width(30.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DIRECT MAP ENGINE", color = Color(0xFF64DDE8), fontSize = 6.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when (mapStyle) {
                        ReliableMapStyle.DARK, ReliableMapStyle.LIGHT -> "© OpenStreetMap © CARTO • fallback OSM"
                        ReliableMapStyle.SATELLITE -> "Esri World Imagery • fallback OSM"
                    } + if (layer == ReliableLayer.RADAR) " • RainViewer" else " • Open-Meteo",
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 5.8.sp
                )
            }
        }
    }
}

private fun buildDirectMapHtml(latitude: Double, longitude: Double, locationName: String): String {
    val quotedName = JSONObject.quote(locationName)
    return """
<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no" />
<style>
html,body,#map{width:100%;height:100%;margin:0;padding:0;overflow:hidden;background:#071820;font-family:system-ui,-apple-system,sans-serif}
#map{position:relative;touch-action:none;user-select:none;-webkit-user-select:none}
.layer{position:absolute;inset:0;overflow:hidden;pointer-events:none;transform:translate3d(0,0,0)}
.tile{position:absolute;object-fit:cover;background:#0a202a}
#base{z-index:1}#weather{z-index:3}#radar{z-index:4}#marker{z-index:6}
.point{position:absolute;border-radius:50%;border:1px solid rgba(255,255,255,.55);box-shadow:0 2px 8px rgba(0,0,0,.55);transform:translate(-50%,-50%);display:flex;align-items:center;justify-content:center;color:white;font-size:9px;font-weight:600;text-shadow:0 1px 3px #000}
.wind{position:absolute;transform:translate(-50%,-50%);color:#eaffff;font-size:17px;text-shadow:0 1px 5px #000}
.marker{position:absolute;width:15px;height:15px;border-radius:50%;background:#27d9e8;border:2px solid #eaffff;box-shadow:0 0 0 8px rgba(39,217,232,.14),0 3px 12px rgba(0,0,0,.55);transform:translate(-50%,-50%)}
.label{position:absolute;padding:4px 7px;border-radius:8px;background:rgba(4,20,28,.88);color:#fff;font-size:10px;white-space:nowrap;transform:translate(-50%,-34px);border:1px solid rgba(255,255,255,.14)}
</style>
</head>
<body>
<div id="map">
  <div id="base" class="layer"></div>
  <div id="weather" class="layer"></div>
  <div id="radar" class="layer"></div>
  <div id="marker" class="layer"></div>
</div>
<script>
const TILE=256, PI=Math.PI;
let centerLat=$latitude, centerLon=$longitude, zoom=8;
let selectedLat=$latitude, selectedLon=$longitude, selectedName=$quotedName;
let baseStyle='dark', radarHost='', radarPath='', radarOpacity=.72;
let fieldKind='', fieldPoints=[], tempUnit='°C', windUnit='km/h', fieldOpacity=.58;
let drag=null, pinch=null, raf=0;
const mapEl=document.getElementById('map');
const baseEl=document.getElementById('base');
const weatherEl=document.getElementById('weather');
const radarEl=document.getElementById('radar');
const markerEl=document.getElementById('marker');
function clamp(v,a,b){return Math.max(a,Math.min(b,v));}
function normLon(v){while(v>180)v-=360;while(v<-180)v+=360;return v;}
function project(lat,lon,z){
 const world=TILE*Math.pow(2,z); const s=Math.sin(clamp(lat,-85.0511,85.0511)*PI/180);
 return {x:(normLon(lon)+180)/360*world,y:(.5-Math.log((1+s)/(1-s))/(4*PI))*world};
}
function unproject(x,y,z){
 const world=TILE*Math.pow(2,z); const lon=x/world*360-180; const n=PI-2*PI*y/world;
 return {lat:180/PI*Math.atan(.5*(Math.exp(n)-Math.exp(-n))),lon:normLon(lon)};
}
function baseUrl(z,x,y){
 const n=Math.pow(2,z), wx=((x%n)+n)%n;
 if(baseStyle==='light')return 'https://a.basemaps.cartocdn.com/light_all/'+z+'/'+wx+'/'+y+'.png';
 if(baseStyle==='satellite')return 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/'+z+'/'+y+'/'+wx;
 return 'https://a.basemaps.cartocdn.com/dark_all/'+z+'/'+wx+'/'+y+'.png';
}
function osmFallback(img,z,x,y){
 const n=Math.pow(2,z),wx=((x%n)+n)%n;
 if(img.dataset.fallback==='1'){img.onerror=null;return;}
 img.dataset.fallback='1';img.src='https://tile.openstreetmap.org/'+z+'/'+wx+'/'+y+'.png';
}
function clear(el){while(el.firstChild)el.removeChild(el.firstChild);}
function renderBase(){
 clear(baseEl); const c=project(centerLat,centerLon,zoom),w=mapEl.clientWidth,h=mapEl.clientHeight,n=Math.pow(2,zoom);
 const minX=Math.floor((c.x-w/2)/TILE)-1,maxX=Math.floor((c.x+w/2)/TILE)+1;
 const minY=Math.max(0,Math.floor((c.y-h/2)/TILE)-1),maxY=Math.min(n-1,Math.floor((c.y+h/2)/TILE)+1);
 for(let ty=minY;ty<=maxY;ty++)for(let tx=minX;tx<=maxX;tx++){
  const img=document.createElement('img');img.className='tile';img.draggable=false;
  img.style.width=TILE+'px';img.style.height=TILE+'px';img.style.left=(tx*TILE-c.x+w/2)+'px';img.style.top=(ty*TILE-c.y+h/2)+'px';
  img.onerror=function(){osmFallback(img,zoom,tx,ty)};img.src=baseUrl(zoom,tx,ty);baseEl.appendChild(img);
 }
}
function renderRadar(){
 clear(radarEl); if(!radarPath||!radarHost)return;
 const nativeZ=Math.min(7,zoom),scale=Math.pow(2,zoom-nativeZ),tileSize=TILE*scale;
 const c=project(centerLat,centerLon,nativeZ),w=mapEl.clientWidth,h=mapEl.clientHeight,n=Math.pow(2,nativeZ);
 const halfW=w/(2*scale),halfH=h/(2*scale);
 const minX=Math.floor((c.x-halfW)/TILE)-1,maxX=Math.floor((c.x+halfW)/TILE)+1;
 const minY=Math.max(0,Math.floor((c.y-halfH)/TILE)-1),maxY=Math.min(n-1,Math.floor((c.y+halfH)/TILE)+1);
 for(let ty=minY;ty<=maxY;ty++)for(let tx=minX;tx<=maxX;tx++){
  const wx=((tx%n)+n)%n,img=document.createElement('img');img.className='tile';img.draggable=false;
  img.style.width=tileSize+'px';img.style.height=tileSize+'px';img.style.opacity=radarOpacity;
  img.style.left=((tx*TILE-c.x)*scale+w/2)+'px';img.style.top=((ty*TILE-c.y)*scale+h/2)+'px';
  img.src=radarHost+radarPath+'/256/'+nativeZ+'/'+wx+'/'+ty+'/2/1_1.png';radarEl.appendChild(img);
 }
}
function tempColor(v){return v<=-10?'#5677ff':v<=0?'#4fc3f7':v<=10?'#42e1d0':v<=20?'#78e46c':v<=30?'#f4dd54':v<=38?'#ff9a3c':'#ff4f48';}
function windColor(v){return v<=10?'#69e6df':v<=25?'#55c8f2':v<=45?'#9b8cff':v<=65?'#f39b58':'#ff5d5d';}
function cloudColor(v){return v<=15?'#295665':v<=40?'#5f7c88':v<=70?'#9aaeb8':'#e0e8ec';}
function precipColor(v){return v<=.05?'#173b48':v<=.5?'#20b8f0':v<=2?'#24dc9a':v<=5?'#e5df4c':v<=10?'#ff8a38':'#f23b4c';}
function screenPoint(lat,lon){const p=project(lat,lon,zoom),c=project(centerLat,centerLon,zoom);return{x:p.x-c.x+mapEl.clientWidth/2,y:p.y-c.y+mapEl.clientHeight/2};}
function renderField(){
 clear(weatherEl); if(!fieldKind||!fieldPoints.length)return;
 fieldPoints.forEach(p=>{const s=screenPoint(p.lat,p.lon);if(s.x<-80||s.y<-80||s.x>mapEl.clientWidth+80||s.y>mapEl.clientHeight+80)return;
  if(fieldKind==='wind'){
   if(p.wind==null)return;const dot=document.createElement('div');dot.className='point';dot.style.left=s.x+'px';dot.style.top=s.y+'px';dot.style.width='28px';dot.style.height='28px';dot.style.opacity=fieldOpacity;dot.style.background=windColor(p.wind);dot.textContent=Math.round(p.wind);weatherEl.appendChild(dot);
   const a=document.createElement('div');a.className='wind';a.style.left=s.x+'px';a.style.top=(s.y-23)+'px';a.style.transform='translate(-50%,-50%) rotate('+Math.round((p.dir||0)+90)+'deg)';a.textContent='➤';weatherEl.appendChild(a);return;
  }
  let v=null,c='#66dce8',txt='';
  if(fieldKind==='temperature'){v=p.temp;if(v==null)return;c=tempColor(v);txt=Math.round(v)+'°';}
  else if(fieldKind==='clouds'){v=p.cloud;if(v==null)return;c=cloudColor(v);txt=Math.round(v)+'%';}
  else if(fieldKind==='precipitation'){v=p.precip;if(v==null)return;c=precipColor(v);txt=v<1?v.toFixed(1):Math.round(v)+'';}
  const dot=document.createElement('div');dot.className='point';dot.style.left=s.x+'px';dot.style.top=s.y+'px';dot.style.width='34px';dot.style.height='34px';dot.style.opacity=fieldOpacity;dot.style.background=c;dot.textContent=txt;weatherEl.appendChild(dot);
 });
}
function renderMarker(){
 clear(markerEl);const s=screenPoint(selectedLat,selectedLon);const m=document.createElement('div');m.className='marker';m.style.left=s.x+'px';m.style.top=s.y+'px';markerEl.appendChild(m);
 const l=document.createElement('div');l.className='label';l.style.left=s.x+'px';l.style.top=s.y+'px';l.textContent=selectedName;markerEl.appendChild(l);
}
function renderAll(){renderBase();renderRadar();renderField();renderMarker();}
function setTransform(dx,dy){[baseEl,weatherEl,radarEl,markerEl].forEach(e=>e.style.transform='translate3d('+dx+'px,'+dy+'px,0)');}
function resetTransform(){setTransform(0,0);}
function emitViewport(){try{if(window.RealWeatherBridge)window.RealWeatherBridge.viewportChanged(centerLat,centerLon,zoom);}catch(e){}}
function moveFrom(startLat,startLon,dx,dy){const c=project(startLat,startLon,zoom),p=unproject(c.x-dx,c.y-dy,zoom);centerLat=clamp(p.lat,-84,84);centerLon=p.lon;}
mapEl.addEventListener('touchstart',e=>{
 if(e.touches.length===1){drag={x:e.touches[0].clientX,y:e.touches[0].clientY,lat:centerLat,lon:centerLon};pinch=null;}
 else if(e.touches.length===2){const dx=e.touches[0].clientX-e.touches[1].clientX,dy=e.touches[0].clientY-e.touches[1].clientY;pinch={dist:Math.hypot(dx,dy),zoom:zoom};drag=null;}
},{passive:false});
mapEl.addEventListener('touchmove',e=>{
 e.preventDefault();
 if(drag&&e.touches.length===1){const dx=e.touches[0].clientX-drag.x,dy=e.touches[0].clientY-drag.y;setTransform(dx,dy);}
},{passive:false});
mapEl.addEventListener('touchend',e=>{
 if(drag){const t=e.changedTouches[0],dx=t.clientX-drag.x,dy=t.clientY-drag.y;moveFrom(drag.lat,drag.lon,dx,dy);drag=null;resetTransform();renderAll();emitViewport();}
 else if(pinch&&e.changedTouches.length){pinch=null;}
},{passive:false});
mapEl.addEventListener('gestureend',e=>{e.preventDefault();},{passive:false});
let lastTouchDistance=0;
mapEl.addEventListener('touchmove',e=>{
 if(e.touches.length===2){const dx=e.touches[0].clientX-e.touches[1].clientX,dy=e.touches[0].clientY-e.touches[1].clientY,d=Math.hypot(dx,dy);if(!lastTouchDistance)lastTouchDistance=d;const ratio=d/lastTouchDistance;if(ratio>1.18){zoomBy(1);lastTouchDistance=d;}else if(ratio<.82){zoomBy(-1);lastTouchDistance=d;}e.preventDefault();}
},{passive:false});
mapEl.addEventListener('touchend',()=>{lastTouchDistance=0;},{passive:true});
function setBaseStyle(v){baseStyle=v||'dark';renderBase();}
function setRadar(host,path,op){radarHost=host||'';radarPath=path||'';radarOpacity=clamp(op||.72,.2,1);renderRadar();}
function clearRadar(){radarPath='';clear(radarEl);}
function setField(kind,points,tUnit,wUnit,op){fieldKind=kind||'';fieldPoints=points||[];tempUnit=tUnit;windUnit=wUnit;fieldOpacity=clamp(op||.58,.2,1);renderField();}
function clearField(){fieldKind='';fieldPoints=[];clear(weatherEl);}
function recenter(){centerLat=selectedLat;centerLon=selectedLon;zoom=Math.max(zoom,8);renderAll();emitViewport();}
function zoomBy(delta){zoom=clamp(zoom+delta,3,12);renderAll();emitViewport();}
window.addEventListener('resize',()=>{if(raf)cancelAnimationFrame(raf);raf=requestAnimationFrame(renderAll);});
renderAll();
setTimeout(()=>{try{if(window.RealWeatherBridge)window.RealWeatherBridge.mapReady();}catch(e){}emitViewport();},80);
</script>
</body>
</html>
    """.trimIndent()
}

private fun ReliableLayer.jsKey(): String = when (this) {
    ReliableLayer.RADAR -> "radar"
    ReliableLayer.TEMPERATURE -> "temperature"
    ReliableLayer.WIND -> "wind"
    ReliableLayer.CLOUDS -> "clouds"
    ReliableLayer.PRECIPITATION -> "precipitation"
}

private fun ReliableMapStyle.next(): ReliableMapStyle = when (this) {
    ReliableMapStyle.DARK -> ReliableMapStyle.LIGHT
    ReliableMapStyle.LIGHT -> ReliableMapStyle.SATELLITE
    ReliableMapStyle.SATELLITE -> ReliableMapStyle.DARK
}

private fun reliableFieldToJson(field: SpatialWeatherField, state: WeatherHomeUiState): String {
    val array = JSONArray()
    field.points.forEach { point ->
        val obj = JSONObject().put("lat", point.latitude).put("lon", point.longitude)
        point.temperatureC?.let {
            obj.put("temp", if (state.temperatureUnit.contains("F", true)) it * 9.0 / 5.0 + 32.0 else it)
        }
        point.windSpeedKmh?.let {
            obj.put("wind", if (state.windUnit.equals("mph", true)) it * 0.621371 else it)
        }
        point.windDirectionDegrees?.let { obj.put("dir", it) }
        point.cloudCoverPercent?.let { obj.put("cloud", it) }
        point.precipitationMm?.let { obj.put("precip", it) }
        array.put(obj)
    }
    return array.toString()
}

private fun reliableFrameLabel(frame: RadarRenderableFrame?, timeZoneId: String): String {
    if (frame == null) return "Radar timeline unavailable"
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("EEE, h:mm a", Locale.getDefault()).withZone(zone)
    val label = formatter.format(Instant.ofEpochSecond(frame.meta.timeEpochSeconds))
    return if (frame.meta.isNowcast) "$label • nowcast" else "$label • observed"
}

private fun reliableSpanForZoom(zoom: Int): Double = when {
    zoom <= 4 -> 4.2
    zoom == 5 -> 3.3
    zoom == 6 -> 2.5
    zoom == 7 -> 1.7
    zoom == 8 -> 1.05
    zoom == 9 -> 0.70
    zoom == 10 -> 0.48
    else -> 0.36
}

private fun normalizeReliableLongitude(value: Double): Double {
    var longitude = value
    while (longitude > 180.0) longitude -= 360.0
    while (longitude < -180.0) longitude += 360.0
    return longitude
}
