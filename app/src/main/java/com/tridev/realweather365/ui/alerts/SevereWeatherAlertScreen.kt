package com.tridev.realweather365.ui.alerts

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import com.tridev.realweather365.ui.theme.RealWeather365Theme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SevereWeatherAlertScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment = remember(state) { SevereWeatherRiskEngine.assess(state) }
    val transition = rememberInfiniteTransition(label = "risk-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "risk-pulse-progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF071821),
                        Color(0xFF0A202A),
                        Color(0xFF071720),
                        Color(0xFF041017)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AlertHeader(location = state.location, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                SevereRiskCard(state = state, assessment = assessment)
                Spacer(modifier = Modifier.height(10.dp))
                AlertTimingCard(assessment = assessment)
                Spacer(modifier = Modifier.height(10.dp))
                RiskMetricsCard(state = state, assessment = assessment)
                Spacer(modifier = Modifier.height(10.dp))
                AlertMapCard(state = state, assessment = assessment, pulse = pulse)
                Spacer(modifier = Modifier.height(10.dp))
                StaySafeCard(assessment = assessment)
                Spacer(modifier = Modifier.height(12.dp))
            }

            AlertFooter(state = state, assessment = assessment)
        }
    }
}

@Composable
private fun AlertHeader(location: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        color = Color(0xF2071720),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF75E5F0),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = location,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Severe Weather Intelligence",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 9.sp,
                    letterSpacing = 0.35.sp
                )
            }

            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun SevereRiskCard(
    state: WeatherHomeUiState,
    assessment: SevereWeatherAssessment
) {
    val accent = riskColor(assessment.level)
    val base = when (assessment.level) {
        SevereRiskLevel.SEVERE -> Color(0xFF5F1715)
        SevereRiskLevel.HIGH -> Color(0xFF542019)
        SevereRiskLevel.MODERATE -> Color(0xFF493019)
        SevereRiskLevel.ADVISORY -> Color(0xFF243B31)
        SevereRiskLevel.NONE -> Color(0xFF113A34)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = base,
        border = BorderStroke(0.9.dp, accent.copy(alpha = 0.58f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (assessment.level == SevereRiskLevel.NONE) Icons.Outlined.Shield else Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = assessment.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accent.copy(alpha = 0.92f)
                        ) {
                            Text(
                                text = assessment.statusLabel,
                                color = Color(0xFF07151B),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = assessment.summary,
                        color = Color.White.copy(alpha = 0.79f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0x30000000),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FlashOn,
                        contentDescription = null,
                        tint = Color(0xFFFFCE68),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Lightning: ${assessment.lightningLabel}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Gust ${assessment.peakGust} ${state.windUnit}",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertTimingCard(assessment: SevereWeatherAssessment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xA50B202A),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = Color(0xFF7BE4EF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast risk window",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 8.sp
                )
                Text(
                    text = assessment.windowLabel,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Severity",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 8.sp
                )
                Text(
                    text = assessment.severityLabel,
                    color = riskColor(assessment.level),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RiskMetricsCard(
    state: WeatherHomeUiState,
    assessment: SevereWeatherAssessment
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xA40B2029),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Text(
                text = "Forecast risk inputs • next 24h",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                RiskMetric(
                    label = "Peak gust",
                    value = "${assessment.peakGust} ${state.windUnit}",
                    hint = assessment.peakHourLabel,
                    modifier = Modifier.weight(1f)
                )
                RiskMetric(
                    label = "Rain chance",
                    value = "${assessment.peakRainChance}%",
                    hint = "maximum",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                RiskMetric(
                    label = "Precipitation",
                    value = "${formatOneDecimal(assessment.peakPrecipitationMm)} mm/h",
                    hint = "peak hourly",
                    modifier = Modifier.weight(1f)
                )
                RiskMetric(
                    label = "CAPE",
                    value = assessment.peakCape?.let { "${it.roundToInt()} J/kg" } ?: "--",
                    hint = if (assessment.peakSnowfallCm > 0.0) "Snow ${formatOneDecimal(assessment.peakSnowfallCm)} cm/h" else "instability",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RiskMetric(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(13.dp),
        color = Color(0x73122630),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 8.sp)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(hint, color = Color.White.copy(alpha = 0.42f), fontSize = 7.sp)
        }
    }
}

@Composable
private fun AlertMapCard(
    state: WeatherHomeUiState,
    assessment: SevereWeatherAssessment,
    pulse: Float
) {
    val accent = riskColor(assessment.level)
    val radiusLabel = if (state.distanceUnit == "mi") {
        "${(assessment.riskRadiusKm * 0.621371).roundToInt()} mi"
    } else {
        "${assessment.riskRadiusKm} km"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xC8071A24),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated local impact zone",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (assessment.riskRadiusKm > 0) "~$radiusLabel" else "LOW RISK",
                    color = accent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(205.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF102B34), Color(0xFF0A2029), Color(0xFF081922))
                        )
                    )
                    repeat(6) { index ->
                        val y = size.height * (0.10f + index * 0.16f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.035f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }
                    repeat(6) { index ->
                        val x = size.width * (0.08f + index * 0.17f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.03f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                    }

                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = when (assessment.level) {
                        SevereRiskLevel.SEVERE -> size.minDimension * 0.38f
                        SevereRiskLevel.HIGH -> size.minDimension * 0.32f
                        SevereRiskLevel.MODERATE -> size.minDimension * 0.26f
                        SevereRiskLevel.ADVISORY -> size.minDimension * 0.20f
                        SevereRiskLevel.NONE -> size.minDimension * 0.12f
                    }
                    val pulseRadius = baseRadius * (1f + pulse * 0.18f)
                    val pulseAlpha = if (assessment.level == SevereRiskLevel.NONE) 0.06f else (0.20f * (1f - pulse)).coerceAtLeast(0.03f)
                    drawCircle(accent.copy(alpha = pulseAlpha), pulseRadius, center)
                    drawCircle(accent.copy(alpha = 0.16f), baseRadius, center)
                    drawCircle(
                        accent.copy(alpha = 0.45f),
                        baseRadius,
                        center,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(Color.White, 5.2f, center)
                    drawCircle(Color(0xFF5DE6F2), 10f, center, style = Stroke(width = 2f))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(55.dp))
                    Text(
                        text = state.location,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.3f, %.3f",
                            state.selectedLocation.latitude,
                            state.selectedLocation.longitude
                        ),
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 8.sp
                    )
                }
            }

            Text(
                text = if (assessment.riskRadiusKm > 0) {
                    "Model-based local radius estimate around the selected point • not an official warning polygon"
                } else {
                    "No local severe-risk radius is estimated from the current 24-hour forecast"
                },
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 8.sp,
                lineHeight = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun StaySafeCard(assessment: SevereWeatherAssessment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xA40B2029),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFF69E1EC),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Safety guidance",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                assessment.guidance.take(3).forEachIndexed { index, guidance ->
                    SafetyItem(
                        icon = when (index) {
                            0 -> Icons.Outlined.Home
                            1 -> Icons.Outlined.FlashOn
                            else -> Icons.Outlined.WarningAmber
                        },
                        title = guidance.title,
                        detail = guidance.detail,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyItem(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(13.dp),
        color = Color(0x73122630),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFFC06A),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = detail,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 6.5.sp,
                lineHeight = 8.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AlertFooter(
    state: WeatherHomeUiState,
    assessment: SevereWeatherAssessment
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = Color(0xF4081821),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.07f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(if (assessment.offline) Color(0xFFFFB65F) else Color(0xFF55EAA5), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (assessment.offline) "Cached forecast risk" else "Forecast risk monitoring",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 8.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = state.updatedAt,
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 7.sp
                )
            }
            Text(
                text = assessment.sourceNote,
                color = Color.White.copy(alpha = 0.40f),
                fontSize = 7.sp,
                lineHeight = 9.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

private fun riskColor(level: SevereRiskLevel): Color = when (level) {
    SevereRiskLevel.SEVERE -> Color(0xFFFF5A42)
    SevereRiskLevel.HIGH -> Color(0xFFFF8A4A)
    SevereRiskLevel.MODERATE -> Color(0xFFFFC45A)
    SevereRiskLevel.ADVISORY -> Color(0xFF8DE1A9)
    SevereRiskLevel.NONE -> Color(0xFF64E2C2)
}

private fun formatOneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SevereAlertPreview() {
    RealWeather365Theme {
        SevereWeatherAlertScreen(state = WeatherHomeUiState(), onBack = {})
    }
}
