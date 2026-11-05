package com.spicetify.mobile.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spicetify.mobile.data.Album
import com.spicetify.mobile.ui.AppViewModel
import com.spicetify.mobile.ui.Routes
import com.spicetify.mobile.ui.components.AppBackground
import com.spicetify.mobile.ui.components.DetailHeader
import com.spicetify.mobile.ui.components.InfoBanner
import com.spicetify.mobile.ui.components.TrackArtwork
import com.spicetify.mobile.ui.formatReleaseYear
import com.spicetify.mobile.ui.openSpotifyUrl

@Composable
fun NewReleasesScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val releases by viewModel.releases.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadNewReleases()
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            DetailHeader(
                title = "Novedades",
                subtitle = "Lanzamientos recientes de Spotify",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadNewReleases(force = true) }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Actualizar novedades",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )

            if (releases.usingDemo) {
                InfoBanner(
                    text = releases.error
                        ?: "Mostrando ejemplos. Conectá Spotify para ver los lanzamientos reales.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    actionLabel = "Conectar",
                    onAction = { onNavigate(Routes.SETTINGS) },
                )
                Spacer(modifier = Modifier.size(8.dp))
            }

            if (releases.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (releases.error != null && !releases.usingDemo) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            InfoBanner(text = releases.error.orEmpty())
                        }
                    }

                    items(releases.albums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { openAlbum(context, album) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        TrackArtwork(
            imageUrl = album.imageUrl,
            title = album.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 18.dp,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatReleaseYear(album.releaseDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun openAlbum(context: Context, album: Album) {
    if (album.id.startsWith("demo")) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(openSpotifyUrl(album.id, "album"))),
        )
    }
}
