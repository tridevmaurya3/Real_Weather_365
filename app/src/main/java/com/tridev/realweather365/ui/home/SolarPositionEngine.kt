package com.tridev.realweather365.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.tridev.realweather365.data.location.WorldLocation
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.coroutines.delay

enum class SolarPhase {
    DEEP_NIGHT,
    ASTRONOMICAL_TWILIGHT,
    NAUTICAL_TWILIGHT,
    CIVIL_TWILIGHT,
    SUNRISE_SUNSET,
    GOLDEN_HOUR,
    DAYLIGHT
}

data class SolarVisualState(
    val elevationDegrees: Double,
    val azimuthDegrees: Double,
    val phase: SolarPhase,
    val isRising: Boolean,
    val sunVisible: Boolean,
    val visualX: Float,
    val visualY: Float,
    val warmth: Float,
    val daylight: Float,
    val starVisibility: Float,
    val twilightStrength: Float,
    val timeZoneId: String
)

object SolarPositionEngine {
    fun calculate(location: WorldLocation, now: ZonedDateTime = nowFor(location)): SolarVisualState {
        val day = now.dayOfYear.toDouble()
        val daysInYear = if (now.toLocalDate().isLeapYear) 366.0 else 365.0
        val localMinutes = now.hour * 60.0 + now.minute + now.second / 60.0
        val fractionalHour = localMinutes / 60.0
        val gamma = 2.0 * PI / daysInYear * (day - 1.0 + (fractionalHour - 12.0) / 24.0)

        val equationOfTime = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) -
                0.040849 * sin(2.0 * gamma)
            )

        val declination =
            0.006918 -
                0.399912 * cos(gamma) +
                0.070257 * sin(gamma) -
                0.006758 * cos(2.0 * gamma) +
                0.000907 * sin(2.0 * gamma) -
                0.002697 * cos(3.0 * gamma) +
                0.00148 * sin(3.0 * gamma)

        val offsetMinutes = now.offset.totalSeconds / 60.0
        val timeOffset = equationOfTime + 4.0 * location.longitude - offsetMinutes
        var trueSolarMinutes = (localMinutes + timeOffset) % 1440.0
        if (trueSolarMinutes < 0.0) trueSolarMinutes += 1440.0

        var hourAngle = trueSolarMinutes / 4.0 - 180.0
        if (hourAngle < -180.0) hourAngle += 360.0

        val latitudeRadians = Math.toRadians(location.latitude.coerceIn(-89.8, 89.8))
        val hourAngleRadians = Math.toRadians(hourAngle)
        val cosZenith = (
            sin(latitudeRadians) * sin(declination) +
                cos(latitudeRadians) * cos(declination) * cos(hourAngleRadians)
            ).coerceIn(-1.0, 1.0)
        val zenith = Math.toDegrees(acos(cosZenith))
        val elevation = 90.0 - zenith

        val azimuthRadians = atan2(
            sin(hourAngleRadians),
            cos(hourAngleRadians) * sin(latitudeRadians) - tan(declination) * cos(latitudeRadians)
        )
        val azimuth = (Math.toDegrees(azimuthRadians) + 180.0 + 360.0) % 360.0

        val sunriseZenithRadians = Math.toRadians(90.833)
        val cosSunriseHourAngle = (
            cos(sunriseZenithRadians) / (cos(latitudeRadians) * cos(declination)) -
                tan(latitudeRadians) * tan(declination)
            )
        val halfDayAngle = when {
            cosSunriseHourAngle <= -1.0 -> 180.0
            cosSunriseHourAngle >= 1.0 -> 0.0
            else -> Math.toDegrees(acos(cosSunriseHourAngle.coerceIn(-1.0, 1.0)))
        }
        val daylightProgress = if (halfDayAngle > 0.0) {
            ((hourAngle + halfDayAngle) / (2.0 * halfDayAngle)).coerceIn(0.0, 1.0)
        } else {
            0.5
        }

        val isRising = hourAngle < 0.0
        val phase = when {
            elevation < -18.0 -> SolarPhase.DEEP_NIGHT
            elevation < -12.0 -> SolarPhase.ASTRONOMICAL_TWILIGHT
            elevation < -6.0 -> SolarPhase.NAUTICAL_TWILIGHT
            elevation < -0.833 -> SolarPhase.CIVIL_TWILIGHT
            elevation < 3.0 -> SolarPhase.SUNRISE_SUNSET
            elevation < 8.0 -> SolarPhase.GOLDEN_HOUR
            else -> SolarPhase.DAYLIGHT
        }

        val warmth = when {
            elevation < -14.0 -> 0.0
            elevation <= 3.0 -> ((elevation + 14.0) / 17.0).coerceIn(0.0, 1.0)
            elevation < 18.0 -> (1.0 - (elevation - 3.0) / 15.0).coerceIn(0.0, 1.0)
            else -> 0.0
        }.toFloat()
        val daylight = ((elevation + 6.0) / 36.0).coerceIn(0.0, 1.0).toFloat()
        val starVisibility = ((-elevation - 4.0) / 12.0).coerceIn(0.0, 1.0).toFloat()
        val twilightStrength = if (elevation in -18.0..14.0) {
            (1.0 - abs(elevation - 1.0) / 19.0).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }

        return SolarVisualState(
            elevationDegrees = elevation,
            azimuthDegrees = azimuth,
            phase = phase,
            isRising = isRising,
            sunVisible = elevation > -1.5,
            visualX = (0.08 + daylightProgress * 0.84).toFloat(),
            visualY = (0.44 - elevation.coerceIn(0.0, 78.0) / 78.0 * 0.34).toFloat(),
            warmth = warmth,
            daylight = daylight,
            starVisibility = starVisibility,
            twilightStrength = twilightStrength,
            timeZoneId = now.zone.id
        )
    }

    fun nowFor(location: WorldLocation): ZonedDateTime {
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrElse { ZoneId.of("UTC") }
        return ZonedDateTime.now(zone)
    }
}

@Composable
fun rememberSolarVisualState(location: WorldLocation): SolarVisualState {
    val solar by produceState(
        initialValue = SolarPositionEngine.calculate(location),
        key1 = location.id,
        key2 = location.latitude,
        key3 = location.longitude
    ) {
        while (true) {
            value = SolarPositionEngine.calculate(location)
            delay(60_000L)
        }
    }
    return solar
}

fun solarAdjustedScene(baseScene: WeatherScene, solar: SolarVisualState): WeatherScene {
    return when (baseScene) {
        WeatherScene.RAIN, WeatherScene.THUNDERSTORM, WeatherScene.SNOW -> baseScene
        else -> when {
            solar.elevationDegrees < -12.0 -> WeatherScene.NIGHT
            solar.elevationDegrees < 12.0 -> WeatherScene.SUNRISE
            else -> WeatherScene.SUNNY
        }
    }
}
