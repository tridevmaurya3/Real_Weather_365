package com.tridev.realweather365.ui.airquality

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.data.weather.WmoWeather
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import kotlin.math.roundToInt

private data class Pollutant(
    val label: String,
    val value: Double?,
    val unit: String,
    val tint: Color
)

@Composable
fun AirQualityScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pollutants = listOf(
        Pollutant("PM2.5", state.pm25, "µg/m³", Color(0xFFFFD65A)),
        Pollutant("PM10", state.pm10, "µg/m³", Color(0xFFFFB84D)),
        Pollutant("NO₂", state.nitrogenDioxide, "µg/m³", Color(0xFF77E59A)),
        Pollutant("O₃", state.ozone, "µg/m³", Color(0xFFB8D94C))
    )

    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF071923), Color(0xFF0B2734), Color(0xFF071821), Color(0xFF041017)))
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(state.location, state.updatedAt, onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                HeroCard(state)
                Spacer(Modifier.height(10.dp))
                GuidanceCard(state.aqi)
                Spacer(Modifier.height(10.dp))
                Text("Air pollutants", color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                PollutantGrid(pollutants)
                Spacer(Modifier.height(10.dp))
                OutlookCard(state)
                Spacer(Modifier.height(8.dp))
            }
            Footer("LIVE AIR QUALITY • ${state.provider}")
        }
    }
}

@Composable
private fun Header(location: String, subtitle: String, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().statusBarsPadding(), color = Color(0xEE071720)) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFF75E5F0), modifier = Modifier.size(13.dp))
                    Text(location, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Air Quality • $subtitle", color = Color.White.copy(alpha = 0.55f), fontSize = 8.sp, maxLines = 1)
            }
            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun HeroCard(state: WeatherHomeUiState) {
    val aqi = state.aqi
    val label = WmoWeather.aqiLabel(aqi)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xA0081B25),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            AqiGauge(aqi, Modifier.size(154.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (aqi == null) "Air-quality data unavailable" else "Air quality is ${label.lowercase()}",
                    color = Color.White, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    aqiMessage(aqi),
                    color = Color.White.copy(alpha = 0.62f), fontSize = 9.sp, lineHeight = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0x3327D17F)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Eco, null, tint = Color(0xFF77E59A), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("US AQI • live location data", color = Color(0xFFB8F3C9), fontSize = 8.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AqiGauge(value: Int?, modifier: Modifier = Modifier) {
    val safe = value ?: 0
    val label = WmoWeather.aqiLabel(value)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val colors = listOf(Color(0xFF59D872), Color(0xFFC9D83B), Color(0xFFFFC23D), Color(0xFFFF7043), Color(0xFFD94C78))
            val start = 145f
            val total = 250f
            val sweep = total / colors.size
            val progress = (safe / 300f).coerceIn(0f, 1f)
            val activeSweep = total * progress
            colors.forEachIndexed { index, color ->
                drawArc(color.copy(alpha = 0.22f), start + index * sweep, sweep - 4f, false, style = Stroke(width = 13f, cap = StrokeCap.Round))
            }
            var remaining = activeSweep
            colors.forEachIndexed { index, color ->
                if (remaining > 0f) {
                    val segment = minOf(sweep - 4f, remaining)
                    drawArc(color, start + index * sweep, segment, false, style = Stroke(width = 13f, cap = StrokeCap.Round))
                    remaining -= sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value?.toString() ?: "--", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
            Text(label, color = aqiTint(value), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("AQI", color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
        }
    }
}

@Composable
private fun GuidanceCard(aqi: Int?) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0x94081922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(Color(0x263CE7F3), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Shield, null, tint = Color(0xFF6FE4EE), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Air-quality guidance", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(aqiGuidance(aqi), color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp, lineHeight = 12.sp)
            }
        }
    }
}

@Composable
private fun PollutantGrid(pollutants: List<Pollutant>) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        pollutants.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { pollutant -> PollutantCard(pollutant, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PollutantCard(pollutant: Pollutant, modifier: Modifier) {
    val value = pollutant.value
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color(0x9A071922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f))) {
        Column(Modifier.padding(11.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(pollutant.label, color = Color.White.copy(alpha = 0.66f), fontSize = 9.sp)
                Box(Modifier.size(7.dp).background(pollutant.tint, CircleShape))
            }
            Spacer(Modifier.height(4.dp))
            Text(value?.let { formatOneDecimal(it) } ?: "--", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
            Text(pollutant.unit, color = Color.White.copy(alpha = 0.45f), fontSize = 7.sp)
            Spacer(Modifier.height(7.dp))
            AirBar(value, pollutant.tint)
            Spacer(Modifier.height(5.dp))
            Text(if (value == null) "Unavailable" else "Live concentration", color = pollutant.tint, fontSize = 8.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AirBar(value: Double?, tint: Color) {
    Canvas(Modifier.fillMaxWidth().height(5.dp)) {
        val y = size.height / 2f
        drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), strokeWidth = size.height, cap = StrokeCap.Round)
        val progress = ((value ?: 0.0) / 150.0).coerceIn(0.0, 1.0).toFloat()
        drawLine(tint, Offset(0f, y), Offset(size.width * progress, y), strokeWidth = size.height, cap = StrokeCap.Round)
    }
}

@Composable
private fun OutlookCard(state: WeatherHomeUiState) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0x90071922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Visibility, null, tint = Color(0xFF70E0EB), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Worldwide air-quality status", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "Coordinates ${"%.3f".format(state.selectedLocation.latitude)}, ${"%.3f".format(state.selectedLocation.longitude)} • Updated with the selected location.",
                color = Color.White.copy(alpha = 0.56f), fontSize = 8.sp, lineHeight = 12.sp
            )
        }
    }
}

private fun aqiMessage(aqi: Int?): String = when {
    aqi == null -> "The provider did not return a current AQI value for this location."
    aqi <= 50 -> "Current air quality is in the good range."
    aqi <= 100 -> "Current air quality is moderate."
    aqi <= 150 -> "Air quality may affect people who are unusually sensitive to pollution."
    aqi <= 200 -> "Air quality is unhealthy; consider reducing prolonged outdoor exertion."
    else -> "Air quality is poor; pay attention to local public-health advice."
}

private fun aqiGuidance(aqi: Int?): String = when {
    aqi == null -> "AQI guidance will appear when live air-quality data is available."
    aqi <= 100 -> "Normal outdoor plans are generally suitable; people who are sensitive to pollution can monitor symptoms and local guidance."
    aqi <= 150 -> "Sensitive people may prefer shorter periods of strenuous outdoor activity."
    else -> "Consider limiting prolonged strenuous outdoor activity and follow local health guidance."
}

private fun aqiTint(aqi: Int?): Color = when {
    aqi == null -> Color.White.copy(alpha = 0.55f)
    aqi <= 50 -> Color(0xFF77E59A)
    aqi <= 100 -> Color(0xFFFFD65A)
    aqi <= 150 -> Color(0xFFFFB84D)
    aqi <= 200 -> Color(0xFFFF7043)
    else -> Color(0xFFD94C78)
}

private fun formatOneDecimal(value: Double): String = if (value % 1.0 == 0.0) value.roundToInt().toString() else String.format("%.1f", value)

@Composable
private fun Footer(text: String) {
    Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = Color(0xEE041119)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(10.dp), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.42f), fontSize = 8.sp)
    }
}
