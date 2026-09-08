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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
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
import androidx.compose.ui.graphics.vector.ImageVector
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
                        0f to Color(0x19000B12),
                        0.22f to Color.Transparent,
                        0.55f to Color.Transparent,
                        0.73f to Color(0x33000A0F),
                        1f to Color(0xE206131A)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
        ) {
            LocationHeader(state)
            Spacer(modifier = Modifier.weight(1f))
            CurrentConditions(state)
            Spacer(modifier = Modifier.height(9.dp))
            HourlyForecastPanel(state.hourly)
            Spacer(modifier = Modifier.height(7.dp))
            MetricsPanel(state.metrics)
            Spacer(modifier = Modifier.height(7.dp))
            BottomNavigation()
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

@Composable
private fun LocationHeader(state: WeatherHomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "Menu",
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .padding(top = 4.dp)
                .size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = state.location,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = state.updatedAt,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 9.sp
            )
        }

        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "More",
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .padding(top = 4.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun CurrentConditions(state: WeatherHomeUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${state.temperature}°",
            color = Color.White,
            fontSize = 52.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = state.condition,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Feels like ${state.feelsLike}°",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 10.sp
            )
            Text(
                text = "H: ${state.high}°   L: ${state.low}°",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun HourlyForecastPanel(hourly: List<HourForecast>) {
    GlassPanel(cornerRadius = 15.dp, verticalPadding = 9.dp) {
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
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = item.condition,
                        tint = Color(0xFFFFD44D),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = "${item.temperature}°",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsPanel(metrics: List<WeatherMetric>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        metrics.take(4).forEach { metric ->
            MetricCard(metric = metric, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(metric: WeatherMetric, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .shadow(7.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9C06171E),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = metricIcon(metric.label),
                contentDescription = null,
                tint = Color(0xFF69DCE9),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = metric.label,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 7.sp,
                maxLines = 1
            )
            Text(
                text = metric.value,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = metric.hint,
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 7.sp,
                maxLines = 1
            )
        }
    }
}

private fun metricIcon(label: String): ImageVector = when (label.lowercase()) {
    "aqi" -> Icons.Outlined.Eco
    "wind" -> Icons.Outlined.Air
    "humidity" -> Icons.Outlined.WaterDrop
    else -> Icons.Outlined.Speed
}

@Composable
private fun GlassPanel(
    cornerRadius: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = Color(0x9605141B),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f)),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = verticalPadding)) {
            content()
        }
    }
}

@Composable
private fun BottomNavigation() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xB905141B),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.09f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 6.dp),
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
    icon: ImageVector,
    label: String,
    selected: Boolean
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.54f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(17.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 7.sp,
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
