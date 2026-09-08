package com.tridev.realweather365.ui.personalization

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.tridev.realweather365.data.preferences.AnimationQuality
import com.tridev.realweather365.data.preferences.BackgroundWorld
import com.tridev.realweather365.data.preferences.WeatherPreferences
import com.tridev.realweather365.ui.home.WeatherHomeUiState

private val ScreenTop = Color(0xFF061722)
private val ScreenMid = Color(0xFF0A2633)
private val ScreenBottom = Color(0xFF030D13)
private val Accent = Color(0xFF3EDAE8)
private val Glass = Color(0xA00A1E28)
private val Border = Color.White.copy(alpha = 0.11f)

@Composable
fun PersonalizationHubScreen(
    state: WeatherHomeUiState,
    preferences: WeatherPreferences,
    onBack: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenBackgrounds: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalizationScaffold(
        title = "Personalize",
        subtitle = "Your weather, your way",
        onBack = onBack,
        modifier = modifier
    ) {
        item {
            HeroCard(
                title = state.location,
                value = "${state.temperature}°",
                subtitle = state.condition
            )
        }
        item {
            Text(
                text = "PERSONALIZED WEATHER EXPERIENCE",
                color = Accent,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            FeatureCard(
                icon = Icons.Outlined.GridView,
                title = "Widgets",
                subtitle = "Small, Medium and Large live-weather layouts",
                trailing = "3 sizes",
                onClick = onOpenWidgets
            )
        }
        item {
            FeatureCard(
                icon = Icons.Outlined.WbSunny,
                title = "Animated Backgrounds",
                subtitle = if (preferences.automaticBackground) {
                    "Auto • Changes with live weather"
                } else {
                    "Manual • ${preferences.manualBackground.label}"
                },
                trailing = if (preferences.automaticBackground) "AUTO" else "MANUAL",
                onClick = onOpenBackgrounds
            )
        }
        item {
            val enabledCount = listOf(
                preferences.rainAlert,
                preferences.lightningAlert,
                preferences.aqiAlert,
                preferences.dailyForecast,
                preferences.sunriseAlert,
                preferences.severeWeatherAlert
            ).count { it }
            FeatureCard(
                icon = Icons.Outlined.NotificationsNone,
                title = "Smart Notifications",
                subtitle = "Rain, lightning, AQI, forecast, sunrise and severe weather",
                trailing = "$enabledCount ON",
                onClick = onOpenNotifications
            )
        }
        item {
            FeatureCard(
                icon = Icons.Outlined.Settings,
                title = "Settings",
                subtitle = "Units, data source, refresh, animation and battery",
                trailing = preferences.animationQuality.label,
                onClick = onOpenSettings
            )
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }
}

@Composable
fun WidgetsScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalizationScaffold(
        title = "Widgets",
        subtitle = "Beautify your home screen with live weather",
        onBack = onBack,
        modifier = modifier
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WidgetPreviewSmall(
                    title = state.location,
                    temperature = state.temperature,
                    condition = state.condition,
                    modifier = Modifier.weight(1f)
                )
                WidgetPreviewMedium(
                    title = state.location,
                    temperature = state.temperature,
                    condition = state.condition,
                    modifier = Modifier.weight(1.25f)
                )
            }
        }
        item {
            Text("Small (2×2)                     Medium (4×2)", color = Color.White.copy(alpha = 0.48f), fontSize = 9.sp)
        }
        item {
            WidgetPreviewLarge(state)
        }
        item {
            Text("Large (4×3)", color = Color.White.copy(alpha = 0.48f), fontSize = 9.sp)
        }
        item {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x2219D4E5),
                    contentColor = Accent
                ),
                border = BorderStroke(1.dp, Accent)
            ) {
                Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Add Widgets to Home Screen", fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            StatusNote("Widget previews are ready. Android home-screen widget pinning is connected in the next widget-engine stage.")
        }
    }
}

@Composable
fun AnimatedBackgroundsScreen(
    preferences: WeatherPreferences,
    onPreferencesChanged: (WeatherPreferences) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalizationScaffold(
        title = "Animated Backgrounds",
        subtitle = "Choose a dynamic background for your weather experience",
        onBack = onBack,
        modifier = modifier
    ) {
        item {
            BackgroundGrid(
                selected = preferences.manualBackground,
                automatic = preferences.automaticBackground,
                onSelected = { world ->
                    onPreferencesChanged(
                        preferences.copy(
                            automaticBackground = false,
                            manualBackground = world
                        )
                    )
                }
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Glass,
                border = BorderStroke(1.dp, if (preferences.automaticBackground) Accent.copy(alpha = 0.55f) else Border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Accent.copy(alpha = 0.12f)) {
                        Icon(
                            Icons.Outlined.Cloud,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Change background based on conditions",
                            color = Color.White.copy(alpha = 0.52f),
                            fontSize = 9.sp
                        )
                    }
                    Switch(
                        checked = preferences.automaticBackground,
                        onCheckedChange = {
                            onPreferencesChanged(preferences.copy(automaticBackground = it))
                        }
                    )
                }
            }
        }
        item {
            StatusNote(
                if (preferences.automaticBackground) {
                    "Automatic Background is active. The live weather engine chooses Clear Sky, Sunrise, Rain, Storm, Snow or Night."
                } else {
                    "Manual Background is active: ${preferences.manualBackground.label}. This choice now overrides the live scene on Weather Home."
                }
            )
        }
    }
}

@Composable
fun SmartNotificationsScreen(
    preferences: WeatherPreferences,
    onPreferencesChanged: (WeatherPreferences) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalizationScaffold(
        title = "Smart Notifications",
        subtitle = "Stay informed about what matters",
        onBack = onBack,
        modifier = modifier
    ) {
        item {
            NotificationToggle(
                Icons.Outlined.Cloud,
                "Rain Alert",
                "Get notified before rain starts",
                preferences.rainAlert
            ) { onPreferencesChanged(preferences.copy(rainAlert = it)) }
        }
        item {
            NotificationToggle(
                Icons.Outlined.FlashOn,
                "Lightning Alert",
                "Be aware of lightning in your area",
                preferences.lightningAlert
            ) { onPreferencesChanged(preferences.copy(lightningAlert = it)) }
        }
        item {
            NotificationToggle(
                Icons.Outlined.Air,
                "AQI Alert",
                "Get notified about poor air quality",
                preferences.aqiAlert
            ) { onPreferencesChanged(preferences.copy(aqiAlert = it)) }
        }
        item {
            NotificationToggle(
                Icons.Outlined.GridView,
                "Daily Forecast",
                "Receive your daily weather summary",
                preferences.dailyForecast
            ) { onPreferencesChanged(preferences.copy(dailyForecast = it)) }
        }
        item {
            NotificationToggle(
                Icons.Outlined.WbSunny,
                "Sunrise Alert",
                "Get notified about sunrise time",
                preferences.sunriseAlert
            ) { onPreferencesChanged(preferences.copy(sunriseAlert = it)) }
        }
        item {
            NotificationToggle(
                Icons.Outlined.FlashOn,
                "Severe Weather",
                "Important alerts for extreme conditions",
                preferences.severeWeatherAlert,
                danger = true
            ) { onPreferencesChanged(preferences.copy(severeWeatherAlert = it)) }
        }
        item {
            StatusNote("These notification preferences are saved on the device. Background scheduling and Android notification delivery are added in the notification-engine stage.")
        }
    }
}

@Composable
fun SettingsScreen(
    preferences: WeatherPreferences,
    onPreferencesChanged: (WeatherPreferences) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalizationScaffold(
        title = "Settings",
        subtitle = "Beautiful • Intelligent • Personal",
        onBack = onBack,
        modifier = modifier
    ) {
        item { SettingValueRow(Icons.Outlined.Speed, "Units", preferences.unitsLabel) }
        item { SettingValueRow(Icons.Outlined.MoreVert, "Language", preferences.languageLabel) }
        item { SettingValueRow(Icons.Outlined.Cloud, "Data Source", preferences.dataSourceLabel) }
        item { SettingValueRow(Icons.Outlined.Speed, "Refresh Rate", preferences.refreshRateLabel) }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp),
                color = Glass,
                border = BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(13.dp)) {
                    Text("Animation Quality", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AnimationQuality.entries.forEach { quality ->
                            ChoiceChip(
                                text = quality.label,
                                selected = preferences.animationQuality == quality,
                                modifier = Modifier.weight(1f),
                                onClick = { onPreferencesChanged(preferences.copy(animationQuality = quality)) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SettingToggleRow(
                icon = Icons.Outlined.Eco,
                title = "Battery Saver",
                subtitle = "Reduce animations and background updates",
                checked = preferences.batterySaver,
                onCheckedChange = { onPreferencesChanged(preferences.copy(batterySaver = it)) }
            )
        }
        item { SettingValueRow(Icons.Outlined.NightsStay, "Theme", preferences.themeLabel) }
        item {
            SettingToggleRow(
                icon = Icons.Outlined.GridView,
                title = "Accessibility",
                subtitle = "Larger text, high contrast",
                checked = preferences.accessibilityEnabled,
                onCheckedChange = { onPreferencesChanged(preferences.copy(accessibilityEnabled = it)) }
            )
        }
        item { SettingValueRow(Icons.Outlined.Cloud, "About", "Real Weather 365 v0.1") }
    }
}

@Composable
private fun PersonalizationScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ScreenTop, ScreenMid, ScreenBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title, subtitle, onBack)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = {
                    item { Spacer(modifier = Modifier.height(2.dp)) }
                    content()
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            )
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = Color(0xEE041119),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Text(
                    text = "REAL WEATHER 365  •  PERSONALIZED EXPERIENCE",
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        color = Color(0xEE06151D)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp, maxLines = 1)
            }
            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Settings, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun HeroCard(title: String, value: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xB00B2632),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.23f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Accent.copy(alpha = 0.12f)) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = Accent, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
            }
            Text(value, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, subtitle: String, trailing: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Glass,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Accent.copy(alpha = 0.10f)) {
                Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Spacer(modifier = Modifier.size(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.48f), fontSize = 9.sp, lineHeight = 12.sp)
            }
            Text(trailing, color = Accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WidgetPreviewSmall(title: String, temperature: Int, condition: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(150.dp), shape = RoundedCornerShape(18.dp), color = Color(0xCC073043), border = BorderStroke(1.dp, Accent.copy(alpha = 0.25f))) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = Color(0xFFFFD84A), modifier = Modifier.size(30.dp))
            Column {
                Text("$temperature°", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
                Text(condition, color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, maxLines = 1)
                Text(title, color = Color.White.copy(alpha = 0.45f), fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun WidgetPreviewMedium(title: String, temperature: Int, condition: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(150.dp), shape = RoundedCornerShape(18.dp), color = Color(0xCC102B38), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.13f))) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Outlined.Air, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
            Text("$temperature°", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Text(condition, color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun WidgetPreviewLarge(state: WeatherHomeUiState) {
    Surface(modifier = Modifier.fillMaxWidth().height(152.dp), shape = RoundedCornerShape(19.dp), color = Color(0xD00A2534), border = BorderStroke(1.dp, Accent.copy(alpha = 0.23f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(state.location, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${state.temperature}°", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light)
                Text(state.condition, color = Color.White.copy(alpha = 0.68f), fontSize = 10.sp)
                Text("H: ${state.high}°   L: ${state.low}°", color = Color.White.copy(alpha = 0.48f), fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("9:41", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
                Spacer(modifier = Modifier.height(7.dp))
                Icon(Icons.Outlined.NightsStay, contentDescription = null, tint = Color(0xFFDDEFFF), modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun BackgroundGrid(selected: BackgroundWorld, automatic: Boolean, onSelected: (BackgroundWorld) -> Unit) {
    val worlds = BackgroundWorld.entries
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        worlds.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { world ->
                    BackgroundTile(
                        world = world,
                        selected = !automatic && selected == world,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelected(world) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundTile(world: BackgroundWorld, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val icon = when (world) {
        BackgroundWorld.CLEAR_SKY, BackgroundWorld.SUNRISE -> Icons.Outlined.WbSunny
        BackgroundWorld.RAIN -> Icons.Outlined.WaterDrop
        BackgroundWorld.STORM -> Icons.Outlined.FlashOn
        BackgroundWorld.SNOW -> Icons.Outlined.AcUnit
        BackgroundWorld.NIGHT -> Icons.Outlined.NightsStay
    }
    val colors = when (world) {
        BackgroundWorld.CLEAR_SKY -> listOf(Color(0xFF2E87C8), Color(0xFF173C5B))
        BackgroundWorld.SUNRISE -> listOf(Color(0xFFF3A348), Color(0xFF5D3242))
        BackgroundWorld.RAIN -> listOf(Color(0xFF4A6B7B), Color(0xFF152C36))
        BackgroundWorld.STORM -> listOf(Color(0xFF273A58), Color(0xFF0B1220))
        BackgroundWorld.SNOW -> listOf(Color(0xFFB9D7EA), Color(0xFF536B7D))
        BackgroundWorld.NIGHT -> listOf(Color(0xFF173658), Color(0xFF040C18))
    }
    Surface(
        modifier = modifier.height(112.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 1.5.dp else 0.7.dp, if (selected) Accent else Color.White.copy(alpha = 0.12f)),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors))) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.align(Alignment.Center).size(32.dp))
            Text(
                world.label,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NotificationToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, danger: Boolean = false, onCheckedChange: (Boolean) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Glass, border = BorderStroke(1.dp, Border)) {
        Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (danger) Color(0xFFFF625E) else Accent, modifier = Modifier.size(21.dp))
            Spacer(modifier = Modifier.size(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.46f), fontSize = 8.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingValueRow(icon: ImageVector, title: String, subtitle: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Glass, border = BorderStroke(1.dp, Border)) {
        Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
            }
            Text("›", color = Color.White.copy(alpha = 0.42f), fontSize = 22.sp)
        }
    }
}

@Composable
private fun SettingToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Glass, border = BorderStroke(1.dp, Border)) {
        Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.46f), fontSize = 8.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) Accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, if (selected) Accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            color = if (selected) Accent else Color.White.copy(alpha = 0.65f),
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun StatusNote(text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = Color(0x50081820), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))) {
        Text(text, modifier = Modifier.padding(12.dp), color = Color.White.copy(alpha = 0.48f), fontSize = 9.sp, lineHeight = 13.sp)
    }
}
