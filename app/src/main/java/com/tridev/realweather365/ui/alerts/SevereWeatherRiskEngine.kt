package com.tridev.realweather365.ui.alerts

import com.tridev.realweather365.data.weather.LiveHourData
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

enum class SevereRiskLevel {
    NONE,
    ADVISORY,
    MODERATE,
    HIGH,
    SEVERE
}

enum class SevereHazardType {
    NONE,
    THUNDERSTORM,
    HEAVY_RAIN,
    DAMAGING_WIND,
    SNOW_ICE,
    MIXED
}

data class RiskGuidance(
    val title: String,
    val detail: String
)

data class SevereWeatherAssessment(
    val available: Boolean,
    val level: SevereRiskLevel,
    val hazard: SevereHazardType,
    val title: String,
    val summary: String,
    val statusLabel: String,
    val severityLabel: String,
    val windowLabel: String,
    val lightningLabel: String,
    val peakGust: Int,
    val peakRainChance: Int,
    val peakPrecipitationMm: Double,
    val peakSnowfallCm: Double,
    val peakCape: Double?,
    val riskRadiusKm: Int,
    val peakHourLabel: String,
    val guidance: List<RiskGuidance>,
    val sourceNote: String,
    val offline: Boolean
)

object SevereWeatherRiskEngine {
    fun assess(state: WeatherHomeUiState): SevereWeatherAssessment {
        val hindi = state.languageCode.equals("hi", ignoreCase = true)
        val hours = state.hourly24.take(24)

        if (!state.hasWeatherData || hours.isEmpty()) {
            return SevereWeatherAssessment(
                available = false,
                level = SevereRiskLevel.NONE,
                hazard = SevereHazardType.NONE,
                title = if (hindi) "जोखिम डेटा उपलब्ध नहीं" else "Risk data unavailable",
                summary = if (hindi) {
                    "गंभीर मौसम का जोखिम निकालने के लिए लाइव या सेव किया हुआ hourly forecast चाहिए।"
                } else {
                    "A live or saved hourly forecast is required to calculate severe-weather risk."
                },
                statusLabel = if (hindi) "प्रतीक्षा" else "WAITING",
                severityLabel = if (hindi) "उपलब्ध नहीं" else "Unavailable",
                windowLabel = if (hindi) "Forecast मिलने पर समय दिखेगा" else "Timing appears when forecast data is available",
                lightningLabel = if (hindi) "उपलब्ध नहीं" else "Unavailable",
                peakGust = state.windGusts,
                peakRainChance = 0,
                peakPrecipitationMm = 0.0,
                peakSnowfallCm = 0.0,
                peakCape = null,
                riskRadiusKm = 0,
                peakHourLabel = "--",
                guidance = defaultGuidance(hindi),
                sourceNote = sourceNote(state, hindi),
                offline = state.isOffline
            )
        }

        val scored = hours.map { hour -> scoreHour(hour, state.windUnit) }
        val peakIndex = scored.indices.maxByOrNull { scored[it].score } ?: 0
        val peak = scored[peakIndex]
        val level = levelFor(peak.score)
        val hazard = hazardFor(peak)
        val window = riskWindow(scored, peakIndex, level, hindi)
        val peakGust = max(state.windGusts, hours.maxOfOrNull { it.windGusts } ?: 0)
        val peakRainChance = hours.maxOfOrNull { it.rainChance } ?: 0
        val peakPrecip = hours.maxOfOrNull { it.precipitationMm } ?: 0.0
        val peakSnow = hours.maxOfOrNull { it.snowfallCm } ?: 0.0
        val peakCape = hours.mapNotNull { it.cape }.maxOrNull()

        return SevereWeatherAssessment(
            available = true,
            level = level,
            hazard = hazard,
            title = titleFor(hazard, level, hindi),
            summary = summaryFor(hazard, level, peak, hindi),
            statusLabel = statusFor(level, hindi),
            severityLabel = severityFor(level, hindi),
            windowLabel = window,
            lightningLabel = lightningLabel(hours, peakCape, hindi),
            peakGust = peakGust,
            peakRainChance = peakRainChance,
            peakPrecipitationMm = peakPrecip,
            peakSnowfallCm = peakSnow,
            peakCape = peakCape,
            riskRadiusKm = radiusFor(level),
            peakHourLabel = formatPeakHour(peak.hour.isoTime),
            guidance = guidanceFor(hazard, level, hindi),
            sourceNote = sourceNote(state, hindi),
            offline = state.isOffline
        )
    }

    private data class HourRisk(
        val hour: LiveHourData,
        val score: Int,
        val thunderScore: Int,
        val windScore: Int,
        val rainScore: Int,
        val snowScore: Int,
        val capeScore: Int
    )

    private fun scoreHour(hour: LiveHourData, windUnit: String): HourRisk {
        val gustKmh = toKmh(hour.windGusts, windUnit)
        val thunderScore = when (hour.weatherCode) {
            96, 99 -> 6
            95 -> 5
            else -> 0
        }
        val windScore = when {
            gustKmh >= 105.0 -> 6
            gustKmh >= 85.0 -> 5
            gustKmh >= 65.0 -> 4
            gustKmh >= 50.0 -> 2
            else -> 0
        }
        val rainScore = when {
            hour.precipitationMm >= 10.0 -> 6
            hour.precipitationMm >= 5.0 -> 4
            hour.precipitationMm >= 2.5 -> 3
            hour.rainChance >= 85 -> 2
            hour.rainChance >= 65 -> 1
            else -> 0
        }
        val snowScore = when {
            hour.snowfallCm >= 3.0 -> 6
            hour.snowfallCm >= 1.5 -> 4
            hour.snowfallCm >= 0.5 -> 2
            hour.weatherCode in listOf(75, 77, 86) -> 4
            hour.weatherCode in 71..73 || hour.weatherCode == 85 -> 2
            else -> 0
        }
        val capeScore = when {
            (hour.cape ?: 0.0) >= 2500.0 -> 4
            (hour.cape ?: 0.0) >= 1500.0 -> 3
            (hour.cape ?: 0.0) >= 800.0 -> 2
            (hour.cape ?: 0.0) >= 400.0 -> 1
            else -> 0
        }
        val components = listOf(thunderScore, windScore, rainScore, snowScore, capeScore)
        val base = components.maxOrNull() ?: 0
        val synergy = (components.count { it >= 2 } - 1).coerceIn(0, 2)
        val score = (base + synergy).coerceAtMost(8)
        return HourRisk(hour, score, thunderScore, windScore, rainScore, snowScore, capeScore)
    }

    private fun levelFor(score: Int): SevereRiskLevel = when {
        score >= 7 -> SevereRiskLevel.SEVERE
        score >= 5 -> SevereRiskLevel.HIGH
        score >= 3 -> SevereRiskLevel.MODERATE
        score >= 2 -> SevereRiskLevel.ADVISORY
        else -> SevereRiskLevel.NONE
    }

    private fun hazardFor(risk: HourRisk): SevereHazardType {
        val strong = listOf(
            SevereHazardType.THUNDERSTORM to risk.thunderScore,
            SevereHazardType.DAMAGING_WIND to risk.windScore,
            SevereHazardType.HEAVY_RAIN to risk.rainScore,
            SevereHazardType.SNOW_ICE to risk.snowScore
        ).filter { it.second >= 3 }
        if (strong.size >= 2) return SevereHazardType.MIXED
        return listOf(
            SevereHazardType.THUNDERSTORM to risk.thunderScore,
            SevereHazardType.DAMAGING_WIND to risk.windScore,
            SevereHazardType.HEAVY_RAIN to risk.rainScore,
            SevereHazardType.SNOW_ICE to risk.snowScore
        ).maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first ?: SevereHazardType.NONE
    }

    private fun riskWindow(
        risks: List<HourRisk>,
        peakIndex: Int,
        level: SevereRiskLevel,
        hindi: Boolean
    ): String {
        if (level == SevereRiskLevel.NONE) {
            return if (hindi) "अगले 24 घंटे में कोई गंभीर window नहीं" else "No severe window in the next 24 hours"
        }
        val peakScore = risks[peakIndex].score
        val threshold = max(2, peakScore - 2)
        var start = peakIndex
        var end = peakIndex
        while (start > 0 && risks[start - 1].score >= threshold) start--
        while (end < risks.lastIndex && risks[end + 1].score >= threshold) end++

        val startTime = parseTime(risks[start].hour.isoTime)
        val endTime = parseTime(risks[end].hour.isoTime)?.plusHours(1)
        if (startTime == null || endTime == null) {
            return risks[peakIndex].hour.label
        }
        val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
        val timeFormatter = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH)
        return if (startTime.toLocalDate() == endTime.toLocalDate()) {
            "${startTime.format(dateFormatter)}, ${startTime.format(timeFormatter)} – ${endTime.format(timeFormatter)}"
        } else {
            "${startTime.format(dateFormatter)} ${startTime.format(timeFormatter)} – ${endTime.format(dateFormatter)} ${endTime.format(timeFormatter)}"
        }
    }

    private fun parseTime(value: String): LocalDateTime? = runCatching { LocalDateTime.parse(value) }.getOrNull()

    private fun formatPeakHour(value: String): String = parseTime(value)?.format(
        DateTimeFormatter.ofPattern("EEE h a", Locale.ENGLISH)
    ) ?: value.takeLast(5)

    private fun lightningLabel(hours: List<LiveHourData>, peakCape: Double?, hindi: Boolean): String {
        val thunder = hours.any { it.weatherCode in 95..99 }
        val cape = peakCape ?: 0.0
        return when {
            thunder || cape >= 1800.0 -> if (hindi) "उच्च" else "High"
            cape >= 800.0 -> if (hindi) "मध्यम" else "Moderate"
            cape >= 400.0 -> if (hindi) "कम" else "Low"
            else -> if (hindi) "बहुत कम" else "Very low"
        }
    }

    private fun titleFor(hazard: SevereHazardType, level: SevereRiskLevel, hindi: Boolean): String {
        if (level == SevereRiskLevel.NONE) {
            return if (hindi) "कोई गंभीर मौसम जोखिम नहीं" else "No severe weather risk detected"
        }
        return if (hindi) {
            when (hazard) {
                SevereHazardType.THUNDERSTORM -> "गरज-चमक का जोखिम"
                SevereHazardType.HEAVY_RAIN -> "भारी बारिश का जोखिम"
                SevereHazardType.DAMAGING_WIND -> "तेज हवा का जोखिम"
                SevereHazardType.SNOW_ICE -> "बर्फ/हिमपात का जोखिम"
                SevereHazardType.MIXED -> "गंभीर मौसम का मिश्रित जोखिम"
                SevereHazardType.NONE -> "मौसम जोखिम"
            }
        } else {
            when (hazard) {
                SevereHazardType.THUNDERSTORM -> "Thunderstorm Risk"
                SevereHazardType.HEAVY_RAIN -> "Heavy Rain Risk"
                SevereHazardType.DAMAGING_WIND -> "Damaging Wind Risk"
                SevereHazardType.SNOW_ICE -> "Snow / Ice Risk"
                SevereHazardType.MIXED -> "Multi-Hazard Severe Weather Risk"
                SevereHazardType.NONE -> "Weather Risk"
            }
        }
    }

    private fun summaryFor(
        hazard: SevereHazardType,
        level: SevereRiskLevel,
        peak: HourRisk,
        hindi: Boolean
    ): String {
        if (level == SevereRiskLevel.NONE) {
            return if (hindi) {
                "अगले 24 घंटे के उपलब्ध forecast inputs में severe threshold पार नहीं हुआ।"
            } else {
                "Available forecast inputs do not cross the severe-weather threshold in the next 24 hours."
            }
        }
        val detail = when (hazard) {
            SevereHazardType.THUNDERSTORM -> if (hindi) "गरज-चमक/बिजली के संकेत" else "thunderstorm and lightning signals"
            SevereHazardType.HEAVY_RAIN -> if (hindi) "भारी वर्षा के संकेत" else "heavy-rain signals"
            SevereHazardType.DAMAGING_WIND -> if (hindi) "तेज झोंकों के संकेत" else "strong wind-gust signals"
            SevereHazardType.SNOW_ICE -> if (hindi) "भारी बर्फ/हिम के संकेत" else "heavy snow or wintry signals"
            SevereHazardType.MIXED -> if (hindi) "एक से अधिक गंभीर मौसम संकेत" else "multiple severe-weather signals"
            SevereHazardType.NONE -> if (hindi) "मौसम जोखिम संकेत" else "weather-risk signals"
        }
        return if (hindi) {
            "Forecast model में $detail हैं; peak risk ${formatPeakHour(peak.hour.isoTime)} के आसपास है।"
        } else {
            "Forecast model shows $detail, with peak risk around ${formatPeakHour(peak.hour.isoTime)}."
        }
    }

    private fun statusFor(level: SevereRiskLevel, hindi: Boolean): String = when (level) {
        SevereRiskLevel.SEVERE -> if (hindi) "गंभीर" else "SEVERE"
        SevereRiskLevel.HIGH -> if (hindi) "उच्च" else "HIGH"
        SevereRiskLevel.MODERATE -> if (hindi) "निगरानी" else "WATCH"
        SevereRiskLevel.ADVISORY -> if (hindi) "सलाह" else "ADVISORY"
        SevereRiskLevel.NONE -> if (hindi) "साफ" else "CLEAR"
    }

    private fun severityFor(level: SevereRiskLevel, hindi: Boolean): String = when (level) {
        SevereRiskLevel.SEVERE -> if (hindi) "बहुत उच्च" else "Very High"
        SevereRiskLevel.HIGH -> if (hindi) "उच्च" else "High"
        SevereRiskLevel.MODERATE -> if (hindi) "मध्यम" else "Moderate"
        SevereRiskLevel.ADVISORY -> if (hindi) "कम-मध्यम" else "Low–Moderate"
        SevereRiskLevel.NONE -> if (hindi) "कम" else "Low"
    }

    private fun radiusFor(level: SevereRiskLevel): Int = when (level) {
        SevereRiskLevel.SEVERE -> 90
        SevereRiskLevel.HIGH -> 60
        SevereRiskLevel.MODERATE -> 35
        SevereRiskLevel.ADVISORY -> 20
        SevereRiskLevel.NONE -> 0
    }

    private fun guidanceFor(hazard: SevereHazardType, level: SevereRiskLevel, hindi: Boolean): List<RiskGuidance> {
        if (level == SevereRiskLevel.NONE) return defaultGuidance(hindi)
        return if (hindi) {
            when (hazard) {
                SevereHazardType.THUNDERSTORM, SevereHazardType.MIXED -> listOf(
                    RiskGuidance("सुरक्षित जगह", "गरज-चमक के समय घर/मजबूत इमारत में रहें"),
                    RiskGuidance("बिजली", "खुले मैदान, ऊंचे पेड़ और पानी से दूर रहें"),
                    RiskGuidance("यात्रा", "तेज बारिश या जलभराव में रास्ता बदलें")
                )
                SevereHazardType.DAMAGING_WIND -> listOf(
                    RiskGuidance("ढीली वस्तुएं", "बाहर की हल्की/ढीली चीजें सुरक्षित करें"),
                    RiskGuidance("खिड़कियां", "तेज झोंकों में खिड़कियों से दूरी रखें"),
                    RiskGuidance("यात्रा", "ऊंचे वाहन और खुले रास्तों पर सावधानी रखें")
                )
                SevereHazardType.HEAVY_RAIN -> listOf(
                    RiskGuidance("जलभराव", "बहते या डूबे रास्ते में प्रवेश न करें"),
                    RiskGuidance("यात्रा", "कम दृश्यता में गति कम रखें"),
                    RiskGuidance("अपडेट", "स्थानीय आधिकारिक चेतावनी देखते रहें")
                )
                SevereHazardType.SNOW_ICE -> listOf(
                    RiskGuidance("फिसलन", "बर्फ/हिम वाली सतहों पर सावधानी रखें"),
                    RiskGuidance("यात्रा", "अनावश्यक यात्रा टालें"),
                    RiskGuidance("अपडेट", "स्थानीय आधिकारिक चेतावनी देखते रहें")
                )
                SevereHazardType.NONE -> defaultGuidance(true)
            }
        } else {
            when (hazard) {
                SevereHazardType.THUNDERSTORM, SevereHazardType.MIXED -> listOf(
                    RiskGuidance("Shelter", "Stay inside a substantial building during thunder"),
                    RiskGuidance("Lightning", "Avoid open ground, tall isolated trees and water"),
                    RiskGuidance("Travel", "Reroute around flooded roads or intense rain")
                )
                SevereHazardType.DAMAGING_WIND -> listOf(
                    RiskGuidance("Secure items", "Bring in or secure loose outdoor objects"),
                    RiskGuidance("Windows", "Keep away from windows during strong gusts"),
                    RiskGuidance("Travel", "Use extra care with high-profile vehicles")
                )
                SevereHazardType.HEAVY_RAIN -> listOf(
                    RiskGuidance("Flooding", "Do not enter flowing or flooded roads"),
                    RiskGuidance("Travel", "Slow down when visibility is reduced"),
                    RiskGuidance("Updates", "Check local official warnings for instructions")
                )
                SevereHazardType.SNOW_ICE -> listOf(
                    RiskGuidance("Slippery roads", "Use care on snow or ice-covered surfaces"),
                    RiskGuidance("Travel", "Avoid unnecessary travel in worsening conditions"),
                    RiskGuidance("Updates", "Check local official warnings for instructions")
                )
                SevereHazardType.NONE -> defaultGuidance(false)
            }
        }
    }

    private fun defaultGuidance(hindi: Boolean): List<RiskGuidance> = if (hindi) {
        listOf(
            RiskGuidance("तैयार रहें", "Forecast बदल सकता है; अपडेट देखते रहें"),
            RiskGuidance("स्थानीय सूचना", "जरूरत पर आधिकारिक मौसम चेतावनी देखें"),
            RiskGuidance("यात्रा", "मौसम बिगड़ने पर योजना बदलें")
        )
    } else {
        listOf(
            RiskGuidance("Stay aware", "Forecast risk can change as new data arrives"),
            RiskGuidance("Official alerts", "Check local authorities when warnings are issued"),
            RiskGuidance("Travel", "Adjust plans if conditions deteriorate")
        )
    }

    private fun sourceNote(state: WeatherHomeUiState, hindi: Boolean): String {
        val cache = state.isOffline || state.isCachedData
        return if (hindi) {
            if (cache) {
                "सेव किए गए Open-Meteo forecast से जोखिम अनुमान • यह आधिकारिक सरकारी चेतावनी नहीं है"
            } else {
                "Open-Meteo forecast से जोखिम अनुमान • यह आधिकारिक सरकारी चेतावनी नहीं है"
            }
        } else {
            if (cache) {
                "Risk estimate from saved Open-Meteo forecast • not an official government warning"
            } else {
                "Risk estimate from Open-Meteo forecast • not an official government warning"
            }
        }
    }

    private fun toKmh(value: Int, windUnit: String): Double {
        return if (windUnit.equals("mph", ignoreCase = true)) value / 0.621371 else value.toDouble()
    }
}
