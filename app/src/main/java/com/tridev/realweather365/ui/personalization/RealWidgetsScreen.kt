package com.tridev.realweather365.ui.personalization

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import com.tridev.realweather365.widget.WeatherWidgetSize
import com.tridev.realweather365.widget.requestWeatherWidgetPin

private val WidgetAccent = Color(0xFF42DDE9)
private val WidgetGlass = Color(0xA20A2130)

@Composable
fun RealWidgetsScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Choose a size and add it directly to your Android home screen.") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF061722), Color(0xFF0A2633), Color(0xFF030D13))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WidgetHeader(onBack)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                item { Spacer(modifier = Modifier.height(3.dp)) }
                item {
                    Text(
                        text = "HOME SCREEN WIDGETS",
                        color = WidgetAccent,
                        fontSize = 10.sp,
                        letterSpacing = 1.3.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item {
                    Text(
                        text = "Live weather at a glance",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Text(
                        text = "Each widget opens Real Weather 365 when tapped and refreshes from the worldwide weather provider approximately every 30 minutes, subject to Android launcher and battery scheduling.",
                        color = Color.White.copy(alpha = 0.58f),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WidgetCard(
                            title = "Small",
                            sizeLabel = "2×2",
                            state = state,
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        WidgetCard(
                            title = "Medium",
                            sizeLabel = "4×2",
                            state = state,
                            compact = false,
                            modifier = Modifier.weight(1.25f)
                        )
                    }
                }

                item {
                    PinButton("Add Small Widget") {
                        status = pinStatus(requestWeatherWidgetPin(context, WeatherWidgetSize.SMALL), "Small")
                    }
                }
                item {
                    PinButton("Add Medium Widget") {
                        status = pinStatus(requestWeatherWidgetPin(context, WeatherWidgetSize.MEDIUM), "Medium")
                    }
                }

                item {
                    LargeWidgetCard(state)
                }
                item {
                    PinButton("Add Large Widget") {
                        status = pinStatus(requestWeatherWidgetPin(context, WeatherWidgetSize.LARGE), "Large")
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x5A0C2834),
                        border = BorderStroke(1.dp, WidgetAccent.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(13.dp),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xEE041119),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Text(
                    text = "REAL WEATHER 365  •  ANDROID WIDGET ENGINE",
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun WidgetHeader(onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xEE06151D)
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
                    .size(42.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Widgets", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Weather at a glance", color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
            }
            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.GridView, contentDescription = null, tint = WidgetAccent)
            }
        }
    }
}

@Composable
private fun WidgetCard(
    title: String,
    sizeLabel: String,
    state: WeatherHomeUiState,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(if (compact) 152.dp else 152.dp),
        shape = RoundedCornerShape(22.dp),
        color = WidgetGlass,
        border = BorderStroke(1.dp, WidgetAccent.copy(alpha = 0.24f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Text(sizeLabel, color = WidgetAccent, fontSize = 8.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(weatherEmoji(state.condition), fontSize = 22.sp)
            Text(
                text = "${state.temperature}°",
                color = Color.White,
                fontSize = if (compact) 28.sp else 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.location,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = state.condition,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LargeWidgetCard(state: WeatherHomeUiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(24.dp),
        color = WidgetGlass,
        border = BorderStroke(1.dp, WidgetAccent.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.location, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(state.condition, color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp)
                }
                Text(weatherEmoji(state.condition), fontSize = 28.sp)
                Spacer(modifier = Modifier.size(8.dp))
                Text("${state.temperature}°", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = "H:${state.high}°   L:${state.low}°",
                color = Color.White.copy(alpha = 0.64f),
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                state.hourly.take(4).forEach { hour ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(hour.time, color = Color.White.copy(alpha = 0.48f), fontSize = 8.sp)
                        Text("${hour.temperature}°", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Humidity", "${state.humidity}%")
                Metric("Wind", "${state.windSpeed} km/h")
                Metric("AQI", state.aqi?.toString() ?: "--")
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = WidgetAccent.copy(alpha = 0.62f), fontSize = 7.sp)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PinButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0x251AD4E5),
            contentColor = WidgetAccent
        ),
        border = BorderStroke(1.dp, WidgetAccent.copy(alpha = 0.72f))
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

private fun pinStatus(started: Boolean, size: String): String {
    return if (started) {
        "$size widget request sent to your launcher. Confirm the placement on the home screen."
    } else {
        "One-tap widget pinning is not supported by this launcher. Long-press the Android home screen → Widgets → Real Weather 365."
    }
}

private fun weatherEmoji(condition: String): String = when {
    condition.contains("thunder", true) || condition.contains("storm", true) -> "⚡"
    condition.contains("snow", true) -> "❄️"
    condition.contains("rain", true) || condition.contains("drizzle", true) -> "🌧️"
    condition.contains("night", true) -> "🌙"
    condition.contains("cloud", true) || condition.contains("overcast", true) -> "☁️"
    else -> "☀️"
}
