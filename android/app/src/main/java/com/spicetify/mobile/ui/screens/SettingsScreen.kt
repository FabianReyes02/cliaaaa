package com.spicetify.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spicetify.mobile.data.AppSettings
import com.spicetify.mobile.data.Profile
import com.spicetify.mobile.data.SpotifyAuth
import com.spicetify.mobile.ui.AppViewModel
import com.spicetify.mobile.ui.AuthUiState
import com.spicetify.mobile.ui.Routes
import com.spicetify.mobile.ui.components.AppBackground
import com.spicetify.mobile.ui.components.InfoBanner
import com.spicetify.mobile.ui.components.Pill
import com.spicetify.mobile.ui.components.TrackArtwork

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val auth by viewModel.authUi.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var clientId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(settings.clientId) {
        if (clientId.isBlank()) clientId = settings.clientId
    }

    AppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Cuenta, apariencia y detalles del port.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                AccountCard(
                    settings = settings,
                    profile = profile,
                    auth = auth,
                    clientId = clientId,
                    onClientIdChange = { clientId = it },
                    onConnect = { viewModel.connectSpotify(context, clientId) },
                    onDisconnect = viewModel::disconnectSpotify,
                    onCopyRedirect = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Redirect URI", SpotifyAuth.REDIRECT_URI),
                        )
                        Toast.makeText(context, "Redirect URI copiado", Toast.LENGTH_SHORT).show()
                    },
                )
            }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("Apariencia", style = MaterialTheme.typography.titleMedium)
                    }
                    SwitchRow(
                        title = "Color desde la portada",
                        subtitle = "Usa el color dominante del álbum actual.",
                        checked = settings.matchArtwork,
                        onCheckedChange = viewModel::setMatchArtwork,
                    )
                    SwitchRow(
                        title = "Fondo AMOLED",
                        subtitle = "Negro puro en pantallas OLED.",
                        checked = settings.amoled,
                        onCheckedChange = viewModel::setAmoled,
                    )
                    OutlinedButton(
                        onClick = { onNavigate(Routes.THEMES) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Abrir editor de temas")
                    }
                }
            }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("Extensiones incluidas", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "El port traduce las extensiones de Spicetify a funciones nativas de Android.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FeatureRow(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        title = "Letras Plus",
                        subtitle = "Letras sincronizadas desde LRCLIB con auto-scroll y seek.",
                    )
                    FeatureRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Control del reproductor",
                        subtitle = "Play, pausa, siguiente, seek, shuffle y repeat vía Spotify Web API.",
                    )
                    FeatureRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Temas dinámicos",
                        subtitle = "Presets de la comunidad y acento extraído de la portada.",
                    )
                }
            }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("Acerca de", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Spicetify Mobile · versión 1.0.0",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Port académico de Spicetify CLI a Kotlin + Jetpack Compose. No modifica ni reemplaza la app oficial de Spotify: consume su Web API pública y usa LRCLIB para las letras.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Las credenciales y los tokens se guardan localmente en el dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { openUrl(context, "https://github.com/spicetify/cli") }) {
                            Text("Spicetify CLI")
                        }
                        TextButton(onClick = { openUrl(context, "https://developer.spotify.com/dashboard") }) {
                            Text("Spotify Dashboard")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    settings: AppSettings,
    profile: Profile?,
    auth: AuthUiState,
    clientId: String,
    onClientIdChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCopyRedirect: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Cuenta de Spotify",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (settings.isConnected) {
                Pill(text = "Conectado", icon = Icons.Rounded.CheckCircle)
            }
        }

        if (settings.isConnected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackArtwork(
                    imageUrl = profile?.imageUrl,
                    title = profile?.displayName.orEmpty(),
                    modifier = Modifier.size(52.dp),
                    cornerRadius = 26.dp,
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.displayName ?: "Sesión activa",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when (profile?.product) {
                            "premium" -> "Premium · control total del reproductor"
                            "free" -> "Free · reproducción limitada por Spotify"
                            else -> "Cuenta conectada"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Desconectar cuenta")
            }
        } else {
            Text(
                text = "Para controlar Spotify necesitás un Client ID gratuito. Es el mismo flujo PKCE que usa cualquier app móvil: no se guarda ningún client secret.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "1. Creá una app en developer.spotify.com/dashboard\n2. Agregá el Redirect URI de abajo\n3. Pegá el Client ID y conectá",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = clientId,
                onValueChange = onClientIdChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Client ID") },
                shape = MaterialTheme.shapes.medium,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Redirect URI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = SpotifyAuth.REDIRECT_URI,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = onCopyRedirect) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = "Copiar Redirect URI",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Button(
                onClick = onConnect,
                enabled = !auth.busy && clientId.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (auth.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Conectar con Spotify")
                }
            }
        }

        auth.message?.let {
            InfoBanner(text = it, icon = Icons.Rounded.CheckCircle)
        }

        auth.error?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
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

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
