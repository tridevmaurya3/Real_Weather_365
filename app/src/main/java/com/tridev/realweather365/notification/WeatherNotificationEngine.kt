package com.tridev.realweather365.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tridev.realweather365.MainActivity
import com.tridev.realweather365.R
import com.tridev.realweather365.data.preferences.AppLanguage
import com.tridev.realweather365.data.preferences.WeatherPreferences
import com.tridev.realweather365.data.preferences.WeatherPreferencesStore
import com.tridev.realweather365.data.preferences.convertSpeed
import com.tridev.realweather365.data.preferences.convertTemperature
import com.tridev.realweather365.data.preferences.translateWeatherCondition
import com.tridev.realweather365.data.weather.LiveHourData
import com.tridev.realweather365.data.weather.LiveWeatherSnapshot
import com.tridev.realweather365.data.weather.OpenMeteoWeatherRepository
import com.tridev.realweather365.data.weather.WmoWeather
import com.tridev.realweather365.widget.WeatherWidgetSnapshotStore
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val UNIQUE_SMART_WEATHER_WORK = "real_weather_365_smart_notifications"
private const val ALERT_HISTORY_PREFS = "real_weather_365_notification_history"
private const val CHANNEL_WEATHER = "weather_updates"
private const val CHANNEL_SEVERE = "severe_weather"
private const val CHANNEL_DAILY = "daily_weather"

object WeatherNotificationScheduler {
    fun apply(context: Context, preferences: WeatherPreferences) {
        NotificationChannels.ensureCreated(context)
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!preferences.hasAnySmartAlertEnabled()) {
            workManager.cancelUniqueWork(UNIQUE_SMART_WEATHER_WORK)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
            preferences.effectiveRefreshMinutes().coerceAtLeast(15L),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_SMART_WEATHER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

private fun WeatherPreferences.hasAnySmartAlertEnabled(): Boolean =
    rainAlert || lightningAlert || aqiAlert || dailyForecast || sunriseAlert || severeWeatherAlert

object NotificationChannels {
    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val weather = NotificationChannel(
            CHANNEL_WEATHER,
            "Weather alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Rain, lightning and air quality alerts from Real Weather 365"
        }
        val severe = NotificationChannel(
            CHANNEL_SEVERE,
            "Severe weather alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important alerts for potentially severe weather conditions"
            enableVibration(true)
        }
        val daily = NotificationChannel(
            CHANNEL_DAILY,
            "Daily weather",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Daily forecast and sunrise reminders"
        }
        manager.createNotificationChannels(listOf(weather, severe, daily))
    }
}

class WeatherNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!canPostNotifications(applicationContext)) return Result.success()

        val preferences = WeatherPreferencesStore(applicationContext).load()
        if (!preferences.hasAnySmartAlertEnabled()) return Result.success()

        val location = WeatherWidgetSnapshotStore(applicationContext).loadLocation()
        val live = runCatching {
            OpenMeteoWeatherRepository().load(location)
        }.getOrElse {
            return Result.retry()
        }

        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val now = ZonedDateTime.now(zone)
        val history = NotificationHistory(applicationContext)
        val notifier = SmartWeatherNotifier(applicationContext, history, preferences)

        if (preferences.severeWeatherAlert) notifier.maybeNotifySevere(location.name, live, now)
        if (preferences.lightningAlert) notifier.maybeNotifyLightning(location.name, live, now)
        if (preferences.rainAlert) notifier.maybeNotifyRain(location.name, live, now)
        if (preferences.aqiAlert) notifier.maybeNotifyAqi(location.name, live, now)
        if (preferences.dailyForecast) notifier.maybeNotifyDailyForecast(location.name, live, now)
        if (preferences.sunriseAlert) notifier.maybeNotifySunrise(location.name, live, now)

        return Result.success()
    }
}

private class SmartWeatherNotifier(
    private val context: Context,
    private val history: NotificationHistory,
    private val preferences: WeatherPreferences
) {
    private val hindi = preferences.appLanguage == AppLanguage.HINDI

    fun maybeNotifyRain(location: String, live: LiveWeatherSnapshot, now: ZonedDateTime) {
        val rainyHour = live.hourly24.take(4).firstOrNull { it.isRainLike() && it.rainChance >= 35 } ?: return
        val token = "$location-${rainyHour.isoTime}"
        val whenText = if (rainyHour.label == "Now") {
            if (hindi) "अभी" else "now"
        } else {
            if (hindi) "${rainyHour.label} के आसपास" else "around ${rainyHour.label}"
        }
        notifyOnce(
            historyKey = "rain",
            token = token,
            notificationId = 2101,
            channel = CHANNEL_WEATHER,
            title = if (hindi) "$location में बारिश की संभावना" else "Rain likely in $location",
            text = if (hindi) "बारिश की ${rainyHour.rainChance}% संभावना $whenText है। बाहर जा रहे हों तो छाता रखें।" else "${rainyHour.rainChance}% chance $whenText. Take an umbrella if you are heading out.",
            highPriority = false
        )
    }

    fun maybeNotifyLightning(location: String, live: LiveWeatherSnapshot, now: ZonedDateTime) {
        val stormHour = live.hourly24.take(4).firstOrNull { it.weatherCode in setOf(95, 96, 99) } ?: return
        val token = "$location-${stormHour.isoTime}"
        val whenText = if (stormHour.label == "Now") {
            if (hindi) "अभी" else "now"
        } else {
            if (hindi) "${stormHour.label} के आसपास" else "around ${stormHour.label}"
        }
        notifyOnce(
            historyKey = "lightning",
            token = token,
            notificationId = 2102,
            channel = CHANNEL_SEVERE,
            title = if (hindi) "$location के पास बिजली गिरने का जोखिम" else "Lightning risk near $location",
            text = if (hindi) "गरज-चमक की स्थिति $whenText पूर्वानुमानित है। बिजली विकसित होने पर सुरक्षित अंदरूनी स्थान पर जाएँ।" else "Thunderstorm conditions are forecast $whenText. Move to a safe indoor place if lightning develops.",
            highPriority = true
        )
    }

    fun maybeNotifyAqi(location: String, live: LiveWeatherSnapshot, now: ZonedDateTime) {
        val aqi = live.aqi ?: return
        if (aqi <= 100) return
        val label = WmoWeather.aqiLabel(aqi)
        val token = "$location-${now.toLocalDate()}-$label"
        notifyOnce(
            historyKey = "aqi",
            token = token,
            notificationId = 2103,
            channel = CHANNEL_WEATHER,
            title = if (hindi) "वायु गुणवत्ता अलर्ट • $location" else "Air quality alert • $location",
            text = if (hindi) "AQI $aqi ($label) है। वायु प्रदूषण के प्रति संवेदनशील हों तो लंबे समय की बाहरी गतिविधि कम करें।" else "AQI is $aqi ($label). Consider reducing prolonged outdoor activity if you are sensitive to air pollution.",
            highPriority = false
        )
    }

    fun maybeNotifyDailyForecast(location: String, live: LiveWeatherSnapshot, now: ZonedDateTime) {
        if (now.hour !in 6..9) return
        val day = live.daily10.firstOrNull() ?: return
        val token = "$location-${now.toLocalDate()}"
        val high = convertTemperature(day.high, preferences.unitSystem)
        val low = convertTemperature(day.low, preferences.unitSystem)
        val condition = translateWeatherCondition(day.condition, preferences.appLanguage)
        notifyOnce(
            historyKey = "daily",
            token = token,
            notificationId = 2104,
            channel = CHANNEL_DAILY,
            title = if (hindi) "$location आज • $condition" else "$location today • $condition",
            text = if (hindi) "अधिकतम $high° • न्यूनतम $low° • बारिश ${day.rainChance}%।" else "High $high° • Low $low° • Rain chance ${day.rainChance}%.",
            highPriority = false
        )
    }

    fun maybeNotifySunrise(location: String, live: LiveWeatherSnapshot, now: ZonedDateTime) {
        val sunrise = runCatching { LocalDateTime.parse(live.sunrise) }.getOrNull() ?: return
        if (sunrise.toLocalDate() != now.toLocalDate()) return
        val localNow = now.toLocalDateTime()
        val minutes = Duration.between(localNow, sunrise).toMinutes()
        if (minutes !in 0..45) return
        val token = "$location-${sunrise.toLocalDate()}"
        val text = when {
            minutes <= 5 -> if (hindi) "सूर्योदय लगभग ${sunrise.toLocalTime()} बजे शुरू हो रहा है।" else "Sunrise is starting around ${sunrise.toLocalTime()}."
            else -> if (hindi) "सूर्योदय लगभग $minutes मिनट में, ${sunrise.toLocalTime()} बजे है।" else "Sunrise is in about $minutes minutes, around ${sunrise.toLocalTime()}."
        }
        notifyOnce(
            historyKey = "sunrise",
            token = token,
            notificationId = 2105,
            channel = CHANNEL_DAILY,
            title = if (hindi) "सूर्योदय जल्द • $location" else "Sunrise soon • $location",
            text = text,
            highPriority = false
        )
    }

    fun maybeNotifySevere(location: String, live: LiveWeatherSnapshot, now: ZonedDateTime) {
        val severeCode = live.weatherCode in setOf(82, 86, 95, 96, 99) ||
            live.hourly24.take(4).any { it.weatherCode in setOf(82, 86, 95, 96, 99) }
        val strongGusts = live.windGusts >= 60
        if (!severeCode && !strongGusts) return

        val bucket = now.hour / 6
        val token = "$location-${now.toLocalDate()}-$bucket-${live.weatherCode}-${strongGusts}"
        val gust = convertSpeed(live.windGusts, preferences.unitSystem)
        val details = when {
            live.weatherCode in setOf(95, 96, 99) -> if (hindi) "गरज-चमक की स्थिति सक्रिय या निकट है।" else "Thunderstorm conditions are active or imminent."
            live.weatherCode == 82 -> if (hindi) "भारी बारिश यात्रा और दृश्यता को प्रभावित कर सकती है।" else "Heavy rain showers may affect travel and visibility."
            live.weatherCode == 86 -> if (hindi) "भारी बर्फबारी सड़क और दृश्यता को प्रभावित कर सकती है।" else "Heavy snow showers may affect roads and visibility."
            strongGusts -> if (hindi) "हवा के झोंके लगभग $gust ${preferences.unitSystem.windUnit} हैं।" else "Wind gusts are around $gust ${preferences.unitSystem.windUnit}."
            else -> if (hindi) "अगले कुछ घंटों में गंभीर मौसम की संभावना है।" else "Potentially severe weather is forecast in the next few hours."
        }
        notifyOnce(
            historyKey = "severe",
            token = token,
            notificationId = 2106,
            channel = CHANNEL_SEVERE,
            title = if (hindi) "गंभीर मौसम निगरानी • $location" else "Severe weather watch • $location",
            text = details,
            highPriority = true
        )
    }

    private fun notifyOnce(
        historyKey: String,
        token: String,
        notificationId: Int,
        channel: String,
        title: String,
        text: String,
        highPriority: Boolean
    ) {
        if (history.alreadySent(historyKey, token)) return
        if (!canPostNotifications(context)) return

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (highPriority) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .build()

        val sent = runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        }.getOrDefault(false)
        if (sent) history.markSent(historyKey, token)
    }
}

private class NotificationHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(ALERT_HISTORY_PREFS, Context.MODE_PRIVATE)

    fun alreadySent(key: String, token: String): Boolean = prefs.getString(key, null) == token

    fun markSent(key: String, token: String) {
        prefs.edit().putString(key, token).apply()
    }
}

private fun LiveHourData.isRainLike(): Boolean = weatherCode in setOf(
    51, 53, 55, 56, 57,
    61, 63, 65, 66, 67,
    80, 81, 82,
    95, 96, 99
)

fun canPostNotifications(context: Context): Boolean {
    val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return runtimePermissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}
