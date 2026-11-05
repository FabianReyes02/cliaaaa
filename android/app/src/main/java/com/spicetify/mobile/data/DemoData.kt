package com.spicetify.mobile.data

/**
 * Offline sample data. It keeps every screen usable during a demo or before
 * the Spotify account is connected, which is very handy for an evaluation.
 */
object DemoData {

    val tracks: List<Track> = listOf(
        Track(
            id = "demo-midnight-city",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            imageUrl = null,
            durationMs = 244_000L,
            uri = "demo:track:midnight-city",
        ),
        Track(
            id = "demo-get-lucky",
            title = "Get Lucky",
            artist = "Daft Punk, Pharrell Williams",
            album = "Random Access Memories",
            imageUrl = null,
            durationMs = 369_000L,
            uri = "demo:track:get-lucky",
        ),
        Track(
            id = "demo-blinding-lights",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            imageUrl = null,
            durationMs = 200_000L,
            uri = "demo:track:blinding-lights",
        ),
    )

    val albums: List<Album> = listOf(
        Album(
            id = "demo-album-hurry-up",
            title = "Hurry Up, We're Dreaming",
            artist = "M83",
            imageUrl = null,
            releaseDate = "2011-10-18",
            uri = "demo:album:hurry-up",
        ),
        Album(
            id = "demo-album-ram",
            title = "Random Access Memories",
            artist = "Daft Punk",
            imageUrl = null,
            releaseDate = "2013-05-17",
            uri = "demo:album:ram",
        ),
        Album(
            id = "demo-album-after-hours",
            title = "After Hours",
            artist = "The Weeknd",
            imageUrl = null,
            releaseDate = "2020-03-20",
            uri = "demo:album:after-hours",
        ),
        Album(
            id = "demo-album-currents",
            title = "Currents",
            artist = "Tame Impala",
            imageUrl = null,
            releaseDate = "2015-07-17",
            uri = "demo:album:currents",
        ),
        Album(
            id = "demo-album-in-rainbows",
            title = "In Rainbows",
            artist = "Radiohead",
            imageUrl = null,
            releaseDate = "2007-10-10",
            uri = "demo:album:in-rainbows",
        ),
        Album(
            id = "demo-album-future-nostalgia",
            title = "Future Nostalgia",
            artist = "Dua Lipa",
            imageUrl = null,
            releaseDate = "2020-03-27",
            uri = "demo:album:future-nostalgia",
        ),
    )
}
