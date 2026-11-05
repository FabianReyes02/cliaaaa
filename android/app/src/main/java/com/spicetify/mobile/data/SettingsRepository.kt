package com.spicetify.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "spicetify_mobile_settings")

/**
 * Single source of truth for preferences and OAuth tokens.
 *
 * Note: tokens are stored in DataStore (plain text) to keep the sample simple.
 * A production app should use EncryptedSharedPreferences or the Android Keystore.
 */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext

    private object Keys {
        val clientId = stringPreferencesKey("spotify_client_id")
        val themeId = stringPreferencesKey("theme_id")
        val accent = intPreferencesKey("accent_argb")
        val amoled = booleanPreferencesKey("amoled")
        val matchArtwork = booleanPreferencesKey("match_artwork")
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val expiresAt = longPreferencesKey("token_expires_at")
        val scope = stringPreferencesKey("token_scope")
        val codeVerifier = stringPreferencesKey("pkce_code_verifier")
        val authState = stringPreferencesKey("pkce_state")
    }

    val settings: Flow<AppSettings> = appContext.dataStore.data.map { prefs ->
        AppSettings(
            clientId = prefs[Keys.clientId].orEmpty(),
            themeId = prefs[Keys.themeId] ?: ThemePresets.SpotifyGreen.id,
            customAccentArgb = prefs[Keys.accent]?.takeIf { it != 0 },
            amoled = prefs[Keys.amoled] ?: false,
            matchArtwork = prefs[Keys.matchArtwork] ?: true,
            accessToken = prefs[Keys.accessToken],
            refreshToken = prefs[Keys.refreshToken],
            tokenExpiresAt = prefs[Keys.expiresAt] ?: 0L,
            scope = prefs[Keys.scope],
        )
    }

    suspend fun snapshot(): AppSettings = settings.first()

    suspend fun setClientId(value: String) {
        appContext.dataStore.edit { it[Keys.clientId] = value.trim() }
    }

    suspend fun setThemeId(value: String) {
        appContext.dataStore.edit { it[Keys.themeId] = value }
    }

    suspend fun setCustomAccent(argb: Int?) {
        appContext.dataStore.edit { prefs ->
            if (argb == null) prefs.remove(Keys.accent) else prefs[Keys.accent] = argb
        }
    }

    suspend fun setAmoled(enabled: Boolean) {
        appContext.dataStore.edit { it[Keys.amoled] = enabled }
    }

    suspend fun setMatchArtwork(enabled: Boolean) {
        appContext.dataStore.edit { it[Keys.matchArtwork] = enabled }
    }

    suspend fun saveToken(token: TokenSet) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.accessToken] = token.accessToken
            token.refreshToken?.let { prefs[Keys.refreshToken] = it }
            prefs[Keys.expiresAt] = token.expiresAt
            token.scope?.let { prefs[Keys.scope] = it }
        }
    }

    suspend fun clearToken() {
        appContext.dataStore.edit { prefs ->
            prefs.remove(Keys.accessToken)
            prefs.remove(Keys.refreshToken)
            prefs.remove(Keys.expiresAt)
            prefs.remove(Keys.scope)
        }
    }

    suspend fun savePkce(verifier: String, state: String) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.codeVerifier] = verifier
            prefs[Keys.authState] = state
        }
    }

    suspend fun readPkce(): Pair<String, String>? {
        val prefs = appContext.dataStore.data.first()
        val verifier = prefs[Keys.codeVerifier] ?: return null
        val state = prefs[Keys.authState] ?: return null
        return verifier to state
    }

    suspend fun clearPkce() {
        appContext.dataStore.edit { prefs ->
            prefs.remove(Keys.codeVerifier)
            prefs.remove(Keys.authState)
        }
    }
}
