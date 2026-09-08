package com.tridev.realweather365.data.location

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SavedCitiesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): List<WorldLocation> {
        val raw = prefs.getString(KEY_CITIES, null) ?: return listOf(WorldLocationCatalog.chandauli)
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val parsed = item.toWorldLocation() ?: continue
                    add(parsed.copy(source = LocationSource.SAVED))
                }
            }.distinctBy { it.id }
        }.getOrElse { listOf(WorldLocationCatalog.chandauli) }
    }

    fun isSaved(locationId: String): Boolean = load().any { it.id == locationId }

    fun toggle(location: WorldLocation): List<WorldLocation> {
        val current = load().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == location.id }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(0, location.copy(source = LocationSource.SAVED))
        }
        return save(current.take(MAX_SAVED_CITIES))
    }

    fun remove(locationId: String): List<WorldLocation> {
        return save(load().filterNot { it.id == locationId })
    }

    private fun save(cities: List<WorldLocation>): List<WorldLocation> {
        val normalized = cities.distinctBy { it.id }.take(MAX_SAVED_CITIES)
        val array = JSONArray()
        normalized.forEach { location -> array.put(location.toJson()) }
        prefs.edit().putString(KEY_CITIES, array.toString()).apply()
        return normalized
    }

    private fun WorldLocation.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("region", region)
        put("country", country)
        put("countryCode", countryCode)
        put("latitude", latitude)
        put("longitude", longitude)
        put("timeZoneId", timeZoneId)
    }

    private fun JSONObject.toWorldLocation(): WorldLocation? {
        val id = optString("id").trim()
        val name = optString("name").trim()
        val latitude = optDouble("latitude", Double.NaN)
        val longitude = optDouble("longitude", Double.NaN)
        if (id.isBlank() || name.isBlank() || !latitude.isFinite() || !longitude.isFinite()) {
            return null
        }

        return WorldLocation(
            id = id,
            name = name,
            region = optString("region").trim(),
            country = optString("country").trim(),
            countryCode = optString("countryCode").trim(),
            latitude = latitude,
            longitude = longitude,
            timeZoneId = optString("timeZoneId", "GMT").ifBlank { "GMT" },
            source = LocationSource.SAVED
        )
    }

    private companion object {
        const val PREFS_NAME = "real_weather_365_locations"
        const val KEY_CITIES = "saved_cities_v1"
        const val MAX_SAVED_CITIES = 30
    }
}
