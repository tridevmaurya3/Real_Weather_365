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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.tridev.realweather365.data.location.OpenMeteoGeocodingRepository
import com.tridev.realweather365.data.location.SavedCitiesStore
import com.tridev.realweather365.data.location.WorldLocation
import com.tridev.realweather365.data.location.WorldLocationCatalog
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAB_SEARCH = "search"
private const val TAB_SAVED = "saved"

@Composable
fun GlobalLocationScreen(
    selectedLocation: WorldLocation,
    onLocationSelected: (WorldLocation) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geocodingRepository = remember { OpenMeteoGeocodingRepository() }
    val savedCitiesStore = remember { SavedCitiesStore(context) }

    var activeTab by rememberSaveable { mutableStateOf(TAB_SEARCH) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(WorldLocationCatalog.featured) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var savedCities by remember { mutableStateOf(savedCitiesStore.load()) }
    var isLocating by remember { mutableStateOf(false) }
    var locationMessage by remember {
        mutableStateOf("Use GPS for your current position or search anywhere in the world.")
    }

    fun resolveDeviceLocation() {
        scope.launch {
            isLocating = true
            locationMessage = "Finding your current location…"
            val resolved = withContext(Dispatchers.IO) { resolveLastKnownLocation(context) }
            isLocating = false
            if (resolved != null) {
                onLocationSelected(resolved)
            } else {
                locationMessage = "Location unavailable. Turn on device location and try again."
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

    LaunchedEffect(query, activeTab) {
        if (activeTab != TAB_SEARCH) return@LaunchedEffect
        val cleaned = query.trim()
        searchError = null

        if (cleaned.isBlank()) {
            isSearching = false
            searchResults = WorldLocationCatalog.featured
            return@LaunchedEffect
        }
        if (cleaned.length < 2) {
            isSearching = false
            searchResults = emptyList()
            return@LaunchedEffect
        }

        delay(350)
        isSearching = true
        try {
            searchResults = geocodingRepository.search(cleaned)
            if (searchResults.isEmpty()) {
                searchError = "No matching place found. Try a city, district, state or country name."
            }
        } catch (_: Exception) {
            searchResults = WorldLocationCatalog.search(cleaned)
            searchError = if (searchResults.isEmpty()) {
                "Online location search is unavailable. Check your internet connection and try again."
            } else {
                "Online search is temporarily unavailable. Showing offline suggestions."
            }
        } finally {
            isSearching = false
        }
    }

    val savedIds = savedCities.map { it.id }.toSet()

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
                item { SelectedLocationCard(selectedLocation) }
                item {
                    LocationTabs(
                        activeTab = activeTab,
                        savedCount = savedCities.size,
                        onTabSelected = { activeTab = it }
                    )
                }

                if (activeTab == TAB_SEARCH) {
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
                        SearchField(
                            query = query,
                            onQueryChange = { query = it },
                            isSearching = isSearching
                        )
                    }

                    item {
                        SectionHeading(
                            title = when {
                                query.isBlank() -> "Popular worldwide"
                                query.trim().length < 2 -> "Find a location"
                                else -> "Search results"
                            },
                            subtitle = when {
                                query.isBlank() -> "Tap any city to load its live weather, or save it to My Cities."
                                query.trim().length < 2 -> "Type at least 2 characters to search cities and places worldwide."
                                else -> "Online global geocoding results"
                            }
                        )
                    }

                    if (searchError != null) {
                        item { StatusMessage(searchError.orEmpty()) }
                    }

                    items(searchResults, key = { it.id }) { location ->
                        LocationRow(
                            location = location,
                            selected = location.id == selectedLocation.id,
                            saved = location.id in savedIds,
                            onClick = { onLocationSelected(location) },
                            onSaveToggle = {
                                savedCities = savedCitiesStore.toggle(location)
                            }
                        )
                    }
                } else {
                    item {
                        SectionHeading(
                            title = "My Cities",
                            subtitle = "Saved on this device for fast switching between your regular locations."
                        )
                    }

                    if (savedCities.isEmpty()) {
                        item {
                            EmptySavedCitiesCard(
                                onFindCities = { activeTab = TAB_SEARCH }
                            )
                        }
                    } else {
                        items(savedCities, key = { it.id }) { location ->
                            SavedCityRow(
                                location = location,
                                selected = location.id == selectedLocation.id,
                                onClick = { onLocationSelected(location) },
                                onRemove = {
                                    savedCities = savedCitiesStore.remove(location.id)
                                }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            LocationFooter(savedCount = savedCities.size)
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
                Text("Find a Location", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Worldwide weather", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
            }

            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = null,
                tint = Color(0xFF64DDE8),
                modifier = Modifier.padding(10.dp).size(21.dp)
            )
        }
    }
}

@Composable
private fun LocationTabs(
    activeTab: String,
    savedCount: Int,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x70081922),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            TabButton(
                title = "Search",
                selected = activeTab == TAB_SEARCH,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(TAB_SEARCH) }
            )
            TabButton(
                title = "My Cities ($savedCount)",
                selected = activeTab == TAB_SAVED,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(TAB_SAVED) }
            )
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF123846) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, Color(0xFF5EDCE8).copy(alpha = 0.30f)) else null
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (selected) Color(0xFF7EE5EE) else Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF73DCE8))
        },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF6DE1EB)
                )
            }
        },
        placeholder = {
            Text("Search city, district, region or country", color = Color.White.copy(alpha = 0.42f))
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

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            subtitle,
            color = Color.White.copy(alpha = 0.48f),
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun StatusMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x6614252D),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
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
                Surface(shape = CircleShape, color = Color(0xFF5DDCE8).copy(alpha = 0.13f)) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF64DDE8),
                        modifier = Modifier.padding(9.dp).size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.size(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current weather location", color = Color.White.copy(alpha = 0.48f), fontSize = 10.sp)
                    Text(
                        selectedLocation.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        selectedLocation.secondaryLabel,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    selectedLocation.countryCode,
                    color = Color(0xFF77E2EC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LocationMetaChip(
                    "LAT / LON",
                    "%.4f, %.4f".format(Locale.US, selectedLocation.latitude, selectedLocation.longitude),
                    Modifier.weight(1f)
                )
                LocationMetaChip(
                    "TIME ZONE",
                    selectedLocation.timeZoneId,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LocationMetaChip(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color.White.copy(alpha = 0.045f), shape = RoundedCornerShape(13.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            Text(
                value,
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
                Icon(Icons.Outlined.MyLocation, null, tint = Color(0xFF6ADFE9), modifier = Modifier.size(21.dp))
                Spacer(modifier = Modifier.size(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use current location", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(message, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, lineHeight = 14.sp)
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
                Icon(Icons.Outlined.MyLocation, null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.size(7.dp))
                Text(if (isLocating) "Locating…" else "Use My Location", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationRow(
    location: WorldLocation,
    selected: Boolean,
    saved: Boolean,
    onClick: () -> Unit,
    onSaveToggle: () -> Unit
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
            modifier = Modifier.padding(start = 13.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.045f)) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = if (selected) Color(0xFF62DFEA) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            LocationText(location, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF62DFEA),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onSaveToggle) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (saved) "Remove from My Cities" else "Save to My Cities",
                    tint = if (saved) Color(0xFFFFD45A) else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedCityRow(
    location: WorldLocation,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
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
            modifier = Modifier.padding(start = 13.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color(0x22FFD45A)) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD45A),
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            LocationText(location, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF62DFEA),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove city",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LocationText(location: WorldLocation, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(location.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            location.secondaryLabel,
            color = Color.White.copy(alpha = 0.46f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.30f),
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                location.timeZoneId.substringAfterLast('/').replace('_', ' '),
                color = Color.White.copy(alpha = 0.36f),
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun EmptySavedCitiesCard(onFindCities: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x70101F28),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.StarBorder, null, tint = Color(0xFFFFD45A), modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No saved cities yet", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                "Save cities from worldwide search and they will appear here.",
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onFindCities,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18BFD1), contentColor = Color(0xFF001A20))
            ) {
                Text("Find Cities", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationFooter(savedCount: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xEE041119),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Public, null, tint = Color(0xFF61DBE8), modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Column {
                Text("Online worldwide search enabled", color = Color.White.copy(alpha = 0.76f), fontSize = 11.sp)
                Text("$savedCount cities saved locally", color = Color.White.copy(alpha = 0.42f), fontSize = 9.sp)
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
        } else null
    }.getOrNull()

    val city = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "Current Location"
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
