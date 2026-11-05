package com.spicetify.mobile.data

/** Snapshot of every user preference plus the Spotify token pair. */
data class AppSettings(
    val clientId: String = "",
    val themeId: String = ThemePresets.SpotifyGreen.id,
    val customAccentArgb: Int? = null,
    val amoled: Boolean = false,
    val matchArtwork: Boolean = true,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenExpiresAt: Long = 0L,
    val scope: String? = null,
) {
    val isConnected: Boolean get() = !accessToken.isNullOrBlank()
}

data class TokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long,
    val scope: String?,
)
