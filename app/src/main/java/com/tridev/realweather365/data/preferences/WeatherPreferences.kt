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

enum class UnitSystem(
    val label: String,
    val temperatureUnit: String,
    val windUnit: String,
    val distanceUnit: String
) {
    METRIC("Metric", "°C", "km/h", "km"),
    IMPERIAL("Imperial", "°F", "mph", "mi")
}

enum class AppLanguage(val label: String, val code: String) {
    ENGLISH("English", "en"),
    HINDI("हिन्दी", "hi")
}

enum class RefreshRate(val label: String, val minutes: Long) {
    MIN_15("Every 15 minutes", 15),
    MIN_30("Every 30 minutes", 30),
    MIN_60("Every 60 minutes", 60)
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
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val refreshRate: RefreshRate = RefreshRate.MIN_30,
    val dataSourceLabel: String = "Global Forecast (High Accuracy)",
    val themeLabel: String = "Dark (Glassmorphism)",
    val accessibilityEnabled: Boolean = false
) {
    val unitsLabel: String
        get() = "${unitSystem.temperatureUnit} • ${unitSystem.windUnit}"

    val languageLabel: String
        get() = appLanguage.label

    val refreshRateLabel: String
        get() = refreshRate.label

    fun effectiveRefreshMinutes(): Long = if (batterySaver) {
        maxOf(60L, refreshRate.minutes)
    } else {
        refreshRate.minutes
    }

    fun effectiveAnimationQuality(): AnimationQuality = if (batterySaver) {
        AnimationQuality.OPTIMIZED
    } else {
        animationQuality
    }
}

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
        unitSystem = enumValueOrDefault(
            prefs.getString("unit_system", null),
            UnitSystem.METRIC
        ),
        appLanguage = enumValueOrDefault(
            prefs.getString("app_language", null),
            AppLanguage.ENGLISH
        ),
        refreshRate = enumValueOrDefault(
            prefs.getString("refresh_rate", null),
            RefreshRate.MIN_30
        ),
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
            .putString("unit_system", value.unitSystem.name)
            .putString("app_language", value.appLanguage.name)
            .putString("refresh_rate", value.refreshRate.name)
            .putBoolean("accessibility", value.accessibilityEnabled)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T {
        return runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)
    }
}
