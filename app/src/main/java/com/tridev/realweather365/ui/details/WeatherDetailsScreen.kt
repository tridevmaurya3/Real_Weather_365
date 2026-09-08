package com.tridev.realweather365.ui.details

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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.theme.RealWeather365Theme
import kotlin.math.cos
import kotlin.math.sin

private data class DetailMetric(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun WeatherDetailsScreen(
    location: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071A24),
                        Color(0xFF0A2531),
                        Color(0xFF061820),
                        Color(0xFF041015)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailsHeader(location = location, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                WindCard()
                Spacer(modifier = Modifier.height(10.dp))
                DetailsGrid()
                Spacer(modifier = Modifier.height(10.dp))
                SunMoonCard()
                Spacer(modifier = Modifier.height(12.dp))
            }

            DetailsFooter()
        }
    }
}

@Composable
private fun DetailsHeader(location: String, onBack: () -> Unit) {
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
                        tint = Color(0xFF71E3EF),
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
                    text = "Weather Details",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 9.sp,
                    letterSpacing = 0.45.sp
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
private fun WindCard() {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WindCompass(modifier = Modifier.size(118.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = null,
                        tint = Color(0xFF61E7F1),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Wind",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Text(
                    text = "18 km/h",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "E • Gusts 27 km/h",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Steady easterly breeze",
                    color = Color(0xFF74E4EF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WindCompass(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension * 0.43f
        drawCircle(Color.White.copy(alpha = 0.08f), radius = r, center = c)
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = r,
            center = c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f)
        )

        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30.0) - 90.0)
            val inner = if (i % 3 == 0) r * 0.78f else r * 0.86f
            val start = Offset(
                x = c.x + (cos(angle) * inner).toFloat(),
                y = c.y + (sin(angle) * inner).toFloat()
            )
            val end = Offset(
                x = c.x + (cos(angle) * r).toFloat(),
                y = c.y + (sin(angle) * r).toFloat()
            )
            drawLine(
                color = Color.White.copy(alpha = if (i % 3 == 0) 0.42f else 0.20f),
                start = start,
                end = end,
                strokeWidth = if (i % 3 == 0) 2.0f else 1.0f,
                cap = StrokeCap.Round
            )
        }

        val path = Path().apply {
            moveTo(c.x + r * 0.62f, c.y)
            lineTo(c.x - r * 0.12f, c.y - r * 0.15f)
            lineTo(c.x - r * 0.12f, c.y + r * 0.15f)
            close()
        }
        drawPath(path, Color(0xFF61E7F1))
        drawCircle(Color.White, radius = r * 0.07f, center = c)
    }
}

@Composable
private fun DetailsGrid() {
    val metrics = listOf(
        DetailMetric("Pressure", "1008 hPa", "Steady", Icons.Outlined.Compress, Color(0xFF77D9FF)),
        DetailMetric("Visibility", "8 km", "Good", Icons.Outlined.Visibility, Color(0xFF75E8D4)),
        DetailMetric("Humidity", "78%", "High", Icons.Outlined.WaterDrop, Color(0xFF5FCBFF)),
        DetailMetric("Dew Point", "24°", "Muggy", Icons.Outlined.Thermostat, Color(0xFF84D6FF)),
        DetailMetric("UV Index", "6", "High", Icons.Outlined.WbSunny, Color(0xFFFFD45A)),
        DetailMetric("Cloud Cover", "72%", "Mostly cloudy", Icons.Outlined.Visibility, Color(0xFFB1C7D3))
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(3).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowMetrics.forEach { metric ->
                    MetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(metric: DetailMetric, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = Color(0xA90A202A),
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = metric.accent,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = metric.title,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 8.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = metric.value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = metric.subtitle,
                color = metric.accent.copy(alpha = 0.84f),
                fontSize = 7.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SunMoonCard() {
    GlassCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Sun & Moon",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Today's light and lunar cycle",
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 8.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SunMoonMetric(
                    icon = Icons.Outlined.WbSunny,
                    title = "Sunrise",
                    value = "5:28 AM",
                    accent = Color(0xFFFFD462),
                    modifier = Modifier.weight(1f)
                )
                SunMoonMetric(
                    icon = Icons.Outlined.NightsStay,
                    title = "Sunset",
                    value = "6:52 PM",
                    accent = Color(0xFFFF9B6A),
                    modifier = Modifier.weight(1f)
                )
                SunMoonMetric(
                    icon = Icons.Outlined.NightsStay,
                    title = "Moon Phase",
                    value = "Waning Gibbous",
                    accent = Color(0xFFC8D8FF),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SunMoonMetric(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.52f),
            fontSize = 7.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = if (value.length > 10) 8.sp else 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xA908202A),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.13f)),
        tonalElevation = 0.dp,
        content = content
    )
}

@Composable
private fun DetailsFooter() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xF006171F),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Updated a few moments ago",
                color = Color.White.copy(alpha = 0.46f),
                fontSize = 8.sp
            )
            Text(
                text = "REAL WEATHER 365",
                color = Color(0xFF72E5EF),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF06131A)
@Composable
private fun WeatherDetailsPreview() {
    RealWeather365Theme {
        WeatherDetailsScreen(location = "Chandauli", onBack = {})
    }
}
