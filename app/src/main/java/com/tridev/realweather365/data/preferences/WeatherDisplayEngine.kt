package com.tridev.realweather365.data.preferences

import com.tridev.realweather365.data.weather.LiveDayData
import com.tridev.realweather365.data.weather.LiveHourData
import com.tridev.realweather365.ui.home.HourForecast
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import com.tridev.realweather365.ui.home.WeatherMetric
import kotlin.math.roundToInt

fun convertTemperature(valueCelsius: Int, unitSystem: UnitSystem): Int = when (unitSystem) {
    UnitSystem.METRIC -> valueCelsius
    UnitSystem.IMPERIAL -> (valueCelsius * 9.0 / 5.0 + 32.0).roundToInt()
}

fun convertSpeed(valueKmh: Int, unitSystem: UnitSystem): Int = when (unitSystem) {
    UnitSystem.METRIC -> valueKmh
    UnitSystem.IMPERIAL -> (valueKmh * 0.621371).roundToInt()
}

fun convertDistance(valueKm: Double?, unitSystem: UnitSystem): Double? = when (unitSystem) {
    UnitSystem.METRIC -> valueKm
    UnitSystem.IMPERIAL -> valueKm?.times(0.621371)
}

fun translateWeatherCondition(condition: String, language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return condition
    val value = condition.lowercase()
    return when {
        "thunder" in value && "hail" in value -> "ओलों के साथ गरज-चमक"
        "thunder" in value || "storm" in value -> "गरज-चमक"
        "heavy snow" in value -> "भारी बर्फबारी"
        "snow" in value -> "बर्फबारी"
        "freezing rain" in value -> "जमी हुई बारिश"
        "heavy rain" in value -> "भारी बारिश"
        "rain shower" in value -> "बारिश की बौछारें"
        "rain" in value -> "बारिश"
        "drizzle" in value -> "बूंदाबांदी"
        "fog" in value -> "कोहरा"
        "overcast" in value -> "घने बादल"
        "partly cloudy" in value -> "आंशिक बादल"
        "cloud" in value -> "बादल"
        "clear night" in value || "mostly clear night" in value -> "साफ रात"
        "mainly clear" in value || "mostly clear" in value -> "अधिकतर साफ"
        "clear sky" in value || "sunny" in value -> "साफ आसमान"
        else -> condition
    }
}

private fun translateAqiLabel(label: String, language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return label
    return when (label.lowercase()) {
        "good" -> "अच्छा"
        "moderate" -> "मध्यम"
        "sensitive" -> "संवेदनशील समूह"
        "unhealthy" -> "अस्वस्थ"
        "very unhealthy" -> "बहुत अस्वस्थ"
        "hazardous" -> "खतरनाक"
        "unavailable" -> "उपलब्ध नहीं"
        else -> label
    }
}

private fun translateDayLabel(label: String, language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return label
    return when (label.lowercase()) {
        "today" -> "आज"
        "mon" -> "सोम"
        "tue" -> "मंगल"
        "wed" -> "बुध"
        "thu" -> "गुरु"
        "fri" -> "शुक्र"
        "sat" -> "शनि"
        "sun" -> "रवि"
        else -> label
    }
}

private fun translateHourLabel(label: String, language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return label
    return if (label.equals("Now", true)) "अब" else label
}

private fun translateUpdatedAt(value: String, language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return value
    return value
        .replace("Connecting to live weather…", "लाइव मौसम से कनेक्ट हो रहा है…")
        .replace("Updating live weather…", "लाइव मौसम अपडेट हो रहा है…")
        .replace("Weather unavailable", "मौसम उपलब्ध नहीं")
        .replace("Live •", "लाइव •")
}

fun WeatherHomeUiState.applyDisplayPreferences(preferences: WeatherPreferences): WeatherHomeUiState {
    val unit = preferences.unitSystem
    val language = preferences.appLanguage
    val displayedHourly24: List<LiveHourData> = hourly24.map { hour ->
        hour.copy(
            label = translateHourLabel(hour.label, language),
            temperature = convertTemperature(hour.temperature, unit),
            windSpeed = convertSpeed(hour.windSpeed, unit),
            windGusts = convertSpeed(hour.windGusts, unit)
        )
    }
    val displayedDaily10: List<LiveDayData> = daily10.map { day ->
        day.copy(
            dayLabel = translateDayLabel(day.dayLabel, language),
            condition = translateWeatherCondition(day.condition, language),
            low = convertTemperature(day.low, unit),
            high = convertTemperature(day.high, unit)
        )
    }
    val displayedHourly: List<HourForecast> = hourly.map { hour ->
        hour.copy(
            time = translateHourLabel(hour.time, language),
            temperature = convertTemperature(hour.temperature, unit),
            condition = translateWeatherCondition(hour.condition, language)
        )
    }
    val aqiMetric = metrics.firstOrNull { it.label.equals("AQI", true) }
    val aqiValue = aqiMetric?.value ?: (aqi?.toString() ?: "--")
    val aqiHint = translateAqiLabel(aqiMetric?.hint ?: "Unavailable", language)
    val windLabel = if (language == AppLanguage.HINDI) "हवा" else "Wind"
    val humidityLabel = if (language == AppLanguage.HINDI) "नमी" else "Humidity"
    val pressureLabel = if (language == AppLanguage.HINDI) "दबाव" else "Pressure"
    val humidityHint = when {
        humidity < 35 -> if (language == AppLanguage.HINDI) "शुष्क" else "Dry"
        humidity <= 70 -> if (language == AppLanguage.HINDI) "आरामदायक" else "Comfortable"
        else -> if (language == AppLanguage.HINDI) "नम" else "Humid"
    }
    val animationLevel = when (preferences.effectiveAnimationQuality()) {
        AnimationQuality.HIGH -> 2
        AnimationQuality.BALANCED -> 1
        AnimationQuality.OPTIMIZED -> 0
    }

    return copy(
        updatedAt = translateUpdatedAt(updatedAt, language),
        temperature = convertTemperature(temperature, unit),
        condition = translateWeatherCondition(condition, language),
        feelsLike = convertTemperature(feelsLike, unit),
        high = convertTemperature(high, unit),
        low = convertTemperature(low, unit),
        windSpeed = convertSpeed(windSpeed, unit),
        windGusts = convertSpeed(windGusts, unit),
        visibilityKm = convertDistance(visibilityKm, unit),
        dewPoint = convertTemperature(dewPoint, unit),
        hourly24 = displayedHourly24,
        daily10 = displayedDaily10,
        hourly = displayedHourly,
        metrics = listOf(
            WeatherMetric("AQI", aqiValue, aqiHint),
            WeatherMetric(windLabel, "${convertSpeed(this.windSpeed, unit)} ${unit.windUnit}", metrics.getOrNull(1)?.hint ?: "--"),
            WeatherMetric(humidityLabel, "$humidity%", humidityHint),
            WeatherMetric(pressureLabel, pressure.toString(), "hPa")
        ),
        temperatureUnit = unit.temperatureUnit,
        windUnit = unit.windUnit,
        distanceUnit = unit.distanceUnit,
        languageCode = language.code,
        animationLevel = animationLevel
    )
}
