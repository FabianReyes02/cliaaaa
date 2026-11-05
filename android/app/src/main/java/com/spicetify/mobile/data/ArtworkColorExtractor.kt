package com.spicetify.mobile.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Extracts a dominant, vivid color from the current cover art.
 * This is the equivalent of Spicetify's "color from album art" dynamic themes.
 */
class ArtworkColorExtractor(private val http: OkHttpClient) {

    private val cache = ConcurrentHashMap<String, Int>()

    suspend fun accentFor(imageUrl: String?): Int? = withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrBlank()) return@withContext null

        val cached = cache[imageUrl]
        if (cached != null) return@withContext cached.takeIf { it != 0 }

        val accent = runCatching { computeAccent(imageUrl) }.getOrNull()
        cache[imageUrl] = accent ?: 0
        accent
    }

    private fun computeAccent(imageUrl: String): Int? {
        val request = Request.Builder().url(imageUrl).build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

            val sample = Bitmap.createScaledBitmap(decoded, 24, 24, true)
            var red = 0.0
            var green = 0.0
            var blue = 0.0
            var totalWeight = 0.0
            val hsv = FloatArray(3)

            for (x in 0 until sample.width) {
                for (y in 0 until sample.height) {
                    val pixel = sample.getPixel(x, y)
                    if (android.graphics.Color.alpha(pixel) < 200) continue

                    android.graphics.Color.colorToHSV(pixel, hsv)
                    if (hsv[2] < 0.10f) continue

                    val weight = 0.25 + hsv[1] * 0.75 + hsv[2] * 0.25
                    red += android.graphics.Color.red(pixel) * weight
                    green += android.graphics.Color.green(pixel) * weight
                    blue += android.graphics.Color.blue(pixel) * weight
                    totalWeight += weight
                }
            }

            if (sample !== decoded) sample.recycle()
            decoded.recycle()
            if (totalWeight == 0.0) return null

            val average = android.graphics.Color.rgb(
                (red / totalWeight).toInt().coerceIn(0, 255),
                (green / totalWeight).toInt().coerceIn(0, 255),
                (blue / totalWeight).toInt().coerceIn(0, 255),
            )

            android.graphics.Color.colorToHSV(average, hsv)
            hsv[1] = max(hsv[1], 0.55f)
            hsv[2] = max(hsv[2], 0.82f)
            return android.graphics.Color.HSVToColor(hsv)
        }
    }
}
