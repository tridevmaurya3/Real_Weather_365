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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
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
import com.tridev.realweather365.data.weather.SpatialWeatherPoint
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

private sealed interface RadarLoadState {
    data object Loading : RadarLoadState
    data class Ready(val bundle: RadarMapBundle) : RadarLoadState
    data class Error(val message: String) : RadarLoadState
}

private sealed interface SpatialFieldLoadState {
    data object Loading : SpatialFieldLoadState
    data class Ready(val field: SpatialWeatherField) : SpatialFieldLoadState
    data class Error(val message: String) : SpatialFieldLoadState
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
    var refreshToken by remember { mutableStateOf(0) }
    var radarState by remember(location.id) { mutableStateOf<RadarLoadState>(RadarLoadState.Loading) }
    var fieldState by remember(location.id) { mutableStateOf<SpatialFieldLoadState>(SpatialFieldLoadState.Loading) }
    var frameIndex by rememberSaveable(location.id) { mutableStateOf(0) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(location.id, refreshToken) {
        radarState = RadarLoadState.Loading
        radarState = runCatching {
            radarRepository.load(location = location, zoom = 6, maxFrames = 7)
        }.fold(
            onSuccess = { RadarLoadState.Ready(it) },
            onFailure = { RadarLoadState.Error(it.message ?: "Radar unavailable") }
        )
    }

    LaunchedEffect(location.id, refreshToken) {
        fieldState = SpatialFieldLoadState.Loading
        fieldState = runCatching {
            fieldRepository.load(location = location, gridSize = 7)
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
            delay(900)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    val safeIndex = frameIndex.coerceIn(0, max(0, frames.lastIndex))
    val currentFrame = frames.getOrNull(safeIndex)

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF06131B))) {
        Column(modifier = Modifier.fillMaxSize()) {
            WeatherMapHeader(
                location = state.location,
                onBack = onBack,
                onRefresh = { refreshToken++ }
            )

            Box(modifier = Modifier.weight(1f)) {
                InteractiveWeatherMap(
                    state = state,
                    bundle = bundle,
                    frame = currentFrame,
                    field = field,
                    layer = selectedLayer,
                    radarLoading = radarState is RadarLoadState.Loading,
                    fieldLoading = fieldState is SpatialFieldLoadState.Loading,
                    radarError = (radarState as? RadarLoadState.Error)?.message,
                    fieldError = (fieldState as? SpatialFieldLoadState.Error)?.message,
                    modifier = Modifier.fillMaxSize()
                )

                WeatherLayerControls(
                    selectedLayer = selectedLayer,
                    onLayerChange = { selectedLayer = it },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)
                )

                WeatherMapStatusBadge(
                    state = state,
                    layer = selectedLayer,
                    bundle = bundle,
                    frame = currentFrame,
                    radarState = radarState,
                    fieldState = fieldState,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp)
                )
            }

            WeatherMapBottomPanel(
                state = state,
                layer = selectedLayer,
                fieldState = fieldState,
                frames = frames,
                frameIndex = safeIndex,
                onFrameIndexChanged = { frameIndex = it },
                isPlaying = isPlaying,
                onTogglePlay = { isPlaying = !isPlaying },
                timeZoneId = location.timeZoneId,
                languageCode = state.languageCode,
                bundle = bundle
            )
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
                    "Weather Map • radar + live model fields",
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InteractiveWeatherMap(
    state: WeatherHomeUiState,
    bundle: RadarMapBundle?,
    frame: RadarRenderableFrame?,
    field: SpatialWeatherField?,
    layer: WeatherMapLayer,
    radarLoading: Boolean,
    fieldLoading: Boolean,
    radarError: String?,
    fieldError: String?,
    modifier: Modifier = Modifier
) {
    val location = state.selectedLocation
    var webView by remember(location.id) { mutableStateOf<WebView?>(null) }
    var mapReady by remember(location.id) { mutableStateOf(false) }

    DisposableEffect(location.id) {
        onDispose {
            webView?.apply {
                stopLoading()
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

        LaunchedEffect(webView, mapReady, frame?.meta?.path, layer) {
            val view = webView ?: return@LaunchedEffect
            if (!mapReady) return@LaunchedEffect
            if (layer == WeatherMapLayer.RADAR && frame != null) {
                view.evaluateJavascript(
                    "setRadarFrame(${JSONObject.quote(RAINVIEWER_TILE_HOST)},${JSONObject.quote(frame.meta.path)},true);",
                    null
                )
            } else {
                view.evaluateJavascript("setRadarVisible(false);", null)
            }
        }

        LaunchedEffect(webView, mapReady, field, layer, state.temperatureUnit, state.windUnit) {
            val view = webView ?: return@LaunchedEffect
            if (!mapReady) return@LaunchedEffect
            if (layer == WeatherMapLayer.RADAR || field == null) {
                view.evaluateJavascript("clearWeatherField();", null)
            } else {
                val json = fieldToJson(field, state)
                view.evaluateJavascript(
                    "setWeatherField(${JSONObject.quote(layer.jsKey())},$json,${field.latitudeStep},${field.longitudeStep});",
                    null
                )
            }
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
                        if (layer == WeatherMapLayer.RADAR) "Loading real radar…" else "Loading live spatial field…",
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
                border = BorderStroke(1.dp, Color(0xFFFFB25C).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Layer temporarily unavailable", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(layerError, color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 44.dp).clickable {
                webView?.evaluateJavascript("recenterMap();", null)
            },
            shape = CircleShape,
            color = Color(0xD9071922),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Icon(
                Icons.Outlined.MyLocation,
                "Recenter map",
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
                "Drag / pinch  •  Map © OpenStreetMap © CARTO  •  Radar RainViewer  •  Fields Open-Meteo",
                Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 6.2.sp,
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
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Radar coverage unavailable", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Switch to Temp, Wind, Cloud or Precip to view provider-backed model fields for this area.",
                        color = Color.White.copy(alpha = 0.58f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
            .rw-tooltip { background:rgba(4,20,28,.94); color:white; border:1px solid rgba(255,255,255,.18); box-shadow:0 4px 18px rgba(0,0,0,.42); border-radius:9px; font-size:10px; padding:5px 8px; }
            .rw-tooltip:before { border-top-color:rgba(4,20,28,.94) !important; }
            .rw-wind { width:42px; height:42px; color:#effcff; text-align:center; filter:drop-shadow(0 2px 4px rgba(0,0,0,.75)); }
            .rw-wind-arrow { display:block; font-size:25px; height:25px; line-height:25px; color:#68ecf4; transform-origin:50% 50%; }
            .rw-wind-speed { display:block; font-size:9px; font-weight:700; margin-top:1px; white-space:nowrap; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin=""></script>
          <script>
            let map = null;
            let radar = null;
            let fieldGroup = null;
            let selectedMarker = null;
            let selectedLat = $latitude;
            let selectedLon = $longitude;
            let selectedName = $quotedName;
            let pendingRadar = null;

            function tempColor(v) {
              if (v <= -15) return '#5259d9';
              if (v <= 0) return '#268fe8';
              if (v <= 12) return '#27c9d7';
              if (v <= 20) return '#42d28f';
              if (v <= 28) return '#d9d84a';
              if (v <= 35) return '#ff9a3d';
              return '#f0444f';
            }
            function cloudColor(v) {
              if (v < 20) return '#294652';
              if (v < 45) return '#54707c';
              if (v < 70) return '#8799a3';
              return '#d7e1e6';
            }
            function precipColor(v) {
              if (v < 0.05) return '#173a48';
              if (v < 0.5) return '#1ab6e8';
              if (v < 2.0) return '#21d68c';
              if (v < 5.0) return '#d8da43';
              if (v < 10.0) return '#ff8d32';
              return '#f13c4b';
            }
            function valueTooltip(kind, p) {
              if (kind === 'temperature') return '<b>' + p.tempDisplay + '</b><br>' + p.lat.toFixed(2) + ', ' + p.lon.toFixed(2);
              if (kind === 'clouds') return '<b>Cloud ' + Math.round(p.cloud) + '%</b><br>' + p.lat.toFixed(2) + ', ' + p.lon.toFixed(2);
              if (kind === 'precipitation') return '<b>' + p.precip.toFixed(1) + ' mm</b><br>' + p.lat.toFixed(2) + ', ' + p.lon.toFixed(2);
              return '<b>' + p.windDisplay + '</b><br>Direction ' + Math.round(p.dir) + '°';
            }
            function clearWeatherField() {
              if (fieldGroup && map) {
                fieldGroup.clearLayers();
                map.removeLayer(fieldGroup);
              }
              fieldGroup = null;
            }
            function setWeatherField(kind, points, latStep, lonStep) {
              clearWeatherField();
              if (!map || !window.L || !points || !points.length) return;
              fieldGroup = L.layerGroup();
              const halfLat = Math.max(0.05, Math.abs(latStep) * 0.53);
              const halfLon = Math.max(0.05, Math.abs(lonStep) * 0.53);
              points.forEach(function(p) {
                if (kind === 'wind') {
                  if (p.windKmh == null || p.dir == null) return;
                  const html = '<div class="rw-wind"><span class="rw-wind-arrow" style="transform:rotate(' + p.dir + 'deg)">↑</span><span class="rw-wind-speed">' + p.windDisplay + '</span></div>';
                  L.marker([p.lat, p.lon], {
                    pane:'weatherPane',
                    interactive:true,
                    icon:L.divIcon({className:'', html:html, iconSize:[42,42], iconAnchor:[21,21]})
                  }).bindTooltip(valueTooltip(kind,p), {className:'rw-tooltip'}).addTo(fieldGroup);
                  return;
                }
                let raw = null;
                let color = '#55ddea';
                let opacity = 0.48;
                if (kind === 'temperature') { raw = p.tempC; color = tempColor(raw); opacity = 0.48; }
                if (kind === 'clouds') { raw = p.cloud; color = cloudColor(raw); opacity = 0.18 + Math.min(0.48, raw / 180); }
                if (kind === 'precipitation') { raw = p.precip; color = precipColor(raw); opacity = raw < 0.05 ? 0.13 : 0.54; }
                if (raw == null || !Number.isFinite(raw)) return;
                L.rectangle(
                  [[p.lat-halfLat, p.lon-halfLon], [p.lat+halfLat, p.lon+halfLon]],
                  {pane:'weatherPane', stroke:false, fill:true, fillColor:color, fillOpacity:opacity, interactive:true}
                ).bindTooltip(valueTooltip(kind,p), {className:'rw-tooltip'}).addTo(fieldGroup);
              });
              fieldGroup.addTo(map);
            }
            function applyRadar(host, path, visible) {
              pendingRadar = {host:host, path:path, visible:visible};
              if (!map || !window.L) return;
              if (radar) { map.removeLayer(radar); radar = null; }
              if (!visible || !path) return;
              const template = host + path + '/256/{z}/{x}/{y}/2/1_1.png';
              radar = L.tileLayer(template, {
                tileSize:256,
                opacity:0.72,
                maxNativeZoom:7,
                minZoom:3,
                maxZoom:18,
                zIndex:430,
                pane:'radarPane',
                updateWhenIdle:false,
                keepBuffer:3
              }).addTo(map);
            }
            function setRadarFrame(host, path, visible) { applyRadar(host, path, visible); }
            function setRadarVisible(visible) {
              if (!visible && radar && map) { map.removeLayer(radar); radar = null; }
              else if (visible && pendingRadar) applyRadar(pendingRadar.host, pendingRadar.path, true);
            }
            function setSelectedLocation(lat, lon, name) {
              selectedLat = lat; selectedLon = lon; selectedName = name;
              if (!map || !window.L) return;
              if (selectedMarker) map.removeLayer(selectedMarker);
              selectedMarker = L.circleMarker([lat, lon], {
                radius:7, color:'#dffcff', weight:2, fillColor:'#24d8e9', fillOpacity:1
              }).addTo(map).bindTooltip(name, {permanent:false, direction:'top', className:'rw-tooltip'});
            }
            function recenterMap() {
              if (map) map.flyTo([selectedLat, selectedLon], Math.max(map.getZoom(), 7), {duration:0.55});
            }
            function init() {
              if (!window.L) { setTimeout(init, 250); return; }
              map = L.map('map', {
                zoomControl:true,
                attributionControl:false,
                minZoom:3,
                maxZoom:18,
                worldCopyJump:true,
                preferCanvas:true
              }).setView([selectedLat, selectedLon], 7);
              map.createPane('weatherPane');
              map.getPane('weatherPane').style.zIndex = 410;
              map.createPane('radarPane');
              map.getPane('radarPane').style.zIndex = 430;
              map.getPane('radarPane').style.pointerEvents = 'none';
              L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png', {
                subdomains:'abcd', maxZoom:19, minZoom:3, updateWhenIdle:false, keepBuffer:4
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

private fun fieldToJson(field: SpatialWeatherField, state: WeatherHomeUiState): String {
    return JSONArray().apply {
        field.points.forEach { point ->
            put(
                JSONObject()
                    .put("lat", point.latitude)
                    .put("lon", point.longitude)
                    .putNullable("tempC", point.temperatureC)
                    .put("tempDisplay", point.temperatureDisplay(state.temperatureUnit))
                    .putNullable("windKmh", point.windSpeedKmh)
                    .put("windDisplay", point.windDisplay(state.windUnit))
                    .putNullable("dir", point.windDirectionDegrees)
                    .putNullable("cloud", point.cloudCoverPercent)
                    .putNullable("precip", point.precipitationMm)
            )
        }
    }.toString()
}

private fun JSONObject.putNullable(key: String, value: Double?): JSONObject = put(key, value ?: JSONObject.NULL)

private fun SpatialWeatherPoint.temperatureDisplay(unit: String): String {
    val value = temperatureC ?: return "--"
    return if (unit == "°F") {
        "${String.format(Locale.US, "%.0f", value * 9.0 / 5.0 + 32.0)}°F"
    } else {
        "${String.format(Locale.US, "%.0f", value)}°C"
    }
}

private fun SpatialWeatherPoint.windDisplay(unit: String): String {
    val value = windSpeedKmh ?: return "--"
    return if (unit.equals("mph", true)) {
        "${String.format(Locale.US, "%.0f", value * 0.621371)} mph"
    } else {
        "${String.format(Locale.US, "%.0f", value)} km/h"
    }
}

private fun WeatherMapLayer.jsKey(): String = when (this) {
    WeatherMapLayer.RADAR -> "radar"
    WeatherMapLayer.TEMPERATURE -> "temperature"
    WeatherMapLayer.WIND -> "wind"
    WeatherMapLayer.CLOUDS -> "clouds"
    WeatherMapLayer.PRECIPITATION -> "precipitation"
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
            radarState is RadarLoadState.Error -> "RADAR • UNAVAILABLE"
            bundle?.coverageAvailable == false -> "RADAR • NO COVERAGE"
            frame == null -> "RADAR • UNAVAILABLE"
            frame.meta.isNowcast -> "RADAR • NOWCAST"
            frame.hasPrecipitationEcho -> "RADAR • OBSERVED"
            else -> "RADAR • NO ECHO"
        }
        else -> when (fieldState) {
            SpatialFieldLoadState.Loading -> "${layer.shortLabel()} • LOADING"
            is SpatialFieldLoadState.Error -> "${layer.shortLabel()} • UNAVAILABLE"
            is SpatialFieldLoadState.Ready -> "${layer.shortLabel()} • LIVE MODEL"
        }
    }
    val dot = when {
        layer == WeatherMapLayer.RADAR && radarState is RadarLoadState.Loading -> Color(0xFFFFC65B)
        layer != WeatherMapLayer.RADAR && fieldState is SpatialFieldLoadState.Loading -> Color(0xFFFFC65B)
        layer == WeatherMapLayer.RADAR && bundle?.coverageAvailable == false -> Color(0xFFFF9F55)
        layer == WeatherMapLayer.TEMPERATURE -> Color(0xFFFFB65F)
        layer == WeatherMapLayer.WIND -> Color(0xFF72EAF2)
        layer == WeatherMapLayer.CLOUDS -> Color(0xFFD7E1E6)
        layer == WeatherMapLayer.PRECIPITATION -> Color(0xFF47DF9B)
        frame?.meta?.isNowcast == true -> Color(0xFF69DDF0)
        else -> Color(0xFF55EAA5)
    }

    Surface(modifier, RoundedCornerShape(12.dp), Color(0xD0081B25), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f))) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(7.dp).background(dot, CircleShape))
            Text(label, color = Color.White.copy(alpha = 0.88f), fontSize = 7.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun WeatherMapLayer.shortLabel(): String = when (this) {
    WeatherMapLayer.RADAR -> "RADAR"
    WeatherMapLayer.TEMPERATURE -> "TEMP"
    WeatherMapLayer.WIND -> "WIND"
    WeatherMapLayer.CLOUDS -> "CLOUD"
    WeatherMapLayer.PRECIPITATION -> "PRECIP"
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
        Column(Modifier.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            WeatherLayerControlItem(Icons.Outlined.Layers, "Radar", selectedLayer == WeatherMapLayer.RADAR) { onLayerChange(WeatherMapLayer.RADAR) }
            WeatherLayerControlItem(Icons.Outlined.Thermostat, "Temp", selectedLayer == WeatherMapLayer.TEMPERATURE) { onLayerChange(WeatherMapLayer.TEMPERATURE) }
            WeatherLayerControlItem(Icons.Outlined.Air, "Wind", selectedLayer == WeatherMapLayer.WIND) { onLayerChange(WeatherMapLayer.WIND) }
            WeatherLayerControlItem(Icons.Outlined.Cloud, "Cloud", selectedLayer == WeatherMapLayer.CLOUDS) { onLayerChange(WeatherMapLayer.CLOUDS) }
            WeatherLayerControlItem(Icons.Outlined.WaterDrop, "Precip", selectedLayer == WeatherMapLayer.PRECIPITATION) { onLayerChange(WeatherMapLayer.PRECIPITATION) }
        }
    }
}

@Composable
private fun WeatherLayerControlItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Color(0xFF64E8F2) else Color.White.copy(alpha = 0.72f)
    val background = if (selected) Color(0x263CE7F3) else Color.Transparent
    Column(
        Modifier.width(54.dp).background(background, RoundedCornerShape(11.dp)).clickable(onClick = onClick).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(17.dp))
        Text(label, color = tint, fontSize = 6.5.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun WeatherMapBottomPanel(
    state: WeatherHomeUiState,
    layer: WeatherMapLayer,
    fieldState: SpatialFieldLoadState,
    frames: List<RadarRenderableFrame>,
    frameIndex: Int,
    onFrameIndexChanged: (Int) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
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
            if (layer != WeatherMapLayer.RADAR) {
                SpatialFieldPanel(state, layer, fieldState)
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
                            fontSize = 7.sp
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
                    } else Spacer(Modifier.height(8.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 46.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (frames.isEmpty()) {
                    Text(
                        bundle?.errorMessage ?: if (bundle?.coverageAvailable == false) "No local radar coverage" else "Radar timeline unavailable",
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 7.sp
                    )
                } else {
                    listOfNotNull(frames.firstOrNull(), frames.getOrNull(frames.size / 2), frames.lastOrNull())
                        .distinctBy { it.meta.timeEpochSeconds }
                        .forEach { Text(compactFrameTime(it, timeZoneId), color = Color.White.copy(alpha = 0.46f), fontSize = 7.sp) }
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
private fun SpatialFieldPanel(state: WeatherHomeUiState, layer: WeatherMapLayer, fieldState: SpatialFieldLoadState) {
    val field = (fieldState as? SpatialFieldLoadState.Ready)?.field
    val range = field?.let { layerRangeLabel(it, layer, state) } ?: when (fieldState) {
        SpatialFieldLoadState.Loading -> "Loading spatial samples…"
        is SpatialFieldLoadState.Error -> fieldState.message
        is SpatialFieldLoadState.Ready -> "No usable samples"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${layer.shortLabel()} • LIVE SPATIAL MODEL FIELD", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Text(range, color = Color.White.copy(alpha = 0.62f), fontSize = 7.5.sp, lineHeight = 10.sp)
        }
        if (fieldState is SpatialFieldLoadState.Loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF65E2EC), strokeWidth = 2.dp)
        }
    }
    Spacer(Modifier.height(8.dp))
    SpatialLegend(layer)
    Spacer(Modifier.height(8.dp))
    Text(
        "Open-Meteo current model values sampled on a ${field?.rows ?: 7}×${field?.columns ?: 7} geographic grid around the selected location. Cells are provider-backed model samples, not radar observations; values between sample points are visualized as local grid cells.",
        color = Color.White.copy(alpha = 0.52f),
        fontSize = 7.sp,
        lineHeight = 9.5.sp
    )
    Spacer(Modifier.height(6.dp))
    MapAttribution()
}

private fun layerRangeLabel(field: SpatialWeatherField, layer: WeatherMapLayer, state: WeatherHomeUiState): String {
    val values = field.points.mapNotNull { point ->
        when (layer) {
            WeatherMapLayer.TEMPERATURE -> point.temperatureC?.let { if (state.temperatureUnit == "°F") it * 9.0 / 5.0 + 32.0 else it }
            WeatherMapLayer.WIND -> point.windSpeedKmh?.let { if (state.windUnit.equals("mph", true)) it * 0.621371 else it }
            WeatherMapLayer.CLOUDS -> point.cloudCoverPercent
            WeatherMapLayer.PRECIPITATION -> point.precipitationMm
            WeatherMapLayer.RADAR -> null
        }
    }
    if (values.isEmpty()) return "No usable values in the current grid"
    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: 0.0
    val unit = when (layer) {
        WeatherMapLayer.TEMPERATURE -> state.temperatureUnit
        WeatherMapLayer.WIND -> state.windUnit
        WeatherMapLayer.CLOUDS -> "%"
        WeatherMapLayer.PRECIPITATION -> "mm"
        WeatherMapLayer.RADAR -> ""
    }
    return "${String.format(Locale.US, "%.1f", minValue)}–${String.format(Locale.US, "%.1f", maxValue)} $unit across ${values.size} live model samples"
}

@Composable
private fun SpatialLegend(layer: WeatherMapLayer) {
    when (layer) {
        WeatherMapLayer.TEMPERATURE -> GradientLegend("Cold", "Hot", listOf(Color(0xFF5259D9), Color(0xFF268FE8), Color(0xFF27C9D7), Color(0xFF42D28F), Color(0xFFD9D84A), Color(0xFFFF9A3D), Color(0xFFF0444F)))
        WeatherMapLayer.WIND -> GradientLegend("Calm", "Strong", listOf(Color(0xFF77DDE8), Color(0xFF43C7E4), Color(0xFF65E29A), Color(0xFFF3D85A), Color(0xFFFF914D)))
        WeatherMapLayer.CLOUDS -> GradientLegend("Clear", "Overcast", listOf(Color(0xFF294652), Color(0xFF54707C), Color(0xFF8799A3), Color(0xFFD7E1E6)))
        WeatherMapLayer.PRECIPITATION -> GradientLegend("Dry", "Heavy", listOf(Color(0xFF173A48), Color(0xFF1AB6E8), Color(0xFF21D68C), Color(0xFFD8DA43), Color(0xFFFF8D32), Color(0xFFF13C4B)))
        WeatherMapLayer.RADAR -> RadarLegend()
    }
}

@Composable
private fun GradientLegend(start: String, end: String, colors: List<Color>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(start, color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
        Box(
            Modifier.weight(1f).padding(horizontal = 8.dp).height(4.dp)
                .background(Brush.horizontalGradient(colors), RoundedCornerShape(4.dp))
        )
        Text(end, color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
    }
}

@Composable
private fun RadarLegend() = GradientLegend(
    "Light",
    "Heavy",
    listOf(Color(0xFF1CBDF2), Color(0xFF20E096), Color(0xFFE8E44A), Color(0xFFFF8A33), Color(0xFFF33449))
)

@Composable
private fun MapAttribution() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("REAL GEOGRAPHIC WEATHER MAP", color = Color(0xFF65DDE8), fontSize = 6.3.sp, fontWeight = FontWeight.SemiBold)
        Text("OSM/CARTO • RainViewer • Open-Meteo", color = Color.White.copy(alpha = 0.40f), fontSize = 6.2.sp)
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
