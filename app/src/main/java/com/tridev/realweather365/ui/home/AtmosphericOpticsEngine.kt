package com.tridev.realweather365.ui.home

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

data class AtmosphericOptics(
    val airMass: Float,
    val rayleigh: Float,
    val mie: Float,
    val horizonExtinction: Float,
    val directExposure: Float,
    val diffuseExposure: Float,
    val sunsetReddening: Float
)

object AtmosphericOpticsEngine {
    fun calculate(
        solarElevationDegrees: Double,
        humidity: Int,
        visibilityKm: Double?,
        cloudCover: Int,
        directRadiation: Double,
        diffuseRadiation: Double
    ): AtmosphericOptics {
        val elevation = solarElevationDegrees.coerceIn(-6.0, 90.0)
        val sine = sin(Math.toRadians(elevation.coerceAtLeast(0.5))).coerceAtLeast(0.0087)
        val airMass = (1.0 / (sine + 0.50572 * (elevation + 6.07995).pow(-1.6364))).toFloat().coerceIn(1f, 38f)
        val visibility = (visibilityKm ?: 35.0).coerceIn(0.2, 80.0)
        val aerosol = ((18.0 / visibility) * 0.32 + humidity.coerceIn(0, 100) / 100.0 * 0.28).toFloat().coerceIn(0.08f, 1f)
        val cloud = cloudCover.coerceIn(0, 100) / 100f
        val measured = (directRadiation + diffuseRadiation).coerceAtLeast(0.0)
        val direct = if (measured > 1.0) (directRadiation / 950.0).toFloat() else exp(-0.115f * airMass)
        val diffuse = if (measured > 1.0) (diffuseRadiation / 500.0).toFloat() else (0.18f + aerosol * 0.32f)
        return AtmosphericOptics(
            airMass = airMass,
            rayleigh = (1.08f - aerosol * 0.24f).coerceIn(0.65f, 1.08f),
            mie = aerosol,
            horizonExtinction = (1f - exp(-0.12f * airMass * (0.65f + aerosol))).coerceIn(0.05f, 0.96f),
            directExposure = (direct * (1f - cloud * 0.72f)).coerceIn(0.05f, 1.15f),
            diffuseExposure = (diffuse + cloud * 0.18f).coerceIn(0.08f, 0.9f),
            sunsetReddening = ((airMass - 1f) / 12f).coerceIn(0f, 1f)
        )
    }
}
