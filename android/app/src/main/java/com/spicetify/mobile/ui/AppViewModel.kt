package com.spicetify.mobile.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spicetify.mobile.SpicetifyApp
import com.spicetify.mobile.data.Album
import com.spicetify.mobile.data.AppSettings
import com.spicetify.mobile.data.DemoData
import com.spicetify.mobile.data.LyricsPayload
import com.spicetify.mobile.data.LyricsResult
import com.spicetify.mobile.data.Profile
import com.spicetify.mobile.data.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUi(
    val isDemo: Boolean = true,
    val track: Track? = DemoData.tracks.first(),
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: String = "off",
    val deviceName: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val artworkAccent: Int? = null,
)

data class LyricsUiState(
    val loading: Boolean = false,
    val payload: LyricsPayload? = null,
    val error: String? = null,
    val trackId: String? = null,
)

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Track> = emptyList(),
    val error: String? = null,
    val searched: Boolean = false,
)

data class ReleasesUiState(
    val loading: Boolean = false,
    val albums: List<Album> = DemoData.albums,
    val usingDemo: Boolean = true,
    val error: String? = null,
)

data class AuthUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val graph = (application as SpicetifyApp).graph

    val settings: StateFlow<AppSettings> = graph.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _playback = MutableStateFlow(PlaybackUi())
    val playback: StateFlow<PlaybackUi> = _playback.asStateFlow()

    private val _lyrics = MutableStateFlow(LyricsUiState())
    val lyrics: StateFlow<LyricsUiState> = _lyrics.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _releases = MutableStateFlow(ReleasesUiState())
    val releases: StateFlow<ReleasesUiState> = _releases.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _auth = MutableStateFlow(AuthUiState())
    val authUi: StateFlow<AuthUiState> = _auth.asStateFlow()

    private var artworkJob: Job? = null
    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            settings.map { it.isConnected }
                .distinctUntilChanged()
                .collect { connected ->
                    if (connected) {
                        _playback.update { it.copy(isDemo = false, error = null) }
                        refreshPlayback(showLoading = true)
                        loadProfile()
                        startPolling()
                    } else {
                        pollingJob?.cancel()
                        _playback.value = PlaybackUi()
                        _profile.value = null
                    }
                }
        }
        startProgressTicker()
    }

    // ------------------------------------------------------------ playback

    fun refreshPlayback(showLoading: Boolean = true) {
        if (!settings.value.isConnected) {
            _playback.update { it.copy(isDemo = true) }
            return
        }

        viewModelScope.launch {
            if (showLoading) {
                _playback.update { it.copy(loading = true, error = null) }
            }
            try {
                val result = graph.api.playback()
                val track = result?.track
                if (result == null || track == null) {
                    _playback.update {
                        it.copy(
                            loading = false,
                            isDemo = false,
                            isPlaying = false,
                            deviceName = result?.deviceName,
                            error = "No hay reproducción activa. Abrí Spotify en algún dispositivo y tocá play.",
                        )
                    }
                    return@launch
                }

                val previousId = _playback.value.track?.id
                _playback.update {
                    it.copy(
                        isDemo = false,
                        track = track,
                        isPlaying = result.isPlaying,
                        progressMs = result.progressMs,
                        shuffle = result.shuffle,
                        repeat = result.repeat,
                        deviceName = result.deviceName,
                        loading = false,
                        error = null,
                    )
                }

                if (previousId != track.id) {
                    checkSaved(track)
                    updateArtworkAccent(track)
                }
            } catch (e: Exception) {
                _playback.update {
                    it.copy(loading = false, isDemo = false, error = e.message)
                }
            }
        }
    }

    fun togglePlay() {
        val state = _playback.value
        if (state.track == null) return

        if (state.isDemo) {
            _playback.update { it.copy(isPlaying = !it.isPlaying) }
            return
        }

        viewModelScope.launch {
            try {
                if (state.isPlaying) graph.api.pause() else graph.api.play()
                _playback.update { it.copy(isPlaying = !state.isPlaying, error = null) }
                delay(400)
                refreshPlayback(showLoading = false)
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }

    fun next() {
        val state = _playback.value
        if (state.isDemo) {
            cycleDemo(delta = 1)
            return
        }
        viewModelScope.launch {
            try {
                graph.api.next()
                delay(500)
                refreshPlayback(showLoading = false)
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }

    fun previous() {
        val state = _playback.value
        if (state.isDemo) {
            cycleDemo(delta = -1)
            return
        }
        viewModelScope.launch {
            try {
                graph.api.previous()
                delay(500)
                refreshPlayback(showLoading = false)
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val track = _playback.value.track ?: return
        val clamped = positionMs.coerceIn(0L, track.durationMs)
        _playback.update { it.copy(progressMs = clamped, error = null) }

        if (_playback.value.isDemo) return

        viewModelScope.launch {
            runCatching { graph.api.seek(clamped) }
                .onFailure { showError(it.message) }
        }
    }

    fun toggleShuffle() {
        val state = _playback.value
        val next = !state.shuffle
        _playback.update { it.copy(shuffle = next, error = null) }
        if (state.isDemo) return

        viewModelScope.launch {
            runCatching { graph.api.setShuffle(next) }
                .onFailure {
                    _playback.update { s -> s.copy(shuffle = !next) }
                    showError(it.message)
                }
        }
    }

    fun cycleRepeat() {
        val state = _playback.value
        val next = when (state.repeat) {
            "off" -> "context"
            "context" -> "track"
            else -> "off"
        }
        _playback.update { it.copy(repeat = next, error = null) }
        if (state.isDemo) return

        viewModelScope.launch {
            runCatching { graph.api.setRepeat(next) }
                .onFailure {
                    _playback.update { s -> s.copy(repeat = state.repeat) }
                    showError(it.message)
                }
        }
    }

    fun toggleSaved() {
        val track = _playback.value.track ?: return
        val next = !_playback.value.isSaved
        _playback.update { it.copy(isSaved = next, error = null) }
        if (_playback.value.isDemo) return

        viewModelScope.launch {
            runCatching {
                if (next) graph.api.saveTrack(track.id) else graph.api.removeSavedTrack(track.id)
            }.onFailure {
                _playback.update { s -> s.copy(isSaved = !next) }
                showError(it.message)
            }
        }
    }

    fun playTrack(track: Track) {
        if (!settings.value.isConnected) {
            showError("Conectá Spotify para reproducir canciones reales.")
            return
        }
        viewModelScope.launch {
            try {
                graph.api.play(track.uri)
                delay(600)
                refreshPlayback(showLoading = false)
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }

    private fun cycleDemo(delta: Int) {
        val state = _playback.value
        val current = DemoData.tracks.indexOfFirst { it.id == state.track?.id }.coerceAtLeast(0)
        val nextIndex = (current + delta).mod(DemoData.tracks.size)
        _playback.update {
            it.copy(
                track = DemoData.tracks[nextIndex],
                progressMs = 0L,
                isPlaying = true,
                isSaved = false,
                artworkAccent = null,
                error = null,
            )
        }
    }

    private fun checkSaved(track: Track) {
        viewModelScope.launch {
            val saved = runCatching { graph.api.isSaved(track.id) }.getOrDefault(false)
            _playback.update { if (it.track?.id == track.id) it.copy(isSaved = saved) else it }
        }
    }

    private fun updateArtworkAccent(track: Track) {
        artworkJob?.cancel()
        artworkJob = viewModelScope.launch {
            val accent = graph.artwork.accentFor(track.imageUrl)
            _playback.update {
                if (it.track?.id == track.id) it.copy(artworkAccent = accent) else it
            }
        }
    }

    private fun showError(message: String?) {
        _playback.update { it.copy(error = message ?: "Ocurrió un error inesperado.") }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                if (settings.value.isConnected) refreshPlayback(showLoading = false)
            }
        }
    }

    private fun startProgressTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _playback.update { state ->
                    val track = state.track
                    if (state.isPlaying && track != null) {
                        state.copy(
                            progressMs = (state.progressMs + 1_000L).coerceAtMost(track.durationMs),
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- auth

    fun connectSpotify(context: Context, clientId: String) {
        viewModelScope.launch {
            _auth.update { it.copy(busy = true, error = null, message = null) }
            try {
                graph.settings.setClientId(clientId)
                graph.auth.beginAuthorization(context, clientId)
                _auth.update {
                    it.copy(
                        busy = false,
                        message = "Terminá el inicio de sesión en el navegador y volvé a la app.",
                    )
                }
            } catch (e: Exception) {
                _auth.update { it.copy(busy = false, error = e.message) }
            }
        }
    }

    fun handleAuthRedirect(uri: Uri) {
        viewModelScope.launch {
            _auth.update { it.copy(busy = true, error = null, message = null) }
            graph.auth.handleRedirect(uri)
                .onSuccess {
                    _auth.update {
                        it.copy(busy = false, message = "¡Cuenta de Spotify conectada!", error = null)
                    }
                    refreshPlayback(showLoading = true)
                    loadProfile()
                }
                .onFailure { throwable ->
                    _auth.update { it.copy(busy = false, error = throwable.message) }
                }
        }
    }

    fun disconnectSpotify() {
        viewModelScope.launch {
            graph.auth.disconnect()
            _playback.value = PlaybackUi()
            _profile.value = null
            _auth.value = AuthUiState(message = "Cuenta desconectada.")
        }
    }

    fun clearAuthFeedback() {
        _auth.update { it.copy(message = null, error = null) }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = runCatching { graph.api.profile() }.getOrNull()
        }
    }

    // -------------------------------------------------------------- lyrics

    fun loadLyrics(track: Track, force: Boolean = false) {
        val current = _lyrics.value
        if (!force && current.trackId == track.id && (current.loading || current.payload != null)) {
            return
        }

        viewModelScope.launch {
            _lyrics.value = LyricsUiState(loading = true, trackId = track.id)
            when (val result = graph.lyrics.forTrack(track)) {
                is LyricsResult.Success -> {
                    _lyrics.value = LyricsUiState(payload = result.payload, trackId = track.id)
                }
                is LyricsResult.Failure -> {
                    _lyrics.value = LyricsUiState(error = result.message, trackId = track.id)
                }
            }
        }
    }

    fun searchLyrics(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _lyrics.value = LyricsUiState(loading = true)
            when (val result = graph.lyrics.search(clean)) {
                is LyricsResult.Success -> {
                    _lyrics.value = LyricsUiState(payload = result.payload)
                }
                is LyricsResult.Failure -> {
                    _lyrics.value = LyricsUiState(error = result.message)
                }
            }
        }
    }

    // --------------------------------------------------------- discovery

    fun searchTracks(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            _search.value = SearchUiState()
            return
        }

        viewModelScope.launch {
            _search.update {
                it.copy(query = clean, loading = true, error = null, searched = true)
            }
            try {
                val results = graph.api.searchTracks(clean)
                _search.update {
                    it.copy(
                        loading = false,
                        results = results,
                        error = if (results.isEmpty()) "Sin resultados para \"$clean\"." else null,
                    )
                }
            } catch (e: Exception) {
                _search.update {
                    it.copy(loading = false, results = emptyList(), error = e.message)
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _search.update { it.copy(query = query) }
    }

    fun loadNewReleases(force: Boolean = false) {
        if (!force && !_releases.value.usingDemo && _releases.value.albums.isNotEmpty()) return

        viewModelScope.launch {
            _releases.update { it.copy(loading = true, error = null) }

            if (!settings.value.isConnected) {
                delay(200)
                _releases.value = ReleasesUiState(
                    loading = false,
                    albums = DemoData.albums,
                    usingDemo = true,
                )
                return@launch
            }

            try {
                val albums = graph.api.newReleases()
                _releases.value = ReleasesUiState(
                    loading = false,
                    albums = albums.ifEmpty { DemoData.albums },
                    usingDemo = albums.isEmpty(),
                    error = if (albums.isEmpty()) {
                        "Spotify no devolvió novedades; mostrando ejemplos."
                    } else {
                        null
                    },
                )
            } catch (e: Exception) {
                _releases.value = ReleasesUiState(
                    loading = false,
                    albums = DemoData.albums,
                    usingDemo = true,
                    error = e.message,
                )
            }
        }
    }

    // ------------------------------------------------------------ settings

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            graph.settings.setThemeId(themeId)
            graph.settings.setCustomAccent(null)
        }
    }

    fun selectAccent(argb: Int?) {
        viewModelScope.launch { graph.settings.setCustomAccent(argb) }
    }

    fun setAmoled(enabled: Boolean) {
        viewModelScope.launch { graph.settings.setAmoled(enabled) }
    }

    fun setMatchArtwork(enabled: Boolean) {
        viewModelScope.launch { graph.settings.setMatchArtwork(enabled) }
    }

    fun saveClientId(clientId: String) {
        viewModelScope.launch { graph.settings.setClientId(clientId) }
    }
}
