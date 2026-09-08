package com.tridev.realweather365.ui.airquality

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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.theme.RealWeather365Theme

private data class Pollutant(
    val label: String,
    val value: Int,
    val unit: String,
    val level: String,
    val tint: Color
)

private val pollutants = listOf(
    Pollutant("PM2.5", 56, "µg/m³", "Elevated", Color(0xFFFFD65A)),
    Pollutant("PM10", 78, "µg/m³", "Elevated", Color(0xFFFFB84D)),
    Pollutant("NO₂", 22, "µg/m³", "Good", Color(0xFF77E59A)),
    Pollutant("O₃", 96, "µg/m³", "Moderate", Color(0xFFB8D94C))
)

@Composable
fun AirQualityScreen(
    location: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF071923),
                        Color(0xFF0B2734),
                        Color(0xFF071821),
                        Color(0xFF041017)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AirQualityHeader(location = location, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                AirQualityHeroCard()
                Spacer(modifier = Modifier.height(10.dp))
                HealthMessageCard()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Air pollutants",
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(7.dp))
                PollutantGrid()
                Spacer(modifier = Modifier.height(10.dp))
                ExposureCard()
                Spacer(modifier = Modifier.height(8.dp))
            }

            AirQualityFooter()
        }
    }
}

@Composable
private fun AirQualityHeader(location: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xEE071720),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack),
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
                    text = "Air Quality",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 9.sp,
                    letterSpacing = 0.4.sp
                )
            }

            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    tint = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun AirQualityHeroCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xA0081B25),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.14f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AqiGauge(value = 78, modifier = Modifier.size(154.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Air quality is acceptable",
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "For most people, outdoor activity is fine. Sensitive people may prefer shorter periods of heavy exertion.",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x3327D17F),
                    border = BorderStroke(0.6.dp, Color(0xFF64E79A).copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Eco,
                            contentDescription = null,
                            tint = Color(0xFF77E59A),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Enjoy regular activities",
                            color = Color(0xFFB8F3C9),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AqiGauge(value: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 13f
            val start = 145f
            val total = 250f
            val segmentGap = 4f
            val colors = listOf(
                Color(0xFF59D872),
                Color(0xFFC9D83B),
                Color(0xFFFFC23D),
                Color(0xFFFF7043),
                Color(0xFFD94C78)
            )
            val sweep = total / colors.size

            colors.forEachIndexed { index, color ->
                drawArc(
                    color = color.copy(alpha = 0.24f),
                    startAngle = start + index * sweep,
                    sweepAngle = sweep - segmentGap,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            val progress = (value / 200f).coerceIn(0f, 1f)
            val activeSweep = total * progress
            var remaining = activeSweep

            colors.forEachIndexed { index, color ->
                if (remaining <= 0f) return@forEachIndexed
                val segmentSweep = minOf(sweep - segmentGap, remaining)
                drawArc(
                    color = color,
                    startAngle = start + index * sweep,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                remaining -= sweep
            }

            val markerAngle = Math.toRadians((start + activeSweep).toDouble())
            val radius = size.minDimension / 2f - stroke / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val marker = Offset(
                x = center.x + kotlin.math.cos(markerAngle).toFloat() * radius,
                y = center.y + kotlin.math.sin(markerAngle).toFloat() * radius
            )
            drawCircle(Color.White, radius = 4.6f, center = marker)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = 38.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "Moderate",
                color = Color(0xFFFFD65A),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "AQI",
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun HealthMessageCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x94081922),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0x263CE7F3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFF6FE4EE),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today’s air-quality guidance",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Normal outdoor plans are suitable. If you are unusually sensitive to pollution, watch for discomfort during prolonged activity.",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 8.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PollutantGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        pollutants.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                row.forEach { pollutant ->
                    PollutantCard(pollutant = pollutant, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PollutantCard(pollutant: Pollutant, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x9A071922),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pollutant.label,
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 9.sp
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(pollutant.tint, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pollutant.value.toString(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = pollutant.unit,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 7.sp
            )
            Spacer(modifier = Modifier.height(7.dp))
            AirQualityBar(value = pollutant.value, tint = pollutant.tint)
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = pollutant.level,
                color = pollutant.tint,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AirQualityBar(value: Int, tint: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
    ) {
        val y = size.height / 2f
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(0f, y),
            end = Offset(size.width * (value / 150f).coerceIn(0f, 1f), y),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ExposureCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x90071922),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF70E0EB),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Air quality outlook",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Moderate conditions are expected through the next few hours, with gradual improvement later in the day.",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 8.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun AirQualityFooter() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xF1081820),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Text(
            text = "AIR QUALITY • UPDATED 9:41 AM",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            color = Color.White.copy(alpha = 0.48f),
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 0.7.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AirQualityPreview() {
    RealWeather365Theme {
        AirQualityScreen(location = "Chandauli", onBack = {})
    }
}
