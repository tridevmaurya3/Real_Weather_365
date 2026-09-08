package com.tridev.realweather365.data.location

data class WorldLocation(
    val id: String,
    val name: String,
    val region: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val source: LocationSource = LocationSource.SEARCH
) {
    val secondaryLabel: String
        get() = listOf(region, country).filter { it.isNotBlank() }.joinToString(", ")
}

enum class LocationSource {
    CURRENT_DEVICE,
    SEARCH,
    SAVED
}

object WorldLocationCatalog {
    val chandauli = WorldLocation(
        id = "in-chandauli",
        name = "Chandauli",
        region = "Uttar Pradesh",
        country = "India",
        countryCode = "IN",
        latitude = 25.2587,
        longitude = 83.2684,
        timeZoneId = "Asia/Kolkata",
        source = LocationSource.SAVED
    )

    val featured: List<WorldLocation> = listOf(
        chandauli,
        WorldLocation("in-delhi", "New Delhi", "Delhi", "India", "IN", 28.6139, 77.2090, "Asia/Kolkata"),
        WorldLocation("in-mumbai", "Mumbai", "Maharashtra", "India", "IN", 19.0760, 72.8777, "Asia/Kolkata"),
        WorldLocation("gb-london", "London", "England", "United Kingdom", "GB", 51.5072, -0.1276, "Europe/London"),
        WorldLocation("us-new-york", "New York", "New York", "United States", "US", 40.7128, -74.0060, "America/New_York"),
        WorldLocation("jp-tokyo", "Tokyo", "Tokyo", "Japan", "JP", 35.6762, 139.6503, "Asia/Tokyo"),
        WorldLocation("ae-dubai", "Dubai", "Dubai", "United Arab Emirates", "AE", 25.2048, 55.2708, "Asia/Dubai"),
        WorldLocation("au-sydney", "Sydney", "New South Wales", "Australia", "AU", -33.8688, 151.2093, "Australia/Sydney"),
        WorldLocation("sg-singapore", "Singapore", "", "Singapore", "SG", 1.3521, 103.8198, "Asia/Singapore"),
        WorldLocation("fr-paris", "Paris", "Île-de-France", "France", "FR", 48.8566, 2.3522, "Europe/Paris"),
        WorldLocation("ca-toronto", "Toronto", "Ontario", "Canada", "CA", 43.6532, -79.3832, "America/Toronto"),
        WorldLocation("br-rio", "Rio de Janeiro", "Rio de Janeiro", "Brazil", "BR", -22.9068, -43.1729, "America/Sao_Paulo")
    )

    fun search(query: String): List<WorldLocation> {
        val normalized = query.trim()
        if (normalized.isBlank()) return featured
        return featured.filter { location ->
            location.name.contains(normalized, ignoreCase = true) ||
                location.region.contains(normalized, ignoreCase = true) ||
                location.country.contains(normalized, ignoreCase = true) ||
                location.countryCode.contains(normalized, ignoreCase = true)
        }
    }
}
