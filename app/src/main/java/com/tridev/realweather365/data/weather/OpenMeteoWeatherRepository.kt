package com.tridev.realweather365.data.weather

import com.tridev.realweather365.data.location.WorldLocation
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LiveHourData(
    val isoTime: String,
    val label: String,
    val temperature: Int,
    val weatherCode: Int,
    val rainChance: Int,
    val isDay: Boolean
)

data class LiveDayData(
    val isoDate: String,
    val dayLabel: String,
    val dateLabel: String,
    val condition: String,
    val low: Int,
    val high: Int,
    val rainChance: Int,
    val weatherCode: Int
)

data class LiveWeatherSnapshot(
    val temperature: Int,
    val feelsLike: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Int,
    val windDirection: Int,
    val aqi: Int?,
    val high: Int,
    val low: Int,
    val sunrise: String,
    val sunset: String,
    val observedAt: String,
    val displayTime: String,
    val hourly24: List<LiveHourData>,
    val daily10: List<LiveDayData>
)

class OpenMeteoWeatherRepository {
    suspend fun load(location: WorldLocation): LiveWeatherSnapshot = withContext(Dispatchers.IO) {
        val weather = fetchWeather(location)
        val aqi = runCatching { fetchAqi(location) }.getOrNull()
        weather.copy(aqi = aqi)
    }

    private fun fetchWeather(location: WorldLocation): LiveWeatherSnapshot {
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast")
            append("?latitude=${location.latitude}")
            append("&longitude=${location.longitude}")
            append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,is_day")
            append("&hourly=temperature_2m,weather_code,precipitation_probability,is_day")
            append("&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,sunrise,sunset")
            append("&forecast_days=10")
            append("&timezone=auto")
        }

        val root = getJson(url)
        val current = root.getJSONObject("current")
        val hourly = root.getJSONObject("hourly")
        val daily = root.getJSONObject("daily")

        val observedAt = current.getString("time")
        val currentHourKey = observedAt.take(13)
        val hourlyTimes = hourly.getJSONArray("time")
        val hourlyTemps = hourly.getJSONArray("temperature_2m")
        val hourlyCodes = hourly.getJSONArray("weather_code")
        val hourlyRain = hourly.getJSONArray("precipitation_probability")
        val hourlyIsDay = hourly.getJSONArray("is_day")

        var startIndex = 0
        for (index in 0 until hourlyTimes.length()) {
            if (hourlyTimes.getString(index).startsWith(currentHourKey)) {
                startIndex = index
                break
            }
        }

        val hours = buildList {
            val endExclusive = minOf(hourlyTimes.length(), startIndex + 24)
            for (index in startIndex until endExclusive) {
                val isoTime = hourlyTimes.getString(index)
                add(
                    LiveHourData(
                        isoTime = isoTime,
                        label = if (index == startIndex) "Now" else formatHour(isoTime),
                        temperature = hourlyTemps.getDouble(index).roundToInt(),
                        weatherCode = hourlyCodes.getInt(index),
                        rainChance = hourlyRain.optInt(index, 0),
                        isDay = hourlyIsDay.optInt(index, 0) == 1
                    )
                )
            }
        }

        val dailyTimes = daily.getJSONArray("time")
        val dailyHigh = daily.getJSONArray("temperature_2m_max")
        val dailyLow = daily.getJSONArray("temperature_2m_min")
        val dailyCodes = daily.getJSONArray("weather_code")
        val dailyRain = daily.getJSONArray("precipitation_probability_max")
        val sunrise = daily.getJSONArray("sunrise")
        val sunset = daily.getJSONArray("sunset")

        val days = buildList {
            for (index in 0 until minOf(10, dailyTimes.length())) {
                val isoDate = dailyTimes.getString(index)
                val parsedDate = runCatching { LocalDate.parse(isoDate) }.getOrNull()
                add(
                    LiveDayData(
                        isoDate = isoDate,
                        dayLabel = if (index == 0) "Today" else parsedDate?.format(
                            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
                        ) ?: isoDate,
                        dateLabel = parsedDate?.format(
                            DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
                        ) ?: isoDate,
                        condition = WmoWeather.condition(dailyCodes.getInt(index), true),
                        low = dailyLow.getDouble(index).roundToInt(),
                        high = dailyHigh.getDouble(index).roundToInt(),
                        rainChance = dailyRain.optInt(index, 0),
                        weatherCode = dailyCodes.getInt(index)
                    )
                )
            }
        }

        return LiveWeatherSnapshot(
            temperature = current.getDouble("temperature_2m").roundToInt(),
            feelsLike = current.getDouble("apparent_temperature").roundToInt(),
            weatherCode = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
            humidity = current.getDouble("relative_humidity_2m").roundToInt(),
            pressure = current.getDouble("surface_pressure").roundToInt(),
            windSpeed = current.getDouble("wind_speed_10m").roundToInt(),
            windDirection = current.getDouble("wind_direction_10m").roundToInt(),
            aqi = null,
            high = dailyHigh.getDouble(0).roundToInt(),
            low = dailyLow.getDouble(0).roundToInt(),
            sunrise = sunrise.optString(0),
            sunset = sunset.optString(0),
            observedAt = observedAt,
            displayTime = formatObservedTime(observedAt),
            hourly24 = hours,
            daily10 = days
        )
    }

    private fun fetchAqi(location: WorldLocation): Int? {
        val url = buildString {
            append("https://air-quality-api.open-meteo.com/v1/air-quality")
            append("?latitude=${location.latitude}")
            append("&longitude=${location.longitude}")
            append("&current=us_aqi")
            append("&timezone=auto")
        }
        val current = getJson(url).optJSONObject("current") ?: return null
        val value = current.optDouble("us_aqi", Double.NaN)
        return if (value.isFinite()) value.roundToInt() else null
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "RealWeather365/0.1")
        }

        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("Weather service returned HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun formatHour(isoTime: String): String = runCatching {
        LocalDateTime.parse(isoTime).format(DateTimeFormatter.ofPattern("h a", Locale.ENGLISH))
    }.getOrElse { isoTime.takeLast(5) }

    private fun formatObservedTime(isoTime: String): String = runCatching {
        LocalDateTime.parse(isoTime).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    }.getOrElse { isoTime.takeLast(5) }
}

object WmoWeather {
    fun condition(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "Clear Sky" else "Clear Night"
        1 -> if (isDay) "Mainly Clear" else "Mostly Clear Night"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing Drizzle"
        61 -> "Light Rain"
        63 -> "Rain"
        65 -> "Heavy Rain"
        66, 67 -> "Freezing Rain"
        71 -> "Light Snow"
        73 -> "Snow"
        75, 77 -> "Heavy Snow"
        80, 81 -> "Rain Showers"
        82 -> "Heavy Rain Showers"
        85, 86 -> "Snow Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with Hail"
        else -> "Weather"
    }

    fun compassDirection(degrees: Int): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val normalized = ((degrees % 360) + 360) % 360
        return directions[((normalized + 22) / 45) % 8]
    }

    fun aqiLabel(aqi: Int?): String = when {
        aqi == null -> "Unavailable"
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Sensitive"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very Unhealthy"
        else -> "Hazardous"
    }
}
