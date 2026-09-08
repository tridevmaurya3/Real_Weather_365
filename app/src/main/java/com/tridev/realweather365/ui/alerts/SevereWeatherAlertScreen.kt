package com.tridev.realweather365.ui.alerts

import android.graphics.Paint
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.theme.RealWeather365Theme
import kotlin.math.sin

@Composable
fun SevereWeatherAlertScreen(
    location: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "alert-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alert-pulse-progress"
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
            AlertHeader(location = location, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                SevereAlertCard()
                Spacer(modifier = Modifier.height(10.dp))
                AlertTimingCard()
                Spacer(modifier = Modifier.height(10.dp))
                AlertMapCard(location = location, pulse = pulse)
                Spacer(modifier = Modifier.height(10.dp))
                StaySafeCard()
                Spacer(modifier = Modifier.height(12.dp))
            }

            AlertFooter()
        }
    }
}

@Composable
private fun AlertHeader(location: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xF2071720),
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
                    text = "Severe Weather Alert",
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
private fun SevereAlertCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF5B1C14),
        border = BorderStroke(0.8.dp, Color(0xFFFF8065).copy(alpha = 0.55f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(0x33FF8C67)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFFB26E),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Severe Thunderstorm",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFF603D)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Heavy rain, strong winds and lightning expected in the selected area.",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0x32000000),
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
                        text = "Lightning risk: High",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Wind 45–65 km/h",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertTimingCard() {
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
                    text = "Expected window",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 8.sp
                )
                Text(
                    text = "Today, 3:00 PM – 7:00 PM",
                    color = Color.White,
                    fontSize = 11.sp,
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
                    text = "Moderate to High",
                    color = Color(0xFFFFB263),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AlertMapCard(location: String, pulse: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xC8071A24),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Affected area",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "LIVE RISK MAP",
                    color = Color(0xFFFF9472),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF102B34), Color(0xFF0A2029), Color(0xFF081922))
                    )
                )

                repeat(6) { index ->
                    val y = h * (0.12f + index * 0.15f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.035f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }
                repeat(5) { index ->
                    val x = w * (0.12f + index * 0.19f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                }

                val riskCenter = Offset(w * 0.55f, h * 0.48f)
                val pulseRadius = 42f + pulse * 32f
                drawCircle(
                    color = Color(0xFFFF542F).copy(alpha = (0.23f * (1f - pulse)).coerceAtLeast(0.04f)),
                    radius = pulseRadius,
                    center = riskCenter
                )
                drawCircle(
                    color = Color(0xFFFF7A3C).copy(alpha = 0.26f),
                    radius = 54f,
                    center = riskCenter
                )
                drawCircle(
                    color = Color(0xFFFFC04E).copy(alpha = 0.20f),
                    radius = 82f,
                    center = riskCenter
                )

                val stormCells = listOf(
                    Offset(w * 0.24f, h * 0.28f) to 28f,
                    Offset(w * 0.39f, h * 0.36f) to 22f,
                    Offset(w * 0.69f, h * 0.35f) to 34f,
                    Offset(w * 0.77f, h * 0.61f) to 26f,
                    Offset(w * 0.31f, h * 0.68f) to 30f
                )
                stormCells.forEachIndexed { index, pair ->
                    val wave = 0.88f + 0.12f * sin((pulse + index * 0.17f) * 6.283f)
                    drawCircle(
                        color = if (index % 2 == 0) Color(0xFFEF5A34).copy(alpha = 0.35f)
                        else Color(0xFFFFB341).copy(alpha = 0.30f),
                        radius = pair.second * wave,
                        center = pair.first
                    )
                }

                drawCircle(
                    color = Color.White,
                    radius = 5.3f,
                    center = riskCenter
                )
                drawCircle(
                    color = Color(0xFF5DE6F2),
                    radius = 9.5f,
                    center = riskCenter,
                    style = Stroke(width = 2f)
                )

                val textPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                    textSize = 23f
                    textAlign = Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(location, riskCenter.x, riskCenter.y - 17f, textPaint)

                val secondaryPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(180, 220, 238, 242)
                    textSize = 17f
                    textAlign = Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText("Varanasi", w * 0.25f, h * 0.18f, secondaryPaint)
                drawContext.canvas.nativeCanvas.drawText("Mughalsarai", w * 0.28f, h * 0.55f, secondaryPaint)
                drawContext.canvas.nativeCanvas.drawText("Ghazipur", w * 0.77f, h * 0.22f, secondaryPaint)
                drawContext.canvas.nativeCanvas.drawText("Mirzapur", w * 0.68f, h * 0.78f, secondaryPaint)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RiskLegendDot(Color(0xFFFFC04E), "Moderate")
                RiskLegendDot(Color(0xFFFF7A3C), "High")
                RiskLegendDot(Color(0xFFFF542F), "Severe")
            }
        }
    }
}

@Composable
private fun RiskLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.67f), fontSize = 8.sp)
    }
}

@Composable
private fun StaySafeCard() {
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
                    text = "Stay Safe",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SafetyItem(
                    icon = Icons.Outlined.Home,
                    title = "Stay indoors",
                    detail = "Avoid open areas",
                    modifier = Modifier.weight(1f)
                )
                SafetyItem(
                    icon = Icons.Outlined.FlashOn,
                    title = "Lightning",
                    detail = "Keep away from trees",
                    modifier = Modifier.weight(1f)
                )
                SafetyItem(
                    icon = Icons.Outlined.WarningAmber,
                    title = "Travel",
                    detail = "Avoid flooded roads",
                    modifier = Modifier.weight(1f)
                )
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
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(13.dp),
        color = Color(0x73122630),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
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
                fontSize = 7.sp,
                lineHeight = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AlertFooter() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xF4081821),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.07f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Color(0xFFFF6545), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Alert monitoring active",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 8.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Updated moments ago",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 8.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SevereAlertPreview() {
    RealWeather365Theme {
        SevereWeatherAlertScreen(location = "Chandauli", onBack = {})
    }
}
