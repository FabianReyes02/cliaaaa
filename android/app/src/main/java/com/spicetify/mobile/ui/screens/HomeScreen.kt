package com.spicetify.mobile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spicetify.mobile.ui.AppViewModel
import com.spicetify.mobile.ui.Routes
import com.spicetify.mobile.ui.components.AppBackground
import com.spicetify.mobile.ui.components.InfoBanner
import com.spicetify.mobile.ui.components.NowPlayingCard
import com.spicetify.mobile.ui.components.SectionHeader

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
) {
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    AppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(accent, secondary))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spicetify",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Mobile port · Android",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                NowPlayingCard(
                    state = playback,
                    onTogglePlay = viewModel::togglePlay,
                    onNext = viewModel::next,
                    onPrevious = viewModel::previous,
                    onSeek = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeat,
                    onToggleSaved = viewModel::toggleSaved,
                    onRefresh = { viewModel.refreshPlayback(showLoading = true) },
                )
            }

            if (!settings.isConnected) {
                item {
                    InfoBanner(
                        text = "Modo demo activo. Conectá tu cuenta para controlar Spotify de verdad.",
                        icon = Icons.Rounded.CloudOff,
                        actionLabel = "Conectar",
                        onAction = { onNavigate(Routes.SETTINGS) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickAction(
                        icon = Icons.Rounded.Search,
                        label = "Buscar",
                        onClick = { onNavigate(Routes.SEARCH) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickAction(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        label = "Letras",
                        onClick = { onNavigate(Routes.LYRICS) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickAction(
                        icon = Icons.Rounded.Palette,
                        label = "Temas",
                        onClick = { onNavigate(Routes.THEMES) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Custom Apps",
                    subtitle = "Las apps del marketplace, llevadas a pantallas nativas",
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CustomAppCard(
                        title = "Letras Plus",
                        subtitle = "Letras sincronizadas vía LRCLIB",
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        onClick = { onNavigate(Routes.LYRICS) },
                        modifier = Modifier.weight(1f),
                    )
                    CustomAppCard(
                        title = "Novedades",
                        subtitle = "Lanzamientos de Spotify",
                        icon = Icons.Rounded.NewReleases,
                        onClick = { onNavigate(Routes.RELEASES) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CustomAppCard(
                        title = "Reddit",
                        subtitle = "r/spotify embebido",
                        icon = Icons.Rounded.Forum,
                        onClick = { onNavigate(Routes.REDDIT) },
                        modifier = Modifier.weight(1f),
                    )
                    CustomAppCard(
                        title = "Temas",
                        subtitle = "Presets, acento y AMOLED",
                        icon = Icons.Rounded.Palette,
                        onClick = { onNavigate(Routes.THEMES) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Text(
                    text = "Port académico de Spicetify CLI. No modifica ni reemplaza la app oficial de Spotify: usa su Web API pública.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CustomAppCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(128.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
