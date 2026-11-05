package com.spicetify.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spicetify.mobile.data.DemoData
import com.spicetify.mobile.data.LyricsPayload
import com.spicetify.mobile.ui.AppViewModel
import com.spicetify.mobile.ui.components.AppBackground
import com.spicetify.mobile.ui.components.InfoBanner
import com.spicetify.mobile.ui.components.Pill

@Composable
fun LyricsScreen(viewModel: AppViewModel) {
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val track = playback.track ?: DemoData.tracks.first()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(track.id) {
        viewModel.loadLyrics(track)
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Letras Plus", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${track.title} · ${track.artist}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Pill(
                        text = if (settings.isConnected) "Spotify" else "Demo",
                        tint = if (settings.isConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Buscar otra letra en LRCLIB…") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (query.isNotBlank()) viewModel.searchLyrics(query)
                            },
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Buscar letra")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) viewModel.searchLyrics(query)
                        },
                    ),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            when {
                lyrics.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                lyrics.error != null -> Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoBanner(
                        text = lyrics.error.orEmpty(),
                        icon = Icons.Rounded.ErrorOutline,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.loadLyrics(track, force = true) }) {
                            Text("Reintentar")
                        }
                        TextButton(
                            onClick = {
                                query = "${track.artist} ${track.title}"
                                viewModel.searchLyrics(query)
                            },
                        ) {
                            Text("Buscar por nombre")
                        }
                    }
                }

                lyrics.payload != null -> LyricsContent(
                    payload = lyrics.payload!!,
                    progressMs = playback.progressMs,
                    onSeek = viewModel::seekTo,
                    modifier = Modifier.weight(1f),
                )

                else -> EmptyLyrics()
            }
        }
    }
}

@Composable
private fun EmptyLyrics() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Elegí una canción o buscá una letra para empezar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LyricsContent(
    payload: LyricsPayload,
    progressMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = payload.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Pill(
                text = when {
                    payload.instrumental -> "Instrumental"
                    payload.lines.isNotEmpty() -> "Sincronizada"
                    else -> "Texto"
                },
            )
        }

        when {
            payload.instrumental -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Esta canción es instrumental.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            payload.lines.isNotEmpty() -> SyncedLyrics(
                payload = payload,
                progressMs = progressMs,
                onSeek = onSeek,
            )

            !payload.plain.isNullOrBlank() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = payload.plain.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35f,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Fuente: LRCLIB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SyncedLyrics(
    payload: LyricsPayload,
    progressMs: Long,
    onSeek: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val activeIndex = payload.lines
        .indexOfLast { it.timeMs <= progressMs }
        .coerceAtLeast(0)

    LaunchedEffect(activeIndex) {
        if (activeIndex in payload.lines.indices) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 120.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(payload.lines) { index, line ->
            val active = index == activeIndex
            Text(
                text = line.text,
                style = if (active) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (index < activeIndex) 0.45f else 0.8f,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 2.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Fuente: LRCLIB · tocá una línea para saltar",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
