package com.tridev.realweather365.data.alerts

import com.tridev.realweather365.data.location.WorldLocation
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class OfficialAlertFeedStatus {
    AVAILABLE,
    UNSUPPORTED,
    ERROR
}

data class OfficialWeatherAlert(
    val id: String,
    val event: String,
    val headline: String,
    val description: String,
    val instruction: String,
    val severity: String,
    val urgency: String,
    val certainty: String,
    val senderName: String,
    val areaDescription: String,
    val sent: String,
    val effective: String,
    val onset: String,
    val expires: String,
    val sourceUrl: String
)

data class OfficialAlertFeed(
    val status: OfficialAlertFeedStatus,
    val providerId: String?,
    val providerName: String?,
    val providerHomepage: String?,
    val alerts: List<OfficialWeatherAlert>,
    val fetchedAtEpochMillis: Long,
    val errorMessage: String? = null
)

interface OfficialAlertProvider {
    val id: String
    val name: String
    val homepage: String

    fun supports(location: WorldLocation): Boolean
    suspend fun load(location: WorldLocation): List<OfficialWeatherAlert>
}

class OfficialWeatherAlertRepository(
    private val providers: List<OfficialAlertProvider> = listOf(NwsOfficialAlertProvider())
) {
    suspend fun load(location: WorldLocation): OfficialAlertFeed {
        val provider = providers.firstOrNull { it.supports(location) }
            ?: return OfficialAlertFeed(
                status = OfficialAlertFeedStatus.UNSUPPORTED,
                providerId = null,
                providerName = null,
                providerHomepage = null,
                alerts = emptyList(),
                fetchedAtEpochMillis = System.currentTimeMillis(),
                errorMessage = null
            )

        return runCatching { provider.load(location) }
            .fold(
                onSuccess = { alerts ->
                    OfficialAlertFeed(
                        status = OfficialAlertFeedStatus.AVAILABLE,
                        providerId = provider.id,
                        providerName = provider.name,
                        providerHomepage = provider.homepage,
                        alerts = alerts,
                        fetchedAtEpochMillis = System.currentTimeMillis(),
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    OfficialAlertFeed(
                        status = OfficialAlertFeedStatus.ERROR,
                        providerId = provider.id,
                        providerName = provider.name,
                        providerHomepage = provider.homepage,
                        alerts = emptyList(),
                        fetchedAtEpochMillis = System.currentTimeMillis(),
                        errorMessage = error.message ?: "Official alert feed unavailable"
                    )
                }
            )
    }
}

private class NwsOfficialAlertProvider : OfficialAlertProvider {
    override val id: String = "noaa-nws"
    override val name: String = "NOAA / National Weather Service"
    override val homepage: String = "https://www.weather.gov/"

    private val supportedCountryCodes = setOf("US", "PR", "VI", "GU", "AS", "MP")

    override fun supports(location: WorldLocation): Boolean =
        location.countryCode.uppercase(Locale.US) in supportedCountryCodes

    override suspend fun load(location: WorldLocation): List<OfficialWeatherAlert> = withContext(Dispatchers.IO) {
        val latitude = String.format(Locale.US, "%.5f", location.latitude)
        val longitude = String.format(Locale.US, "%.5f", location.longitude)
        val endpoint = "https://api.weather.gov/alerts/active?point=$latitude,$longitude"

        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                val root = fetchJson(endpoint)
                return@withContext parseAlerts(root)
            } catch (error: Throwable) {
                lastError = error
                if (attempt < 2) delay(600L * (attempt + 1))
            }
        }
        throw lastError ?: IOException("Official NWS alert feed unavailable")
    }

    private fun fetchJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/geo+json, application/ld+json, application/json")
            setRequestProperty(
                "User-Agent",
                "RealWeather365/0.26 (https://github.com/tridevmaurya3/Real_Weather_365)"
            )
        }

        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("NWS alerts returned HTTP $code")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAlerts(root: JSONObject): List<OfficialWeatherAlert> {
        val features = root.optJSONArray("features") ?: JSONArray()
        return buildList {
            for (index in 0 until features.length()) {
                val feature = features.optJSONObject(index) ?: continue
                val properties = feature.optJSONObject("properties") ?: JSONObject()
                val event = properties.cleanString("event") ?: "Weather Alert"
                val sourceUrl = feature.cleanString("id")
                    ?: properties.cleanString("@id")
                    ?: properties.cleanString("id")
                    ?: homepage

                add(
                    OfficialWeatherAlert(
                        id = sourceUrl,
                        event = event,
                        headline = properties.cleanString("headline") ?: event,
                        description = properties.cleanString("description").orEmpty(),
                        instruction = properties.cleanString("instruction").orEmpty(),
                        severity = properties.cleanString("severity") ?: "Unknown",
                        urgency = properties.cleanString("urgency") ?: "Unknown",
                        certainty = properties.cleanString("certainty") ?: "Unknown",
                        senderName = properties.cleanString("senderName") ?: name,
                        areaDescription = properties.cleanString("areaDesc").orEmpty(),
                        sent = properties.cleanString("sent").orEmpty(),
                        effective = properties.cleanString("effective").orEmpty(),
                        onset = properties.cleanString("onset").orEmpty(),
                        expires = properties.cleanString("expires").orEmpty(),
                        sourceUrl = sourceUrl
                    )
                )
            }
        }.sortedWith(
            compareByDescending<OfficialWeatherAlert> { severityRank(it.severity) }
                .thenByDescending { it.sent }
        )
    }

    private fun severityRank(value: String): Int = when (value.lowercase(Locale.US)) {
        "extreme" -> 5
        "severe" -> 4
        "moderate" -> 3
        "minor" -> 2
        "unknown" -> 1
        else -> 0
    }
}

private fun JSONObject.cleanString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = optString(key).trim()
    return value.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
}
