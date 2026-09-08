package com.tridev.realweather365.data.weather

import com.tridev.realweather365.data.location.WorldLocation
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SpatialWeatherPoint(
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double?,
    val windSpeedKmh: Double?,
    val windDirectionDegrees: Double?,
    val cloudCoverPercent: Double?,
    val precipitationMm: Double?
)

data class SpatialWeatherField(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val rows: Int,
    val columns: Int,
    val latitudeStep: Double,
    val longitudeStep: Double,
    val points: List<SpatialWeatherPoint>,
    val fetchedAtEpochMillis: Long,
    val provider: String = "Open-Meteo"
)

class OpenMeteoMapLayerRepository {
    suspend fun load(
        location: WorldLocation,
        gridSize: Int = 7,
        halfSpanLatitudeDegrees: Double = 2.4
    ): SpatialWeatherField = load(
        centerLatitude = location.latitude,
        centerLongitude = location.longitude,
        gridSize = gridSize,
        halfSpanLatitudeDegrees = halfSpanLatitudeDegrees
    )

    suspend fun load(
        centerLatitude: Double,
        centerLongitude: Double,
        gridSize: Int = 7,
        halfSpanLatitudeDegrees: Double = 2.4
    ): SpatialWeatherField = withContext(Dispatchers.IO) {
        val size = gridSize.coerceIn(3, 9).let { if (it % 2 == 0) it + 1 else it }
        val centerLat = centerLatitude.coerceIn(-84.0, 84.0)
        val centerLon = normalizeLongitude(centerLongitude)
        val latitudeHalfSpan = min(4.5, max(0.35, halfSpanLatitudeDegrees))
        val longitudeScale = max(0.28, abs(cos(Math.toRadians(centerLat))))
        val longitudeHalfSpan = min(9.0, latitudeHalfSpan / longitudeScale)
        val latitudeStep = (latitudeHalfSpan * 2.0) / (size - 1)
        val longitudeStep = (longitudeHalfSpan * 2.0) / (size - 1)

        val requested = buildList {
            for (row in 0 until size) {
                val lat = (centerLat - latitudeHalfSpan + row * latitudeStep).coerceIn(-85.0, 85.0)
                for (column in 0 until size) {
                    val lon = normalizeLongitude(centerLon - longitudeHalfSpan + column * longitudeStep)
                    add(lat to lon)
                }
            }
        }

        val latitudeList = requested.joinToString(",") { formatCoordinate(it.first) }
        val longitudeList = requested.joinToString(",") { formatCoordinate(it.second) }
        val endpoint = buildString {
            append("https://api.open-meteo.com/v1/forecast")
            append("?latitude=$latitudeList")
            append("&longitude=$longitudeList")
            append("&current=temperature_2m,precipitation,cloud_cover,wind_speed_10m,wind_direction_10m")
            append("&timezone=GMT")
            append("&forecast_days=1")
        }

        val body = fetchWithRetry(endpoint)
        val responses = parseResponses(body)
        if (responses.length() == 0) throw IOException("Weather map field returned no locations")

        val points = buildList {
            for (index in requested.indices) {
                val response = responses.optJSONObject(index) ?: continue
                val current = response.optJSONObject("current") ?: continue
                val requestedPoint = requested[index]
                add(
                    SpatialWeatherPoint(
                        latitude = requestedPoint.first,
                        longitude = requestedPoint.second,
                        temperatureC = current.number("temperature_2m"),
                        windSpeedKmh = current.number("wind_speed_10m"),
                        windDirectionDegrees = current.number("wind_direction_10m"),
                        cloudCoverPercent = current.number("cloud_cover"),
                        precipitationMm = current.number("precipitation")
                    )
                )
            }
        }

        if (points.isEmpty()) throw IOException("Weather map field contained no usable samples")

        SpatialWeatherField(
            centerLatitude = centerLat,
            centerLongitude = centerLon,
            rows = size,
            columns = size,
            latitudeStep = latitudeStep,
            longitudeStep = longitudeStep,
            points = points,
            fetchedAtEpochMillis = System.currentTimeMillis()
        )
    }

    private fun fetchWithRetry(url: String): String {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                return fetch(url)
            } catch (error: Throwable) {
                lastError = error
                if (attempt < 2) Thread.sleep(450L * (attempt + 1))
            }
        }
        throw lastError ?: IOException("Weather map field unavailable")
    }

    private fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 15_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "RealWeather365/0.28")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Open-Meteo map field returned HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponses(body: String): JSONArray {
        val trimmed = body.trim()
        return when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> JSONArray().put(JSONObject(trimmed))
            else -> throw IOException("Unexpected weather map field response")
        }
    }

    private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.4f", value)

    private fun normalizeLongitude(value: Double): Double {
        var longitude = value
        while (longitude > 180.0) longitude -= 360.0
        while (longitude < -180.0) longitude += 360.0
        return longitude
    }
}

private fun JSONObject.number(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val value = optDouble(key, Double.NaN)
    return value.takeIf { it.isFinite() }
}
