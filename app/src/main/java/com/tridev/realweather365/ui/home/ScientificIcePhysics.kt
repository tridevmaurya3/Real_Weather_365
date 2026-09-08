package com.tridev.realweather365.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class SnowCrystalHabit { PLATE, COLUMN, NEEDLE, DENDRITE, AGGREGATE, GRAUPEL }

data class IceMicrophysics(
    val habit: SnowCrystalHabit,
    val fallSpeedMs: Float,
    val crystalSize: Float,
    val flutter: Float,
    val meltFraction: Float,
    val stickingFraction: Float
)

object IceMicrophysicsEngine {
    fun calculate(physics: SnowPhysics): IceMicrophysics {
        val temperature = physics.temperatureC
        val humid = physics.humidity / 100f
        val habit = when {
            physics.weatherCode == 77 -> SnowCrystalHabit.GRAUPEL
            temperature in -18..-12 && humid >= 0.76f -> SnowCrystalHabit.DENDRITE
            temperature in -11..-7 -> SnowCrystalHabit.COLUMN
            temperature in -6..-3 && humid < 0.82f -> SnowCrystalHabit.NEEDLE
            temperature <= -18 -> SnowCrystalHabit.PLATE
            temperature >= -2 && humid >= 0.84f -> SnowCrystalHabit.AGGREGATE
            else -> SnowCrystalHabit.PLATE
        }
        val baseSpeed = when (habit) {
            SnowCrystalHabit.DENDRITE -> 0.62f
            SnowCrystalHabit.AGGREGATE -> 1.05f
            SnowCrystalHabit.PLATE -> 0.78f
            SnowCrystalHabit.COLUMN -> 0.92f
            SnowCrystalHabit.NEEDLE -> 1.12f
            SnowCrystalHabit.GRAUPEL -> 2.25f
        }
        val melt = ((temperature + 1f) / 4f).coerceIn(0f, 1f)
        return IceMicrophysics(
            habit = habit,
            fallSpeedMs = (baseSpeed + physics.intensity * 0.38f + melt * 0.42f).coerceIn(0.45f, 3.1f),
            crystalSize = (0.72f + humid * 0.58f + physics.intensity * 0.42f).coerceIn(0.7f, 1.72f),
            flutter = if (habit == SnowCrystalHabit.GRAUPEL) 0.18f else (1.18f - baseSpeed * 0.28f).coerceIn(0.35f, 1f),
            meltFraction = melt,
            stickingFraction = ((1f - melt) * (0.48f + humid * 0.45f)).coerceIn(0f, 1f)
        )
    }
}

data class HailMicrophysics(val diameterMm: Float, val fallSpeedMs: Float, val opacity: Float)

object HailMicrophysicsEngine {
    fun calculate(physics: PrecipitationPhysics): HailMicrophysics {
        val updraftProxy = sqrt((physics.cape / 2600.0).coerceIn(0.0, 1.5)).toFloat()
        val diameter = (4f + updraftProxy * 17f + physics.intensity * 7f).coerceIn(4f, 30f)
        return HailMicrophysics(
            diameterMm = diameter,
            fallSpeedMs = (7f + sqrt(diameter) * 3.9f).coerceIn(8f, 29f),
            opacity = (0.48f + diameter / 70f).coerceIn(0.5f, 0.9f)
        )
    }
}

internal fun DrawScope.drawScientificSnowField(physics: SnowPhysics, progress: Float, near: Boolean) {
    val ice = IceMicrophysicsEngine.calculate(physics)
    val count = if (near) (13 + physics.intensity * 42f).toInt() else (40 + physics.intensity * 92f).toInt()
    val destination = ((physics.windDirection + 180) % 360 + 360) % 360
    val windX = sin(destination * PI / 180.0).toFloat()
    val windFactor = (if (near) physics.windGustKmh else physics.windSpeedKmh).coerceIn(0, 100) / 100f
    repeat(count) { index ->
        val depth = if (near) 0.66f + ((index * 17) % 34) / 100f else ((index * 19) % 63) / 100f
        val speed = ice.fallSpeedMs * (0.34f + depth * 0.42f)
        val phase = positiveIceModulo(index * 0.137f + progress * speed, 1f)
        val flutter = sin(progress * 6.28318f * (1.1f + ice.flutter) + index * 0.83f).toFloat()
        val xBase = ((index * 0.237f + 0.071f) % 1f) * size.width
        val x = wrapIce(xBase + flutter * size.width * 0.018f * ice.flutter + phase * windX * windFactor * size.width * 0.12f, size.width)
        val y = phase * size.height
        val radius = (if (near) 2.0f else 0.75f) * ice.crystalSize * (0.75f + depth * 0.55f)
        if (near && index % 3 == 0 && radius > 2.2f) {
            drawCrystal(Offset(x, y), radius, ice.habit, progress * 2f * PI.toFloat() + index)
        } else {
            drawCircle(
                color = Color.White.copy(alpha = (0.32f + depth * 0.55f) * (1f - ice.meltFraction * 0.22f)),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

internal fun DrawScope.drawScientificHail(physics: PrecipitationPhysics, progress: Float) {
    val hail = HailMicrophysicsEngine.calculate(physics)
    val count = (7 + physics.intensity * 22f).toInt()
    val direction = ((physics.windDirection + 180) % 360 + 360) % 360
    val windX = sin(direction * PI / 180.0).toFloat()
    repeat(count) { index ->
        val phase = positiveIceModulo(((index * 41 + 3) % 163) / 163f + progress * hail.fallSpeedMs / 13f, 1.08f)
        val x = wrapIce(size.width * (((index * 73 + 5) % 157) / 157f + progress * windX * 0.07f), size.width)
        val y = phase * size.height
        val radius = (1.2f + hail.diameterMm / 14f) * (0.72f + (index % 4) * 0.12f)
        drawCircle(Color(0xFFD5E8F3).copy(alpha = hail.opacity * 0.30f), radius * 1.55f, Offset(x, y))
        drawCircle(Color(0xFFEDF8FC).copy(alpha = hail.opacity), radius, Offset(x, y))
        drawCircle(Color.White.copy(alpha = 0.58f), radius * 0.25f, Offset(x - radius * 0.30f, y - radius * 0.30f))
    }
}

private fun DrawScope.drawCrystal(center: Offset, radius: Float, habit: SnowCrystalHabit, rotation: Float) {
    val color = Color.White.copy(alpha = 0.82f)
    when (habit) {
        SnowCrystalHabit.GRAUPEL, SnowCrystalHabit.AGGREGATE -> {
            repeat(if (habit == SnowCrystalHabit.AGGREGATE) 4 else 2) { i ->
                val angle = rotation + i * 1.57f
                drawCircle(color.copy(alpha = 0.62f), radius * 0.58f, Offset(center.x + cos(angle) * radius * 0.35f, center.y + sin(angle) * radius * 0.35f))
            }
        }
        SnowCrystalHabit.COLUMN, SnowCrystalHabit.NEEDLE -> {
            val length = if (habit == SnowCrystalHabit.NEEDLE) radius * 1.65f else radius * 1.15f
            drawLine(color, Offset(center.x - cos(rotation) * length, center.y - sin(rotation) * length), Offset(center.x + cos(rotation) * length, center.y + sin(rotation) * length), if (habit == SnowCrystalHabit.COLUMN) radius * 0.45f else 1f)
        }
        SnowCrystalHabit.PLATE -> {
            val path = Path()
            repeat(6) { arm ->
                val angle = rotation + arm * PI.toFloat() / 3f
                val point = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
                if (arm == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            drawPath(path, color.copy(alpha = 0.66f))
        }
        SnowCrystalHabit.DENDRITE -> repeat(6) { arm ->
            val angle = rotation + arm * PI.toFloat() / 3f
            val end = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
            drawLine(color, center, end, 0.85f)
            listOf(0.52f, 0.76f).forEach { fraction ->
                val joint = Offset(center.x + cos(angle) * radius * fraction, center.y + sin(angle) * radius * fraction)
                listOf(-0.55f, 0.55f).forEach { branch ->
                    val branchAngle = angle + branch
                    drawLine(color.copy(alpha = 0.68f), joint, Offset(joint.x + cos(branchAngle) * radius * 0.25f, joint.y + sin(branchAngle) * radius * 0.25f), 0.65f)
                }
            }
        }
    }
}

private fun wrapIce(value: Float, width: Float): Float = positiveIceModulo(value, width)
private fun positiveIceModulo(value: Float, divisor: Float): Float = ((value % divisor) + divisor) % divisor
