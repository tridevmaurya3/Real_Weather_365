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
import com.tridev.realweather365.ui.home.rememberMoonVisualState
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeatherDetailsScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hindi = state.languageCode == "hi"
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF071923), Color(0xFF0A2230), Color(0xFF061720), Color(0xFF031018)))
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(state.location, state.updatedAt, hindi, onBack)
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
            Footer(if (hindi) "लाइव मौसम विवरण • ${state.provider}" else "LIVE WEATHER DETAILS • ${state.provider}")
        }
    }
}

@Composable
private fun Header(location: String, subtitle: String, hindi: Boolean, onBack: () -> Unit) {
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
                Text(if (hindi) "मौसम विवरण • $subtitle" else "Weather Details • $subtitle", color = Color.White.copy(alpha = 0.55f), fontSize = 8.sp, maxLines = 1)
            }
            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun WindCard(state: WeatherHomeUiState) {
    val hindi = state.languageCode == "hi"
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
                Text(if (hindi) "हवा" else "Wind", color = Color.White.copy(alpha = 0.58f), fontSize = 9.sp)
                Text("${state.windSpeed} ${state.windUnit}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                Text(
                    if (hindi) "झोंके ${state.windGusts} ${state.windUnit} • ${WmoWeather.compassDirection(state.windDirection)}" else "Gusts ${state.windGusts} ${state.windUnit} • ${WmoWeather.compassDirection(state.windDirection)}",
                    color = Color(0xFF78DDE8), fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun DetailGrid(state: WeatherHomeUiState) {
    val hindi = state.languageCode == "hi"
    val details = listOf(
        Detail(if (hindi) "दबाव" else "Pressure", if (state.pressure > 0) "${state.pressure} hPa" else "--", Icons.Outlined.Speed),
        Detail(if (hindi) "दृश्यता" else "Visibility", state.visibilityKm?.let { String.format(Locale.ENGLISH, "%.1f %s", it, state.distanceUnit) } ?: "--", Icons.Outlined.Visibility),
        Detail(if (hindi) "नमी" else "Humidity", if (state.humidity > 0) "${state.humidity}%" else "--", Icons.Outlined.WaterDrop),
        Detail(if (hindi) "ओसांक" else "Dew Point", "${state.dewPoint}°", Icons.Outlined.WaterDrop),
        Detail(if (hindi) "यूवी इंडेक्स" else "UV Index", state.uvIndex?.let { String.format(Locale.ENGLISH, "%.1f", it) } ?: "--", Icons.Outlined.WbSunny),
        Detail(if (hindi) "बादल" else "Cloud Cover", "${state.cloudCover}%", Icons.Outlined.Cloud)
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
    val hindi = state.languageCode == "hi"
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0x98071922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WbSunny, null, tint = Color(0xFFFFD45A), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (hindi) "सूर्योदय और सूर्यास्त" else "Sunrise & sunset", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                TimeBlock(if (hindi) "सूर्योदय" else "Sunrise", formatTime(state.sunrise))
                TimeBlock(if (hindi) "सूर्यास्त" else "Sunset", formatTime(state.sunset))
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
    val hindi = state.languageCode == "hi"
    val moon = rememberMoonVisualState(state.selectedLocation)
    val phaseName = if (hindi) moon.phase.hindi else moon.phase.english
    val rise = formatMoonEvent(moon.moonrise, moon.alwaysAboveHorizon, hindi)
    val set = formatMoonEvent(moon.moonset, moon.alwaysAboveHorizon, hindi)
    val horizonStatus = when {
        moon.aboveHorizon -> if (hindi) "क्षितिज के ऊपर" else "Above horizon"
        else -> if (hindi) "क्षितिज के नीचे" else "Below horizon"
    }

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0x98071922), border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(Color(0x262A9BD8), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.NightsStay, null, tint = Color(0xFFDCEEFF), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (hindi) "चंद्र खगोल विज्ञान" else "Moon Astronomy", color = Color.White.copy(alpha = 0.50f), fontSize = 8.sp)
                    Text(phaseName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (hindi) "रोशनी ${moon.illuminationPercent}% • $horizonStatus" else "${moon.illuminationPercent}% illuminated • $horizonStatus",
                        color = Color(0xFF9EDFF0), fontSize = 8.sp
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                TimeBlock(if (hindi) "चंद्रोदय" else "Moonrise", rise)
                TimeBlock(if (hindi) "चंद्रास्त" else "Moonset", set)
                TimeBlock(if (hindi) "ऊंचाई" else "Altitude", String.format(Locale.ENGLISH, "%.0f°", moon.altitudeDegrees))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (hindi) {
                    "दिशा ${String.format(Locale.ENGLISH, "%.0f°", moon.azimuthDegrees)} • खगोलीय स्थिति चयनित स्थान, तारीख और समय क्षेत्र से गणना की गई"
                } else {
                    "Azimuth ${String.format(Locale.ENGLISH, "%.0f°", moon.azimuthDegrees)} • position calculated from selected location, date and time zone"
                },
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 7.sp,
                lineHeight = 10.sp
            )
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

private fun formatMoonEvent(value: ZonedDateTime?, alwaysAbove: Boolean, hindi: Boolean): String {
    if (value != null) {
        return value.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    }
    return when {
        alwaysAbove -> if (hindi) "पूरे दिन ऊपर" else "All day"
        else -> "--"
    }
}

@Composable
private fun Footer(text: String) {
    Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = Color(0xEE041119)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(10.dp), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.42f), fontSize = 8.sp)
    }
}
