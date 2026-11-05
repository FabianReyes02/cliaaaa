package com.spicetify.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.absoluteValue

/**
 * Cover artwork. If there is no remote URL (offline / demo mode) we render a
 * deterministic "generated cover": a two-tone gradient seeded by the title and
 * the album initials, so demo albums don't all look identical.
 */
@Composable
fun TrackArtwork(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Portada de $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            GeneratedCover(title = title, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun GeneratedCover(title: String, modifier: Modifier = Modifier) {
    val seed = title.hashCode().absoluteValue
    val (from, to) = CoverPalettes[seed % CoverPalettes.size]
    val initials = initialsOf(title)

    BoxWithConstraints(
        modifier = modifier.background(Brush.linearGradient(listOf(from, to))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        radius = maxWidth.value * 1.1f,
                    ),
                ),
        )
        if (initials.isBlank()) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.fillMaxSize(0.42f),
            )
        } else {
            Text(
                text = initials,
                color = Color.White.copy(alpha = 0.94f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSizeFor(maxWidthValue = maxWidth.value),
                letterSpacing = (-2).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun fontSizeFor(maxWidthValue: Float): TextUnit = (maxWidthValue * 0.34f).sp

private fun initialsOf(title: String): String {
    val words = title.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return ""
    return if (words.size == 1) {
        words[0].take(2)
    } else {
        words.take(2).joinToString("") { it.first().uppercase() }
    }.uppercase()
}

private val CoverPalettes = listOf(
    Color(0xFF8E2DE2) to Color(0xFF4A00E0),
    Color(0xFF11998E) to Color(0xFF38EF7D),
    Color(0xFFFC466B) to Color(0xFF3F5EFB),
    Color(0xFFF7971E) to Color(0xFFFFD200),
    Color(0xFF00C6FF) to Color(0xFF0072FF),
    Color(0xFFF953C6) to Color(0xFFB91D73),
    Color(0xFF43C6AC) to Color(0xFF191654),
    Color(0xFFEB3349) to Color(0xFFF45C43),
    Color(0xFF1D2671) to Color(0xFFC33764),
    Color(0xFF134E5E) to Color(0xFF71B280),
)
