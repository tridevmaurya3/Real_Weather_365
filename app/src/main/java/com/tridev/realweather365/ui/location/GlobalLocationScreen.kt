package com.tridev.realweather365.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tridev.realweather365.data.location.LocationSource
import com.tridev.realweather365.data.location.WorldLocation
import com.tridev.realweather365.data.location.WorldLocationCatalog
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GlobalLocationScreen(
    selectedLocation: WorldLocation,
    onLocationSelected: (WorldLocation) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var isLocating by remember { mutableStateOf(false) }
    var locationMessage by remember {
        mutableStateOf("Choose any city or use your phone's current location.")
    }

    fun resolveDeviceLocation() {
        scope.launch {
            isLocating = true
            locationMessage = "Finding your current location…"
            val resolved = withContext(Dispatchers.IO) {
                resolveLastKnownLocation(context)
            }
            isLocating = false
            if (resolved != null) {
                onLocationSelected(resolved)
                locationMessage = "Current location selected. Weather data will follow this position."
            } else {
                locationMessage = "Location is unavailable. Turn on device location and try again."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            resolveDeviceLocation()
        } else {
            locationMessage = "Location permission is needed to detect your current position."
        }
    }

    val results = remember(query) { WorldLocationCatalog.search(query) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF061720),
                        Color(0xFF0A2230),
                        Color(0xFF071923),
                        Color(0xFF030D13)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LocationHeader(onBack = onBack)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                item {
                    SelectedLocationCard(selectedLocation = selectedLocation)
                }

                item {
                    CurrentLocationCard(
                        isLocating = isLocating,
                        message = locationMessage,
                        onUseCurrentLocation = {
                            if (hasLocationPermission(context)) {
                                resolveDeviceLocation()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = Color(0xFF73DCE8)
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Search city, region or country",
                                color = Color.White.copy(alpha = 0.42f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF66DCE8),
                            focusedBorderColor = Color(0xFF54D8E8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                            focusedContainerColor = Color(0x4011242D),
                            unfocusedContainerColor = Color(0x4011242D)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                }

                item {
                    Column {
                        Text(
                            text = if (query.isBlank()) "Featured worldwide locations" else "Search results",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "The live-data stage will replace this starter catalog with provider-backed worldwide place search.",
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                items(results, key = { it.id }) { location ->
                    LocationRow(
                        location = location,
                        selected = location.id == selectedLocation.id,
                        onClick = { onLocationSelected(location) }
                    )
                }

                if (results.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0x40101F28),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "No starter location found",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Worldwide online geocoding/search will be connected in the real weather data stage.",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xEE041119),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = Color(0xFF61DBE8),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Worldwide location foundation enabled",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationHeader(onBack: () -> Unit) {
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
                Text(
                    text = "Locations",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Worldwide weather foundation",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }

            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = null,
                tint = Color(0xFF64DDE8),
                modifier = Modifier
                    .padding(10.dp)
                    .size(21.dp)
            )
        }
    }
}

@Composable
private fun SelectedLocationCard(selectedLocation: WorldLocation) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xC20A2632),
        border = BorderStroke(1.dp, Color(0xFF62DDE8).copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF5DDCE8).copy(alpha = 0.13f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF64DDE8),
                        modifier = Modifier.padding(9.dp).size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.size(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selected location",
                        color = Color.White.copy(alpha = 0.48f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = selectedLocation.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedLocation.secondaryLabel,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = selectedLocation.countryCode,
                    color = Color(0xFF77E2EC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocationMetaChip(
                    title = "LAT / LON",
                    value = "%.4f, %.4f".format(Locale.US, selectedLocation.latitude, selectedLocation.longitude),
                    modifier = Modifier.weight(1f)
                )
                LocationMetaChip(
                    title = "TIME ZONE",
                    value = selectedLocation.timeZoneId,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LocationMetaChip(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CurrentLocationCard(
    isLocating: Boolean,
    message: String,
    onUseCurrentLocation: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = Color(0x80101F28),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = null,
                    tint = Color(0xFF6ADFE9),
                    modifier = Modifier.size(21.dp)
                )
                Spacer(modifier = Modifier.size(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use current location",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = message,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onUseCurrentLocation,
                enabled = !isLocating,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF18BFD1),
                    contentColor = Color(0xFF001A20)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.size(7.dp))
                Text(
                    text = if (isLocating) "Locating…" else "Use My Location",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LocationRow(
    location: WorldLocation,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xB0133340) else Color(0x70101F28),
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFF5EDCE8).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.045f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = if (selected) Color(0xFF62DFEA) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = location.secondaryLabel,
                    color = Color.White.copy(alpha = 0.46f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = location.timeZoneId.substringAfterLast('/').replace('_', ' '),
                        color = Color.White.copy(alpha = 0.44f),
                        fontSize = 9.sp
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFF62DFEA),
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

@Suppress("DEPRECATION")
private fun resolveLastKnownLocation(context: Context): WorldLocation? {
    if (!hasLocationPermission(context)) return null

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val location = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    ).mapNotNull { provider ->
        runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
    }.maxByOrNull { it.time } ?: return null

    val address = runCatching {
        if (Geocoder.isPresent()) {
            Geocoder(context, Locale.getDefault())
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
        } else {
            null
        }
    }.getOrNull()

    val city = address?.locality
        ?: address?.subAdminArea
        ?: address?.adminArea
        ?: "Current Location"
    val region = address?.adminArea.orEmpty()
    val country = address?.countryName ?: "Current Location"
    val countryCode = address?.countryCode ?: "--"

    return WorldLocation(
        id = "device-${"%.4f".format(Locale.US, location.latitude)}-${"%.4f".format(Locale.US, location.longitude)}",
        name = city,
        region = region,
        country = country,
        countryCode = countryCode,
        latitude = location.latitude,
        longitude = location.longitude,
        timeZoneId = TimeZone.getDefault().id,
        source = LocationSource.CURRENT_DEVICE
    )
}
