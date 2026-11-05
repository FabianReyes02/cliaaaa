package com.spicetify.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class LyricLine(val timeMs: Long, val text: String)

data class LyricsPayload(
    val label: String,
    val lines: List<LyricLine> = emptyList(),
    val plain: String? = null,
    val instrumental: Boolean = false,
)

sealed interface LyricsResult {
    data class Success(val payload: LyricsPayload) : LyricsResult
    data class Failure(val message: String) : LyricsResult
}

/**
 * Lyrics Plus custom app: port of the idea behind Spicetify's lyrics-plus.
 * It uses the free LRCLIB database (https://lrclib.net), including synced LRC.
 */
class LyricsRepository(
    private val http: OkHttpClient,
    private val json: Json,
) {

    companion object {
        private const val BASE = "https://lrclib.net/api"
        private const val USER_AGENT = "SpicetifyMobile/1.0 (Android; academic port)"
    }

    suspend fun forTrack(track: Track): LyricsResult = withContext(Dispatchers.IO) {
        try {
            val getUrl = "$BASE/get".toHttpUrl().newBuilder()
                .addQueryParameter("track_name", track.title)
                .addQueryParameter("artist_name", track.artist.substringBefore(",").trim())
                .apply {
                    if (track.durationMs > 0) {
                        addQueryParameter("duration", (track.durationMs / 1000).toString())
                    }
                }
                .build()

            val exact = fetch(getUrl)
            if (exact != null) {
                return@withContext fromDto(exact, fallbackLabel = "${track.title} — ${track.artist}")
            }

            val searchUrl = "$BASE/search".toHttpUrl().newBuilder()
                .addQueryParameter("q", "${track.artist} ${track.title}")
                .build()
            val candidates = fetchList(searchUrl)
            val best = candidates.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                ?: candidates.firstOrNull { !it.plainLyrics.isNullOrBlank() }
            if (best != null) {
                return@withContext fromDto(best, fallbackLabel = "${track.title} — ${track.artist}")
            }

            LyricsResult.Failure("No encontramos la letra de \"${track.title}\" en LRCLIB.")
        } catch (e: Exception) {
            LyricsResult.Failure(e.message ?: "No se pudo consultar LRCLIB.")
        }
    }

    suspend fun search(query: String): LyricsResult = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE/search".toHttpUrl().newBuilder()
                .addQueryParameter("q", query)
                .build()
            val candidates = fetchList(url)
            val best = candidates.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                ?: candidates.firstOrNull { !it.plainLyrics.isNullOrBlank() }
            if (best == null) {
                LyricsResult.Failure("Sin resultados para \"$query\".")
            } else {
                fromDto(best, fallbackLabel = query)
            }
        } catch (e: Exception) {
            LyricsResult.Failure(e.message ?: "No se pudo buscar la letra.")
        }
    }

    private fun fromDto(dto: LrcDto, fallbackLabel: String): LyricsResult {
        val label = listOfNotNull(dto.trackName, dto.artistName)
            .joinToString(" — ")
            .ifBlank { fallbackLabel }

        if (dto.instrumental) {
            return LyricsResult.Success(
                LyricsPayload(label = label, instrumental = true),
            )
        }

        val synced = dto.syncedLyrics?.let(::parseLrc).orEmpty()
        if (synced.isNotEmpty()) {
            return LyricsResult.Success(
                LyricsPayload(label = label, lines = synced),
            )
        }

        if (!dto.plainLyrics.isNullOrBlank()) {
            return LyricsResult.Success(
                LyricsPayload(label = label, plain = dto.plainLyrics),
            )
        }

        return LyricsResult.Failure("La letra existe pero no tiene contenido reproducible.")
    }

    private fun fetch(url: HttpUrl): LrcDto? {
        val (status, body) = request(url)
        if (status == 404 || body.isBlank()) return null
        if (status !in 200..299) return null
        return runCatching { json.decodeFromString<LrcDto>(body) }.getOrNull()
    }

    private fun fetchList(url: HttpUrl): List<LrcDto> {
        val (status, body) = request(url)
        if (status !in 200..299 || body.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<LrcDto>>(body) }.getOrDefault(emptyList())
    }

    private fun request(url: HttpUrl): Pair<Int, String> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        http.newCall(request).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }
}

fun parseLrc(raw: String): List<LyricLine> {
    val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    val lines = mutableListOf<LyricLine>()

    raw.lineSequence().forEach { line ->
        val stamps = timestamp.findAll(line).toList()
        if (stamps.isEmpty()) return@forEach

        val text = line.substring(stamps.last().range.last + 1).trim()
        if (text.isEmpty()) return@forEach

        stamps.forEach { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3]
            val millis = when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLong() * 100L
                2 -> fraction.toLong() * 10L
                else -> fraction.take(3).toLong()
            }
            lines += LyricLine(
                timeMs = minutes * 60_000L + seconds * 1_000L + millis,
                text = text,
            )
        }
    }

    return lines.distinctBy { it.timeMs to it.text }.sortedBy { it.timeMs }
}

@Serializable
private data class LrcDto(
    @SerialName("trackName") val trackName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("albumName") val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    @SerialName("plainLyrics") val plainLyrics: String? = null,
    @SerialName("syncedLyrics") val syncedLyrics: String? = null,
)
