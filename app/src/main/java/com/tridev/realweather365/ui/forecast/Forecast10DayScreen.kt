package com.tridev.realweather365.ui.forecast

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.LocationOn
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
import com.tridev.realweather365.data.weather.LiveDayData
import com.tridev.realweather365.ui.home.WeatherHomeUiState

@Composable
fun Forecast10DayScreen(
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
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SummaryCard(state)
                    Spacer(Modifier.height(4.dp))
                }
                if (state.daily10.isEmpty()) {
                    item { EmptyCard(state) }
                } else {
                    items(state.daily10, key = { it.isoDate }) { day -> DayRow(day) }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
            Footer("10-DAY LIVE FORECAST • ${state.provider}")
        }
    }
}

@Composable
private fun Header(location: String, subtitle: String, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().statusBarsPadding(), color = Color(0xEF071720)) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFF75E5F0), modifier = Modifier.size(13.dp))
                    Text(location, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("10-Day Forecast • $subtitle", color = Color.White.copy(alpha = 0.55f), fontSize = 8.sp, maxLines = 1)
            }
            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun SummaryCard(state: WeatherHomeUiState) {
    val days = state.daily10
    val min = days.minOfOrNull { it.low } ?: state.low
    val max = days.maxOfOrNull { it.high } ?: state.high
    val rainiest = days.maxByOrNull { it.rainChance }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xA50A202A),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.13f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Next 10 days", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Live outlook for ${state.selectedLocation.secondaryLabel}", color = Color.White.copy(alpha = 0.58f), fontSize = 9.sp)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0x252DE0ED)) {
                    Text("$min°–$max°", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color(0xFF78E4EE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SummaryChip("Today", state.condition, Modifier.weight(1f))
                SummaryChip("Rainiest", rainiest?.let { "${it.dayLabel} ${it.rainChance}%" } ?: "--", Modifier.weight(1f))
                SummaryChip("Updated", state.updatedAt.removePrefix("Live • "), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = Color(0x77071922), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.09f))) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.White.copy(alpha = 0.45f), fontSize = 7.sp)
            Text(value, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun DayRow(day: LiveDayData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x98071922),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(54.dp)) {
                Text(day.dayLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(day.dateLabel, color = Color.White.copy(alpha = 0.46f), fontSize = 7.sp)
            }
            Icon(iconFor(day.weatherCode), null, tint = tintFor(day.weatherCode), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(day.condition, color = Color.White.copy(alpha = 0.82f), fontSize = 9.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WaterDrop, null, tint = Color(0xFF72DDE9), modifier = Modifier.size(10.dp))
                    Text("${day.rainChance}%", color = Color(0xFF80DEE9), fontSize = 7.sp)
                }
            }
            Text("${day.low}°", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(70.dp).height(5.dp).background(Color.White.copy(alpha = 0.10f), CircleShape)) {
                Box(Modifier.fillMaxWidth(((day.high - day.low + 3) / 18f).coerceIn(0.18f, 1f)).height(5.dp).background(Brush.horizontalGradient(listOf(Color(0xFF5DD9EA), Color(0xFFFFC85A))), CircleShape))
            }
            Spacer(Modifier.width(8.dp))
            Text("${day.high}°", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyCard(state: WeatherHomeUiState) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0x90071922)) {
        Text(
            if (state.isLoading) "Loading live 10-day forecast…" else state.errorMessage ?: "Forecast unavailable",
            modifier = Modifier.padding(16.dp), color = Color.White.copy(alpha = 0.62f), fontSize = 9.sp
        )
    }
}

private fun iconFor(code: Int): ImageVector = when (code) {
    95,96,99 -> Icons.Outlined.FlashOn
    71,73,75,77,85,86 -> Icons.Outlined.AcUnit
    51,53,55,56,57,61,63,65,66,67,80,81,82 -> Icons.Outlined.WaterDrop
    2,3,45,48 -> Icons.Outlined.Cloud
    else -> Icons.Outlined.WbSunny
}

private fun tintFor(code: Int): Color = when (code) {
    95,96,99 -> Color(0xFFFFD45A)
    71,73,75,77,85,86 -> Color(0xFFE5F5FF)
    51,53,55,56,57,61,63,65,66,67,80,81,82 -> Color(0xFF79DDEA)
    2,3,45,48 -> Color(0xFFD5E3E7)
    else -> Color(0xFFFFD45A)
}

@Composable
private fun Footer(text: String) {
    Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = Color(0xEE041119)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(10.dp), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.42f), fontSize = 8.sp)
    }
}
