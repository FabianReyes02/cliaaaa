package com.spicetify.mobile.data

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String?,
    val durationMs: Long,
    val uri: String,
    val explicit: Boolean = false,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val releaseDate: String,
    val uri: String,
)

data class Playback(
    val track: Track?,
    val isPlaying: Boolean,
    val progressMs: Long,
    val shuffle: Boolean,
    val repeat: String,
    val deviceName: String?,
)

data class Profile(
    val id: String,
    val displayName: String,
    val imageUrl: String?,
)
