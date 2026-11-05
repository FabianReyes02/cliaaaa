package com.spicetify.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spicetify.mobile.ui.PlaybackUi
import com.spicetify.mobile.ui.formatDuration

@Composable
fun NowPlayingCard(
    state: PlaybackUi,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleSaved: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.track
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val duration = (track?.durationMs ?: 1L).coerceAtLeast(1L)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row {
                TrackArtwork(
                    imageUrl = track?.imageUrl,
                    title = track?.title.orEmpty(),
                    modifier = Modifier.size(116.dp),
                    cornerRadius = 20.dp,
                )

                Spacer(modifier = Modifier.size(16.dp))

                Column(modifier = Modifier.height(116.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (state.isDemo) "VISTA PREVIA" else "REPRODUCIENDO",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = accent,
                            )
                        } else {
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "Actualizar reproducción",
                                    tint = muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = track?.title ?: "Sin reproducción",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track?.artist.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.isDemo) {
                            Pill(text = "Modo demo", tint = MaterialTheme.colorScheme.secondary)
                        } else if (!state.deviceName.isNullOrBlank()) {
                            Pill(
                                text = state.deviceName,
                                icon = Icons.Rounded.Devices,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = onToggleSaved,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = if (state.isSaved) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Rounded.FavoriteBorder
                                },
                                contentDescription = "Guardar canción",
                                tint = if (state.isSaved) accent else muted,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            var dragValue by remember(track?.id) { mutableFloatStateOf(state.progressMs.toFloat()) }
            var dragging by remember { mutableStateOf(false) }

            LaunchedEffect(state.progressMs, dragging) {
                if (!dragging) dragValue = state.progressMs.toFloat()
            }

            Slider(
                value = dragValue.coerceIn(0f, duration.toFloat()),
                onValueChange = { value ->
                    dragging = true
                    dragValue = value
                },
                onValueChangeFinished = {
                    dragging = false
                    onSeek(dragValue.toLong())
                },
                valueRange = 0f..duration.toFloat(),
                enabled = track != null,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = muted.copy(alpha = 0.25f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(if (dragging) dragValue.toLong() else state.progressMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatDuration(track?.durationMs ?: 0L),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (state.shuffle) accent else muted,
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Anterior",
                        modifier = Modifier.size(34.dp),
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente",
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.repeat == "track") {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = "Repetir",
                            tint = if (state.repeat == "off") muted else accent,
                        )
                    }
                }
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Reintentar", color = accent)
                    }
                }
            }
        }
    }
}
