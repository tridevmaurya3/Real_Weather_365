package com.tridev.realweather365.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.tridev.realweather365.data.location.WorldLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.coroutines.delay

enum class MoonPhase(val english: String, val hindi: String) {
    NEW("New Moon", "अमावस्या"),
    WAXING_CRESCENT("Waxing Crescent", "बढ़ता अर्धचंद्र"),
    FIRST_QUARTER("First Quarter", "प्रथम चतुर्थांश"),
    WAXING_GIBBOUS("Waxing Gibbous", "बढ़ता गिबस"),
    FULL("Full Moon", "पूर्णिमा"),
    WANING_GIBBOUS("Waning Gibbous", "घटता गिबस"),
    LAST_QUARTER("Last Quarter", "अंतिम चतुर्थांश"),
    WANING_CRESCENT("Waning Crescent", "घटता अर्धचंद्र")
}

data class MoonVisualState(
    val phase: MoonPhase,
    val phaseFraction: Double,
    val illuminationFraction: Double,
    val illuminationPercent: Int,
    val altitudeDegrees: Double,
    val azimuthDegrees: Double,
    val visualX: Float,
    val visualY: Float,
    val aboveHorizon: Boolean,
    val moonrise: ZonedDateTime?,
    val moonset: ZonedDateTime?,
    val alwaysAboveHorizon: Boolean,
    val rightAscensionDegrees: Double,
    val declinationDegrees: Double,
    val timeZoneId: String
)

private data class MoonCoordinates(
    val eclipticLongitudeDegrees: Double,
    val rightAscensionDegrees: Double,
    val declinationDegrees: Double
)

private data class HorizontalMoonPosition(
    val altitudeDegrees: Double,
    val azimuthDegrees: Double,
    val rightAscensionDegrees: Double,
    val declinationDegrees: Double,
    val eclipticLongitudeDegrees: Double
)

private data class RiseSetResult(
    val rise: ZonedDateTime?,
    val set: ZonedDateTime?,
    val alwaysAbove: Boolean
)

object MoonAstronomyEngine {
    private const val SYNODIC_MONTH_DAYS = 29.53058867
    private const val HORIZON_DEGREES = -0.30

    fun calculate(
        location: WorldLocation,
        now: ZonedDateTime = nowFor(location)
    ): MoonVisualState {
        val horizontal = horizontalPosition(location, now.toInstant())
        val jd = julianDay(now.toInstant())
        val days = jd - 2451543.5
        val sunLongitude = sunEclipticLongitude(days)
        val elongation = normalizeDegrees(horizontal.eclipticLongitudeDegrees - sunLongitude)
        val phaseFraction = elongation / 360.0
        val illumination = ((1.0 - cos(Math.toRadians(elongation))) / 2.0).coerceIn(0.0, 1.0)
        val phase = phaseFor(phaseFraction)
        val riseSet = findRiseSet(location, now.toLocalDate(), now.zone)

        val x = (0.5 - sin(Math.toRadians(horizontal.azimuthDegrees)) * 0.42).coerceIn(0.08, 0.92).toFloat()
        val y = (0.46 - horizontal.altitudeDegrees.coerceIn(0.0, 78.0) / 78.0 * 0.34).coerceIn(0.10, 0.46).toFloat()

        return MoonVisualState(
            phase = phase,
            phaseFraction = phaseFraction,
            illuminationFraction = illumination,
            illuminationPercent = (illumination * 100.0).roundToInt(),
            altitudeDegrees = horizontal.altitudeDegrees,
            azimuthDegrees = horizontal.azimuthDegrees,
            visualX = x,
            visualY = y,
            aboveHorizon = horizontal.altitudeDegrees > HORIZON_DEGREES,
            moonrise = riseSet.rise,
            moonset = riseSet.set,
            alwaysAboveHorizon = riseSet.alwaysAbove,
            rightAscensionDegrees = horizontal.rightAscensionDegrees,
            declinationDegrees = horizontal.declinationDegrees,
            timeZoneId = now.zone.id
        )
    }

    fun nowFor(location: WorldLocation): ZonedDateTime {
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrElse { ZoneId.of("UTC") }
        return ZonedDateTime.now(zone)
    }

    private fun phaseFor(fraction: Double): MoonPhase = when {
        fraction < 0.0625 || fraction >= 0.9375 -> MoonPhase.NEW
        fraction < 0.1875 -> MoonPhase.WAXING_CRESCENT
        fraction < 0.3125 -> MoonPhase.FIRST_QUARTER
        fraction < 0.4375 -> MoonPhase.WAXING_GIBBOUS
        fraction < 0.5625 -> MoonPhase.FULL
        fraction < 0.6875 -> MoonPhase.WANING_GIBBOUS
        fraction < 0.8125 -> MoonPhase.LAST_QUARTER
        else -> MoonPhase.WANING_CRESCENT
    }

    private fun findRiseSet(location: WorldLocation, date: LocalDate, zone: ZoneId): RiseSetResult {
        val start = date.atStartOfDay(zone)
        val stepSeconds = 10L * 60L
        var previousTime = start
        var previousAltitude = horizontalPosition(location, previousTime.toInstant()).altitudeDegrees
        var rise: ZonedDateTime? = null
        var set: ZonedDateTime? = null
        var anyAbove = previousAltitude > HORIZON_DEGREES
        var anyBelow = previousAltitude <= HORIZON_DEGREES

        var seconds = stepSeconds
        while (seconds <= 24L * 60L * 60L) {
            val time = start.plusSeconds(seconds)
            val altitude = horizontalPosition(location, time.toInstant()).altitudeDegrees
            anyAbove = anyAbove || altitude > HORIZON_DEGREES
            anyBelow = anyBelow || altitude <= HORIZON_DEGREES

            if (previousAltitude <= HORIZON_DEGREES && altitude > HORIZON_DEGREES && rise == null) {
                rise = interpolateCrossing(previousTime, previousAltitude, time, altitude)
            }
            if (previousAltitude > HORIZON_DEGREES && altitude <= HORIZON_DEGREES && set == null) {
                set = interpolateCrossing(previousTime, previousAltitude, time, altitude)
            }

            previousTime = time
            previousAltitude = altitude
            seconds += stepSeconds
        }

        return RiseSetResult(
            rise = rise,
            set = set,
            alwaysAbove = anyAbove && !anyBelow
        )
    }

    private fun interpolateCrossing(
        firstTime: ZonedDateTime,
        firstAltitude: Double,
        secondTime: ZonedDateTime,
        secondAltitude: Double
    ): ZonedDateTime {
        val delta = secondAltitude - firstAltitude
        val fraction = if (kotlin.math.abs(delta) < 0.0001) 0.5 else {
            ((HORIZON_DEGREES - firstAltitude) / delta).coerceIn(0.0, 1.0)
        }
        val seconds = java.time.Duration.between(firstTime, secondTime).seconds
        return firstTime.plusSeconds((seconds * fraction).roundToLong())
    }

    private fun horizontalPosition(location: WorldLocation, instant: Instant): HorizontalMoonPosition {
        val jd = julianDay(instant)
        val days = jd - 2451543.5
        val coordinates = moonCoordinates(days)

        val t = (jd - 2451545.0) / 36525.0
        val gmst = normalizeDegrees(
            280.46061837 +
                360.98564736629 * (jd - 2451545.0) +
                0.000387933 * t * t -
                t * t * t / 38710000.0
        )
        val localSidereal = normalizeDegrees(gmst + location.longitude)
        val hourAngle = normalizeSignedDegrees(localSidereal - coordinates.rightAscensionDegrees)

        val lat = Math.toRadians(location.latitude.coerceIn(-89.8, 89.8))
        val dec = Math.toRadians(coordinates.declinationDegrees)
        val ha = Math.toRadians(hourAngle)

        val altitude = Math.toDegrees(
            asin(
                (
                    sin(dec) * sin(lat) +
                        cos(dec) * cos(lat) * cos(ha)
                    ).coerceIn(-1.0, 1.0)
            )
        )
        val azimuth = normalizeDegrees(
            Math.toDegrees(
                atan2(
                    sin(ha),
                    cos(ha) * sin(lat) - tan(dec) * cos(lat)
                )
            ) + 180.0
        )

        return HorizontalMoonPosition(
            altitudeDegrees = altitude,
            azimuthDegrees = azimuth,
            rightAscensionDegrees = coordinates.rightAscensionDegrees,
            declinationDegrees = coordinates.declinationDegrees,
            eclipticLongitudeDegrees = coordinates.eclipticLongitudeDegrees
        )
    }

    private fun moonCoordinates(days: Double): MoonCoordinates {
        val node = normalizeDegrees(125.1228 - 0.0529538083 * days)
        val inclination = 5.1454
        val perihelion = normalizeDegrees(318.0634 + 0.1643573223 * days)
        val eccentricity = 0.054900
        val meanAnomaly = normalizeDegrees(115.3654 + 13.0649929509 * days)
        val semiMajorAxis = 60.2666

        val meanAnomalyRad = Math.toRadians(meanAnomaly)
        val eccentricAnomaly = Math.toRadians(
            meanAnomaly +
                eccentricity * (180.0 / PI) * sin(meanAnomalyRad) *
                (1.0 + eccentricity * cos(meanAnomalyRad))
        )

        val xv = semiMajorAxis * (cos(eccentricAnomaly) - eccentricity)
        val yv = semiMajorAxis * sqrt(1.0 - eccentricity * eccentricity) * sin(eccentricAnomaly)
        val trueAnomaly = Math.toDegrees(atan2(yv, xv))
        val radius = sqrt(xv * xv + yv * yv)

        val meanLongitude = normalizeDegrees(node + perihelion + meanAnomaly)
        val sunMeanAnomaly = normalizeDegrees(356.0470 + 0.9856002585 * days)
        val sunPerihelion = normalizeDegrees(282.9404 + 4.70935E-5 * days)
        val sunMeanLongitude = normalizeDegrees(sunMeanAnomaly + sunPerihelion)
        val elongation = normalizeDegrees(meanLongitude - sunMeanLongitude)
        val argumentLatitude = normalizeDegrees(meanLongitude - node)

        var longitude = normalizeDegrees(trueAnomaly + perihelion)
        var latitude = 0.0

        longitude +=
            -1.274 * sinDegrees(meanAnomaly - 2.0 * elongation) +
                0.658 * sinDegrees(2.0 * elongation) -
                0.186 * sinDegrees(sunMeanAnomaly) -
                0.059 * sinDegrees(2.0 * meanAnomaly - 2.0 * elongation) -
                0.057 * sinDegrees(meanAnomaly - 2.0 * elongation + sunMeanAnomaly) +
                0.053 * sinDegrees(meanAnomaly + 2.0 * elongation) +
                0.046 * sinDegrees(2.0 * elongation - sunMeanAnomaly) +
                0.041 * sinDegrees(meanAnomaly - sunMeanAnomaly) -
                0.035 * sinDegrees(elongation) -
                0.031 * sinDegrees(meanAnomaly + sunMeanAnomaly) -
                0.015 * sinDegrees(2.0 * argumentLatitude - 2.0 * elongation) +
                0.011 * sinDegrees(meanAnomaly - 4.0 * elongation)

        latitude +=
            -0.173 * sinDegrees(argumentLatitude - 2.0 * elongation) -
                0.055 * sinDegrees(meanAnomaly - argumentLatitude - 2.0 * elongation) -
                0.046 * sinDegrees(meanAnomaly + argumentLatitude - 2.0 * elongation) +
                0.033 * sinDegrees(argumentLatitude + 2.0 * elongation) +
                0.017 * sinDegrees(2.0 * meanAnomaly + argumentLatitude)

        val lonRad = Math.toRadians(normalizeDegrees(longitude))
        val latRad = Math.toRadians(latitude)
        val xEcliptic = radius * cos(lonRad) * cos(latRad)
        val yEcliptic = radius * sin(lonRad) * cos(latRad)
        val zEcliptic = radius * sin(latRad)

        val obliquity = Math.toRadians(23.4393 - 3.563E-7 * days)
        val xEquatorial = xEcliptic
        val yEquatorial = yEcliptic * cos(obliquity) - zEcliptic * sin(obliquity)
        val zEquatorial = yEcliptic * sin(obliquity) + zEcliptic * cos(obliquity)

        val rightAscension = normalizeDegrees(Math.toDegrees(atan2(yEquatorial, xEquatorial)))
        val declination = Math.toDegrees(atan2(zEquatorial, sqrt(xEquatorial * xEquatorial + yEquatorial * yEquatorial)))

        return MoonCoordinates(
            eclipticLongitudeDegrees = normalizeDegrees(longitude),
            rightAscensionDegrees = rightAscension,
            declinationDegrees = declination
        )
    }

    private fun sunEclipticLongitude(days: Double): Double {
        val perihelion = normalizeDegrees(282.9404 + 4.70935E-5 * days)
        val eccentricity = 0.016709 - 1.151E-9 * days
        val meanAnomaly = normalizeDegrees(356.0470 + 0.9856002585 * days)
        val meanRad = Math.toRadians(meanAnomaly)
        val eccentricAnomaly = Math.toRadians(
            meanAnomaly +
                eccentricity * (180.0 / PI) * sin(meanRad) * (1.0 + eccentricity * cos(meanRad))
        )
        val xv = cos(eccentricAnomaly) - eccentricity
        val yv = sqrt(1.0 - eccentricity * eccentricity) * sin(eccentricAnomaly)
        val trueAnomaly = Math.toDegrees(atan2(yv, xv))
        return normalizeDegrees(trueAnomaly + perihelion)
    }

    private fun julianDay(instant: Instant): Double =
        instant.epochSecond / 86400.0 + instant.nano / 86400.0 / 1_000_000_000.0 + 2440587.5

    private fun sinDegrees(value: Double): Double = sin(Math.toRadians(value))

    private fun normalizeDegrees(value: Double): Double {
        var result = value % 360.0
        if (result < 0.0) result += 360.0
        return result
    }

    private fun normalizeSignedDegrees(value: Double): Double {
        val normalized = normalizeDegrees(value)
        return if (normalized > 180.0) normalized - 360.0 else normalized
    }
}

@Composable
fun rememberMoonVisualState(location: WorldLocation): MoonVisualState {
    val moon by produceState(
        initialValue = MoonAstronomyEngine.calculate(location),
        key1 = location.id,
        key2 = location.latitude,
        key3 = location.longitude
    ) {
        while (true) {
            value = MoonAstronomyEngine.calculate(location)
            delay(60_000L)
        }
    }
    return moon
}
