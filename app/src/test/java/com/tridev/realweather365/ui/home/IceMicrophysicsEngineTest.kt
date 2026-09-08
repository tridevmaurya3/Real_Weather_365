package com.tridev.realweather365.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IceMicrophysicsEngineTest {
    @Test
    fun moistDendriticGrowthLayerProducesDendrites() {
        val result = IceMicrophysicsEngine.calculate(snow(temperature = -15, humidity = 90, code = 73))
        assertEquals(SnowCrystalHabit.DENDRITE, result.habit)
        assertTrue(result.fallSpeedMs < 1.2f)
    }

    @Test
    fun snowPelletCodeProducesFasterGraupel() {
        val dendrite = IceMicrophysicsEngine.calculate(snow(temperature = -15, humidity = 90, code = 73))
        val graupel = IceMicrophysicsEngine.calculate(snow(temperature = -4, humidity = 88, code = 77))
        assertEquals(SnowCrystalHabit.GRAUPEL, graupel.habit)
        assertTrue(graupel.fallSpeedMs > dendrite.fallSpeedMs)
    }

    @Test
    fun aboveFreezingAirIncreasesMelting() {
        val cold = IceMicrophysicsEngine.calculate(snow(temperature = -5, humidity = 85, code = 73))
        val warm = IceMicrophysicsEngine.calculate(snow(temperature = 2, humidity = 85, code = 73))
        assertTrue(warm.meltFraction > cold.meltFraction)
        assertTrue(warm.stickingFraction < cold.stickingFraction)
    }

    private fun snow(temperature: Int, humidity: Int, code: Int) = SnowPhysics(
        snowfallCm = 0.8,
        precipitationChance = 90,
        weatherCode = code,
        temperatureC = temperature,
        windSpeedKmh = 15,
        windGustKmh = 25,
        windDirection = 310,
        visibilityKm = 7.0,
        humidity = humidity,
        intensity = 0.65f,
        accumulation = 0.55f,
        blowingSnow = 0.32f
    )
}
