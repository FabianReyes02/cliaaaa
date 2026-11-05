package com.spicetify.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spicetify.mobile.data.AppSettings
import com.spicetify.mobile.data.ThemePreset
import com.spicetify.mobile.data.ThemePresets
import com.spicetify.mobile.ui.AppViewModel
import com.spicetify.mobile.ui.components.AppBackground
import com.spicetify.mobile.ui.components.Pill
import com.spicetify.mobile.ui.components.SectionHeader
import com.spicetify.mobile.ui.components.TrackArtwork

@Composable
fun ThemesScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val selectedPreset = ThemePresets.byId(settings.themeId)

    AppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Temas",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Personalizá la app con la misma idea de los temas de Spicetify.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                SectionHeader(
                    title = "Presets",
                    subtitle = "Inspirados en el marketplace de Spicetify",
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ThemePresets.all, key = { it.id }) { preset ->
                        ThemeCard(
                            preset = preset,
                            selected = settings.themeId == preset.id,
                            onClick = { viewModel.selectTheme(preset.id) },
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Color de acento",
                    subtitle = if (settings.matchArtwork) {
                        "Ahora mismo se toma de la portada de la canción."
                    } else {
                        "Elegí un color fijo para toda la interfaz."
                    },
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        AutoAccentSwatch(
                            selected = settings.matchArtwork,
                            onClick = {
                                viewModel.setMatchArtwork(true)
                                viewModel.selectAccent(null)
                            },
                        )
                    }
                    items(ThemePresets.accents.size) { index ->
                        val color = ThemePresets.accents[index]
                        AccentSwatch(
                            color = color,
                            selected = !settings.matchArtwork &&
                                settings.customAccentArgb == color.toArgb(),
                            onClick = {
                                viewModel.setMatchArtwork(false)
                                viewModel.selectAccent(color.toArgb())
                            },
                        )
                    }
                }
            }

            item {
                SwitchRow(
                    title = "Color desde la portada",
                    subtitle = "Extrae el color dominante del álbum actual (como los temas dinámicos).",
                    checked = settings.matchArtwork,
                    onCheckedChange = { viewModel.setMatchArtwork(it) },
                )
            }

            item {
                SwitchRow(
                    title = "Fondo AMOLED",
                    subtitle = "Usa negro puro para ahorrar batería en pantallas OLED.",
                    checked = settings.amoled,
                    onCheckedChange = { viewModel.setAmoled(it) },
                )
            }

            item {
                SectionHeader(title = "Vista previa")
                Spacer(modifier = Modifier.height(10.dp))
                PreviewCard(settings = settings, presetName = selectedPreset.name, viewModel = viewModel)
            }

            item {
                Text(
                    text = "Los presets son una reinterpretación de temas populares de la comunidad. No se distribuye ningún tema original de Spicetify.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    preset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
    Column(
        modifier = Modifier
            .width(128.dp)
            .height(126.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(preset.accent.copy(alpha = 0.65f), preset.background),
                ),
            )
            .border(if (selected) 2.dp else 1.dp, outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(preset.accent),
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(preset.secondary),
            )
        }

        Column {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Seleccionado",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoAccentSwatch(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.sweepGradient(ThemePresets.accents))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.3f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = "Color automático desde la portada",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Color seleccionado",
                tint = if (color.luminance() > 0.55f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun PreviewCard(
    settings: AppSettings,
    presetName: String,
    viewModel: AppViewModel,
) {
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val track = playback.track

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(
                imageUrl = track?.imageUrl,
                title = track?.title.orEmpty(),
                modifier = Modifier.size(76.dp),
                cornerRadius = 16.dp,
            )
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "Tu música",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track?.artist.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(text = presetName)
                    if (settings.amoled) {
                        Pill(text = "AMOLED", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                ),
        )
    }
}
