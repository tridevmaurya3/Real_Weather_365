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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.tridev.realweather365.data.preferences.WeatherPreferences

private val NotificationAccent = Color(0xFF3EDAE8)
private val NotificationGlass = Color(0xA00A1E28)
private val NotificationBorder = Color.White.copy(alpha = 0.11f)

@Composable
fun RealSmartNotificationsScreen(
    preferences: WeatherPreferences,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onPreferencesChanged: (WeatherPreferences) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF061722), Color(0xFF0A2633), Color(0xFF030D13))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartNotificationHeader(onBack)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }
                item {
                    PermissionCard(
                        granted = permissionGranted,
                        onRequestPermission = onRequestPermission
                    )
                }
                item {
                    EngineStatusCard(preferences)
                }
                item {
                    NotificationToggleCard(
                        icon = Icons.Outlined.Cloud,
                        title = "Rain Alert",
                        subtitle = "Notify when rain is likely in the next few hours",
                        checked = preferences.rainAlert,
                        onCheckedChange = { onPreferencesChanged(preferences.copy(rainAlert = it)) }
                    )
                }
                item {
                    NotificationToggleCard(
                        icon = Icons.Outlined.FlashOn,
                        title = "Lightning Alert",
                        subtitle = "High-priority warning when thunderstorm risk is detected",
                        checked = preferences.lightningAlert,
                        danger = true,
                        onCheckedChange = { onPreferencesChanged(preferences.copy(lightningAlert = it)) }
                    )
                }
                item {
                    NotificationToggleCard(
                        icon = Icons.Outlined.Air,
                        title = "AQI Alert",
                        subtitle = "Notify when US AQI rises above 100",
                        checked = preferences.aqiAlert,
                        onCheckedChange = { onPreferencesChanged(preferences.copy(aqiAlert = it)) }
                    )
                }
                item {
                    NotificationToggleCard(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Daily Forecast",
                        subtitle = "Morning summary with high, low and rain chance",
                        checked = preferences.dailyForecast,
                        onCheckedChange = { onPreferencesChanged(preferences.copy(dailyForecast = it)) }
                    )
                }
                item {
                    NotificationToggleCard(
                        icon = Icons.Outlined.WbSunny,
                        title = "Sunrise Alert",
                        subtitle = "Reminder shortly before local sunrise",
                        checked = preferences.sunriseAlert,
                        onCheckedChange = { onPreferencesChanged(preferences.copy(sunriseAlert = it)) }
                    )
                }
                item {
                    NotificationToggleCard(
                        icon = Icons.Outlined.WarningAmber,
                        title = "Severe Weather",
                        subtitle = "High-priority alerts for storms, heavy weather and strong gusts",
                        checked = preferences.severeWeatherAlert,
                        danger = true,
                        onCheckedChange = { onPreferencesChanged(preferences.copy(severeWeatherAlert = it)) }
                    )
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.035f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
                    ) {
                        Text(
                            text = "Background checks run about every 30 minutes when Android allows scheduled work. Alerts use your currently selected worldwide location and are deduplicated to avoid repeated notifications.",
                            modifier = Modifier.padding(13.dp),
                            color = Color.White.copy(alpha = 0.54f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xEE041119),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Text(
                    text = "REAL WEATHER 365  •  SMART ALERT ENGINE",
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
private fun SmartNotificationHeader(onBack: () -> Unit) {
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
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Smart Notifications", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Live worldwide weather intelligence", color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
            }
            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = NotificationAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(granted: Boolean, onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = NotificationGlass,
        border = BorderStroke(
            1.dp,
            if (granted) NotificationAccent.copy(alpha = 0.35f) else Color(0xFFFFA24A).copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (granted) NotificationAccent.copy(alpha = 0.12f) else Color(0xFFFFA24A).copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = if (granted) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = if (granted) NotificationAccent else Color(0xFFFFB15C),
                        modifier = Modifier.padding(9.dp).size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (granted) "Android notifications enabled" else "Permission required",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (granted) {
                            "Real Weather 365 can deliver smart weather alerts in the background."
                        } else {
                            "Allow notifications so enabled weather alerts can reach you."
                        },
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }
            if (!granted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotificationAccent,
                        contentColor = Color(0xFF001A20)
                    )
                ) {
                    Text("Enable Android Notifications", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EngineStatusCard(preferences: WeatherPreferences) {
    val enabledCount = listOf(
        preferences.rainAlert,
        preferences.lightningAlert,
        preferences.aqiAlert,
        preferences.dailyForecast,
        preferences.sunriseAlert,
        preferences.severeWeatherAlert
    ).count { it }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = Color(0x66091922),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Smart alert engine", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(
                    if (enabledCount == 0) "Paused • No alert types enabled" else "Active • $enabledCount of 6 alert types enabled",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }
            Text(
                if (enabledCount == 0) "OFF" else "30 MIN",
                color = if (enabledCount == 0) Color.White.copy(alpha = 0.45f) else NotificationAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NotificationToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    danger: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    val iconColor = if (danger) Color(0xFFFFB15C) else NotificationAccent
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = NotificationGlass,
        border = BorderStroke(1.dp, NotificationBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = iconColor.copy(alpha = 0.11f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp).size(19.dp)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
