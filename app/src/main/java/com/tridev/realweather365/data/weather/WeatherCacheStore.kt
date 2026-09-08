package com.tridev.realweather365.data.weather

import android.content.Context
import com.tridev.realweather365.data.location.LocationSource
import com.tridev.realweather365.data.location.WorldLocation
import org.json.JSONArray
import org.json.JSONObject

private const val CACHE_PREFS = "real_weather_365_weather_cache"
private const val LAST_LOCATION_KEY = "last_location_id"
private const val CACHE_SCHEMA = 1

data class CachedWeatherRecord(
    val location: WorldLocation,
    val snapshot: LiveWeatherSnapshot,
    val savedAtEpochMillis: Long
)

class WeatherCacheStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)

    fun save(location: WorldLocation, snapshot: LiveWeatherSnapshot, savedAtEpochMillis: Long = System.currentTimeMillis()) {
        val root = JSONObject()
            .put("schema", CACHE_SCHEMA)
            .put("saved_at", savedAtEpochMillis)
            .put("location", location.toJson())
            .put("snapshot", snapshot.toJson())

        prefs.edit()
            .putString(cacheKey(location.id), root.toString())
            .putString(LAST_LOCATION_KEY, location.id)
            .apply()
    }

    fun loadLast(): CachedWeatherRecord? {
        val locationId = prefs.getString(LAST_LOCATION_KEY, null) ?: return null
        return loadFor(locationId)
    }

    fun loadFor(locationId: String): CachedWeatherRecord? {
        val raw = prefs.getString(cacheKey(locationId), null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            if (root.optInt("schema", CACHE_SCHEMA) != CACHE_SCHEMA) return@runCatching null
            val location = root.getJSONObject("location").toWorldLocation()
            if (location.id != locationId) return@runCatching null
            CachedWeatherRecord(
                location = location,
                snapshot = root.getJSONObject("snapshot").toSnapshot(),
                savedAtEpochMillis = root.optLong("saved_at", 0L)
            )
        }.getOrNull()
    }

    private fun cacheKey(locationId: String): String = "record:$locationId"
}

private fun WorldLocation.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("region", region)
    .put("country", country)
    .put("country_code", countryCode)
    .put("latitude", latitude)
    .put("longitude", longitude)
    .put("timezone", timeZoneId)
    .put("source", source.name)

private fun JSONObject.toWorldLocation(): WorldLocation = WorldLocation(
    id = getString("id"),
    name = getString("name"),
    region = optString("region"),
    country = optString("country"),
    countryCode = optString("country_code"),
    latitude = getDouble("latitude"),
    longitude = getDouble("longitude"),
    timeZoneId = optString("timezone", "UTC"),
    source = runCatching { LocationSource.valueOf(optString("source", LocationSource.SAVED.name)) }
        .getOrDefault(LocationSource.SAVED)
)

private fun LiveWeatherSnapshot.toJson(): JSONObject = JSONObject().apply {
    put("temperature", temperature)
    put("feels_like", feelsLike)
    put("weather_code", weatherCode)
    put("is_day", isDay)
    put("humidity", humidity)
    put("pressure", pressure)
    put("wind_speed", windSpeed)
    put("wind_direction", windDirection)
    put("wind_gusts", windGusts)
    put("cloud_cover", cloudCover)
    put("dew_point", dewPoint)
    putNullable("visibility_km", visibilityKm)
    putNullable("aqi", aqi)
    putNullable("pm25", pm25)
    putNullable("pm10", pm10)
    putNullable("nitrogen_dioxide", nitrogenDioxide)
    putNullable("ozone", ozone)
    putNullable("uv_index", uvIndex)
    put("high", high)
    put("low", low)
    put("sunrise", sunrise)
    put("sunset", sunset)
    put("observed_at", observedAt)
    put("display_time", displayTime)
    put("hourly24", JSONArray().apply {
        hourly24.forEach { hour ->
            put(
                JSONObject()
                    .put("iso_time", hour.isoTime)
                    .put("label", hour.label)
                    .put("temperature", hour.temperature)
                    .put("weather_code", hour.weatherCode)
                    .put("rain_chance", hour.rainChance)
                    .put("is_day", hour.isDay)
                    .put("wind_speed", hour.windSpeed)
                    .put("wind_gusts", hour.windGusts)
                    .put("precipitation_mm", hour.precipitationMm)
                    .put("snowfall_cm", hour.snowfallCm)
                    .putNullable("cape", hour.cape)
            )
        }
    })
    put("daily10", JSONArray().apply {
        daily10.forEach { day ->
            put(
                JSONObject()
                    .put("iso_date", day.isoDate)
                    .put("day_label", day.dayLabel)
                    .put("date_label", day.dateLabel)
                    .put("condition", day.condition)
                    .put("low", day.low)
                    .put("high", day.high)
                    .put("rain_chance", day.rainChance)
                    .put("weather_code", day.weatherCode)
            )
        }
    })
}

private fun JSONObject.toSnapshot(): LiveWeatherSnapshot {
    val hourly = optJSONArray("hourly24") ?: JSONArray()
    val daily = optJSONArray("daily10") ?: JSONArray()
    return LiveWeatherSnapshot(
        temperature = optInt("temperature"),
        feelsLike = optInt("feels_like"),
        weatherCode = optInt("weather_code"),
        isDay = optBoolean("is_day", true),
        humidity = optInt("humidity"),
        pressure = optInt("pressure"),
        windSpeed = optInt("wind_speed"),
        windDirection = optInt("wind_direction"),
        windGusts = optInt("wind_gusts"),
        cloudCover = optInt("cloud_cover"),
        dewPoint = optInt("dew_point"),
        visibilityKm = nullableDouble("visibility_km"),
        aqi = nullableInt("aqi"),
        pm25 = nullableDouble("pm25"),
        pm10 = nullableDouble("pm10"),
        nitrogenDioxide = nullableDouble("nitrogen_dioxide"),
        ozone = nullableDouble("ozone"),
        uvIndex = nullableDouble("uv_index"),
        high = optInt("high"),
        low = optInt("low"),
        sunrise = optString("sunrise"),
        sunset = optString("sunset"),
        observedAt = optString("observed_at"),
        displayTime = optString("display_time"),
        hourly24 = buildList {
            for (index in 0 until hourly.length()) {
                val item = hourly.optJSONObject(index) ?: continue
                add(
                    LiveHourData(
                        isoTime = item.optString("iso_time"),
                        label = item.optString("label"),
                        temperature = item.optInt("temperature"),
                        weatherCode = item.optInt("weather_code"),
                        rainChance = item.optInt("rain_chance"),
                        isDay = item.optBoolean("is_day", true),
                        windSpeed = item.optInt("wind_speed"),
                        windGusts = item.optInt("wind_gusts"),
                        precipitationMm = item.optDouble("precipitation_mm", 0.0),
                        snowfallCm = item.optDouble("snowfall_cm", 0.0),
                        cape = item.nullableDouble("cape")
                    )
                )
            }
        },
        daily10 = buildList {
            for (index in 0 until daily.length()) {
                val item = daily.optJSONObject(index) ?: continue
                add(
                    LiveDayData(
                        isoDate = item.optString("iso_date"),
                        dayLabel = item.optString("day_label"),
                        dateLabel = item.optString("date_label"),
                        condition = item.optString("condition"),
                        low = item.optInt("low"),
                        high = item.optInt("high"),
                        rainChance = item.optInt("rain_chance"),
                        weatherCode = item.optInt("weather_code")
                    )
                )
            }
        }
    )
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    put(key, value ?: JSONObject.NULL)
}

private fun JSONObject.nullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key, Double.NaN).takeIf { it.isFinite() }
}

private fun JSONObject.nullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}
