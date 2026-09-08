package com.tridev.realweather365.ui.alerts

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridev.realweather365.data.alerts.OfficialAlertFeed
import com.tridev.realweather365.data.alerts.OfficialAlertFeedStatus
import com.tridev.realweather365.data.alerts.OfficialWeatherAlert
import com.tridev.realweather365.data.alerts.OfficialWeatherAlertRepository
import com.tridev.realweather365.ui.home.WeatherHomeUiState
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface OfficialAlertsUiState {
    data object Loading : OfficialAlertsUiState
    data class Ready(val feed: OfficialAlertFeed) : OfficialAlertsUiState
}

@Composable
fun OfficialWeatherAlertsScreen(
    state: WeatherHomeUiState,
    onBack: () -> Unit,
    onOpenForecastRisk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { OfficialWeatherAlertRepository() }
    var refreshToken by remember { mutableStateOf(0) }
    var officialState by remember(state.selectedLocation.id) {
        mutableStateOf<OfficialAlertsUiState>(OfficialAlertsUiState.Loading)
    }
    val assessment = remember(state) { SevereWeatherRiskEngine.assess(state) }

    LaunchedEffect(state.selectedLocation.id, refreshToken) {
        officialState = OfficialAlertsUiState.Loading
        officialState = OfficialAlertsUiState.Ready(repository.load(state.selectedLocation))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF06161E),
                        Color(0xFF0A202A),
                        Color(0xFF071720),
                        Color(0xFF041017)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OfficialAlertHeader(
                location = state.location,
                onBack = onBack,
                onRefresh = { refreshToken++ }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                OfficialFeedSection(
                    state = state,
                    uiState = officialState,
                    onRefresh = { refreshToken++ }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ForecastRiskSummaryCard(
                    state = state,
                    assessment = assessment,
                    onOpen = onOpenForecastRisk
                )
                Spacer(modifier = Modifier.height(12.dp))
                SourceSeparationCard()
                Spacer(modifier = Modifier.height(14.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = Color(0xF4081821),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.07f))
            ) {
                Text(
                    text = "Official warnings and app-calculated forecast risk are kept separate by design.",
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 7.5.sp,
                    lineHeight = 10.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun OfficialAlertHeader(
    location: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
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
                Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFF75E5F0), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(location, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "Official Alerts + Forecast Risk",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 9.sp,
                    letterSpacing = 0.35.sp
                )
            }

            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Refresh, "Refresh official alerts", tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun OfficialFeedSection(
    state: WeatherHomeUiState,
    uiState: OfficialAlertsUiState,
    onRefresh: () -> Unit
) {
    when (uiState) {
        OfficialAlertsUiState.Loading -> OfficialLoadingCard()
        is OfficialAlertsUiState.Ready -> {
            val feed = uiState.feed
            when (feed.status) {
                OfficialAlertFeedStatus.UNSUPPORTED -> UnsupportedOfficialFeedCard(state = state)
                OfficialAlertFeedStatus.ERROR -> OfficialFeedErrorCard(feed = feed, onRefresh = onRefresh)
                OfficialAlertFeedStatus.AVAILABLE -> {
                    if (feed.alerts.isEmpty()) {
                        NoActiveOfficialAlertCard(feed = feed)
                    } else {
                        ActiveOfficialAlertsCard(feed = feed)
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficialLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB50B202A),
        border = BorderStroke(0.8.dp, Color(0xFF54DDEA).copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(25.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF68E4EF)
            )
            Column {
                Text("Checking official warning feed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Official source is checked independently from the app forecast model.", color = Color.White.copy(alpha = 0.55f), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun UnsupportedOfficialFeedCard(state: WeatherHomeUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xA80C222B),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(39.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.07f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Info, null, tint = Color(0xFF79DCE8), modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Official feed not connected for ${state.selectedLocation.country}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("No official warning is being invented or inferred for this country.", color = Color.White.copy(alpha = 0.62f), fontSize = 8.5.sp, lineHeight = 12.sp)
                }
            }
            Text(
                text = "Stage 26 connects verified NOAA/NWS alerts for supported U.S. locations. The provider interface is ready for additional authoritative national feeds as they are verified.",
                color = Color.White.copy(alpha = 0.48f),
                fontSize = 8.sp,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
private fun OfficialFeedErrorCard(feed: OfficialAlertFeed, onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB53A2419),
        border = BorderStroke(0.8.dp, Color(0xFFFFB15F).copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudOff, null, tint = Color(0xFFFFB768), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Official alerts could not be verified", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(feed.providerName ?: "Official provider", color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp)
                }
            }
            Text(
                text = feed.errorMessage ?: "The official warning service is temporarily unavailable.",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 8.5.sp,
                lineHeight = 12.sp
            )
            Surface(
                modifier = Modifier.clickable(onClick = onRefresh),
                shape = RoundedCornerShape(10.dp),
                color = Color(0x33FFFFFF),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Text("Retry official feed", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun NoActiveOfficialAlertCard(feed: OfficialAlertFeed) {
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB5123A32),
        border = BorderStroke(0.9.dp, Color(0xFF61E1B9).copy(alpha = 0.40f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = Color(0x2261E1B9)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Verified, null, tint = Color(0xFF6EE8C3), modifier = Modifier.size(23.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("No active official alert returned", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(feed.providerName ?: "Official source", color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp)
                }
            }
            Text(
                text = "The official point-based feed currently returns no active warning for this location. This does not replace normal local safety guidance or the separate forecast-risk estimate below.",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 8.5.sp,
                lineHeight = 12.sp
            )
            feed.providerHomepage?.let { url ->
                Row(
                    modifier = Modifier.clickable { uriHandler.openUri(url) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.OpenInNew, null, tint = Color(0xFF78E5EF), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Open official provider", color = Color(0xFF78E5EF), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ActiveOfficialAlertsCard(feed: OfficialAlertFeed) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xB3412018),
            border = BorderStroke(0.9.dp, Color(0xFFFF8060).copy(alpha = 0.48f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Verified, null, tint = Color(0xFFFFB06A), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${feed.alerts.size} active official alert${if (feed.alerts.size == 1) "" else "s"}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(feed.providerName ?: "Official government source", color = Color.White.copy(alpha = 0.62f), fontSize = 8.sp)
                }
                Surface(shape = RoundedCornerShape(50), color = Color(0xFFFF714E)) {
                    Text("OFFICIAL", color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        feed.alerts.take(5).forEach { alert ->
            OfficialAlertCard(alert = alert)
        }

        if (feed.alerts.size > 5) {
            Text(
                text = "+ ${feed.alerts.size - 5} additional official alerts in the source feed",
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 8.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        feed.providerHomepage?.let { url ->
            Row(
                modifier = Modifier.clickable { uriHandler.openUri(url) }.padding(horizontal = 3.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.OpenInNew, null, tint = Color(0xFF78E5EF), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Open ${feed.providerName ?: "official provider"}", color = Color(0xFF78E5EF), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OfficialAlertCard(alert: OfficialWeatherAlert) {
    val severityColor = officialSeverityColor(alert.severity)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = Color(0xB70A202A),
        border = BorderStroke(0.8.dp, severityColor.copy(alpha = 0.48f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = severityColor.copy(alpha = 0.14f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.WarningAmber, null, tint = severityColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(alert.event, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(alert.headline, color = Color.White.copy(alpha = 0.72f), fontSize = 8.5.sp, lineHeight = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(50), color = severityColor.copy(alpha = 0.18f), border = BorderStroke(0.5.dp, severityColor.copy(alpha = 0.45f))) {
                    Text(alert.severity.uppercase(Locale.US), color = severityColor, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OfficialMetaChip("Urgency", alert.urgency, Modifier.weight(1f))
                OfficialMetaChip("Certainty", alert.certainty, Modifier.weight(1f))
            }

            if (alert.areaDescription.isNotBlank()) {
                Text("Area • ${alert.areaDescription}", color = Color.White.copy(alpha = 0.58f), fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = "Effective ${formatOfficialTime(alert.effective.ifBlank { alert.onset })} • Expires ${formatOfficialTime(alert.expires)}",
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 7.5.sp
            )
            Text("Issued by ${alert.senderName}", color = Color.White.copy(alpha = 0.44f), fontSize = 7.5.sp)

            if (alert.instruction.isNotBlank()) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0x24FFFFFF)) {
                    Text(
                        text = alert.instruction,
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficialMetaChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0x25FFFFFF),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.40f), fontSize = 7.sp)
            Text(value, color = Color.White.copy(alpha = 0.82f), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ForecastRiskSummaryCard(
    state: WeatherHomeUiState,
    assessment: SevereWeatherAssessment,
    onOpen: () -> Unit
) {
    val accent = when (assessment.level) {
        SevereRiskLevel.SEVERE -> Color(0xFFFF5A42)
        SevereRiskLevel.HIGH -> Color(0xFFFF8A4A)
        SevereRiskLevel.MODERATE -> Color(0xFFFFC45A)
        SevereRiskLevel.ADVISORY -> Color(0xFF8DE1A9)
        SevereRiskLevel.NONE -> Color(0xFF64E2C2)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB50B202A),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.38f))
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, tint = accent, modifier = Modifier.size(21.dp))
                Spacer(modifier = Modifier.width(7.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("App forecast risk", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("MODEL-BASED • NOT AN OFFICIAL WARNING", color = Color.White.copy(alpha = 0.43f), fontSize = 7.sp, letterSpacing = 0.25.sp)
                }
                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.16f)) {
                    Text(assessment.statusLabel, color = accent, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }

            Text(assessment.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(assessment.summary, color = Color.White.copy(alpha = 0.62f), fontSize = 8.5.sp, lineHeight = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OfficialMetaChip("Risk window", assessment.windowLabel, Modifier.weight(1f))
                OfficialMetaChip("Peak gust", "${assessment.peakGust} ${state.windUnit}", Modifier.weight(1f))
            }

            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF123745),
                border = BorderStroke(0.6.dp, Color(0xFF5BE1ED).copy(alpha = 0.38f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Open detailed 24-hour forecast risk intelligence", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.OpenInNew, null, tint = Color(0xFF70E5EF), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SourceSeparationCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x7D0B2029),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.09f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Source integrity", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "OFFICIAL means a warning returned directly by a connected authoritative government feed. APP FORECAST RISK is calculated from weather-model inputs and never receives an official badge.",
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 8.sp,
                lineHeight = 11.sp
            )
        }
    }
}

private fun officialSeverityColor(severity: String): Color = when (severity.lowercase(Locale.US)) {
    "extreme" -> Color(0xFFFF4E3B)
    "severe" -> Color(0xFFFF754A)
    "moderate" -> Color(0xFFFFB657)
    "minor" -> Color(0xFF80DFA7)
    else -> Color(0xFF7EDDE8)
}

private fun formatOfficialTime(value: String): String {
    if (value.isBlank()) return "--"
    return runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH))
    }.getOrElse { value.take(16).replace('T', ' ') }
}
