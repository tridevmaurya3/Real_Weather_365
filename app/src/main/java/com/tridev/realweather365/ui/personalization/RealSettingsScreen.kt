package com.tridev.realweather365.ui.personalization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.data.preferences.AnimationQuality
import com.tridev.realweather365.data.preferences.AppLanguage
import com.tridev.realweather365.data.preferences.RefreshRate
import com.tridev.realweather365.data.preferences.UnitSystem
import com.tridev.realweather365.data.preferences.WeatherPreferences

private val SettingsAccent = Color(0xFF43DCE9)
private val SettingsGlass = Color(0xA0081B25)

@Composable
fun RealSettingsScreen(
    preferences: WeatherPreferences,
    onPreferencesChanged: (WeatherPreferences) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hindi = preferences.appLanguage == AppLanguage.HINDI
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF061722), Color(0xFF0A2633), Color(0xFF030D13)))
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            SettingsHeader(
                title = if (hindi) "सेटिंग्स" else "Settings",
                subtitle = if (hindi) "मौसम आपके तरीके से" else "Weather your way",
                onBack = onBack
            )
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(2.dp)) }
                item {
                    ChoiceSection(
                        icon = Icons.Outlined.Thermostat,
                        title = if (hindi) "इकाइयाँ" else "Units",
                        subtitle = if (hindi) "तापमान, हवा और दूरी" else "Temperature, wind and distance"
                    ) {
                        UnitSystem.entries.forEach { unit ->
                            SettingsChoice(
                                text = if (unit == UnitSystem.METRIC) "Metric • °C • km/h" else "Imperial • °F • mph",
                                selected = preferences.unitSystem == unit,
                                onClick = { onPreferencesChanged(preferences.copy(unitSystem = unit)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    ChoiceSection(
                        icon = Icons.Outlined.Language,
                        title = if (hindi) "भाषा" else "Language",
                        subtitle = if (hindi) "मौसम सामग्री की भाषा" else "Language used across weather content"
                    ) {
                        AppLanguage.entries.forEach { language ->
                            SettingsChoice(
                                text = language.label,
                                selected = preferences.appLanguage == language,
                                onClick = { onPreferencesChanged(preferences.copy(appLanguage = language)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    ChoiceSection(
                        icon = Icons.Outlined.Refresh,
                        title = if (hindi) "रिफ्रेश दर" else "Refresh Rate",
                        subtitle = if (hindi) "ऐप और बैकग्राउंड मौसम अपडेट" else "App and background weather refresh"
                    ) {
                        RefreshRate.entries.forEach { rate ->
                            SettingsChoice(
                                text = "${rate.minutes}m",
                                selected = preferences.refreshRate == rate,
                                onClick = { onPreferencesChanged(preferences.copy(refreshRate = rate)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    ChoiceSection(
                        icon = Icons.Outlined.Speed,
                        title = if (hindi) "एनीमेशन गुणवत्ता" else "Animation Quality",
                        subtitle = if (hindi) "रियलिज्म और परफॉर्मेंस का संतुलन" else "Balance realism and performance"
                    ) {
                        AnimationQuality.entries.forEach { quality ->
                            SettingsChoice(
                                text = when (quality) {
                                    AnimationQuality.HIGH -> if (hindi) "उच्च" else "High"
                                    AnimationQuality.BALANCED -> if (hindi) "संतुलित" else "Balanced"
                                    AnimationQuality.OPTIMIZED -> if (hindi) "अनुकूलित" else "Optimized"
                                },
                                selected = preferences.animationQuality == quality,
                                onClick = { onPreferencesChanged(preferences.copy(animationQuality = quality)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = SettingsGlass,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.11f))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.BatterySaver, null, tint = SettingsAccent, modifier = Modifier.size(21.dp))
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (hindi) "बैटरी सेवर" else "Battery Saver", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (hindi) "एनीमेशन कम और रिफ्रेश कम-से-कम 60 मिनट" else "Static weather scene and minimum 60-minute refresh",
                                    color = Color.White.copy(alpha = 0.52f), fontSize = 8.sp, lineHeight = 11.sp
                                )
                            }
                            Switch(
                                checked = preferences.batterySaver,
                                onCheckedChange = { onPreferencesChanged(preferences.copy(batterySaver = it)) }
                            )
                        }
                    }
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x6511C6D5),
                        border = BorderStroke(1.dp, SettingsAccent.copy(alpha = 0.28f))
                    ) {
                        Column(Modifier.padding(13.dp)) {
                            Text(if (hindi) "लाइव प्रभाव" else "Live effect", color = SettingsAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (hindi) {
                                    "${preferences.unitSystem.temperatureUnit} • ${preferences.unitSystem.windUnit} • ${preferences.appLanguage.label} • प्रभावी रिफ्रेश ${preferences.effectiveRefreshMinutes()} मिनट • ${preferences.effectiveAnimationQuality().label}"
                                } else {
                                    "${preferences.unitSystem.temperatureUnit} • ${preferences.unitSystem.windUnit} • ${preferences.appLanguage.label} • effective refresh ${preferences.effectiveRefreshMinutes()} min • ${preferences.effectiveAnimationQuality().label}"
                                },
                                color = Color.White.copy(alpha = 0.72f), fontSize = 9.sp, lineHeight = 13.sp
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
            Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = Color(0xEE041119)) {
                Text(
                    if (hindi) "REAL WEATHER 365 • सेटिंग्स तुरंत लागू होती हैं" else "REAL WEATHER 365 • SETTINGS APPLY INSTANTLY",
                    modifier = Modifier.fillMaxWidth().padding(10.dp), textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.42f), fontSize = 8.sp, letterSpacing = 0.6.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().statusBarsPadding(), color = Color(0xEE06151D)) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
            }
            Spacer(Modifier.size(42.dp))
        }
    }
}

@Composable
private fun ChoiceSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    choices: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SettingsGlass,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.11f))
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = SettingsAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(9.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), content = choices)
        }
    }
}

@Composable
private fun SettingsChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) SettingsAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, if (selected) SettingsAccent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.08f))
    ) {
        Text(
            text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            textAlign = TextAlign.Center,
            color = if (selected) SettingsAccent else Color.White.copy(alpha = 0.68f),
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
