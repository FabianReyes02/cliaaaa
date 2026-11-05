package com.spicetify.mobile

import android.content.Context
import com.spicetify.mobile.data.ArtworkColorExtractor
import com.spicetify.mobile.data.LyricsRepository
import com.spicetify.mobile.data.SettingsRepository
import com.spicetify.mobile.data.SpotifyApi
import com.spicetify.mobile.data.SpotifyAuth
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Small manual dependency graph, enough for a single-module academic app. */
class AppGraph(context: Context) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val settings = SettingsRepository(context.applicationContext)
    val auth = SpotifyAuth(context.applicationContext, settings, http, json)
    val api = SpotifyApi(auth, http, json)
    val lyrics = LyricsRepository(http, json)
    val artwork = ArtworkColorExtractor(http)
}
