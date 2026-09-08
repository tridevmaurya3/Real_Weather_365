package com.tridev.realweather365.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NightsStay
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
    navigation: WeatherHomeNavigation = WeatherHomeNavigation(),
    modifier: Modifier = Modifier
) {
    val solar = rememberSolarVisualState(state.selectedLocation)
    val moon = rememberMoonVisualState(state.selectedLocation)
    val visualScene = solarAdjustedScene(state.scene, solar)

    Box(modifier = modifier.fillMaxSize()) {
        if (state.animationLevel <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(staticSceneBrush(visualScene))
            )
        } else {
            Crossfade(
                targetState = visualScene,
                animationSpec = tween(durationMillis = if (state.animationLevel == 1) 450 else 900),
                label = "weather-world-transition"
            ) { scene ->
                when (scene) {
                    WeatherScene.SUNNY,
                    WeatherScene.SUNRISE -> AstronomicalSkyEnvironment(
                        solar = solar,
                        cloudCover = state.cloudCover
                    )
                    WeatherScene.RAIN -> RainEnvironment()
                    WeatherScene.THUNDERSTORM -> ThunderstormEnvironment()
                    WeatherScene.SNOW -> SnowEnvironment()
                    WeatherScene.NIGHT -> AstronomicalNightEnvironment(
                        moon = moon,
                        solar = solar,
                        cloudCover = state.cloudCover
                    )
                }
            }
        }

        ScientificVolumetricCloudSystem(
            state = state,
            scene = visualScene,
            solar = solar,
            moon = moon,
            modifier = Modifier.fillMaxSize()
        )

        SolarLightingOverlay(
            solar = solar,
            cloudCover = state.cloudCover,
            modifier = Modifier.fillMaxSize()
        )

        VisibilityAtmosphereOverlay(
            animationLevel = state.animationLevel,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(sceneScrim(visualScene))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
        ) {
            LocationHeader(
                state = state,
                onOpenLocations = navigation.openLocations,
                onOpenDetails = navigation.openDetails
            )
            Spacer(modifier = Modifier.weight(1f))
            CurrentConditions(state = state, onOpenTenDay = navigation.openForecast10)
            Spacer(modifier = Modifier.height(9.dp))
            HourlyForecastPanel(hourly = state.hourly, onOpenForecast = navigation.openForecast24)
            Spacer(modifier = Modifier.height(7.dp))
            MetricsPanel(
                metrics = state.metrics,
                onOpenAirQuality = navigation.openAirQuality,
                onOpenDetails = navigation.openDetails
            )
            Spacer(modifier = Modifier.height(7.dp))
            BottomNavigation(
                languageCode = state.languageCode,
                onOpenRadar = navigation.openRadar,
                onOpenForecast = navigation.openForecast10,
                onOpenAlerts = navigation.openAlerts,
                onOpenMore = navigation.openPersonalization
            )
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

private fun staticSceneBrush(scene: WeatherScene): Brush = when (scene) {
    WeatherScene.SUNNY -> Brush.verticalGradient(listOf(Color(0xFF2A93CF), Color(0xFF7CCFE7), Color(0xFF153542)))
    WeatherScene.SUNRISE -> Brush.verticalGradient(listOf(Color(0xFF4B3155), Color(0xFFF09B58), Color(0xFF18333B)))
    WeatherScene.RAIN -> Brush.verticalGradient(listOf(Color(0xFF314C5C), Color(0xFF17333F), Color(0xFF07151C)))
    WeatherScene.THUNDERSTORM -> Brush.verticalGradient(listOf(Color(0xFF1C2436), Color(0xFF28364A), Color(0xFF060D15)))
    WeatherScene.SNOW -> Brush.verticalGradient(listOf(Color(0xFF8FB8CC), Color(0xFFD5E6EC), Color(0xFF1B3543)))
    WeatherScene.NIGHT -> Brush.verticalGradient(listOf(Color(0xFF06132E), Color(0xFF0D3150), Color(0xFF02070D)))
}

private fun sceneScrim(scene: WeatherScene): Brush = when (scene) {
    WeatherScene.SUNNY -> Brush.verticalGradient(
        0f to Color(0x19000B12), 0.22f to Color.Transparent, 0.55f to Color.Transparent,
        0.73f to Color(0x33000A0F), 1f to Color(0xE206131A)
    )
    WeatherScene.SUNRISE -> Brush.verticalGradient(
        0f to Color(0x26020A14), 0.22f to Color.Transparent, 0.50f to Color(0x10000000),
        0.72f to Color(0x4A090B0C), 1f to Color(0xEA071016)
    )
    WeatherScene.RAIN -> Brush.verticalGradient(
        0f to Color(0x2A03101A), 0.26f to Color.Transparent, 0.50f to Color(0x14010B10),
        0.70f to Color(0x52040B10), 1f to Color(0xEE061218)
    )
    WeatherScene.THUNDERSTORM -> Brush.verticalGradient(
        0f to Color(0x42000712), 0.22f to Color(0x17020B13), 0.48f to Color.Transparent,
        0.69f to Color(0x62030A10), 1f to Color(0xF0030E15)
    )
    WeatherScene.SNOW -> Brush.verticalGradient(
        0f to Color(0x220A2235), 0.22f to Color(0x0EFFFFFF), 0.50f to Color.Transparent,
        0.72f to Color(0x42081926), 1f to Color(0xE706151E)
    )
    WeatherScene.NIGHT -> Brush.verticalGradient(
        0f to Color(0x26000A17), 0.20f to Color(0x0D082038), 0.48f to Color.Transparent,
        0.70f to Color(0x4A03121F), 1f to Color(0xEE020C13)
    )
}

@Composable
private fun LocationHeader(
    state: WeatherHomeUiState,
    onOpenLocations: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(34.dp).clickable(onClick = onOpenLocations),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Locations",
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onOpenLocations).padding(vertical = 1.dp),
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

        Box(
            modifier = Modifier.size(34.dp).clickable(onClick = onOpenDetails),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Weather details",
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CurrentConditions(state: WeatherHomeUiState, onOpenTenDay: () -> Unit) {
    val hindi = state.languageCode == "hi"
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTenDay)) {
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
                if (hindi) "महसूस ${state.feelsLike}°" else "Feels like ${state.feelsLike}°",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 10.sp
            )
            Text(
                if (hindi) "अधिक: ${state.high}°   कम: ${state.low}°" else "H: ${state.high}°   L: ${state.low}°",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun HourlyForecastPanel(hourly: List<HourForecast>, onOpenForecast: () -> Unit) {
    GlassPanel(cornerRadius = 15.dp, verticalPadding = 9.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenForecast),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hourly.take(6).forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(item.time, color = Color.White.copy(alpha = 0.74f), fontSize = 8.sp, maxLines = 1)
                    Icon(
                        imageVector = weatherIconFor(item.condition),
                        contentDescription = item.condition,
                        tint = weatherIconTint(item.condition),
                        modifier = Modifier.padding(vertical = 4.dp).size(16.dp)
                    )
                    Text("${item.temperature}°", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun weatherIconFor(condition: String): ImageVector = when {
    condition.contains("night", true) || condition.contains("moon", true) || condition.contains("रात") -> Icons.Outlined.NightsStay
    condition.contains("snow", true) || condition.contains("बर्फ") -> Icons.Outlined.AcUnit
    condition.contains("thunder", true) || condition.contains("storm", true) || condition.contains("गरज") || condition.contains("तूफान") -> Icons.Outlined.FlashOn
    condition.contains("rain", true) || condition.contains("drizzle", true) || condition.contains("बारिश") || condition.contains("बूंद") -> Icons.Outlined.WaterDrop
    condition.contains("cloud", true) || condition.contains("overcast", true) || condition.contains("बादल") -> Icons.Outlined.Cloud
    else -> Icons.Outlined.WbSunny
}

private fun weatherIconTint(condition: String): Color = when {
    condition.contains("night", true) || condition.contains("moon", true) || condition.contains("रात") -> Color(0xFFD8EFFF)
    condition.contains("snow", true) || condition.contains("बर्फ") -> Color(0xFFE7F6FF)
    condition.contains("thunder", true) || condition.contains("storm", true) || condition.contains("गरज") -> Color(0xFFFFD65A)
    condition.contains("rain", true) || condition.contains("बारिश") -> Color(0xFF82DDF2)
    condition.contains("cloud", true) || condition.contains("बादल") -> Color(0xFFCEE2E7)
    condition.equals("Sunrise", true) -> Color(0xFFFFC66B)
    else -> Color(0xFFFFD44D)
}

@Composable
private fun MetricsPanel(
    metrics: List<WeatherMetric>,
    onOpenAirQuality: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        metrics.take(4).forEach { metric ->
            MetricCard(
                metric = metric,
                modifier = Modifier.weight(1f),
                onClick = if (metric.label.equals("AQI", true)) onOpenAirQuality else onOpenDetails
            )
        }
    }
}

@Composable
private fun MetricCard(metric: WeatherMetric, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(72.dp).shadow(7.dp, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
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
            Icon(metricIcon(metric.label), contentDescription = null, tint = Color(0xFF69DCE9), modifier = Modifier.size(16.dp))
            Text(metric.label, color = Color.White.copy(alpha = 0.72f), fontSize = 7.sp, maxLines = 1)
            Text(metric.value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
            Text(metric.hint, color = Color.White.copy(alpha = 0.52f), fontSize = 7.sp, maxLines = 1)
        }
    }
}

private fun metricIcon(label: String): ImageVector = when (label.lowercase()) {
    "aqi" -> Icons.Outlined.Eco
    "wind", "हवा" -> Icons.Outlined.Air
    "humidity", "नमी" -> Icons.Outlined.WaterDrop
    else -> Icons.Outlined.Speed
}

@Composable
private fun GlassPanel(
    cornerRadius: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = Color(0x9605141B),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f)),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = verticalPadding)) { content() }
    }
}

@Composable
private fun BottomNavigation(
    languageCode: String,
    onOpenRadar: () -> Unit,
    onOpenForecast: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenMore: () -> Unit
) {
    val hindi = languageCode == "hi"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xB905141B),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.09f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(Icons.Outlined.WbSunny, if (hindi) "मौसम" else "Weather", true, onClick = {})
            NavItem(Icons.Outlined.Map, if (hindi) "नक्शा" else "Map", false, onClick = onOpenRadar)
            NavItem(Icons.Outlined.Cloud, if (hindi) "पूर्वानुमान" else "Forecast", false, onClick = onOpenForecast)
            NavItem(Icons.Outlined.NotificationsNone, if (hindi) "अलर्ट" else "Alerts", false, onClick = onOpenAlerts)
            NavItem(Icons.Outlined.GridView, if (hindi) "और" else "More", false, onClick = onOpenMore)
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.54f)
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(17.dp))
        Text(label, color = tint, fontSize = 7.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NightHomePreview() {
    RealWeather365Theme { WeatherHomeScreen(WeatherHomeUiState()) }
}
