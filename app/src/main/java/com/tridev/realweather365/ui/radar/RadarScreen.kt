package com.tridev.realweather365.ui.radar

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

private const val RAINVIEWER_TILE_HOST = "https://tilecache.rainviewer.com"

private enum class WeatherMapLayer {
    RADAR,
    TEMPERATURE,
    WIND,
    CLOUDS,
    PRECIPITATION
}

private enum class BaseMapStyle(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SATELLITE("Satellite")
}

private data class MapViewport(
    val latitude: Double,
    val longitude: Double,
    val zoom: Int
)

private sealed interface RadarLoadState {
    data object Loading : RadarLoadState
    data class Ready(val bundle: RadarMapBundle) : RadarLoadState
    data class Error(val message: String) : RadarLoadState
}

private sealed interface SpatialFieldLoadState {
    data object Idle : SpatialFieldLoadState
    data object Loading : SpatialFieldLoadState
    data class Ready(val field: SpatialWeatherField) : SpatialFieldLoadState
    data class Error(val message: String) : SpatialFieldLoadState
}

private class WeatherMapBridge(
    private val onViewportChanged: (MapViewport) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun viewportChanged(latitude: Double, longitude: Double, zoom: Int) {
        mainHandler.post {
            onViewportChanged(
                MapViewport(
                    latitude = latitude.coerceIn(-84.0, 84.0),
                    longitude = normalizeLongitude(longitude),
                    zoom = zoom.coerceIn(3, 18)
                )
            )
        }
    }
}

@Composable
fun RadarScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val radarRepository = remember(context) { RainViewerRadarRepository(context) }
    val fieldRepository = remember { OpenMeteoMapLayerRepository() }
    val location = state.selectedLocation

    var selectedLayer by rememberSaveable { mutableStateOf(WeatherMapLayer.RADAR) }
    var mapStyle by rememberSaveable { mutableStateOf(BaseMapStyle.DARK) }
    var radarOpacity by rememberSaveable { mutableStateOf(0.72f) }
    var fieldOpacity by rememberSaveable { mutableStateOf(0.58f) }
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    var refreshToken by remember { mutableStateOf(0) }
    var radarState by remember(location.id) { mutableStateOf<RadarLoadState>(RadarLoadState.Loading) }
    var fieldState by remember(location.id) { mutableStateOf<SpatialFieldLoadState>(SpatialFieldLoadState.Idle) }
    var frameIndex by rememberSaveable(location.id) { mutableStateOf(0) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var viewport by remember(location.id) {
        mutableStateOf(MapViewport(location.latitude, location.longitude, 8))
    }

    BackHandler(enabled = fullScreen) {
        fullScreen = false
    }

    LaunchedEffect(location.id, refreshToken) {
        radarState = RadarLoadState.Loading
        radarState = runCatching {
            radarRepository.load(location = location, zoom = 6, maxFrames = 9)
        }.fold(
            onSuccess = { RadarLoadState.Ready(it) },
            onFailure = { RadarLoadState.Error(it.message ?: "Radar unavailable") }
        )
    }

    LaunchedEffect(
        location.id,
        refreshToken,
        selectedLayer,
        viewport.latitude,
        viewport.longitude,
        viewport.zoom
    ) {
        if (selectedLayer == WeatherMapLayer.RADAR) return@LaunchedEffect
        delay(550)
        fieldState = SpatialFieldLoadState.Loading
        val span = spanForZoom(viewport.zoom)
        fieldState = runCatching {
            fieldRepository.load(
                centerLatitude = viewport.latitude,
                centerLongitude = viewport.longitude,
                gridSize = 7,
                halfSpanLatitudeDegrees = span
            )
        }.fold(
            onSuccess = { SpatialFieldLoadState.Ready(it) },
            onFailure = { SpatialFieldLoadState.Error(it.message ?: "Weather map field unavailable") }
        )
    }

    val bundle = (radarState as? RadarLoadState.Ready)?.bundle
    val field = (fieldState as? SpatialFieldLoadState.Ready)?.field
    val frames = bundle?.frames.orEmpty()

    LaunchedEffect(location.id, frames.size) {
        frameIndex = 0
    }

    LaunchedEffect(isPlaying, frames.size, selectedLayer) {
        if (selectedLayer != WeatherMapLayer.RADAR || !isPlaying || frames.size < 2) return@LaunchedEffect
        while (true) {
            delay(1050)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    val safeIndex = frameIndex.coerceIn(0, max(0, frames.lastIndex))
    val currentFrame = frames.getOrNull(safeIndex)

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF06131B))) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!fullScreen) {
                WeatherMapHeader(
                    location = state.location,
                    onBack = onBack,
                    onRefresh = { refreshToken++ }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                InteractiveWeatherMap(
                    state = state,
                    bundle = bundle,
                    frame = currentFrame,
                    field = field,
                    layer = selectedLayer,
                    mapStyle = mapStyle,
                    radarOpacity = radarOpacity,
                    fieldOpacity = fieldOpacity,
                    radarLoading = radarState is RadarLoadState.Loading,
                    fieldLoading = fieldState is SpatialFieldLoadState.Loading,
                    radarError = (radarState as? RadarLoadState.Error)?.message,
                    fieldError = (fieldState as? SpatialFieldLoadState.Error)?.message,
                    onViewportChanged = { viewport = it },
                    modifier = Modifier.fillMaxSize()
                )

                WeatherLayerControls(
                    selectedLayer = selectedLayer,
                    onLayerChange = { selectedLayer = it },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                )

                WeatherMapStatusBadge(
                    state = state,
                    layer = selectedLayer,
                    bundle = bundle,
                    frame = currentFrame,
                    radarState = radarState,
                    fieldState = fieldState,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = 12.dp,
                            top = if (fullScreen) 62.dp else 12.dp
                        )
                )

                MapStyleButton(
                    style = mapStyle,
                    onCycle = { mapStyle = mapStyle.next() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 10.dp, top = 10.dp)
                )

                FullScreenButton(
                    fullScreen = fullScreen,
                    onClick = { fullScreen = !fullScreen },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 58.dp, bottom = 48.dp)
                )

                if (fullScreen) {
                    FullScreenTopBar(
                        location = state.location,
                        onBack = onBack,
                        onExitFullScreen = { fullScreen = false },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    CompactOpacityControl(
                        value = if (selectedLayer == WeatherMapLayer.RADAR) radarOpacity else fieldOpacity,
                        onValueChanged = {
                            if (selectedLayer == WeatherMapLayer.RADAR) radarOpacity = it else fieldOpacity = it
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
            }

            if (!fullScreen) {
                WeatherMapBottomPanel(
                    state = state,
                    layer = selectedLayer,
                    mapStyle = mapStyle,
                    onMapStyleChanged = { mapStyle = it },
                    fieldState = fieldState,
                    frames = frames,
                    frameIndex = safeIndex,
                    onFrameIndexChanged = { frameIndex = it },
                    isPlaying = isPlaying,
                    onTogglePlay = { isPlaying = !isPlaying },
                    radarOpacity = radarOpacity,
                    onRadarOpacityChanged = { radarOpacity = it },
                    fieldOpacity = fieldOpacity,
                    onFieldOpacityChanged = { fieldOpacity = it },
                    timeZoneId = location.timeZoneId,
                    languageCode = state.languageCode,
                    bundle = bundle
                )
            }
        }
    }
}

@Composable
private fun WeatherMapHeader(location: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        color = Color(0xF2071720),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFF75E5F0), modifier = Modifier.size(13.dp))
                    Text(location, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Advanced Weather Map • live geographic layers",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 8.sp,
                    letterSpacing = 0.3.sp
                )
            }
            Box(Modifier.size(40.dp).clickable(onClick = onRefresh), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Refresh, "Refresh map data", tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FullScreenTopBar(
    location: String,
    onBack: () -> Unit,
    onExitFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().statusBarsPadding(),
        color = Color(0xB905171F),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(38.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Text(
                text = location,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Box(Modifier.size(38.dp).clickable(onClick = onExitFullScreen), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FullscreenExit, "Exit full screen", tint = Color.White, modifier = Modifier.size(21.dp))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InteractiveWeatherMap(
    state: WeatherHomeUiState,
    bundle: RadarMapBundle?,
    frame: RadarRenderableFrame?,
    field: SpatialWeatherField?,
    layer: WeatherMapLayer,
    mapStyle: BaseMapStyle,
    radarOpacity: Float,
    fieldOpacity: Float,
    radarLoading: Boolean,
    fieldLoading: Boolean,
    radarError: String?,
    fieldError: String?,
    onViewportChanged: (MapViewport) -> Unit,
    modifier: Modifier = Modifier
) {
    val location = state.selectedLocation
    var webView by remember(location.id) { mutableStateOf<WebView?>(null) }
    var mapReady by remember(location.id) { mutableStateOf(false) }
    val bridge = remember(location.id) { WeatherMapBridge(onViewportChanged) }

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

    Box(modifier = modifier.background(Color(0xFF0A202A))) {
        key(location.id) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.rgb(8, 24, 32))
                        overScrollMode = View.OVER_SCROLL_NEVER
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(false)
                        addJavascriptInterface(bridge, "RealWeatherBridge")
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                mapReady = true
                            }
                        }
                        loadDataWithBaseURL(
                            "https://realweather365.local/",
                            buildMapHtml(location.latitude, location.longitude, location.name),
                            "text/html",
                            "UTF-8",
                            null
                        )
                        webView = this
                    }
                },
                update = { webView = it }
            )
        }

        LaunchedEffect(webView, mapReady, frame?.meta?.path, layer, radarOpacity) {
            val view = webView ?: return@LaunchedEffect
            if (!mapReady) return@LaunchedEffect
            view.evaluateJavascript("setRadarOpacity(${radarOpacity.coerceIn(0.2f, 1f)});", null)
            if (layer == WeatherMapLayer.RADAR && frame != null) {
                view.evaluateJavascript(
                    "setRadarFrame(${JSONObject.quote(RAINVIEWER_TILE_HOST)},${JSONObject.quote(frame.meta.path)},true);",
                    null
                )
            } else {
                view.evaluateJavascript("setRadarVisible(false);", null)
            }
        }

        LaunchedEffect(webView, mapReady, field, layer, state.temperatureUnit, state.windUnit, fieldOpacity) {
            val view = webView ?: return@LaunchedEffect
            if (!mapReady) return@LaunchedEffect
            view.evaluateJavascript("setWeatherOpacity(${fieldOpacity.coerceIn(0.2f, 0.95f)});", null)
            if (layer == WeatherMapLayer.RADAR || field == null) {
                view.evaluateJavascript("clearWeatherField();", null)
            } else {
                view.evaluateJavascript(
                    "setWeatherField(${JSONObject.quote(layer.jsKey())},${fieldToJson(field, state)},${field.latitudeStep},${field.longitudeStep},${JSONObject.quote(state.temperatureUnit)},${JSONObject.quote(state.windUnit)});",
                    null
                )
            }
        }

        LaunchedEffect(webView, mapReady, mapStyle) {
            val view = webView ?: return@LaunchedEffect
            if (!mapReady) return@LaunchedEffect
            view.evaluateJavascript("setMapStyle(${JSONObject.quote(mapStyle.name.lowercase(Locale.US))});", null)
        }

        LaunchedEffect(webView, mapReady, location.latitude, location.longitude) {
            val view = webView ?: return@LaunchedEffect
            if (!mapReady) return@LaunchedEffect
            view.evaluateJavascript(
                "setSelectedLocation(${location.latitude},${location.longitude},${JSONObject.quote(location.name)});",
                null
            )
        }

        val loading = if (layer == WeatherMapLayer.RADAR) radarLoading else fieldLoading
        if (loading) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xD9071922),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF69E3EE), strokeWidth = 2.dp)
                    Text(
                        if (layer == WeatherMapLayer.RADAR) "Loading real radar…" else "Refreshing viewport weather field…",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }

        val layerError = if (layer == WeatherMapLayer.RADAR) radarError else fieldError
        if (!loading && layerError != null) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xE40A1C25),
                border = BorderStroke(1.dp, Color(0xFFFFB15B).copy(alpha = 0.32f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Weather layer unavailable", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(layerError, color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp, textAlign = TextAlign.Center)
                }
            }
        }

        RecenterButton(
            onClick = { webView?.evaluateJavascript("recenterMap();", null) },
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 48.dp)
        )

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xC905161E),
            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f))
        ) {
            Text(
                "Drag / pinch • viewport-aware fields • ${mapStyleAttribution(mapStyle)}",
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 6.5.sp,
                textAlign = TextAlign.Center
            )
        }

        if (layer == WeatherMapLayer.RADAR && bundle?.coverageAvailable == false) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xE40A1C25),
                border = BorderStroke(1.dp, Color(0xFFFFB25C).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Radar coverage unavailable", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Switch to Temp, Wind, Cloud or Precip for model-backed spatial fields.",
                        color = Color.White.copy(alpha = 0.58f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RecenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = Color(0xD9071922),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Icon(
            Icons.Outlined.MyLocation,
            contentDescription = "Recenter map",
            tint = Color(0xFF70E2EC),
            modifier = Modifier.padding(10.dp).size(19.dp)
        )
    }
}

@Composable
private fun FullScreenButton(fullScreen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = Color(0xD9071922),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Icon(
            imageVector = if (fullScreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
            contentDescription = if (fullScreen) "Exit full screen" else "Full screen",
            tint = Color.White,
            modifier = Modifier.padding(10.dp).size(19.dp)
        )
    }
}

@Composable
private fun MapStyleButton(style: BaseMapStyle, onCycle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onCycle),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xD0071922),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Outlined.Layers, null, tint = Color(0xFF6CE3EE), modifier = Modifier.size(15.dp))
            Text(style.label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CompactOpacityControl(
    value: Float,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(220.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xDA071922),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Opacity", color = Color.White.copy(alpha = 0.70f), fontSize = 7.sp)
            Slider(
                value = value,
                onValueChange = onValueChanged,
                valueRange = 0.2f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF55DDE9),
                    inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                ),
                modifier = Modifier.weight(1f).height(28.dp)
            )
            Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 7.sp)
        }
    }
}

private fun buildMapHtml(latitude: Double, longitude: Double, locationName: String): String {
    val quotedName = JSONObject.quote(locationName)
    return """
        <!doctype html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="" />
          <style>
            html, body, #map { width:100%; height:100%; margin:0; padding:0; background:#081820; overflow:hidden; }
            .leaflet-container { background:#081820; font-family:system-ui,-apple-system,sans-serif; }
            .leaflet-control-zoom { border:none !important; box-shadow:0 4px 16px rgba(0,0,0,.38) !important; }
            .leaflet-control-zoom a { background:rgba(6,24,32,.92) !important; color:#e9fbff !important; border-color:rgba(255,255,255,.12) !important; }
            .leaflet-control-zoom a:hover { background:rgba(12,44,56,.96) !important; }
            .rw-tooltip { background:rgba(4,20,28,.94); color:white; border:1px solid rgba(255,255,255,.18); box-shadow:0 4px 18px rgba(0,0,0,.42); border-radius:9px; font-size:11px; padding:5px 8px; }
            .rw-tooltip:before { border-top-color:rgba(4,20,28,.94) !important; }
            .wind-arrow { color:#eaffff; text-shadow:0 1px 5px rgba(0,0,0,.85); font-size:18px; line-height:18px; width:20px; height:20px; text-align:center; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin=""></script>
          <script>
            let map = null;
            let base = null;
            let baseStyle = 'dark';
            let radarCurrent = null;
            let radarPending = null;
            let radarOpacity = 0.72;
            let weatherGroup = null;
            let weatherOpacity = 0.58;
            let lastWeather = null;
            let selectedMarker = null;
            let selectedLat = $latitude;
            let selectedLon = $longitude;
            let selectedName = $quotedName;
            let viewportTimer = null;

            function normalizeLon(lon) {
              while (lon > 180) lon -= 360;
              while (lon < -180) lon += 360;
              return lon;
            }

            function baseConfig(style) {
              if (style === 'light') {
                return {url:'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png', options:{subdomains:'abcd',maxZoom:19,minZoom:3}};
              }
              if (style === 'satellite') {
                return {url:'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', options:{maxZoom:19,minZoom:3}};
              }
              return {url:'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png', options:{subdomains:'abcd',maxZoom:19,minZoom:3}};
            }

            function setMapStyle(style) {
              baseStyle = style || 'dark';
              if (!map || !window.L) return;
              if (base) map.removeLayer(base);
              const cfg = baseConfig(baseStyle);
              base = L.tileLayer(cfg.url, Object.assign({updateWhenIdle:false,keepBuffer:4}, cfg.options)).addTo(map);
              base.bringToBack();
            }

            function promoteRadar(next) {
              if (!map || !next) return;
              const old = radarCurrent;
              radarCurrent = next;
              radarCurrent.setOpacity(radarOpacity);
              if (old && old !== next) {
                old.setOpacity(0.0);
                setTimeout(function(){ if (map && map.hasLayer(old)) map.removeLayer(old); }, 380);
              }
            }

            function setRadarFrame(host, path, visible) {
              radarPending = {host:host,path:path,visible:visible};
              if (!map || !window.L) return;
              if (!visible || !path) {
                setRadarVisible(false);
                return;
              }
              const template = host + path + '/256/{z}/{x}/{y}/2/1_1.png';
              const next = L.tileLayer(template, {
                tileSize:256,
                opacity:0.0,
                maxNativeZoom:7,
                minZoom:3,
                maxZoom:18,
                pane:'radarPane',
                updateWhenIdle:false,
                keepBuffer:3
              });
              let promoted = false;
              const promote = function(){
                if (promoted) return;
                promoted = true;
                promoteRadar(next);
              };
              next.once('load', promote);
              next.addTo(map);
              setTimeout(promote, 750);
            }

            function setRadarVisible(visible) {
              if (!visible) {
                if (radarCurrent && map && map.hasLayer(radarCurrent)) map.removeLayer(radarCurrent);
                radarCurrent = null;
              } else if (radarPending) {
                setRadarFrame(radarPending.host, radarPending.path, true);
              }
            }

            function setRadarOpacity(value) {
              radarOpacity = Math.max(0.2, Math.min(1.0, value));
              if (radarCurrent) radarCurrent.setOpacity(radarOpacity);
            }

            function clearWeatherField() {
              lastWeather = null;
              if (weatherGroup && map) map.removeLayer(weatherGroup);
              weatherGroup = null;
            }

            function tempColor(v) {
              if (v <= -10) return '#5677ff';
              if (v <= 0) return '#4fc3f7';
              if (v <= 10) return '#42e1d0';
              if (v <= 20) return '#78e46c';
              if (v <= 30) return '#f4dd54';
              if (v <= 38) return '#ff9a3c';
              return '#ff4f48';
            }
            function windColor(v) {
              if (v <= 10) return '#69e6df';
              if (v <= 25) return '#55c8f2';
              if (v <= 45) return '#9b8cff';
              if (v <= 65) return '#f39b58';
              return '#ff5d5d';
            }
            function cloudColor(v) {
              if (v <= 15) return '#295665';
              if (v <= 40) return '#5f7c88';
              if (v <= 70) return '#9aaeb8';
              return '#e0e8ec';
            }
            function precipColor(v) {
              if (v <= 0.05) return '#173b48';
              if (v <= 0.5) return '#20b8f0';
              if (v <= 2.0) return '#24dc9a';
              if (v <= 5.0) return '#e5df4c';
              if (v <= 10.0) return '#ff8a38';
              return '#f23b4c';
            }

            function renderWeatherField() {
              if (!map || !window.L || !lastWeather) return;
              if (weatherGroup) map.removeLayer(weatherGroup);
              weatherGroup = L.layerGroup([], {pane:'weatherPane'}).addTo(map);
              const kind = lastWeather.kind;
              const points = lastWeather.points || [];
              const latHalf = Math.max(0.02, lastWeather.latStep * 0.52);
              const lonHalf = Math.max(0.02, lastWeather.lonStep * 0.52);

              points.forEach(function(p) {
                let value = null;
                let color = '#66dce8';
                let label = '';
                if (kind === 'temperature') {
                  value = p.temp;
                  if (value == null) return;
                  color = tempColor(value);
                  label = value.toFixed(1) + ' ' + lastWeather.tempUnit;
                } else if (kind === 'clouds') {
                  value = p.cloud;
                  if (value == null) return;
                  color = cloudColor(value);
                  label = Math.round(value) + '% cloud';
                } else if (kind === 'precipitation') {
                  value = p.precip;
                  if (value == null) return;
                  color = precipColor(value);
                  label = value.toFixed(2) + ' mm';
                } else if (kind === 'wind') {
                  value = p.wind;
                  if (value == null) return;
                  color = windColor(value);
                  label = value.toFixed(1) + ' ' + lastWeather.windUnit + ' • ' + Math.round(p.dir || 0) + '°';
                  const marker = L.circleMarker([p.lat,p.lon], {
                    radius:8,
                    color:color,
                    weight:1,
                    fillColor:color,
                    fillOpacity:Math.min(0.82, weatherOpacity + 0.08),
                    pane:'weatherPane'
                  }).bindTooltip(label, {direction:'top', className:'rw-tooltip'});
                  weatherGroup.addLayer(marker);
                  const arrow = L.marker([p.lat,p.lon], {
                    pane:'weatherPane',
                    interactive:false,
                    icon:L.divIcon({
                      className:'',
                      html:'<div class="wind-arrow" style="transform:rotate(' + Math.round((p.dir || 0) + 90) + 'deg)">➤</div>',
                      iconSize:[20,20],
                      iconAnchor:[10,10]
                    })
                  });
                  weatherGroup.addLayer(arrow);
                  return;
                }

                const bounds = [[p.lat-latHalf,p.lon-lonHalf],[p.lat+latHalf,p.lon+lonHalf]];
                const rect = L.rectangle(bounds, {
                  pane:'weatherPane',
                  color:color,
                  weight:0.35,
                  opacity:Math.min(0.75, weatherOpacity + 0.08),
                  fillColor:color,
                  fillOpacity:weatherOpacity
                }).bindTooltip(label, {direction:'top', className:'rw-tooltip'});
                weatherGroup.addLayer(rect);
              });
            }

            function setWeatherField(kind, points, latStep, lonStep, tempUnit, windUnit) {
              lastWeather = {kind:kind,points:points,latStep:latStep,lonStep:lonStep,tempUnit:tempUnit,windUnit:windUnit};
              renderWeatherField();
            }

            function setWeatherOpacity(value) {
              weatherOpacity = Math.max(0.2, Math.min(0.95, value));
              if (lastWeather) renderWeatherField();
            }

            function setSelectedLocation(lat, lon, name) {
              selectedLat = lat; selectedLon = lon; selectedName = name;
              if (!map || !window.L) return;
              if (selectedMarker) map.removeLayer(selectedMarker);
              selectedMarker = L.circleMarker([lat,lon], {
                radius:7,
                color:'#dffcff',
                weight:2,
                fillColor:'#24d8e9',
                fillOpacity:1
              }).addTo(map).bindTooltip(name, {permanent:false,direction:'top',className:'rw-tooltip'});
            }

            function recenterMap() {
              if (map) map.flyTo([selectedLat,selectedLon], Math.max(map.getZoom(),8), {duration:0.55});
            }

            function emitViewport() {
              if (!map || !window.RealWeatherBridge || !window.RealWeatherBridge.viewportChanged) return;
              const center = map.getCenter();
              window.RealWeatherBridge.viewportChanged(center.lat, normalizeLon(center.lng), map.getZoom());
            }

            function scheduleViewport() {
              if (viewportTimer) clearTimeout(viewportTimer);
              viewportTimer = setTimeout(emitViewport, 320);
            }

            function init() {
              if (!window.L) {
                setTimeout(init, 250);
                return;
              }
              map = L.map('map', {
                zoomControl:true,
                attributionControl:false,
                minZoom:3,
                maxZoom:18,
                worldCopyJump:true,
                preferCanvas:true,
                zoomAnimation:true,
                fadeAnimation:true,
                markerZoomAnimation:true
              }).setView([selectedLat,selectedLon],8);

              map.createPane('weatherPane');
              map.getPane('weatherPane').style.zIndex = 420;
              map.getPane('weatherPane').style.pointerEvents = 'none';
              map.createPane('radarPane');
              map.getPane('radarPane').style.zIndex = 430;
              map.getPane('radarPane').style.pointerEvents = 'none';

              setMapStyle(baseStyle);
              setSelectedLocation(selectedLat,selectedLon,selectedName);
              map.on('moveend zoomend', scheduleViewport);
              scheduleViewport();
              if (radarPending) setRadarFrame(radarPending.host,radarPending.path,radarPending.visible);
              if (lastWeather) renderWeatherField();
            }
            init();
          </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun WeatherMapStatusBadge(
    state: WeatherHomeUiState,
    layer: WeatherMapLayer,
    bundle: RadarMapBundle?,
    frame: RadarRenderableFrame?,
    radarState: RadarLoadState,
    fieldState: SpatialFieldLoadState,
    modifier: Modifier = Modifier
) {
    val label = when (layer) {
        WeatherMapLayer.RADAR -> when {
            radarState is RadarLoadState.Loading -> "RADAR • CONNECTING"
            radarState is RadarLoadState.Error -> "RADAR • ERROR"
            bundle?.coverageAvailable == false -> "RADAR • NO COVERAGE"
            frame == null -> "RADAR • UNAVAILABLE"
            frame.meta.isNowcast -> "PRECIPITATION • NOWCAST"
            frame.hasPrecipitationEcho -> "PRECIPITATION • OBSERVED"
            else -> "PRECIPITATION • NO ECHO"
        }
        WeatherMapLayer.TEMPERATURE -> fieldStatus("TEMPERATURE", fieldState)
        WeatherMapLayer.WIND -> fieldStatus("WIND", fieldState)
        WeatherMapLayer.CLOUDS -> fieldStatus("CLOUD COVER", fieldState)
        WeatherMapLayer.PRECIPITATION -> fieldStatus("MODEL PRECIP", fieldState)
    }
    val dot = when {
        radarState is RadarLoadState.Error || fieldState is SpatialFieldLoadState.Error -> Color(0xFFFF8C57)
        radarState is RadarLoadState.Loading || fieldState is SpatialFieldLoadState.Loading -> Color(0xFFFFC65B)
        layer == WeatherMapLayer.RADAR && bundle?.coverageAvailable == false -> Color(0xFFFF9F55)
        layer == WeatherMapLayer.RADAR && frame?.meta?.isNowcast == true -> Color(0xFF69DDF0)
        else -> Color(0xFF55EAA5)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xD0081B25),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(7.dp).background(dot, CircleShape))
            Text(label, color = Color.White.copy(alpha = 0.88f), fontSize = 7.5.sp, fontWeight = FontWeight.Medium)
            if (layer == WeatherMapLayer.TEMPERATURE && fieldState is SpatialFieldLoadState.Ready) {
                Text("• ${state.temperature}°", color = Color.White.copy(alpha = 0.55f), fontSize = 7.sp)
            }
        }
    }
}

private fun fieldStatus(prefix: String, state: SpatialFieldLoadState): String = when (state) {
    SpatialFieldLoadState.Idle -> "$prefix • READY"
    SpatialFieldLoadState.Loading -> "$prefix • REFRESHING"
    is SpatialFieldLoadState.Error -> "$prefix • ERROR"
    is SpatialFieldLoadState.Ready -> "$prefix • LIVE MODEL"
}

@Composable
private fun WeatherLayerControls(
    selectedLayer: WeatherMapLayer,
    onLayerChange: (WeatherMapLayer) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xD0071922),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LayerControlItem(Icons.Outlined.WaterDrop, "Radar", selectedLayer == WeatherMapLayer.RADAR) { onLayerChange(WeatherMapLayer.RADAR) }
            LayerControlItem(Icons.Outlined.Thermostat, "Temp", selectedLayer == WeatherMapLayer.TEMPERATURE) { onLayerChange(WeatherMapLayer.TEMPERATURE) }
            LayerControlItem(Icons.Outlined.Air, "Wind", selectedLayer == WeatherMapLayer.WIND) { onLayerChange(WeatherMapLayer.WIND) }
            LayerControlItem(Icons.Outlined.Cloud, "Cloud", selectedLayer == WeatherMapLayer.CLOUDS) { onLayerChange(WeatherMapLayer.CLOUDS) }
            LayerControlItem(Icons.Outlined.WaterDrop, "Precip", selectedLayer == WeatherMapLayer.PRECIPITATION) { onLayerChange(WeatherMapLayer.PRECIPITATION) }
        }
    }
}

@Composable
private fun LayerControlItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Color(0xFF64E8F2) else Color.White.copy(alpha = 0.72f)
    val background = if (selected) Color(0x263CE7F3) else Color.Transparent
    Column(
        modifier = Modifier
            .width(54.dp)
            .background(background, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(17.dp))
        Text(label, color = tint, fontSize = 6.7.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun WeatherMapBottomPanel(
    state: WeatherHomeUiState,
    layer: WeatherMapLayer,
    mapStyle: BaseMapStyle,
    onMapStyleChanged: (BaseMapStyle) -> Unit,
    fieldState: SpatialFieldLoadState,
    frames: List<RadarRenderableFrame>,
    frameIndex: Int,
    onFrameIndexChanged: (Int) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    radarOpacity: Float,
    onRadarOpacityChanged: (Float) -> Unit,
    fieldOpacity: Float,
    onFieldOpacityChanged: (Float) -> Unit,
    timeZoneId: String,
    languageCode: String,
    bundle: RadarMapBundle?
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = Color(0xF4081821),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            MapStyleSelector(mapStyle, onMapStyleChanged)
            Spacer(Modifier.height(7.dp))

            if (layer == WeatherMapLayer.RADAR) {
                RadarTimeline(
                    frames = frames,
                    frameIndex = frameIndex,
                    onFrameIndexChanged = onFrameIndexChanged,
                    isPlaying = isPlaying,
                    onTogglePlay = onTogglePlay,
                    timeZoneId = timeZoneId,
                    languageCode = languageCode,
                    bundle = bundle
                )
                Spacer(Modifier.height(6.dp))
                OpacityRow("Radar opacity", radarOpacity, onRadarOpacityChanged)
                Spacer(Modifier.height(7.dp))
                RadarLegend()
            } else {
                SpatialFieldPanel(
                    state = state,
                    layer = layer,
                    fieldState = fieldState,
                    opacity = fieldOpacity,
                    onOpacityChanged = onFieldOpacityChanged
                )
            }

            Spacer(Modifier.height(7.dp))
            MapAttribution(mapStyle, layer)
        }
    }
}

@Composable
private fun MapStyleSelector(style: BaseMapStyle, onStyleChanged: (BaseMapStyle) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BaseMapStyle.entries.forEach { item ->
            val selected = item == style
            Surface(
                modifier = Modifier.weight(1f).clickable { onStyleChanged(item) },
                shape = RoundedCornerShape(10.dp),
                color = if (selected) Color(0xFF153845) else Color(0xFF0D2530),
                border = BorderStroke(0.6.dp, if (selected) Color(0xFF61DDE9) else Color.White.copy(alpha = 0.10f))
            ) {
                Text(
                    item.label,
                    color = if (selected) Color(0xFF75E8F1) else Color.White.copy(alpha = 0.62f),
                    fontSize = 7.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun RadarTimeline(
    frames: List<RadarRenderableFrame>,
    frameIndex: Int,
    onFrameIndexChanged: (Int) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    timeZoneId: String,
    languageCode: String,
    bundle: RadarMapBundle?
) {
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
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    frameLabel(frames.getOrNull(frameIndex), timeZoneId, languageCode),
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (frames.getOrNull(frameIndex)?.meta?.isNowcast == true) "NOWCAST" else "OBSERVED",
                    color = if (frames.getOrNull(frameIndex)?.meta?.isNowcast == true) Color(0xFF6DE3EF) else Color.White.copy(alpha = 0.48f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (frames.size > 1) {
                Slider(
                    value = frameIndex.toFloat(),
                    onValueChange = { onFrameIndexChanged(it.toInt().coerceIn(0, frames.lastIndex)) },
                    valueRange = 0f..frames.lastIndex.toFloat(),
                    steps = (frames.size - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF55DDE9),
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            } else {
                Text(
                    bundle?.errorMessage ?: "Radar timeline unavailable",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 7.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SpatialFieldPanel(
    state: WeatherHomeUiState,
    layer: WeatherMapLayer,
    fieldState: SpatialFieldLoadState,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit
) {
    when (fieldState) {
        SpatialFieldLoadState.Idle -> Text("Move or select a model layer to load the visible area.", color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp)
        SpatialFieldLoadState.Loading -> Text("Sampling the visible map area from Open-Meteo…", color = Color.White.copy(alpha = 0.68f), fontSize = 8.sp)
        is SpatialFieldLoadState.Error -> Text(fieldState.message, color = Color(0xFFFFAF65), fontSize = 8.sp)
        is SpatialFieldLoadState.Ready -> {
            val field = fieldState.field
            Text(
                "Viewport field • ${field.points.size} live model samples • ${formatFieldTime(field.fetchedAtEpochMillis)}",
                color = Color.White.copy(alpha = 0.66f),
                fontSize = 7.5.sp
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    OpacityRow("Layer opacity", opacity, onOpacityChanged)
    Spacer(Modifier.height(7.dp))
    SpatialLegend(layer, state)
}

@Composable
private fun OpacityRow(label: String, value: Float, onValueChanged: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 7.sp, modifier = Modifier.width(72.dp))
        Slider(
            value = value,
            onValueChange = onValueChanged,
            valueRange = 0.2f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF55DDE9),
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.weight(1f).height(26.dp)
        )
        Text("${(value * 100).toInt()}%", color = Color.White.copy(alpha = 0.70f), fontSize = 7.sp, modifier = Modifier.width(30.dp))
    }
}

@Composable
private fun RadarLegend() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Light", color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF1CBDF2),
                            Color(0xFF20E096),
                            Color(0xFFE8E44A),
                            Color(0xFFFF8A33),
                            Color(0xFFF33449)
                        )
                    ),
                    RoundedCornerShape(4.dp)
                )
        )
        Text("Heavy", color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
    }
}

@Composable
private fun SpatialLegend(layer: WeatherMapLayer, state: WeatherHomeUiState) {
    val labels: Pair<String, String>
    val colors: List<Color>
    when (layer) {
        WeatherMapLayer.TEMPERATURE -> {
            labels = (if (state.temperatureUnit.contains("F")) "Cold °F" else "Cold °C") to "Hot"
            colors = listOf(Color(0xFF5677FF), Color(0xFF4FC3F7), Color(0xFF78E46C), Color(0xFFF4DD54), Color(0xFFFF4F48))
        }
        WeatherMapLayer.WIND -> {
            labels = "Calm" to "Strong ${state.windUnit}"
            colors = listOf(Color(0xFF69E6DF), Color(0xFF55C8F2), Color(0xFF9B8CFF), Color(0xFFF39B58), Color(0xFFFF5D5D))
        }
        WeatherMapLayer.CLOUDS -> {
            labels = "Clear" to "Overcast"
            colors = listOf(Color(0xFF295665), Color(0xFF5F7C88), Color(0xFF9AAEB8), Color(0xFFE0E8EC))
        }
        WeatherMapLayer.PRECIPITATION -> {
            labels = "0 mm" to "Heavy"
            colors = listOf(Color(0xFF173B48), Color(0xFF20B8F0), Color(0xFF24DC9A), Color(0xFFE5DF4C), Color(0xFFFF8A38), Color(0xFFF23B4C))
        }
        WeatherMapLayer.RADAR -> return
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(labels.first, color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(4.dp)
                .background(Brush.horizontalGradient(colors), RoundedCornerShape(4.dp))
        )
        Text(labels.second, color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
    }
}

@Composable
private fun MapAttribution(style: BaseMapStyle, layer: WeatherMapLayer) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("REAL GEOGRAPHIC MAP", color = Color(0xFF65DDE8), fontSize = 6.5.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (layer == WeatherMapLayer.RADAR) {
                "${mapStyleAttribution(style)} • Radar: RainViewer"
            } else {
                "${mapStyleAttribution(style)} • Fields: Open-Meteo"
            },
            color = Color.White.copy(alpha = 0.40f),
            fontSize = 6.2.sp
        )
    }
}

private fun fieldToJson(field: SpatialWeatherField, state: WeatherHomeUiState): String {
    val array = JSONArray()
    field.points.forEach { point ->
        val obj = JSONObject()
            .put("lat", point.latitude)
            .put("lon", point.longitude)
        point.temperatureC?.let { obj.put("temp", convertTemperature(it, state.temperatureUnit)) }
        point.windSpeedKmh?.let { obj.put("wind", convertWind(it, state.windUnit)) }
        point.windDirectionDegrees?.let { obj.put("dir", it) }
        point.cloudCoverPercent?.let { obj.put("cloud", it) }
        point.precipitationMm?.let { obj.put("precip", it) }
        array.put(obj)
    }
    return array.toString()
}

private fun WeatherMapLayer.jsKey(): String = when (this) {
    WeatherMapLayer.RADAR -> "radar"
    WeatherMapLayer.TEMPERATURE -> "temperature"
    WeatherMapLayer.WIND -> "wind"
    WeatherMapLayer.CLOUDS -> "clouds"
    WeatherMapLayer.PRECIPITATION -> "precipitation"
}

private fun BaseMapStyle.next(): BaseMapStyle = when (this) {
    BaseMapStyle.DARK -> BaseMapStyle.LIGHT
    BaseMapStyle.LIGHT -> BaseMapStyle.SATELLITE
    BaseMapStyle.SATELLITE -> BaseMapStyle.DARK
}

private fun mapStyleAttribution(style: BaseMapStyle): String = when (style) {
    BaseMapStyle.DARK, BaseMapStyle.LIGHT -> "© OpenStreetMap © CARTO"
    BaseMapStyle.SATELLITE -> "Esri World Imagery"
}

private fun spanForZoom(zoom: Int): Double = when {
    zoom <= 4 -> 4.2
    zoom == 5 -> 3.3
    zoom == 6 -> 2.5
    zoom == 7 -> 1.7
    zoom == 8 -> 1.05
    zoom == 9 -> 0.70
    zoom == 10 -> 0.48
    else -> 0.36
}

private fun convertTemperature(celsius: Double, unit: String): Double =
    if (unit.contains("F", ignoreCase = true)) celsius * 9.0 / 5.0 + 32.0 else celsius

private fun convertWind(kmh: Double, unit: String): Double =
    if (unit.equals("mph", ignoreCase = true)) kmh * 0.621371 else kmh

private fun frameLabel(frame: RadarRenderableFrame?, timeZoneId: String, languageCode: String): String {
    if (frame == null) return if (languageCode == "hi") "रडार डेटा लोड हो रहा है" else "Radar data loading"
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("EEE, h:mm a", Locale.getDefault()).withZone(zone)
    val time = formatter.format(Instant.ofEpochSecond(frame.meta.timeEpochSeconds))
    return if (frame.meta.isNowcast) "$time • forecast radar" else time
}

private fun formatFieldTime(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

private fun normalizeLongitude(value: Double): Double {
    var longitude = value
    while (longitude > 180.0) longitude -= 360.0
    while (longitude < -180.0) longitude += 360.0
    return longitude
}
