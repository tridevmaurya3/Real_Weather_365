package com.tridev.realweather365.ui.home

import kotlin.math.max

/** WMO-inspired genera used to choose the vertical density profile, not a forecast label. */
enum class PhysicalCloudType(val shaderId: Float) {
    CLEAR(0f),
    CIRRUS(1f),
    CIRROSTRATUS(2f),
    ALTOCUMULUS(3f),
    ALTOSTRATUS(4f),
    STRATUS(5f),
    STRATOCUMULUS(6f),
    CUMULUS(7f),
    NIMBOSTRATUS(8f),
    CUMULONIMBUS(9f)
}

data class CloudPhysicsState(
    val type: PhysicalCloudType,
    val totalCoverage: Float,
    val lowCoverage: Float,
    val midCoverage: Float,
    val highCoverage: Float,
    val baseHeight: Float,
    val topHeight: Float,
    val verticalDevelopment: Float,
    val opticalDepth: Float,
    val edgeErosion: Float,
    val precipitationStrength: Float,
    val instability: Float,
    val directLightFraction: Float,
    val diffuseLightFraction: Float
)

object CloudPhysicsEngine {
    fun infer(state: WeatherHomeUiState): CloudPhysicsState {
        val hour = state.hourly24.firstOrNull()
        val code = hour?.weatherCode ?: weatherCodeFromScene(state.scene)
        val precipitation = (hour?.precipitationMm ?: 0.0).toFloat().coerceIn(0f, 12f) / 12f
        val cape = (hour?.cape ?: 0.0).toFloat()
        val instability = (cape / 1_800f).coerceIn(0f, 1f)
        val low = state.cloudCoverLow.percent()
        val mid = state.cloudCoverMid.percent()
        val high = state.cloudCoverHigh.percent()
        val total = max(state.cloudCover.percent(), max(low, max(mid, high)))
        val type = classify(code, total, low, mid, high, instability, precipitation)
        val profile = profile(type)
        val radiationTotal = (state.directRadiation + state.diffuseRadiation).coerceAtLeast(0.0)
        val direct = if (radiationTotal > 0.1) (state.directRadiation / radiationTotal).toFloat() else state.isDay.let { if (it) 0.55f else 0f }

        return CloudPhysicsState(
            type = type,
            totalCoverage = total,
            lowCoverage = low,
            midCoverage = mid,
            highCoverage = high,
            baseHeight = profile.base,
            topHeight = profile.top,
            verticalDevelopment = (profile.vertical + instability * 0.35f).coerceIn(0f, 1f),
            opticalDepth = (profile.opticalDepth * (0.45f + total * 0.75f) + precipitation * 0.35f).coerceIn(0.08f, 2.4f),
            edgeErosion = (profile.erosion + state.windGusts.coerceIn(0, 90) / 300f).coerceIn(0.1f, 0.9f),
            precipitationStrength = precipitation,
            instability = instability,
            directLightFraction = direct.coerceIn(0f, 1f),
            diffuseLightFraction = (1f - direct).coerceIn(0f, 1f)
        )
    }

    private fun classify(
        code: Int,
        total: Float,
        low: Float,
        mid: Float,
        high: Float,
        instability: Float,
        precipitation: Float
    ): PhysicalCloudType = when {
        code in 95..99 || (instability > 0.62f && precipitation > 0.08f) -> PhysicalCloudType.CUMULONIMBUS
        code in 51..82 && (precipitation > 0.03f || total > 0.7f) -> PhysicalCloudType.NIMBOSTRATUS
        total < 0.06f -> PhysicalCloudType.CLEAR
        low >= 0.72f && mid < 0.42f -> PhysicalCloudType.STRATUS
        low >= 0.45f && low >= mid -> PhysicalCloudType.STRATOCUMULUS
        high >= 0.72f && mid > 0.38f -> PhysicalCloudType.CIRROSTRATUS
        high >= 0.38f && high > low && high > mid -> PhysicalCloudType.CIRRUS
        mid >= 0.68f -> PhysicalCloudType.ALTOSTRATUS
        mid >= 0.38f && mid > low -> PhysicalCloudType.ALTOCUMULUS
        instability > 0.3f || low > 0.18f -> PhysicalCloudType.CUMULUS
        else -> PhysicalCloudType.CIRRUS
    }

    private fun weatherCodeFromScene(scene: WeatherScene): Int = when (scene) {
        WeatherScene.THUNDERSTORM -> 95
        WeatherScene.RAIN -> 63
        WeatherScene.SNOW -> 73
        else -> 0
    }

    private data class Profile(val base: Float, val top: Float, val vertical: Float, val opticalDepth: Float, val erosion: Float)

    private fun profile(type: PhysicalCloudType): Profile = when (type) {
        PhysicalCloudType.CLEAR -> Profile(0.30f, 0.34f, 0f, 0.08f, 0.8f)
        PhysicalCloudType.CIRRUS -> Profile(0.12f, 0.28f, 0.12f, 0.24f, 0.74f)
        PhysicalCloudType.CIRROSTRATUS -> Profile(0.10f, 0.31f, 0.08f, 0.42f, 0.48f)
        PhysicalCloudType.ALTOCUMULUS -> Profile(0.24f, 0.48f, 0.35f, 0.68f, 0.58f)
        PhysicalCloudType.ALTOSTRATUS -> Profile(0.20f, 0.52f, 0.18f, 0.98f, 0.34f)
        PhysicalCloudType.STRATUS -> Profile(0.43f, 0.62f, 0.08f, 1.18f, 0.24f)
        PhysicalCloudType.STRATOCUMULUS -> Profile(0.34f, 0.64f, 0.34f, 1.08f, 0.42f)
        PhysicalCloudType.CUMULUS -> Profile(0.34f, 0.72f, 0.64f, 0.94f, 0.56f)
        PhysicalCloudType.NIMBOSTRATUS -> Profile(0.23f, 0.75f, 0.28f, 1.72f, 0.22f)
        PhysicalCloudType.CUMULONIMBUS -> Profile(0.10f, 0.94f, 1f, 2.25f, 0.36f)
    }
}

private fun Int.percent(): Float = coerceIn(0, 100) / 100f
