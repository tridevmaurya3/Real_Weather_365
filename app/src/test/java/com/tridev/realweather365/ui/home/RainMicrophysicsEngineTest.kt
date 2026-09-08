package com.tridev.realweather365.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test

class RainMicrophysicsEngineTest {
    @Test
    fun drizzleDropsStaySmallAndSlow() {
        val result = RainMicrophysicsEngine.calculate(physics(code = 51, millimetres = 0.2, intensity = 0.16f))
        assertTrue(result.medianDiameterMm < 0.6f)
        assertTrue(result.terminalVelocityMs < 4f)
    }

    @Test
    fun heavyRainProducesFasterDropsAndStrongerImpacts() {
        val light = RainMicrophysicsEngine.calculate(physics(code = 61, millimetres = 0.8, intensity = 0.25f))
        val heavy = RainMicrophysicsEngine.calculate(physics(code = 65, millimetres = 14.0, intensity = 0.92f))
        assertTrue(heavy.medianDiameterMm > light.medianDiameterMm)
        assertTrue(heavy.terminalVelocityMs > light.terminalVelocityMs)
        assertTrue(heavy.splashEnergy > light.splashEnergy)
        assertTrue(heavy.surfaceWetness > light.surfaceWetness)
    }

    private fun physics(code: Int, millimetres: Double, intensity: Float) = PrecipitationPhysics(
        precipitationMm = millimetres,
        rainChance = 90,
        weatherCode = code,
        cape = 0.0,
        windSpeedKmh = 12,
        windGustKmh = 20,
        windDirection = 240,
        intensity = intensity,
        lightningPotential = 0f,
        thunderstorm = false,
        hail = false
    )
}
