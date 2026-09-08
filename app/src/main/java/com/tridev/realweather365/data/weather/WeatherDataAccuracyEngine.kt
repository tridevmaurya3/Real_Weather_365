package com.tridev.realweather365.data.weather

import com.tridev.realweather365.data.location.WorldLocation
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

enum class WeatherDataQuality {
    VERIFIED,
    CHECKED_WITH_WARNINGS,
    REJECTED
}

data class WeatherAccuracyResult(
    val snapshot: LiveWeatherSnapshot,
    val quality: WeatherDataQuality,
    val warnings: List<String>,
    val dataAgeMinutes: Long?,
    val timeZoneId: String
) {
    val isRejected: Boolean get() = quality == WeatherDataQuality.REJECTED
    val label: String
        get() = when (quality) {
            WeatherDataQuality.VERIFIED -> "Verified"
            WeatherDataQuality.CHECKED_WITH_WARNINGS -> "Checked"
            WeatherDataQuality.REJECTED -> "Rejected"
        }
}

class WeatherDataRejectedException(message: String) : IllegalStateException(message)

/**
 * Production guardrail between a provider response/cache record and the UI.
 * It never invents meteorological values. Clearly impossible or malformed live
 * responses are rejected; safe presentation bounds and ordering are normalized.
 */
class WeatherDataAccuracyEngine {
    fun validateLive(
        location: WorldLocation,
        snapshot: LiveWeatherSnapshot,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): WeatherAccuracyResult = evaluate(location, snapshot, nowEpochMillis, allowStale = false)

    fun validateCached(
        location: WorldLocation,
        snapshot: LiveWeatherSnapshot,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): WeatherAccuracyResult = evaluate(location, snapshot, nowEpochMillis, allowStale = true)

    private fun evaluate(
        location: WorldLocation,
        source: LiveWeatherSnapshot,
        nowEpochMillis: Long,
        allowStale: Boolean
    ): WeatherAccuracyResult {
        val warnings = mutableListOf<String>()
        val fatal = mutableListOf<String>()

        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrElse {
            fatal += "Invalid location time zone"
            ZoneId.of("UTC")
        }

        val observed = runCatching { LocalDateTime.parse(source.observedAt) }.getOrNull()
        val ageMinutes = observed?.let {
            val observedInstant = it.atZone(zone).toInstant()
            val nowInstant = java.time.Instant.ofEpochMilli(nowEpochMillis)
            Duration.between(observedInstant, nowInstant).toMinutes()
        }

        if (observed == null) {
            fatal += "Malformed observation time"
        } else if (ageMinutes != null) {
            when {
                ageMinutes < -90L -> fatal += "Observation time is implausibly in the future"
                !allowStale && ageMinutes > 360L -> fatal += "Live weather response is stale"
                !allowStale && ageMinutes > 90L -> warnings += "Live observation is older than expected"
                allowStale && ageMinutes > 180L -> warnings += "Using an older saved observation"
            }
        }

        if (source.temperature !in -100..70) fatal += "Temperature is outside physical sanity bounds"
        if (source.feelsLike !in -120..85) warnings += "Apparent temperature is outside normal presentation bounds"
        if (source.humidity !in 0..100) fatal += "Humidity is outside 0-100%"
        if (source.pressure !in 350..1150) fatal += "Surface pressure is outside sanity bounds"
        if (source.windSpeed !in 0..450) fatal += "Wind speed is outside sanity bounds"
        if (source.windGusts !in 0..550) fatal += "Wind gust is outside sanity bounds"
        if (source.cloudCover !in 0..100) fatal += "Cloud cover is outside 0-100%"
        if (source.visibilityKm != null && source.visibilityKm !in 0.0..500.0) warnings += "Visibility value was normalized"
        if (!isKnownWmoCode(source.weatherCode)) warnings += "Unknown WMO weather code"

        val cleanHours = source.hourly24
            .mapNotNull { sanitizeHour(it, warnings) }
            .distinctBy { it.isoTime }
            .sortedBy { it.isoTime }
            .take(24)

        val cleanDays = source.daily10
            .mapNotNull { sanitizeDay(it, warnings) }
            .distinctBy { it.isoDate }
            .sortedBy { it.isoDate }
            .take(10)

        if (cleanHours.size < 6) fatal += "Hourly forecast is incomplete"
        if (cleanDays.size < 3) fatal += "Daily forecast is incomplete"

        val currentDate = observed?.toLocalDate()
        if (currentDate != null && cleanDays.isNotEmpty()) {
            val firstDate = runCatching { LocalDate.parse(cleanDays.first().isoDate) }.getOrNull()
            if (firstDate != null && firstDate.isAfter(currentDate.plusDays(1))) {
                fatal += "Daily forecast does not align with observation date"
            }
        }

        if (source.sunrise.isBlank() || source.sunset.isBlank()) {
            warnings += "Sunrise or sunset is missing"
        } else {
            val sunriseOk = runCatching { LocalDateTime.parse(source.sunrise) }.isSuccess
            val sunsetOk = runCatching { LocalDateTime.parse(source.sunset) }.isSuccess
            if (!sunriseOk || !sunsetOk) warnings += "Sunrise or sunset time is malformed"
        }

        val safeLow = min(source.low, source.high)
        val safeHigh = max(source.low, source.high)
        if (safeLow != source.low || safeHigh != source.high) warnings += "Daily high/low order was normalized"

        val safeDirection = ((source.windDirection % 360) + 360) % 360
        if (safeDirection != source.windDirection) warnings += "Wind direction was normalized"

        val safeGust = max(source.windSpeed, source.windGusts)
        if (safeGust != source.windGusts) warnings += "Wind gust was normalized against sustained wind"

        val sanitized = source.copy(
            feelsLike = source.feelsLike.coerceIn(-120, 85),
            weatherCode = source.weatherCode.takeIf(::isKnownWmoCode) ?: 3,
            windDirection = safeDirection,
            windGusts = safeGust,
            visibilityKm = source.visibilityKm?.coerceIn(0.0, 500.0),
            aqi = source.aqi?.coerceIn(0, 500),
            pm25 = source.pm25?.takeIf { it >= 0.0 },
            pm10 = source.pm10?.takeIf { it >= 0.0 },
            nitrogenDioxide = source.nitrogenDioxide?.takeIf { it >= 0.0 },
            ozone = source.ozone?.takeIf { it >= 0.0 },
            uvIndex = source.uvIndex?.coerceIn(0.0, 40.0),
            high = safeHigh,
            low = safeLow,
            hourly24 = cleanHours,
            daily10 = cleanDays
        )

        val quality = when {
            fatal.isNotEmpty() -> WeatherDataQuality.REJECTED
            warnings.isNotEmpty() -> WeatherDataQuality.CHECKED_WITH_WARNINGS
            else -> WeatherDataQuality.VERIFIED
        }
        val allIssues = (fatal + warnings).distinct()

        return WeatherAccuracyResult(
            snapshot = sanitized,
            quality = quality,
            warnings = allIssues,
            dataAgeMinutes = ageMinutes,
            timeZoneId = zone.id
        )
    }

    private fun sanitizeHour(hour: LiveHourData, warnings: MutableList<String>): LiveHourData? {
        if (runCatching { LocalDateTime.parse(hour.isoTime) }.isFailure) {
            warnings += "Malformed hourly forecast time was removed"
            return null
        }
        if (hour.temperature !in -100..70) {
            warnings += "Implausible hourly temperature was removed"
            return null
        }
        val code = hour.weatherCode.takeIf(::isKnownWmoCode) ?: run {
            warnings += "Unknown hourly WMO code was normalized"
            3
        }
        return hour.copy(
            weatherCode = code,
            rainChance = hour.rainChance.coerceIn(0, 100),
            windSpeed = hour.windSpeed.coerceIn(0, 450),
            windGusts = max(hour.windSpeed.coerceIn(0, 450), hour.windGusts.coerceIn(0, 550)),
            precipitationMm = hour.precipitationMm.coerceAtLeast(0.0),
            snowfallCm = hour.snowfallCm.coerceAtLeast(0.0),
            cape = hour.cape?.takeIf { it >= 0.0 }
        )
    }

    private fun sanitizeDay(day: LiveDayData, warnings: MutableList<String>): LiveDayData? {
        if (runCatching { LocalDate.parse(day.isoDate) }.isFailure) {
            warnings += "Malformed daily forecast date was removed"
            return null
        }
        if (day.low !in -100..70 || day.high !in -100..70) {
            warnings += "Implausible daily temperature was removed"
            return null
        }
        val low = min(day.low, day.high)
        val high = max(day.low, day.high)
        val code = day.weatherCode.takeIf(::isKnownWmoCode) ?: 3
        return day.copy(
            condition = WmoWeather.condition(code, true),
            low = low,
            high = high,
            rainChance = day.rainChance.coerceIn(0, 100),
            weatherCode = code
        )
    }

    private fun isKnownWmoCode(code: Int): Boolean = code in KNOWN_WMO_CODES

    private companion object {
        val KNOWN_WMO_CODES = setOf(
            0, 1, 2, 3, 45, 48,
            51, 53, 55, 56, 57,
            61, 63, 65, 66, 67,
            71, 73, 75, 77,
            80, 81, 82, 85, 86,
            95, 96, 99
        )
    }
}
