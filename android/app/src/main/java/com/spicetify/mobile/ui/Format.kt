package com.spicetify.mobile.ui

import java.util.Locale

fun formatDuration(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSeconds = safe / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

fun formatReleaseYear(date: String): String =
    date.take(4).takeIf { it.length == 4 } ?: date

fun openSpotifyUrl(id: String, type: String): String =
    "https://open.spotify.com/$type/$id"
