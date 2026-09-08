package com.tridev.realweather365.ui.details

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.data.weather.WmoWeather
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor

@Composable
fun WeatherDetailsScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF071923), Color(0xFF0A2230), Color(0xFF061720), Color(0xFF031018)))
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(state.location, state.updatedAt, onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                WindCard(state)
                DetailGrid(state)
                SunCard(state)
                MoonCard(state)
                ProviderCard(state)
            }
            Footer("LIVE WEATHER DETAILS • ${state.provider}")
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
                Text("Weather Details • $subtitle", color = Color.White.copy(alpha = 0.55f), fontSize = 8.sp, maxLines = 1)
            }
            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun WindCard(state: WeatherHomeUiState) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color(0xA0081B25), border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.13f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp).background(Color(0x241AD5E2), CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Air, null, tint = Color(0xFF75E4EE), modifier = Modifier.size(26.dp))
                    Text(WmoWeather.compassDirection(state.windDirection), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${state.windDirection}°", color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Wind", color = Color.White.copy(alpha = 0.58f), fontSize = 9.sp)
                Text("${state.windSpeed} km/h", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                Text("Gusts ${state.windGusts} km/h • ${WmoWeather.compassDirection(state.windDirection)}", color = Color(0xFF78DDE8), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun DetailGrid(state: WeatherHomeUiState) {
    val details = listOf(
        Detail("Pressure", if (state.pressure > 0) "${state.pressure} hPa" else "--", Icons.Outlined.Speed),
        Detail("Visibility", state.visibilityKm?.let { String.format(Locale.ENGLISH, "%.1f km", it) } ?: "--", Icons.Outlined.Visibility),
        Detail("Humidity", if (state.humidity > 0) "${state.humidity}%" else "--", Icons.Outlined.WaterDrop),
        Detail("Dew Point", "${state.dewPoint}°", Icons.Outlined.WaterDrop),
        Detail("UV Index", state.uvIndex?.let { String.format(Locale.ENGLISH, "%.1f", it) } ?: "--", Icons.Outlined.WbSunny),
        Detail("Cloud Cover", "${state.cloudCover}%", Icons.Outlined.Cloud)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        details.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item -> DetailCard(item, Modifier.weight(1f)) }
            }
        }
    }
}

private data class Detail(val label: String, val value: String, val icon: ImageVector)

@Composable
private fun DetailCard(detail: Detail, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color(0x98071922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))) {
        Column(Modifier.padding(12.dp)) {
            Icon(detail.icon, null, tint = Color(0xFF72DDE8), modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(7.dp))
            Text(detail.label, color = Color.White.copy(alpha = 0.52f), fontSize = 8.sp)
            Text(detail.value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SunCard(state: WeatherHomeUiState) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0x98071922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WbSunny, null, tint = Color(0xFFFFD45A), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text("Sunrise & sunset", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                TimeBlock("Sunrise", formatTime(state.sunrise))
                TimeBlock("Sunset", formatTime(state.sunset))
            }
        }
    }
}

@Composable
private fun TimeBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MoonCard(state: WeatherHomeUiState) {
    val moon = moonPhase(state.observedAt)
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0x98071922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0x262A9BD8), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.NightsStay, null, tint = Color(0xFFDCEEFF), modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Moon Phase", color = Color.White.copy(alpha = 0.50f), fontSize = 8.sp)
                Text(moon, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Astronomical estimate from local forecast date", color = Color.White.copy(alpha = 0.42f), fontSize = 7.sp)
            }
        }
    }
}

@Composable
private fun ProviderCard(state: WeatherHomeUiState) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = Color(0x70071922)) {
        Text(
            "${state.selectedLocation.secondaryLabel} • ${String.format(Locale.ENGLISH, "%.3f", state.selectedLocation.latitude)}, ${String.format(Locale.ENGLISH, "%.3f", state.selectedLocation.longitude)} • ${state.selectedLocation.timeZoneId}",
            modifier = Modifier.padding(12.dp), color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp, lineHeight = 12.sp
        )
    }
}

private fun formatTime(value: String): String {
    if (value.isBlank()) return "--"
    return runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    }.getOrElse { value.substringAfter('T', value) }
}

private fun moonPhase(observedAt: String): String {
    val date = runCatching { LocalDateTime.parse(observedAt).toLocalDate() }.getOrElse { LocalDate.now() }
    val epoch = LocalDate.of(2000, 1, 6)
    val days = java.time.temporal.ChronoUnit.DAYS.between(epoch, date).toDouble()
    val cycle = ((days % 29.53058867) + 29.53058867) % 29.53058867
    val fraction = cycle / 29.53058867
    return when {
        fraction < 0.0625 || fraction >= 0.9375 -> "New Moon"
        fraction < 0.1875 -> "Waxing Crescent"
        fraction < 0.3125 -> "First Quarter"
        fraction < 0.4375 -> "Waxing Gibbous"
        fraction < 0.5625 -> "Full Moon"
        fraction < 0.6875 -> "Waning Gibbous"
        fraction < 0.8125 -> "Last Quarter"
        else -> "Waning Crescent"
    }
}

@Composable
private fun Footer(text: String) {
    Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = Color(0xEE041119)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(10.dp), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.42f), fontSize = 8.sp)
    }
}
