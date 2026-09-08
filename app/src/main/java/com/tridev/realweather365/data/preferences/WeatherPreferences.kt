package com.tridev.realweather365.data.preferences

import android.content.Context

enum class BackgroundWorld(val label: String) {
    CLEAR_SKY("Clear Sky"),
    SUNRISE("Sunrise"),
    RAIN("Rain"),
    STORM("Storm"),
    SNOW("Snow"),
    NIGHT("Night")
}

enum class AnimationQuality(val label: String) {
    HIGH("High"),
    BALANCED("Balanced"),
    OPTIMIZED("Optimized")
}

data class WeatherPreferences(
    val automaticBackground: Boolean = true,
    val manualBackground: BackgroundWorld = BackgroundWorld.CLEAR_SKY,
    val rainAlert: Boolean = true,
    val lightningAlert: Boolean = true,
    val aqiAlert: Boolean = true,
    val dailyForecast: Boolean = true,
    val sunriseAlert: Boolean = true,
    val severeWeatherAlert: Boolean = true,
    val animationQuality: AnimationQuality = AnimationQuality.HIGH,
    val batterySaver: Boolean = false,
    val unitsLabel: String = "Celsius (°C)",
    val languageLabel: String = "English",
    val dataSourceLabel: String = "Global Forecast (High Accuracy)",
    val refreshRateLabel: String = "Every 30 minutes",
    val themeLabel: String = "Dark (Glassmorphism)",
    val accessibilityEnabled: Boolean = false
)

class WeatherPreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("real_weather_365_preferences", Context.MODE_PRIVATE)

    fun load(): WeatherPreferences = WeatherPreferences(
        automaticBackground = prefs.getBoolean("automatic_background", true),
        manualBackground = enumValueOrDefault(
            prefs.getString("manual_background", null),
            BackgroundWorld.CLEAR_SKY
        ),
        rainAlert = prefs.getBoolean("rain_alert", true),
        lightningAlert = prefs.getBoolean("lightning_alert", true),
        aqiAlert = prefs.getBoolean("aqi_alert", true),
        dailyForecast = prefs.getBoolean("daily_forecast", true),
        sunriseAlert = prefs.getBoolean("sunrise_alert", true),
        severeWeatherAlert = prefs.getBoolean("severe_weather_alert", true),
        animationQuality = enumValueOrDefault(
            prefs.getString("animation_quality", null),
            AnimationQuality.HIGH
        ),
        batterySaver = prefs.getBoolean("battery_saver", false),
        accessibilityEnabled = prefs.getBoolean("accessibility", false)
    )

    fun save(value: WeatherPreferences) {
        prefs.edit()
            .putBoolean("automatic_background", value.automaticBackground)
            .putString("manual_background", value.manualBackground.name)
            .putBoolean("rain_alert", value.rainAlert)
            .putBoolean("lightning_alert", value.lightningAlert)
            .putBoolean("aqi_alert", value.aqiAlert)
            .putBoolean("daily_forecast", value.dailyForecast)
            .putBoolean("sunrise_alert", value.sunriseAlert)
            .putBoolean("severe_weather_alert", value.severeWeatherAlert)
            .putString("animation_quality", value.animationQuality.name)
            .putBoolean("battery_saver", value.batterySaver)
            .putBoolean("accessibility", value.accessibilityEnabled)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T {
        return runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)
    }
}
