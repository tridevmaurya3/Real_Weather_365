package com.tridev.realweather365.data.radar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.tridev.realweather365.data.location.WorldLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

private const val WEATHER_MAPS_URL = "https://api.rainviewer.com/public/weather-maps.json"
private const val USER_AGENT = "RealWeather365/0.23 (Android; https://github.com/tridevmaurya3/Real_Weather_365)"
private const val RADAR_CACHE_MAX_AGE_MS = 12L * 60L * 60L * 1000L
private const val COVERAGE_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L

/**
 * Metadata for a RainViewer composite radar frame.
 */
data class RadarFrameMeta(
    val timeEpochSeconds: Long,
    val path: String,
    val isNowcast: Boolean
)

/**
 * A renderable transparent radar tile centered on the selected world location.
 */
data class RadarRenderableFrame(
    val meta: RadarFrameMeta,
    val bitmap: Bitmap,
    val hasPrecipitationEcho: Boolean
)

data class RadarMapBundle(
    val frames: List<RadarRenderableFrame>,
    val generatedEpochSeconds: Long,
    val zoom: Int,
    val coverageAvailable: Boolean?,
    val errorMessage: String? = null
)

/**
 * Stage 23 real radar provider.
 *
 * The public RainViewer manifest provides timestamped/hash-based composite radar paths. For each
 * selected location we request a transparent coordinate tile centered on its latitude/longitude.
 * Images are cached locally by their immutable frame URL so replaying the same timeline does not
 * repeatedly hit the provider.
 */
class RainViewerRadarRepository(context: Context) {
    private val cacheDir = File(context.applicationContext.cacheDir, "rainviewer_radar").apply { mkdirs() }
    private val memoryCache = LinkedHashMap<String, Bitmap>(18, 0.75f, true)

    suspend fun load(
        location: WorldLocation,
        zoom: Int = 6,
        maxFrames: Int = 7
    ): RadarMapBundle = withContext(Dispatchers.IO) {
        cleanupOldCache()

        val manifest = runCatching { fetchManifest() }.getOrElse { error ->
            return@withContext RadarMapBundle(
                frames = emptyList(),
                generatedEpochSeconds = 0L,
                zoom = zoom,
                coverageAvailable = null,
                errorMessage = error.message ?: "Unable to reach radar provider"
            )
        }

        val coverage = runCatching {
            loadCoverage(
                host = manifest.host,
                location = location,
                zoom = zoom
            )
        }.getOrNull()

        if (coverage == false) {
            return@withContext RadarMapBundle(
                frames = emptyList(),
                generatedEpochSeconds = manifest.generatedEpochSeconds,
                zoom = zoom,
                coverageAvailable = false,
                errorMessage = null
            )
        }

        val requestedFrames = chooseFrames(manifest, maxFrames)
        val rendered = requestedFrames.mapNotNull { frame ->
            runCatching {
                val bitmap = loadRadarBitmap(
                    host = manifest.host,
                    frame = frame,
                    location = location,
                    zoom = zoom
                )
                RadarRenderableFrame(
                    meta = frame,
                    bitmap = bitmap,
                    hasPrecipitationEcho = bitmapHasEcho(bitmap)
                )
            }.getOrNull()
        }

        RadarMapBundle(
            frames = rendered,
            generatedEpochSeconds = manifest.generatedEpochSeconds,
            zoom = zoom,
            coverageAvailable = coverage,
            errorMessage = when {
                requestedFrames.isEmpty() -> "Radar timeline is temporarily unavailable"
                rendered.isEmpty() -> "Radar images could not be loaded"
                else -> null
            }
        )
    }

    private fun chooseFrames(manifest: RadarManifest, maxFrames: Int): List<RadarFrameMeta> {
        val safeMax = maxFrames.coerceIn(3, 10)
        val nowcastCount = manifest.nowcast.size.coerceAtMost(2)
        val pastCount = (safeMax - nowcastCount).coerceAtLeast(1)
        return (manifest.past.takeLast(pastCount) + manifest.nowcast.take(nowcastCount))
            .sortedBy { it.timeEpochSeconds }
    }

    private fun fetchManifest(): RadarManifest {
        val raw = fetchText(WEATHER_MAPS_URL)
        val root = JSONObject(raw)
        val radar = root.optJSONObject("radar") ?: JSONObject()
        return RadarManifest(
            host = root.optString("host", "https://tilecache.rainviewer.com"),
            generatedEpochSeconds = root.optLong("generated", 0L),
            past = parseFrames(radar.optJSONArray("past"), isNowcast = false),
            nowcast = parseFrames(radar.optJSONArray("nowcast"), isNowcast = true)
        )
    }

    private fun parseFrames(array: JSONArray?, isNowcast: Boolean): List<RadarFrameMeta> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val path = item.optString("path")
                val time = item.optLong("time", 0L)
                if (path.isNotBlank() && time > 0L) {
                    add(RadarFrameMeta(time, path, isNowcast))
                }
            }
        }
    }

    private fun loadRadarBitmap(
        host: String,
        frame: RadarFrameMeta,
        location: WorldLocation,
        zoom: Int
    ): Bitmap {
        val latitude = String.format(Locale.US, "%.5f", location.latitude)
        val longitude = String.format(Locale.US, "%.5f", location.longitude)
        val url = "$host${frame.path}/512/$zoom/$latitude/$longitude/2/1_1.png"
        return loadCachedBitmap(url, RADAR_CACHE_MAX_AGE_MS)
            ?: throw IOException("Radar tile unavailable")
    }

    /**
     * RainViewer's coverage mask is transparent where composite radar coverage exists and black
     * where it does not. We sample a small center area because coordinate tiles are centered on the
     * user's selected location.
     */
    private fun loadCoverage(host: String, location: WorldLocation, zoom: Int): Boolean? {
        val latitude = String.format(Locale.US, "%.5f", location.latitude)
        val longitude = String.format(Locale.US, "%.5f", location.longitude)
        val url = "$host/v2/coverage/0/256/$zoom/$latitude/$longitude/0/0_0.png"
        val bitmap = loadCachedBitmap(url, COVERAGE_CACHE_MAX_AGE_MS) ?: return null

        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        var opaqueSamples = 0
        var totalSamples = 0
        for (dx in -8..8 step 4) {
            for (dy in -8..8 step 4) {
                val x = (centerX + dx).coerceIn(0, bitmap.width - 1)
                val y = (centerY + dy).coerceIn(0, bitmap.height - 1)
                val alpha = Color.alpha(bitmap.getPixel(x, y))
                if (alpha > 60) opaqueSamples++
                totalSamples++
            }
        }
        val mostlyBlackMask = totalSamples > 0 && opaqueSamples.toFloat() / totalSamples >= 0.60f
        return !mostlyBlackMask
    }

    private fun bitmapHasEcho(bitmap: Bitmap): Boolean {
        val stepX = (bitmap.width / 32).coerceAtLeast(4)
        val stepY = (bitmap.height / 32).coerceAtLeast(4)
        var x = 0
        while (x < bitmap.width) {
            var y = 0
            while (y < bitmap.height) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 18) return true
                y += stepY
            }
            x += stepX
        }
        return false
    }

    private fun loadCachedBitmap(url: String, maxAgeMillis: Long): Bitmap? {
        synchronized(memoryCache) {
            memoryCache[url]?.let { return it }
        }

        val file = File(cacheDir, sha256(url) + ".png")
        val fresh = file.exists() && file.length() > 0L &&
            System.currentTimeMillis() - file.lastModified() <= maxAgeMillis

        if (fresh) {
            BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                putMemory(url, bitmap)
                return bitmap
            }
        }

        val stale = if (file.exists() && file.length() > 0L) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }

        return runCatching {
            val bytes = fetchBytes(url)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IOException("Invalid radar image")
            runCatching {
                val temp = File(cacheDir, file.name + ".tmp")
                temp.writeBytes(bytes)
                if (file.exists()) file.delete()
                temp.renameTo(file)
                file.setLastModified(System.currentTimeMillis())
            }
            putMemory(url, bitmap)
            bitmap
        }.getOrElse {
            stale?.also { putMemory(url, it) }
        }
    }

    private fun putMemory(key: String, bitmap: Bitmap) {
        synchronized(memoryCache) {
            memoryCache[key] = bitmap
            while (memoryCache.size > 16) {
                val eldest = memoryCache.entries.firstOrNull()?.key ?: break
                memoryCache.remove(eldest)
            }
        }
    }

    private fun fetchText(url: String): String {
        val connection = open(url)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Radar provider HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchBytes(url: String): ByteArray {
        val connection = open(url)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Radar image HTTP $code")
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            useCaches = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json,image/png,image/*;q=0.9,*/*;q=0.7")
        }

    private fun cleanupOldCache() {
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                runCatching { file.delete() }
            }
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private data class RadarManifest(
        val host: String,
        val generatedEpochSeconds: Long,
        val past: List<RadarFrameMeta>,
        val nowcast: List<RadarFrameMeta>
    )
}
