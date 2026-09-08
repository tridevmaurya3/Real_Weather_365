package com.tridev.realweather365.ui.home

import android.graphics.RuntimeShader
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time cloud renderer. Android 13+ ray-marches a procedural 3D density field;
 * older devices retain the deterministic Canvas renderer instead of showing nothing.
 */
@Composable
fun ScientificVolumetricCloudSystem(
    state: WeatherHomeUiState,
    scene: WeatherScene,
    solar: SolarVisualState,
    moon: MoonVisualState,
    modifier: Modifier = Modifier
) {
    val physics = remember(
        state.cloudCover,
        state.cloudCoverLow,
        state.cloudCoverMid,
        state.cloudCoverHigh,
        state.directRadiation,
        state.diffuseRadiation,
        state.windGusts,
        state.hourly24,
        scene
    ) { CloudPhysicsEngine.infer(state.copy(scene = scene)) }

    if (physics.type == PhysicalCloudType.CLEAR && physics.totalCoverage < 0.025f) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslVolumetricClouds(
            state = state,
            scene = scene,
            physics = physics,
            windSpeed = state.windSpeed,
            windDirection = state.windDirection,
            solar = solar,
            moon = moon,
            animationLevel = state.animationLevel,
            modifier = modifier
        )
    } else {
        RealCloudSystem(
            cloudCover = state.cloudCover,
            windSpeed = state.windSpeed,
            windDirection = state.windDirection,
            condition = state.condition,
            scene = scene,
            solar = solar,
            moon = moon,
            animationLevel = state.animationLevel,
            modifier = modifier
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslVolumetricClouds(
    state: WeatherHomeUiState,
    scene: WeatherScene,
    physics: CloudPhysicsState,
    windSpeed: Int,
    windDirection: Int,
    solar: SolarVisualState,
    moon: MoonVisualState,
    animationLevel: Int,
    modifier: Modifier
) {
    val shader = remember { runCatching { RuntimeShader(VOLUMETRIC_CLOUD_SHADER) }.getOrNull() }
    if (shader == null) {
        RealCloudSystem(
            cloudCover = state.cloudCover,
            windSpeed = state.windSpeed,
            windDirection = state.windDirection,
            condition = state.condition,
            scene = scene,
            solar = solar,
            moon = moon,
            animationLevel = state.animationLevel,
            modifier = modifier
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "volumetric-cloud-advection")
    val clock = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (150_000f / (0.55f + windSpeed.coerceIn(0, 100) / 30f)).toInt().coerceAtLeast(22_000),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "volumetric-cloud-time"
    )
    val destination = ((windDirection + 180) % 360 + 360) % 360
    val radians = destination / 180.0 * PI
    val flowX = sin(radians).toFloat()
    val flowY = (-cos(radians)).toFloat()
    val moonLight = if (moon.aboveHorizon) moon.illuminationFraction.toFloat() else 0f
    val time = if (animationLevel <= 0) 0.37f else clock.value

    Canvas(modifier = modifier) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("clock", time)
        shader.setFloatUniform("coverage", physics.totalCoverage)
        shader.setFloatUniform("layerCoverage", physics.lowCoverage, physics.midCoverage, physics.highCoverage)
        shader.setFloatUniform("heightRange", physics.baseHeight, physics.topHeight)
        shader.setFloatUniform("shape", physics.type.shaderId, physics.verticalDevelopment, physics.edgeErosion)
        shader.setFloatUniform("weather", physics.opticalDepth, physics.precipitationStrength, physics.instability)
        shader.setFloatUniform("lightBalance", physics.directLightFraction, physics.diffuseLightFraction)
        shader.setFloatUniform("sunPosition", solar.visualX, solar.visualY)
        shader.setFloatUniform("atmosphere", solar.daylight, solar.warmth, moonLight)
        shader.setFloatUniform("wind", flowX, flowY, windSpeed.coerceIn(0, 120) / 120f)
        shader.setFloatUniform("quality", if (animationLevel >= 2) 1f else 0.65f)

        val paint = Paint().also { it.shader = shader }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}

private const val VOLUMETRIC_CLOUD_SHADER = """
uniform float2 resolution;
uniform float clock;
uniform float coverage;
uniform float3 layerCoverage;
uniform float2 heightRange;
uniform float3 shape;
uniform float3 weather;
uniform float2 lightBalance;
uniform float2 sunPosition;
uniform float3 atmosphere;
uniform float3 wind;
uniform float quality;

float hash31(float3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(float3 p) {
    float3 i = floor(p);
    float3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i + float3(0.0, 0.0, 0.0));
    float n100 = hash31(i + float3(1.0, 0.0, 0.0));
    float n010 = hash31(i + float3(0.0, 1.0, 0.0));
    float n110 = hash31(i + float3(1.0, 1.0, 0.0));
    float n001 = hash31(i + float3(0.0, 0.0, 1.0));
    float n101 = hash31(i + float3(1.0, 0.0, 1.0));
    float n011 = hash31(i + float3(0.0, 1.0, 1.0));
    float n111 = hash31(i + float3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float fbm(float3 p) {
    float value = noise3(p) * 0.55;
    value += noise3(p * 2.03 + 17.1) * 0.28;
    value += noise3(p * 4.11 + 41.7) * 0.12;
    value += noise3(p * 8.07 + 9.2) * 0.05;
    return value;
}

float heightProfile(float h) {
    float base = smoothstep(heightRange.x, heightRange.x + 0.10, h);
    float top = 1.0 - smoothstep(heightRange.y - 0.11, heightRange.y, h);
    float tower = mix(1.0, pow(clamp(h / max(heightRange.y, 0.01), 0.0, 1.0), 0.42), shape.y);
    return base * top * tower;
}

float cloudDensity(float3 p) {
    float layer = mix(layerCoverage.z, layerCoverage.x, smoothstep(0.25, 0.72, p.y));
    layer = max(layer, layerCoverage.y * (1.0 - abs(p.y - 0.45) * 1.8));
    float3 advected = p;
    advected.xz += float2(wind.x, wind.y) * clock * (0.8 + wind.z * 2.2);
    float broad = fbm(advected * float3(1.6, 2.0, 1.15));
    float detail = fbm(advected * 5.8 + float3(3.0, 11.0, 7.0));
    float threshold = 0.76 - coverage * 0.48 - layer * 0.16;
    float formed = smoothstep(threshold, threshold + 0.19, broad);
    float erosion = mix(1.0, smoothstep(0.22, 0.75, detail), shape.z);
    return formed * erosion * heightProfile(p.y);
}

float phaseHG(float cosineAngle, float g) {
    float gg = g * g;
    return (1.0 - gg) / max(0.15, pow(1.0 + gg - 2.0 * g * cosineAngle, 1.5));
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float aspect = resolution.x / max(resolution.y, 1.0);
    float3 ray = normalize(float3((uv.x - 0.5) * aspect, (0.61 - uv.y) * 0.92, 1.35));
    float3 origin = float3(0.0, 0.54, -1.2);
    float3 sunDir = normalize(float3((sunPosition.x - 0.5) * 1.4, (0.62 - sunPosition.y) * 1.2, 0.72));
    float transmittance = 1.0;
    float3 radiance = float3(0.0);
    float stepLength = mix(0.095, 0.066, quality);

    for (int i = 0; i < 24; ++i) {
        float distance = (float(i) + 0.7) * stepLength;
        float3 p = origin + ray * distance;
        p.x += 0.5;
        float density = cloudDensity(p);
        if (density > 0.002) {
            float sunDensity = 0.0;
            sunDensity += cloudDensity(p + sunDir * 0.08);
            sunDensity += cloudDensity(p + sunDir * 0.17);
            sunDensity += cloudDensity(p + sunDir * 0.31);
            float sunTransmission = exp(-sunDensity * weather.x * 0.72);
            float forward = phaseHG(dot(ray, sunDir), 0.58) * 0.12;
            float daylight = atmosphere.x;
            float3 cool = mix(float3(0.22, 0.28, 0.38), float3(0.70, 0.79, 0.88), daylight);
            float3 warm = float3(1.0, 0.57, 0.31);
            float3 sunColor = mix(float3(0.66, 0.76, 0.92), warm, atmosphere.y * 0.78);
            float3 moonColor = float3(0.42, 0.52, 0.70) * atmosphere.z * 0.42;
            float powder = 1.0 - exp(-density * 2.2);
            float3 lighting = cool * (0.34 + lightBalance.y * 0.38)
                + sunColor * sunTransmission * (0.30 + forward + powder * 0.18) * max(daylight, 0.08)
                + moonColor;
            float extinction = density * weather.x * stepLength * 2.7;
            float alphaStep = 1.0 - exp(-extinction);
            radiance += transmittance * alphaStep * lighting;
            transmittance *= (1.0 - alphaStep);
        }
    }
    float alpha = clamp(1.0 - transmittance, 0.0, 0.96);
    radiance *= 1.0 - weather.y * 0.18;
    return half4(half3(radiance * alpha), half(alpha));
}
"""
