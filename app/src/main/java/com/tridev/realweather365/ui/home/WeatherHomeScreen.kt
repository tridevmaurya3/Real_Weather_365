package com.tridev.realweather365.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.theme.RealWeather365Theme

@Composable
fun WeatherHomeScreen(
    state: WeatherHomeUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        SunnyEnvironment()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.72f to Color(0x22020A0F),
                        1f to Color(0xD909151B)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
        ) {
            LocationHeader(state)
            Spacer(modifier = Modifier.weight(1f))
            CurrentConditions(state)
            Spacer(modifier = Modifier.height(12.dp))
            HourlyForecastPanel(state.hourly)
            Spacer(modifier = Modifier.height(9.dp))
            MetricsPanel(state.metrics)
            Spacer(modifier = Modifier.height(9.dp))
            BottomNavigation()
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LocationHeader(state: WeatherHomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = state.location,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = state.updatedAt,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CurrentConditions(state: WeatherHomeUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${state.temperature}°",
            color = Color.White,
            fontSize = 58.sp,
            lineHeight = 58.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = state.condition,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Feels like ${state.feelsLike}°",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 11.sp
            )
            Text(
                text = "H ${state.high}°  L ${state.low}°",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun HourlyForecastPanel(hourly: List<HourForecast>) {
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hourly.take(6).forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.time,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFFFD44D),
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .size(17.dp)
                    )
                    Text(
                        text = "${item.temperature}°",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsPanel(metrics: List<WeatherMetric>) {
    GlassPanel {
        Row(modifier = Modifier.fillMaxWidth()) {
            metrics.take(4).forEachIndexed { index, metric ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = metric.label,
                        color = Color(0xFF91E6F0),
                        fontSize = 9.sp
                    )
                    Text(
                        text = metric.value,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = metric.hint,
                        color = Color.White.copy(alpha = 0.58f),
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassPanel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color(0x9A07171F),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f)),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            content()
        }
    }
}

@Composable
private fun BottomNavigation() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xC506151C),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(Icons.Outlined.WbSunny, "Weather", true)
            NavItem(Icons.Outlined.Map, "Map", false)
            NavItem(Icons.Outlined.Cloud, "Forecast", false)
            NavItem(Icons.Outlined.NotificationsNone, "Alerts", false)
            NavItem(Icons.Outlined.GridView, "More", false)
        }
    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.56f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SunnyHomePreview() {
    RealWeather365Theme {
        WeatherHomeScreen(WeatherHomeUiState())
    }
}
