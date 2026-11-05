package com.spicetify.mobile.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom

class SpotifyAuthException(message: String) : Exception(message)

/**
 * Authorization Code flow with PKCE.
 *
 * It does not require a client secret, which is exactly what a mobile app
 * should use. The access token is refreshed automatically before it expires.
 */
class SpotifyAuth(
    private val context: Context,
    private val settings: SettingsRepository,
    private val http: OkHttpClient,
    private val json: Json,
) {

    companion object {
        const val REDIRECT_URI = "spicetify-mobile://callback"
        private const val AUTHORIZE_URL = "https://accounts.spotify.com/authorize"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"

        // user-read-private / user-read-email dropped: GET /me no longer returns
        // product/email for development-mode apps (Feb 2026 changes).
        val SCOPES = listOf(
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "user-library-read",
            "user-library-modify",
        )
    }

    private val refreshMutex = Mutex()

    suspend fun beginAuthorization(activity: Context, clientId: String) {
        val id = clientId.trim()
        if (id.isEmpty()) {
            throw SpotifyAuthException("Ingresá el Client ID de tu app de Spotify.")
        }

        settings.setClientId(id)

        val verifier = randomUrlSafe(64)
        val state = randomUrlSafe(16)
        settings.savePkce(verifier, state)

        val authorizeUri = AUTHORIZE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", id)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("redirect_uri", REDIRECT_URI)
            .addQueryParameter("scope", SCOPES.joinToString(" "))
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("code_challenge", codeChallenge(verifier))
            .addQueryParameter("state", state)
            .build()
            .toString()
            .toUri()

        withContext(Dispatchers.Main) {
            val customTabs = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            if (activity !is Activity) {
                customTabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { customTabs.launchUrl(activity, authorizeUri) }
                .onFailure {
                    val browser = Intent(Intent.ACTION_VIEW, authorizeUri)
                    if (activity !is Activity) {
                        browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(browser)
                }
        }
    }

    suspend fun handleRedirect(uri: Uri): Result<Unit> {
        uri.getQueryParameter("error")?.let { error ->
            return Result.failure(SpotifyAuthException("Spotify devolvió: $error"))
        }

        val code = uri.getQueryParameter("code")
            ?: return Result.failure(SpotifyAuthException("No se recibió el código de autorización."))
        val receivedState = uri.getQueryParameter("state")
        val saved = settings.readPkce()
            ?: return Result.failure(SpotifyAuthException("No hay una autorización en curso."))

        if (receivedState != saved.second) {
            return Result.failure(SpotifyAuthException("El estado OAuth no coincide. Intentá de nuevo."))
        }

        val clientId = settings.snapshot().clientId
        if (clientId.isBlank()) {
            return Result.failure(SpotifyAuthException("Falta el Client ID."))
        }

        return try {
            val token = requestToken(
                FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("redirect_uri", REDIRECT_URI)
                    .add("client_id", clientId)
                    .add("code_verifier", saved.first)
                    .build(),
            )
            settings.saveToken(token.toTokenSet(fallbackRefresh = null))
            settings.clearPkce()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a valid access token, refreshing it when it is about to expire.
     * Returns null when the user is not connected or the refresh token is invalid.
     */
    suspend fun validAccessToken(forceRefresh: Boolean = false): String? {
        val current = settings.snapshot()
        val token = current.accessToken ?: return null
        val remaining = current.tokenExpiresAt - System.currentTimeMillis()

        if (!forceRefresh && remaining > 60_000) return token

        return refreshMutex.withLock {
            val latest = settings.snapshot()
            val latestToken = latest.accessToken
            val latestRemaining = latest.tokenExpiresAt - System.currentTimeMillis()

            if (!forceRefresh && latestToken != null && latestRemaining > 60_000) {
                latestToken
            } else {
                val refreshToken = latest.refreshToken
                if (refreshToken.isNullOrBlank()) {
                    settings.clearToken()
                    null
                } else {
                    runCatching {
                        val refreshed = requestToken(
                            FormBody.Builder()
                                .add("grant_type", "refresh_token")
                                .add("refresh_token", refreshToken)
                                .add("client_id", latest.clientId)
                                .build(),
                        )
                        settings.saveToken(refreshed.toTokenSet(fallbackRefresh = refreshToken))
                        refreshed.accessToken
                    }.getOrElse {
                        settings.clearToken()
                        null
                    }
                }
            }
        }
    }

    suspend fun disconnect() {
        settings.clearToken()
        settings.clearPkce()
    }

    private suspend fun requestToken(form: FormBody): TokenResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(form)
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val parsed = runCatching { json.decodeFromString<TokenResponse>(body) }.getOrNull()

            if (!response.isSuccessful || parsed?.accessToken.isNullOrBlank()) {
                val message = parsed?.errorDescription
                    ?: parsed?.error
                    ?: "No se pudo obtener el token (HTTP ${response.code})."
                throw SpotifyAuthException(message)
            }
            parsed
        }
    }

    private fun TokenResponse.toTokenSet(fallbackRefresh: String?): TokenSet = TokenSet(
        accessToken = accessToken,
        refreshToken = refreshToken ?: fallbackRefresh,
        expiresAt = System.currentTimeMillis() + (expiresIn * 1000L),
        scope = scope,
    )

    private fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }
}

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)
