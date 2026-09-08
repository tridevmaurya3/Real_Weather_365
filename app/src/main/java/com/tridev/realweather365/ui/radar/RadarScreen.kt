package com.tridev.realweather365.ui.radar

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tridev.realweather365.data.radar.RadarMapBundle
import com.tridev.realweather365.data.radar.RadarRenderableFrame
import com.tridev.realweather365.data.radar.RainViewerRadarRepository
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private const val RAINVIEWER_TILE_HOST = "https://tilecache.rainviewer.com"

private enum class RadarLayer {
    RAIN,
    TEMPERATURE_CONTEXT
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
    var refreshToken by remember { mutableStateOf(0) }
    var radarState by remember(location.id) { mutableStateOf<RadarLoadState>(RadarLoadState.Loading) }
    var frameIndex by rememberSaveable(location.id) { mutableStateOf(0) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(location.id, refreshToken) {
        radarState = RadarLoadState.Loading
        radarState = RadarLoadState.Ready(
            repository.load(
                location = location,
                zoom = 6,
                maxFrames = 7
            )
        )
    }

    val bundle = (radarState as? RadarLoadState.Ready)?.bundle
    val frames = bundle?.frames.orEmpty()

    LaunchedEffect(location.id, frames.size) {
        frameIndex = 0
    }

    LaunchedEffect(isPlaying, frames.size, selectedLayer) {
        if (selectedLayer != RadarLayer.RAIN || !isPlaying || frames.size < 2) return@LaunchedEffect
        while (true) {
            delay(900)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    val safeIndex = frameIndex.coerceIn(0, max(0, frames.lastIndex))
    val currentFrame = frames.getOrNull(safeIndex)

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
                InteractiveGeographicRadarMap(
                    state = state,
                    bundle = bundle,
                    frame = currentFrame,
                    layer = selectedLayer,
                    loading = radarState is RadarLoadState.Loading,
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
                    state = state,
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
                frameIndex = safeIndex,
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
                    "Live Radar • interactive world map",
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InteractiveGeographicRadarMap(
    state: WeatherHomeUiState,
    bundle: RadarMapBundle?,
    frame: RadarRenderableFrame?,
    layer: RadarLayer,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val location = state.selectedLocation
    var webView by remember(location.id) { mutableStateOf<WebView?>(null) }

    DisposableEffect(location.id) {
        onDispose {
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webView = null
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
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(
                            "https://realweather365.local/",
                            buildMapHtml(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                locationName = location.name
                            ),
                            "text/html",
                            "UTF-8",
                            null
                        )
                        webView = this
                    }
                },
                update = { view ->
                    webView = view
                }
            )
        }

        LaunchedEffect(webView, frame?.meta?.path, layer) {
            val view = webView ?: return@LaunchedEffect
            if (layer == RadarLayer.RAIN && frame != null) {
                val host = JSONObject.quote(RAINVIEWER_TILE_HOST)
                val path = JSONObject.quote(frame.meta.path)
                view.evaluateJavascript("setRadarFrame($host,$path,true);", null)
            } else {
                view.evaluateJavascript("setRadarVisible(false);", null)
            }
        }

        LaunchedEffect(webView, location.latitude, location.longitude) {
            val view = webView ?: return@LaunchedEffect
            view.evaluateJavascript(
                "setSelectedLocation(${location.latitude},${location.longitude},${JSONObject.quote(location.name)});",
                null
            )
        }

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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF69E3EE), strokeWidth = 2.dp)
                    Text("Loading real radar…", color = Color.White, fontSize = 10.sp)
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 44.dp)
                .clickable {
                    webView?.evaluateJavascript("recenterMap();", null)
                },
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

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xC905161E),
            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f))
        ) {
            Text(
                "Drag / pinch to explore  •  Map © OpenStreetMap © CARTO  •  Radar RainViewer",
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 6.5.sp,
                textAlign = TextAlign.Center
            )
        }

        if (bundle?.coverageAvailable == false) {
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
                        "The geographic map is still fully interactive for this location.",
                        color = Color.White.copy(alpha = 0.58f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun buildMapHtml(
    latitude: Double,
    longitude: Double,
    locationName: String
): String {
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
            .rw-tooltip { background:rgba(4,20,28,.92); color:white; border:1px solid rgba(255,255,255,.18); box-shadow:0 4px 18px rgba(0,0,0,.42); border-radius:9px; font-size:11px; padding:5px 8px; }
            .rw-tooltip:before { border-top-color:rgba(4,20,28,.92) !important; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin=""></script>
          <script>
            let map = null;
            let base = null;
            let radar = null;
            let selectedMarker = null;
            let selectedLat = $latitude;
            let selectedLon = $longitude;
            let selectedName = $quotedName;
            let pendingRadar = null;

            function applyRadar(host, path, visible) {
              pendingRadar = {host:host, path:path, visible:visible};
              if (!map || !window.L) return;
              if (radar) {
                map.removeLayer(radar);
                radar = null;
              }
              if (!visible || !path) return;
              const template = host + path + '/256/{z}/{x}/{y}/2/1_1.png';
              radar = L.tileLayer(template, {
                tileSize: 256,
                opacity: 0.72,
                maxNativeZoom: 7,
                minZoom: 3,
                maxZoom: 18,
                zIndex: 430,
                pane: 'radarPane',
                updateWhenIdle: false,
                keepBuffer: 3
              });
              radar.addTo(map);
            }

            function setRadarFrame(host, path, visible) { applyRadar(host, path, visible); }
            function setRadarVisible(visible) {
              if (!visible && radar && map) {
                map.removeLayer(radar);
                radar = null;
              } else if (visible && pendingRadar) {
                applyRadar(pendingRadar.host, pendingRadar.path, true);
              }
            }
            function setSelectedLocation(lat, lon, name) {
              selectedLat = lat; selectedLon = lon; selectedName = name;
              if (!map || !window.L) return;
              if (selectedMarker) map.removeLayer(selectedMarker);
              selectedMarker = L.circleMarker([lat, lon], {
                radius:7,
                color:'#dffcff',
                weight:2,
                fillColor:'#24d8e9',
                fillOpacity:1
              }).addTo(map).bindTooltip(name, {permanent:false, direction:'top', className:'rw-tooltip'});
            }
            function recenterMap() {
              if (map) map.flyTo([selectedLat, selectedLon], Math.max(map.getZoom(), 8), {duration:0.55});
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
                preferCanvas:true
              }).setView([selectedLat, selectedLon], 8);

              map.createPane('radarPane');
              map.getPane('radarPane').style.zIndex = 430;
              map.getPane('radarPane').style.pointerEvents = 'none';

              base = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png', {
                subdomains:'abcd',
                maxZoom:19,
                minZoom:3,
                updateWhenIdle:false,
                keepBuffer:4
              }).addTo(map);

              setSelectedLocation(selectedLat, selectedLon, selectedName);
              if (pendingRadar) applyRadar(pendingRadar.host, pendingRadar.path, pendingRadar.visible);
            }
            init();
          </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun RadarStatusBadge(
    state: WeatherHomeUiState,
    layer: RadarLayer,
    bundle: RadarMapBundle?,
    frame: RadarRenderableFrame?,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val label = when {
        loading -> "RADAR • CONNECTING"
        layer == RadarLayer.TEMPERATURE_CONTEXT -> "CURRENT TEMP • ${state.temperature}°${state.temperatureUnit.removePrefix("°")}"
        bundle?.coverageAvailable == false -> "RADAR • NO COVERAGE"
        frame == null -> "RADAR • UNAVAILABLE"
        frame.meta.isNowcast -> "PRECIPITATION • NOWCAST"
        frame.hasPrecipitationEcho -> "PRECIPITATION • OBSERVED"
        else -> "PRECIPITATION • NO ECHO"
    }
    val dot = when {
        loading -> Color(0xFFFFC65B)
        bundle?.coverageAvailable == false -> Color(0xFFFF9F55)
        layer == RadarLayer.TEMPERATURE_CONTEXT -> Color(0xFFFFB65F)
        frame?.meta?.isNowcast == true -> Color(0xFF69DDF0)
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
            LayerControlItem(
                icon = Icons.Outlined.Layers,
                label = "Map",
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
                selected = selectedLayer == RadarLayer.TEMPERATURE_CONTEXT,
                onClick = { onLayerChange(RadarLayer.TEMPERATURE_CONTEXT) }
            )
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
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
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
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            if (layer == RadarLayer.TEMPERATURE_CONTEXT) {
                Text(
                    "Temperature button shows the selected location's current live temperature over the real map. It is not presented as a spatial temperature layer.",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 8.sp,
                    lineHeight = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                MapAttribution()
                return@Column
            }

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
                            fontSize = 10.sp,
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
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 46.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (frames.isEmpty()) {
                    Text(
                        bundle?.errorMessage ?: if (bundle?.coverageAvailable == false) "No local radar coverage" else "Radar timeline unavailable",
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 7.sp
                    )
                } else {
                    val first = frames.firstOrNull()
                    val middle = frames.getOrNull(frames.size / 2)
                    val last = frames.lastOrNull()
                    listOfNotNull(first, middle, last).distinctBy { it.meta.timeEpochSeconds }.forEach { item ->
                        Text(
                            compactFrameTime(item, timeZoneId),
                            color = Color.White.copy(alpha = 0.46f),
                            fontSize = 7.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(7.dp))
            RadarLegend()
            Spacer(Modifier.height(7.dp))
            MapAttribution()
        }
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
private fun MapAttribution() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("REAL GEOGRAPHIC MAP", color = Color(0xFF65DDE8), fontSize = 6.5.sp, fontWeight = FontWeight.SemiBold)
        Text("OSM + CARTO  •  Radar: RainViewer", color = Color.White.copy(alpha = 0.40f), fontSize = 6.5.sp)
    }
}

private fun frameLabel(frame: RadarRenderableFrame?, timeZoneId: String, languageCode: String): String {
    if (frame == null) return if (languageCode == "hi") "रडार डेटा लोड हो रहा है" else "Radar data loading"
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("EEE, h:mm a", Locale.getDefault()).withZone(zone)
    val time = formatter.format(Instant.ofEpochSecond(frame.meta.timeEpochSeconds))
    return if (frame.meta.isNowcast) "$time • forecast radar" else time
}

private fun compactFrameTime(frame: RadarRenderableFrame, timeZoneId: String): String {
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        .withZone(zone)
        .format(Instant.ofEpochSecond(frame.meta.timeEpochSeconds))
}
