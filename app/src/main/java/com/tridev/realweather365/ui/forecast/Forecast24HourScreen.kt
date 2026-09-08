package com.tridev.realweather365.ui.forecast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.data.weather.LiveHourData
import com.tridev.realweather365.ui.home.WeatherHomeUiState

@Composable
fun Forecast24HourScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hindi = state.languageCode == "hi"
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF071923), Color(0xFF0B2734), Color(0xFF071821), Color(0xFF041017)))
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ForecastHeader(state.location, state.updatedAt, hindi, onBack)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Spacer(Modifier.height(12.dp))
                CurrentForecastCard(state)
                Spacer(Modifier.height(10.dp))
                TemperatureChartCard(state.hourly24, hindi)
                Spacer(Modifier.height(10.dp))
                Text(if (hindi) "अगले 24 घंटे" else "Next 24 hours", color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                HourlyStrip(state.hourly24, hindi)
                Spacer(Modifier.height(10.dp))
                ForecastInsightCard(state)
                Spacer(Modifier.weight(1f))
            }
            Footer(if (hindi) "लाइव विश्वव्यापी • ${state.provider}" else "LIVE WORLDWIDE • ${state.provider}")
        }
    }
}

@Composable
private fun ForecastHeader(location: String, subtitle: String, hindi: Boolean, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().statusBarsPadding(), color = Color(0xEE071720)) {
        Row(modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFF75E5F0), modifier = Modifier.size(13.dp))
                    Text(location, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(if (hindi) "24-घंटे का पूर्वानुमान • $subtitle" else "24-Hour Forecast • $subtitle", color = Color.White.copy(alpha = 0.54f), fontSize = 8.sp, maxLines = 1)
            }
            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun CurrentForecastCard(state: WeatherHomeUiState) {
    val hindi = state.languageCode == "hi"
    Glass {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("${state.temperature}°", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Light)
                Text(state.condition, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (hindi) "महसूस ${state.feelsLike}°  •  अधिक ${state.high}° / कम ${state.low}°" else "Feels ${state.feelsLike}°  •  H ${state.high}° / L ${state.low}°",
                    color = Color.White.copy(alpha = 0.58f), fontSize = 9.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val rain = state.hourly24.firstOrNull()?.rainChance ?: 0
                Text("$rain%", color = Color(0xFF7DE6F0), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(if (hindi) "वर्षा" else "precipitation", color = Color.White.copy(alpha = 0.50f), fontSize = 8.sp)
                Spacer(Modifier.height(5.dp))
                Text(if (hindi) "नमी ${state.humidity}%" else "Humidity ${state.humidity}%", color = Color.White.copy(alpha = 0.72f), fontSize = 9.sp)
                Text(if (hindi) "हवा ${state.windSpeed} ${state.windUnit}" else "Wind ${state.windSpeed} ${state.windUnit}", color = Color.White.copy(alpha = 0.72f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun TemperatureChartCard(hours: List<LiveHourData>, hindi: Boolean) {
    val data = hours.take(12)
    Glass {
        Text(if (hindi) "तापमान और वर्षा" else "Temperature & precipitation", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (data.size < 2) {
            Text(if (hindi) "लाइव प्रति-घंटा पूर्वानुमान की प्रतीक्षा…" else "Waiting for live hourly forecast…", color = Color.White.copy(alpha = 0.52f), fontSize = 9.sp)
        } else {
            Canvas(Modifier.fillMaxWidth().height(104.dp)) {
                val temps = data.map { it.temperature }
                val minT = temps.minOrNull() ?: 0
                val maxT = temps.maxOrNull() ?: 1
                val span = (maxT - minT).coerceAtLeast(1)
                val stepX = size.width / (data.size - 1)
                var previous: Offset? = null
                data.forEachIndexed { index, hour ->
                    val x = index * stepX
                    val y = size.height * (0.18f + 0.50f * (1f - (hour.temperature - minT).toFloat() / span))
                    previous?.let { drawLine(Color(0xFF64DFEA), it, Offset(x, y), strokeWidth = 4f, cap = StrokeCap.Round) }
                    drawCircle(Color.White, radius = 4f, center = Offset(x, y))
                    previous = Offset(x, y)
                    val bar = size.height * 0.22f * (hour.rainChance.coerceIn(0, 100) / 100f)
                    drawLine(Color(0xFF4FC3F7).copy(alpha = 0.55f), Offset(x, size.height), Offset(x, size.height - bar), strokeWidth = 6f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun HourlyStrip(hours: List<LiveHourData>, hindi: Boolean) {
    if (hours.isEmpty()) {
        Glass { Text(if (hindi) "लाइव प्रति-घंटा डेटा लोड हो रहा है…" else "Live hourly data is loading…", color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp) }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(hours, key = { it.isoTime }) { hour ->
            Surface(modifier = Modifier.width(72.dp), shape = RoundedCornerShape(16.dp), color = Color(0xA0081B25), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f))) {
                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hour.label, color = Color.White.copy(alpha = 0.64f), fontSize = 8.sp)
                    Icon(weatherIcon(hour.weatherCode, hour.isDay), null, tint = iconTint(hour.weatherCode, hour.isDay), modifier = Modifier.padding(vertical = 6.dp).size(18.dp))
                    Text("${hour.temperature}°", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (hindi) "${hour.rainChance}% बारिश" else "${hour.rainChance}% rain", color = Color(0xFF79DDE8), fontSize = 7.sp)
                }
            }
        }
    }
}

@Composable
private fun ForecastInsightCard(state: WeatherHomeUiState) {
    val hindi = state.languageCode == "hi"
    val wettest = state.hourly24.maxByOrNull { it.rainChance }
    val text = when {
        state.isLoading -> if (hindi) "${state.location} का लाइव पूर्वानुमान अपडेट हो रहा है…" else "Refreshing live forecast for ${state.location}…"
        state.errorMessage != null -> state.errorMessage ?: if (hindi) "पूर्वानुमान उपलब्ध नहीं" else "Forecast unavailable"
        wettest != null && wettest.rainChance >= 50 -> if (hindi) "सबसे अधिक बारिश की संभावना ${wettest.rainChance}% ${wettest.label} के आसपास है।" else "Highest rain chance is ${wettest.rainChance}% around ${wettest.label}. Forecast follows the selected worldwide location."
        else -> if (hindi) "अगले 24 घंटों में तेज वर्षा का स्पष्ट संकेत नहीं है।" else "No strong precipitation signal in the next 24 hours. Forecast follows the selected worldwide location."
    }
    Glass { Text(text, color = Color.White.copy(alpha = 0.66f), fontSize = 9.sp, lineHeight = 13.sp) }
}

private fun weatherIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Outlined.WaterDrop
    2, 3, 45, 48 -> Icons.Outlined.Cloud
    else -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
}

private fun iconTint(code: Int, isDay: Boolean): Color = when (code) {
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Color(0xFF7EDDF0)
    2, 3, 45, 48 -> Color(0xFFD4E4E8)
    else -> if (isDay) Color(0xFFFFD65A) else Color(0xFFD9EDFF)
}

@Composable
private fun Glass(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0xA0081B25), border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.13f))) {
        Column(Modifier.padding(13.dp), content = content)
    }
}

@Composable
private fun Footer(text: String) {
    Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = Color(0xEE041119)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(10.dp), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.42f), fontSize = 8.sp)
    }
}
