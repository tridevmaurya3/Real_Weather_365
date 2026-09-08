package com.tridev.realweather365.data.location

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OpenMeteoGeocodingRepository {
    suspend fun search(query: String, count: Int = 20): List<WorldLocation> = withContext(Dispatchers.IO) {
        val cleaned = query.trim()
        if (cleaned.length < 2) return@withContext emptyList()

        val encoded = URLEncoder.encode(cleaned, Charsets.UTF_8.name())
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=$encoded&count=${count.coerceIn(1, 50)}&language=en&format=json"

        val root = getJson(url)
        val results = root.optJSONArray("results") ?: return@withContext emptyList()

        buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val latitude = item.optDouble("latitude", Double.NaN)
                val longitude = item.optDouble("longitude", Double.NaN)
                if (!latitude.isFinite() || !longitude.isFinite()) continue

                val name = item.optString("name").trim()
                if (name.isBlank()) continue

                val apiId = item.optLong("id", 0L)
                val stableId = if (apiId != 0L) {
                    "openmeteo-$apiId"
                } else {
                    "geo-${"%.4f".format(java.util.Locale.US, latitude)}-${"%.4f".format(java.util.Locale.US, longitude)}"
                }

                add(
                    WorldLocation(
                        id = stableId,
                        name = name,
                        region = item.optString("admin1").trim(),
                        country = item.optString("country").trim(),
                        countryCode = item.optString("country_code").trim().uppercase(),
                        latitude = latitude,
                        longitude = longitude,
                        timeZoneId = item.optString("timezone", "GMT").ifBlank { "GMT" },
                        source = LocationSource.SEARCH
                    )
                )
            }
        }.distinctBy { it.id }
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
                throw IOException("Location service returned HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
