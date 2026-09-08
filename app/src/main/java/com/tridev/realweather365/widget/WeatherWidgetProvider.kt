package com.tridev.realweather365.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.tridev.realweather365.MainActivity
import com.tridev.realweather365.R
import com.tridev.realweather365.data.location.LocationSource
import com.tridev.realweather365.data.location.WorldLocation
import com.tridev.realweather365.data.location.WorldLocationCatalog
import com.tridev.realweather365.data.weather.LiveWeatherSnapshot
import com.tridev.realweather365.data.weather.OpenMeteoWeatherRepository
import com.tridev.realweather365.data.weather.WmoWeather
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val WIDGET_PREFS = "real_weather_365_widget_snapshot"

enum class WeatherWidgetSize {
    SMALL,
    MEDIUM,
    LARGE
}

data class WidgetHour(
    val time: String,
    val temperature: Int
)

data class WeatherWidgetSnapshot(
    val location: WorldLocation,
    val temperature: Int,
    val condition: String,
    val high: Int,
    val low: Int,
    val humidity: Int,
    val windSpeed: Int,
    val aqi: Int?,
    val updatedAt: String,
    val hours: List<WidgetHour>
)

class WeatherWidgetSnapshotStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)

    fun saveFromApp(state: WeatherHomeUiState) {
        val hours = state.hourly.take(4).map { WidgetHour(it.time, it.temperature) }
        saveSnapshot(
            WeatherWidgetSnapshot(
                location = state.selectedLocation,
                temperature = state.temperature,
                condition = state.condition,
                high = state.high,
                low = state.low,
                humidity = state.humidity,
                windSpeed = state.windSpeed,
                aqi = state.aqi,
                updatedAt = state.updatedAt,
                hours = hours
            )
        )
    }

    fun saveFromLive(location: WorldLocation, live: LiveWeatherSnapshot) {
        saveSnapshot(
            WeatherWidgetSnapshot(
                location = location,
                temperature = live.temperature,
                condition = WmoWeather.condition(live.weatherCode, live.isDay),
                high = live.high,
                low = live.low,
                humidity = live.humidity,
                windSpeed = live.windSpeed,
                aqi = live.aqi,
                updatedAt = if (live.displayTime.isBlank()) "Updated now" else "Updated ${live.displayTime}",
                hours = live.hourly24.take(4).map { WidgetHour(it.label, it.temperature) }
            )
        )
    }

    fun load(): WeatherWidgetSnapshot {
        val fallback = WorldLocationCatalog.chandauli
        val location = WorldLocation(
            id = prefs.getString("location_id", fallback.id) ?: fallback.id,
            name = prefs.getString("location_name", fallback.name) ?: fallback.name,
            region = prefs.getString("location_region", fallback.region) ?: fallback.region,
            country = prefs.getString("location_country", fallback.country) ?: fallback.country,
            countryCode = prefs.getString("location_country_code", fallback.countryCode) ?: fallback.countryCode,
            latitude = java.lang.Double.longBitsToDouble(
                prefs.getLong("location_latitude", java.lang.Double.doubleToRawLongBits(fallback.latitude))
            ),
            longitude = java.lang.Double.longBitsToDouble(
                prefs.getLong("location_longitude", java.lang.Double.doubleToRawLongBits(fallback.longitude))
            ),
            timeZoneId = prefs.getString("location_timezone", fallback.timeZoneId) ?: fallback.timeZoneId,
            source = LocationSource.SAVED
        )
        val hours = (0 until 4).map { index ->
            WidgetHour(
                time = prefs.getString("hour_${index}_time", if (index == 0) "Now" else "--") ?: "--",
                temperature = prefs.getInt("hour_${index}_temperature", prefs.getInt("temperature", 18))
            )
        }
        val aqiValue = prefs.getInt("aqi", -1)
        return WeatherWidgetSnapshot(
            location = location,
            temperature = prefs.getInt("temperature", 18),
            condition = prefs.getString("condition", "Clear Night") ?: "Clear Night",
            high = prefs.getInt("high", 25),
            low = prefs.getInt("low", 14),
            humidity = prefs.getInt("humidity", 0),
            windSpeed = prefs.getInt("wind_speed", 0),
            aqi = aqiValue.takeIf { it >= 0 },
            updatedAt = prefs.getString("updated_at", "Open app to sync") ?: "Open app to sync",
            hours = hours
        )
    }

    fun loadLocation(): WorldLocation = load().location

    private fun saveSnapshot(snapshot: WeatherWidgetSnapshot) {
        prefs.edit().apply {
            putString("location_id", snapshot.location.id)
            putString("location_name", snapshot.location.name)
            putString("location_region", snapshot.location.region)
            putString("location_country", snapshot.location.country)
            putString("location_country_code", snapshot.location.countryCode)
            putLong("location_latitude", java.lang.Double.doubleToRawLongBits(snapshot.location.latitude))
            putLong("location_longitude", java.lang.Double.doubleToRawLongBits(snapshot.location.longitude))
            putString("location_timezone", snapshot.location.timeZoneId)
            putInt("temperature", snapshot.temperature)
            putString("condition", snapshot.condition)
            putInt("high", snapshot.high)
            putInt("low", snapshot.low)
            putInt("humidity", snapshot.humidity)
            putInt("wind_speed", snapshot.windSpeed)
            putInt("aqi", snapshot.aqi ?: -1)
            putString("updated_at", snapshot.updatedAt)
            repeat(4) { index ->
                val hour = snapshot.hours.getOrNull(index)
                putString("hour_${index}_time", hour?.time ?: "--")
                putInt("hour_${index}_temperature", hour?.temperature ?: snapshot.temperature)
            }
        }.apply()
    }
}

object WeatherWidgetUpdater {
    fun syncFromApp(context: Context, state: WeatherHomeUiState) {
        val appContext = context.applicationContext
        WeatherWidgetSnapshotStore(appContext).saveFromApp(state)
        updateAllFromCache(appContext)
    }

    fun updateAllFromCache(context: Context) {
        val appContext = context.applicationContext
        val snapshot = WeatherWidgetSnapshotStore(appContext).load()
        val manager = AppWidgetManager.getInstance(appContext)
        updateProvider(appContext, manager, SmallWeatherWidgetProvider::class.java, WeatherWidgetSize.SMALL, snapshot)
        updateProvider(appContext, manager, MediumWeatherWidgetProvider::class.java, WeatherWidgetSize.MEDIUM, snapshot)
        updateProvider(appContext, manager, LargeWeatherWidgetProvider::class.java, WeatherWidgetSize.LARGE, snapshot)
    }

    private fun updateProvider(
        context: Context,
        manager: AppWidgetManager,
        providerClass: Class<out AppWidgetProvider>,
        size: WeatherWidgetSize,
        snapshot: WeatherWidgetSnapshot
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
        ids.forEach { id ->
            manager.updateAppWidget(id, buildViews(context, size, snapshot))
        }
    }
}

fun requestWeatherWidgetPin(context: Context, size: WeatherWidgetSize): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    val provider = when (size) {
        WeatherWidgetSize.SMALL -> ComponentName(context, SmallWeatherWidgetProvider::class.java)
        WeatherWidgetSize.MEDIUM -> ComponentName(context, MediumWeatherWidgetProvider::class.java)
        WeatherWidgetSize.LARGE -> ComponentName(context, LargeWeatherWidgetProvider::class.java)
    }
    return manager.requestPinAppWidget(provider, null, null)
}

abstract class BaseWeatherWidgetProvider : AppWidgetProvider() {
    protected abstract val widgetSize: WeatherWidgetSize

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val store = WeatherWidgetSnapshotStore(appContext)
                val location = store.loadLocation()
                val live = runCatching {
                    OpenMeteoWeatherRepository().load(location)
                }.getOrNull()
                if (live != null) {
                    store.saveFromLive(location, live)
                }
                val snapshot = store.load()
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(appContext, widgetSize, snapshot))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class SmallWeatherWidgetProvider : BaseWeatherWidgetProvider() {
    override val widgetSize = WeatherWidgetSize.SMALL
}

class MediumWeatherWidgetProvider : BaseWeatherWidgetProvider() {
    override val widgetSize = WeatherWidgetSize.MEDIUM
}

class LargeWeatherWidgetProvider : BaseWeatherWidgetProvider() {
    override val widgetSize = WeatherWidgetSize.LARGE
}

private fun buildViews(
    context: Context,
    size: WeatherWidgetSize,
    snapshot: WeatherWidgetSnapshot
): RemoteViews {
    val layout = when (size) {
        WeatherWidgetSize.SMALL -> R.layout.widget_weather_small
        WeatherWidgetSize.MEDIUM -> R.layout.widget_weather_medium
        WeatherWidgetSize.LARGE -> R.layout.widget_weather_large
    }
    val views = RemoteViews(context.packageName, layout)
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        100 + size.ordinal,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

    views.setTextViewText(R.id.widget_city, snapshot.location.name)
    views.setTextViewText(R.id.widget_icon, weatherEmoji(snapshot.condition))
    views.setTextViewText(R.id.widget_temp, "${snapshot.temperature}°")
    views.setTextViewText(R.id.widget_condition, snapshot.condition)
    views.setTextViewText(R.id.widget_high_low, "H:${snapshot.high}°  L:${snapshot.low}°")
    views.setTextViewText(R.id.widget_updated, snapshot.updatedAt)

    if (size != WeatherWidgetSize.SMALL) {
        views.setTextViewText(R.id.widget_humidity, "${snapshot.humidity}%")
        views.setTextViewText(R.id.widget_wind, "${snapshot.windSpeed} km/h")
        views.setTextViewText(R.id.widget_aqi, snapshot.aqi?.toString() ?: "--")
    }

    if (size == WeatherWidgetSize.LARGE) {
        val timeIds = intArrayOf(
            R.id.widget_hour_1_time,
            R.id.widget_hour_2_time,
            R.id.widget_hour_3_time,
            R.id.widget_hour_4_time
        )
        val tempIds = intArrayOf(
            R.id.widget_hour_1_temp,
            R.id.widget_hour_2_temp,
            R.id.widget_hour_3_temp,
            R.id.widget_hour_4_temp
        )
        repeat(4) { index ->
            val hour = snapshot.hours.getOrNull(index) ?: WidgetHour("--", snapshot.temperature)
            views.setTextViewText(timeIds[index], hour.time)
            views.setTextViewText(tempIds[index], "${hour.temperature}°")
        }
    }
    return views
}

private fun weatherEmoji(condition: String): String = when {
    condition.contains("thunder", ignoreCase = true) || condition.contains("storm", ignoreCase = true) -> "⚡"
    condition.contains("snow", ignoreCase = true) -> "❄️"
    condition.contains("rain", ignoreCase = true) || condition.contains("drizzle", ignoreCase = true) -> "🌧️"
    condition.contains("night", ignoreCase = true) -> "🌙"
    condition.contains("cloud", ignoreCase = true) || condition.contains("overcast", ignoreCase = true) -> "☁️"
    condition.contains("fog", ignoreCase = true) -> "🌫️"
    else -> "☀️"
}
