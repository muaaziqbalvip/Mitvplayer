package com.mitv.player.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mitv.player.data.Channel
import com.mitv.player.ui.theme.LocalAccentColor
import com.mitv.player.ui.theme.MiTVGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelCard(
    channel: Channel,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
    val isPlayingAnim by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        label = "playing_anim"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isPlaying) BorderStroke(1.5.dp, accent) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPlaying) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Channel Logo ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    // Fallback: first letter of channel name
                    Text(
                        text = channel.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = accent
                    )
                }

                // ── Now Playing Indicator ────────────────────────────────────
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(accent.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Now Playing",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Channel Info ─────────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlaying) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = channel.group,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Live Badge ───────────────────────────────────────────────────
            if (channel.url.contains("m3u8", ignoreCase = true)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isPlaying) accent else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
