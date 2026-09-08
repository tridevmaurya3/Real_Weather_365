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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.theme.RealWeather365Theme

private data class DailyForecast(
    val day: String,
    val date: String,
    val condition: String,
    val low: Int,
    val high: Int,
    val rainChance: Int
)

private val tenDayForecast = listOf(
    DailyForecast("Today", "8 Sep", "Light Rain", 24, 32, 70),
    DailyForecast("Wed", "9 Sep", "Cloudy", 25, 33, 40),
    DailyForecast("Thu", "10 Sep", "Rain", 24, 31, 65),
    DailyForecast("Fri", "11 Sep", "Thunderstorm", 23, 30, 80),
    DailyForecast("Sat", "12 Sep", "Cloudy", 24, 32, 35),
    DailyForecast("Sun", "13 Sep", "Sunny", 25, 34, 10),
    DailyForecast("Mon", "14 Sep", "Sunny", 25, 34, 10),
    DailyForecast("Tue", "15 Sep", "Cloudy", 24, 33, 30),
    DailyForecast("Wed", "16 Sep", "Rain", 24, 31, 60),
    DailyForecast("Thu", "17 Sep", "Cloudy", 24, 32, 35)
)

@Composable
fun Forecast10DayScreen(
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
                        Color(0xFF0A2230),
                        Color(0xFF061720),
                        Color(0xFF031018)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TenDayHeader(location = location, onBack = onBack)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    TenDaySummaryCard()
                    Spacer(modifier = Modifier.height(4.dp))
                    ForecastColumnHeader()
                }

                items(tenDayForecast) { day ->
                    DailyForecastRow(day)
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            TenDayFooter()
        }
    }
}

@Composable
private fun TenDayHeader(location: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xEF071720),
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
                    text = "10-Day Forecast",
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
private fun TenDaySummaryCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xA50A202A),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.13f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Next 10 days",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Warm, humid with several rain chances",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 9.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x252DE0ED),
                    border = BorderStroke(0.6.dp, Color(0xFF5DE6F0).copy(alpha = 0.30f))
                ) {
                    Text(
                        text = "24°–34°",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Color(0xFF9AF3F7),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip("Rainiest", "Fri • 80%", Modifier.weight(1f))
                SummaryChip("Warmest", "Sun • 34°", Modifier.weight(1f))
                SummaryChip("Best day", "Mon • Sunny", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0x65061720),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.White.copy(alpha = 0.48f), fontSize = 7.sp)
            Text(
                value,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ForecastColumnHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("DAY", color = Color.White.copy(alpha = 0.38f), fontSize = 7.sp, modifier = Modifier.width(58.dp))
        Text("WEATHER", color = Color.White.copy(alpha = 0.38f), fontSize = 7.sp, modifier = Modifier.width(92.dp))
        Text("RANGE", color = Color.White.copy(alpha = 0.38f), fontSize = 7.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("RAIN", color = Color.White.copy(alpha = 0.38f), fontSize = 7.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun DailyForecastRow(day: DailyForecast) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = Color(0x91071A23),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(58.dp)) {
                Text(
                    text = day.day,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = day.date,
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 7.sp
                )
            }

            Row(
                modifier = Modifier.width(92.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = dayIcon(day.condition),
                    contentDescription = day.condition,
                    tint = dayIconTint(day.condition),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = day.condition,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 7.sp,
                    maxLines = 2
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${day.low}°",
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(25.dp)
                )
                TemperatureRangeBar(
                    low = day.low,
                    high = day.high,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${day.high}°",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(25.dp)
                )
            }

            Row(
                modifier = Modifier.width(42.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF6BDFF0),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "${day.rainChance}%",
                    color = Color(0xFF8CE8F4),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TemperatureRangeBar(low: Int, high: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(12.dp)) {
        val minTemp = 20f
        val maxTemp = 36f
        val startRatio = ((low - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
        val endRatio = ((high - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
        val y = size.height / 2f

        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF5BDCEA), Color(0xFFFFD25C), Color(0xFFFF9A58))
            ),
            start = Offset(size.width * startRatio, y),
            end = Offset(size.width * endRatio, y),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }
}

private fun dayIcon(condition: String): ImageVector = when {
    condition.contains("thunder", ignoreCase = true) -> Icons.Outlined.FlashOn
    condition.contains("rain", ignoreCase = true) -> Icons.Outlined.WaterDrop
    condition.contains("sun", ignoreCase = true) -> Icons.Outlined.WbSunny
    else -> Icons.Outlined.Cloud
}

private fun dayIconTint(condition: String): Color = when {
    condition.contains("thunder", ignoreCase = true) -> Color(0xFFFFD65A)
    condition.contains("rain", ignoreCase = true) -> Color(0xFF76DFEE)
    condition.contains("sun", ignoreCase = true) -> Color(0xFFFFD553)
    else -> Color(0xFFCEE1E7)
}

@Composable
private fun TenDayFooter() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xED06161E),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.07f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFF58E6AB), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Forecast model ready",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 8.sp
                )
            }
            Text(
                text = "Updated 9:41 AM",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 8.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Forecast10DayPreview() {
    RealWeather365Theme {
        Forecast10DayScreen(location = "Chandauli", onBack = {})
    }
}
