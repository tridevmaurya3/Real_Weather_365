package com.tridev.realweather365.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RealWeatherDarkColors = darkColorScheme(
    primary = Color(0xFF17D7EA),
    onPrimary = Color(0xFF001F24),
    secondary = Color(0xFF84DDEC),
    background = Color(0xFF06131A),
    surface = Color(0xFF0A1A22),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun RealWeather365Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RealWeatherDarkColors,
        content = content
    )
}
