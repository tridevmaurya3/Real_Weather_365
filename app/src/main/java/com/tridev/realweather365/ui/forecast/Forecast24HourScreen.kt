package com.tridev.realweather365.ui.forecast

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.theme.RealWeather365Theme

private data class ForecastHour(
    val time: String,
    val temperature: Int,
    val rainChance: Int,
    val condition: String
)

private val forecastHours = listOf(
    ForecastHour("Now", 28, 70, "Rain"),
    ForecastHour("11 AM", 29, 62, "Rain"),
    ForecastHour("1 PM", 31, 48, "Cloudy"),
    ForecastHour("3 PM", 32, 35, "Cloudy"),
    ForecastHour("5 PM", 30, 52, "Rain"),
    ForecastHour("7 PM", 27, 64, "Rain"),
    ForecastHour("9 PM", 26, 44, "Cloudy"),
    ForecastHour("11 PM", 25, 28, "Night")
)

@Composable
fun Forecast24HourScreen(
    location: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF071923),
                        Color(0xFF0B2734),
                        Color(0xFF071821),
                        Color(0xFF041017)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ForecastHeader(location = location, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                CurrentForecastCard()
                Spacer(modifier = Modifier.height(10.dp))
                TemperatureChartCard()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Next 24 hours",
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(7.dp))
                HourlyStrip()
                Spacer(modifier = Modifier.height(10.dp))
                ForecastInsightCard()
                Spacer(modifier = Modifier.weight(1f))
            }

            ForecastFooter()
        }
    }
}

@Composable
private fun ForecastHeader(location: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xEE071720),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF75E5F0),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = location,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "24-Hour Forecast",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 9.sp,
                    letterSpacing = 0.4.sp
                )
            }

            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    tint = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun CurrentForecastCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xC60B202B),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF75DCF2),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "28°",
                            color = Color.White,
                            fontSize = 44.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Light Rain",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Text(
                        text = "Feels like 31°   •   H: 32°  L: 24°",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 9.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "70%",
                        color = Color(0xFF78E2F0),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "precipitation",
                        color = Color.White.copy(alpha = 0.48f),
                        fontSize = 7.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniStat(Icons.Outlined.WaterDrop, "Humidity", "84%")
                MiniStat(Icons.Outlined.Air, "Wind", "18 km/h")
                MiniStat(Icons.Outlined.WbSunny, "Sunset", "6:22 PM")
            }
        }
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF62D7E8), modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(text = label, color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
            Text(text = value, color = Color.White.copy(alpha = 0.88f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TemperatureChartCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB90A1E28),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Temperature & precipitation",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot(Color(0xFFFFC85D), "Temp")
                    LegendDot(Color(0xFF4CCFE4), "Rain")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ForecastChart()
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Now", "1 PM", "5 PM", "9 PM", "1 AM", "5 AM").forEach {
                    Text(text = it, color = Color.White.copy(alpha = 0.42f), fontSize = 7.sp)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.54f), fontSize = 7.sp)
    }
}

@Composable
private fun ForecastChart() {
    val temps = listOf(28f, 29f, 31f, 32f, 30f, 27f, 26f, 25f, 24f)
    val rain = listOf(70f, 62f, 48f, 35f, 52f, 64f, 44f, 28f, 18f)

    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val left = 10f
        val right = size.width - 10f
        val top = 12f
        val bottom = size.height - 12f
        val step = (right - left) / (temps.size - 1)

        repeat(4) { index ->
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1f
            )
        }

        rain.forEachIndexed { index, chance ->
            val x = left + step * index
            val barHeight = (bottom - top) * (chance / 100f) * 0.42f
            drawLine(
                color = Color(0xFF4CCFE4).copy(alpha = 0.45f),
                start = Offset(x, bottom),
                end = Offset(x, bottom - barHeight),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
        }

        val minTemp = temps.minOrNull() ?: 24f
        val maxTemp = temps.maxOrNull() ?: 32f
        fun tempY(value: Float): Float {
            val normalized = if (maxTemp == minTemp) 0.5f else (value - minTemp) / (maxTemp - minTemp)
            return bottom - 38f - normalized * (bottom - top - 58f)
        }

        val path = Path()
        temps.forEachIndexed { index, temp ->
            val x = left + step * index
            val y = tempY(temp)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = Color(0xFFFFC85D), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, cap = StrokeCap.Round))
        temps.forEachIndexed { index, temp ->
            val x = left + step * index
            val y = tempY(temp)
            drawCircle(color = Color(0xFFFFD77A), radius = 4.2f, center = Offset(x, y))
            drawCircle(color = Color(0xFF0A1E28), radius = 2.1f, center = Offset(x, y))
        }
    }
}

@Composable
private fun HourlyStrip() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(forecastHours) { hour ->
            HourCard(hour)
        }
    }
}

@Composable
private fun HourCard(hour: ForecastHour) {
    Surface(
        modifier = Modifier.width(72.dp),
        shape = RoundedCornerShape(15.dp),
        color = if (hour.time == "Now") Color(0xCF123544) else Color(0xA80A1D27),
        border = BorderStroke(
            0.7.dp,
            if (hour.time == "Now") Color(0xFF55DDEB).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f)
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = hour.time, color = Color.White.copy(alpha = 0.64f), fontSize = 7.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Icon(
                imageVector = when {
                    hour.condition.contains("rain", true) -> Icons.Outlined.WaterDrop
                    hour.condition.contains("night", true) -> Icons.Outlined.NightsStay
                    hour.condition.contains("cloud", true) -> Icons.Outlined.Cloud
                    else -> Icons.Outlined.WbSunny
                },
                contentDescription = null,
                tint = when {
                    hour.condition.contains("rain", true) -> Color(0xFF77DFF0)
                    hour.condition.contains("night", true) -> Color(0xFFD9EEFA)
                    hour.condition.contains("cloud", true) -> Color(0xFFD5E2E7)
                    else -> Color(0xFFFFD36A)
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${hour.temperature}°", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "${hour.rainChance}%", color = Color(0xFF65D9EA), fontSize = 7.sp)
        }
    }
}

@Composable
private fun ForecastInsightCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = Color(0xA40A1D27),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0x1E53D9EA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = Color(0xFF6CDEEE), modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Rain eases by evening", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Highest rain chance is before 1 PM. Temperatures peak near 32° around 3 PM.", color = Color.White.copy(alpha = 0.52f), fontSize = 8.sp, lineHeight = 11.sp)
            }
        }
    }
}

@Composable
private fun ForecastFooter() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xEE06151D),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.07f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "24-HOUR", color = Color(0xFF64E1EC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(text = "Updated just now", color = Color.White.copy(alpha = 0.40f), fontSize = 8.sp)
            Text(text = "10-DAY ›", color = Color.White.copy(alpha = 0.68f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Forecast24HourPreview() {
    RealWeather365Theme {
        Forecast24HourScreen(location = "Chandauli", onBack = {})
    }
}
