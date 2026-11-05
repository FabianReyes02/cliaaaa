package com.spicetify.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SpotifyApiException(val status: Int, message: String) : Exception(message)

/**
 * Thin Spotify Web API client. Only the endpoints used by the app are wrapped,
 * so the code stays readable for an academic project.
 */
class SpotifyApi(
    private val auth: SpotifyAuth,
    private val http: OkHttpClient,
    private val json: Json,
) {

    companion object {
        private const val BASE = "https://api.spotify.com/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    // ---------------------------------------------------------------- player

    suspend fun playback(): Playback? {
        val body = get("/me/player") ?: return null
        val dto = json.decodeFromString<PlaybackDto>(body)
        return Playback(
            track = dto.item?.toDomain(),
            isPlaying = dto.isPlaying,
            progressMs = dto.progressMs ?: 0L,
            shuffle = dto.shuffleState,
            repeat = dto.repeatState,
            deviceName = dto.device?.name,
        )
    }

    suspend fun play(uri: String? = null) {
        if (uri == null) {
            putEmpty("/me/player/play")
        } else {
            putJson("/me/player/play", json.encodeToString(PlayRequest(listOf(uri))))
        }
    }

    suspend fun pause() = putEmpty("/me/player/pause")

    suspend fun next() = postEmpty("/me/player/next")

    suspend fun previous() = postEmpty("/me/player/previous")

    suspend fun seek(positionMs: Long) = putEmpty("/me/player/seek?position_ms=$positionMs")

    suspend fun setShuffle(enabled: Boolean) = putEmpty("/me/player/shuffle?state=$enabled")

    suspend fun setRepeat(state: String) = putEmpty("/me/player/repeat?state=$state")

    // --------------------------------------------------------------- library
    // Since Feb 2026, content-specific endpoints (/me/tracks/*) were replaced
    // by the generic library endpoints that take Spotify URIs.

    suspend fun isSaved(trackId: String): Boolean {
        val uri = trackUri(trackId)
        val body = get("/me/library/contains?uris=$uri") ?: return false
        return runCatching { json.decodeFromString<List<Boolean>>(body).firstOrNull() ?: false }
            .getOrDefault(false)
    }

    suspend fun saveTrack(trackId: String) = putJson("/me/library", libraryBody(trackId))

    suspend fun removeSavedTrack(trackId: String) = deleteJson("/me/library", libraryBody(trackId))

    // ------------------------------------------------------------ discovery

    suspend fun searchTracks(query: String): List<Track> {
        // February 2026 changes: /search caps limit at 10 for development-mode apps.
        val url = url("/search", mapOf("q" to query, "type" to "track", "limit" to "10"))
        val body = get(url) ?: return emptyList()
        return json.decodeFromString<SearchDto>(body)
            .tracks?.items
            .orEmpty()
            .mapNotNull { it.toDomain() }
    }

    suspend fun newReleases(): List<Album> {
        // /browse/new-releases was removed for new development-mode apps (Feb 2026).
        // Approximate it with a search for albums of the current year.
        val year = java.time.LocalDate.now().year
        val url = url("/search", mapOf("q" to "year:$year", "type" to "album", "limit" to "10"))
        val body = get(url) ?: return emptyList()
        return json.decodeFromString<SearchAlbumsDto>(body)
            .albums?.items
            .orEmpty()
            .mapNotNull { it.toDomain() }
    }

    suspend fun profile(): Profile? {
        val body = get("/me") ?: return null
        val dto = json.decodeFromString<ProfileDto>(body)
        return Profile(
            id = dto.id,
            displayName = dto.displayName ?: "Usuario de Spotify",
            imageUrl = dto.images.best(),
        )
    }

    // ------------------------------------------------------------ internals

    private suspend fun get(pathOrUrl: String): String? {
        val target = if (pathOrUrl.startsWith("http")) pathOrUrl else BASE + pathOrUrl
        return execute { token ->
            Request.Builder()
                .url(target)
                .header("Authorization", "Bearer $token")
                .build()
        }
    }

    private suspend fun putEmpty(path: String) = executeMutation("PUT", path, null)

    private suspend fun postEmpty(path: String) = executeMutation("POST", path, null)

    private suspend fun putJson(path: String, body: String) = executeMutation("PUT", path, body)

    private suspend fun deleteJson(path: String, body: String) = executeMutation("DELETE", path, body)

    private suspend fun executeMutation(method: String, path: String, body: String?) {
        val requestBody = body?.toRequestBody(JSON_MEDIA_TYPE)
            ?: ByteArray(0).toRequestBody(null)
        execute { token ->
            Request.Builder()
                .url(BASE + path)
                .method(method, requestBody)
                .header("Authorization", "Bearer $token")
                .build()
        }
    }

    private suspend fun execute(build: (String) -> Request): String? = withContext(Dispatchers.IO) {
        var token = auth.validAccessToken()
            ?: throw SpotifyApiException(401, "Conectá tu cuenta de Spotify desde Ajustes.")

        var response = http.newCall(build(token)).execute()

        if (response.code == 401) {
            response.close()
            token = auth.validAccessToken(forceRefresh = true)
                ?: throw SpotifyApiException(
                    401,
                    "La sesión de Spotify expiró. Volvé a conectar tu cuenta desde Ajustes.",
                )
            response = http.newCall(build(token)).execute()
        }

        response.use { resp ->
            val raw = resp.body?.string()
            if (resp.code == 204) return@use null
            if (!resp.isSuccessful) {
                throw SpotifyApiException(resp.code, errorMessage(resp.code, raw))
            }
            raw
        }
    }

    private fun errorMessage(status: Int, body: String?): String {
        val apiMessage = body?.let {
            runCatching { json.decodeFromString<ErrorDto>(it).error?.message }.getOrNull()
        }
        return apiMessage ?: when (status) {
            403 -> "Spotify Premium y un dispositivo activo son necesarios para controlar la reproducción."
            404 -> "No se encontró el recurso en Spotify."
            429 -> "Demasiadas solicitudes. Esperá unos segundos y reintentá."
            else -> "Error ${status} de Spotify."
        }
    }

    private fun url(path: String, query: Map<String, String>): String {
        val builder = (BASE + path).toHttpUrl().newBuilder()
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private fun trackUri(trackId: String): String =
        if (trackId.startsWith("spotify:")) trackId else "spotify:track:$trackId"

    private fun libraryBody(trackId: String): String =
        json.encodeToString(LibraryRequest(listOf(trackUri(trackId))))
}

// ------------------------------------------------------------------- DTOs

@Serializable
private data class LibraryRequest(val uris: List<String>)

@Serializable
private data class PlayRequest(val uris: List<String>)

@Serializable
private data class PlaybackDto(
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("progress_ms") val progressMs: Long? = null,
    val item: TrackDto? = null,
    val device: DeviceDto? = null,
    @SerialName("shuffle_state") val shuffleState: Boolean = false,
    @SerialName("repeat_state") val repeatState: String = "off",
)

@Serializable
private data class DeviceDto(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
)

@Serializable
private data class TrackDto(
    val id: String? = null,
    val name: String = "",
    val artists: List<ArtistDto> = emptyList(),
    val album: AlbumDto? = null,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    val explicit: Boolean = false,
    val uri: String = "",
    val images: List<ImageDto> = emptyList(),
)

@Serializable
private data class ArtistDto(val name: String = "")

@Serializable
private data class AlbumDto(
    val id: String? = null,
    val name: String = "",
    val artists: List<ArtistDto> = emptyList(),
    val images: List<ImageDto> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
    val uri: String = "",
)

@Serializable
private data class ImageDto(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
private data class SearchDto(val tracks: TracksPageDto? = null)

@Serializable
private data class SearchAlbumsDto(val albums: AlbumsPageDto? = null)

@Serializable
private data class TracksPageDto(val items: List<TrackDto> = emptyList())

@Serializable
private data class AlbumsPageDto(val items: List<AlbumDto> = emptyList())

@Serializable
private data class ProfileDto(
    val id: String = "",
    @SerialName("display_name") val displayName: String? = null,
    val images: List<ImageDto> = emptyList(),
)

@Serializable
private data class ErrorDto(val error: ErrorBodyDto? = null)

@Serializable
private data class ErrorBodyDto(
    val status: Int = 0,
    val message: String = "",
)

// -------------------------------------------------------------- mappings

private fun List<ImageDto>.best(): String? =
    maxByOrNull { it.width * it.height }?.url?.takeIf { it.isNotBlank() }

private fun TrackDto.toDomain(): Track? {
    val trackId = id ?: return null
    return Track(
        id = trackId,
        title = name.ifBlank { "Sin título" },
        artist = artists.joinToString(", ") { it.name }.ifBlank { "Spotify" },
        album = album?.name.orEmpty(),
        imageUrl = album?.images?.best() ?: images.best(),
        durationMs = durationMs,
        uri = uri,
        explicit = explicit,
    )
}

private fun AlbumDto.toDomain(): Album? {
    val albumId = id ?: return null
    return Album(
        id = albumId,
        title = name.ifBlank { "Sin título" },
        artist = artists.joinToString(", ") { it.name }.ifBlank { "Spotify" },
        imageUrl = images.best(),
        releaseDate = releaseDate.orEmpty(),
        uri = uri,
    )
}
