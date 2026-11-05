package com.spicetify.mobile.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spicetify.mobile.data.Track
import com.spicetify.mobile.ui.AppViewModel
import com.spicetify.mobile.ui.Routes
import com.spicetify.mobile.ui.components.AppBackground
import com.spicetify.mobile.ui.components.DetailHeader
import com.spicetify.mobile.ui.components.InfoBanner
import com.spicetify.mobile.ui.components.TrackArtwork
import com.spicetify.mobile.ui.formatDuration
import com.spicetify.mobile.ui.openSpotifyUrl

@Composable
fun SearchScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }

    LaunchedEffect(search.error) {
        // Nothing to do: the error is rendered below.
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            DetailHeader(
                title = "Buscar",
                subtitle = "Catálogo de Spotify",
                onBack = onBack,
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.updateSearchQuery(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Canciones, artistas, álbumes…") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { viewModel.searchTracks(query) }) {
                                Icon(Icons.Rounded.Search, contentDescription = "Buscar")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchTracks(query) }),
                    shape = MaterialTheme.shapes.medium,
                )

                if (!settings.isConnected) {
                    Spacer(modifier = Modifier.size(12.dp))
                    InfoBanner(
                        text = "Conectá tu cuenta para buscar en Spotify.",
                        actionLabel = "Conectar",
                        onAction = { onNavigate(Routes.SETTINGS) },
                    )
                }

                if (playback.error != null && !playback.isDemo) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = playback.error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            when {
                search.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                search.error != null && search.results.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = search.error.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        if (!settings.isConnected) {
                            TextButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                                Text("Ir a Ajustes")
                            }
                        }
                    }
                }

                search.results.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Escribí y tocá buscar para explorar el catálogo.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(search.results, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            onPlay = { viewModel.playTrack(track) },
                            onOpenExternal = { openInSpotify(context, track) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    onPlay: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Card(
        onClick = onPlay,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(
                imageUrl = track.imageUrl,
                title = track.title,
                modifier = Modifier.size(52.dp),
                cornerRadius = 12.dp,
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onOpenExternal) {
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Abrir en Spotify",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun openInSpotify(context: Context, track: Track) {
    if (track.id.startsWith("demo")) return
    val url = openSpotifyUrl(track.id, "track")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
