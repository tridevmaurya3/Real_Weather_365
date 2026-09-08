package com.tridev.realweather365.ui.home

import com.tridev.realweather365.data.weather.LiveHourData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudPhysicsEngineTest {
    @Test
    fun clearSkyRemainsClear() {
        val result = CloudPhysicsEngine.infer(WeatherHomeUiState(scene = WeatherScene.SUNNY, cloudCover = 2))
        assertEquals(PhysicalCloudType.CLEAR, result.type)
    }

    @Test
    fun thunderstormsCreateDeepConvectiveProfile() {
        val result = CloudPhysicsEngine.infer(
            WeatherHomeUiState(
                scene = WeatherScene.THUNDERSTORM,
                cloudCover = 94,
                cloudCoverLow = 91,
                cloudCoverMid = 83,
                cloudCoverHigh = 76,
                hourly24 = listOf(hour(weatherCode = 95, precipitation = 8.0, cape = 2_400.0))
            )
        )
        assertEquals(PhysicalCloudType.CUMULONIMBUS, result.type)
        assertTrue(result.verticalDevelopment > 0.9f)
        assertTrue(result.opticalDepth > 1.8f)
    }

    @Test
    fun widespreadRainCreatesNimbostratus() {
        val result = CloudPhysicsEngine.infer(
            WeatherHomeUiState(
                scene = WeatherScene.RAIN,
                cloudCover = 92,
                cloudCoverLow = 88,
                cloudCoverMid = 77,
                hourly24 = listOf(hour(weatherCode = 63, precipitation = 3.5))
            )
        )
        assertEquals(PhysicalCloudType.NIMBOSTRATUS, result.type)
    }

    @Test
    fun radiationBalanceTracksDirectSun() {
        val result = CloudPhysicsEngine.infer(
            WeatherHomeUiState(isDay = true, cloudCover = 35, directRadiation = 600.0, diffuseRadiation = 200.0)
        )
        assertEquals(0.75f, result.directLightFraction, 0.001f)
        assertEquals(0.25f, result.diffuseLightFraction, 0.001f)
    }

    private fun hour(weatherCode: Int, precipitation: Double, cape: Double? = null) = LiveHourData(
        isoTime = "2026-09-08T12:00",
        label = "Now",
        temperature = 28,
        weatherCode = weatherCode,
        rainChance = 90,
        isDay = true,
        precipitationMm = precipitation,
        cape = cape
    )
}
