package com.mitv.player.ui.screens

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mitv.player.data.*
import com.mitv.player.player.MiVideoPlayer
import com.mitv.player.ui.components.FallbackView
import com.mitv.player.ui.theme.LocalAccentColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.abs

private const val CONTROLS_HIDE_DELAY = 3500L

// ─── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val miPlayer: MiVideoPlayer
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = miPlayer.playbackState
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var hideControlsJob: Job? = null

    fun playChannel(channel: Channel) {
        miPlayer.play(channel)
        _uiState.update { it.copy(currentChannel = channel, showFallback = false) }
        scheduleHideControls()
    }

    fun togglePlayPause() {
        miPlayer.togglePlayPause()
        scheduleHideControls()
    }

    fun toggleControls() {
        val current = _uiState.value.showControls
        _uiState.update { it.copy(showControls = !current) }
        if (!current) scheduleHideControls()
        else hideControlsJob?.cancel()
    }

    fun showControls() {
        _uiState.update { it.copy(showControls = true) }
        scheduleHideControls()
    }

    fun updateBrightness(delta: Float) {
        val new = (_uiState.value.brightness + delta).coerceIn(0f, 1f)
        _uiState.update { it.copy(brightness = new) }
    }

    fun updateVolume(delta: Float) {
        val new = (_uiState.value.volume + delta).coerceIn(0f, 1f)
        _uiState.update { it.copy(volume = new) }
        miPlayer.setVolume(new)
    }

    fun retry() {
        miPlayer.retry()
        _uiState.update { it.copy(showFallback = false) }
    }

    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(CONTROLS_HIDE_DELAY)
            _uiState.update { it.copy(showControls = false) }
        }
    }

    val player get() = miPlayer.player

    init {
        viewModelScope.launch {
            playbackState.collect { state ->
                _uiState.update { it.copy(playbackState = state) }
                if (state is PlaybackState.Error) {
                    _uiState.update { it.copy(showFallback = true) }
                }
            }
        }
    }
}

// ─── Player Screen ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accent = LocalAccentColor.current
    val context = LocalContext.current

    // Lock landscape
    LaunchedEffect(Unit) {
        (context as? ComponentActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    DisposableEffect(Unit) {
        onDispose {
            (context as? ComponentActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(channel) {
        viewModel.playChannel(channel)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .gestureDetector(
                onTap = { viewModel.toggleControls() },
                onVerticalDrag = { x, dy, width ->
                    if (x < width / 2f) viewModel.updateBrightness(-dy / 300f)
                    else viewModel.updateVolume(-dy / 300f)
                }
            )
    ) {
        // ── ExoPlayer Surface ────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Fallback View ────────────────────────────────────────────────────
        val errorState = uiState.playbackState as? PlaybackState.Error
        if (uiState.showFallback && errorState != null) {
            FallbackView(
                channelName = channel.name,
                errorMessage = errorState.message,
                onRetry = viewModel::retry,
                onContactSupport = {
                    // Open support URL
                }
            )
        }

        // ── Buffering Indicator ──────────────────────────────────────────────
        if (uiState.playbackState is PlaybackState.Buffering ||
            uiState.playbackState is PlaybackState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent, strokeWidth = 3.dp)
            }
        }

        // ── Player Controls Overlay ──────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControlsOverlay(
                channel = channel,
                uiState = uiState,
                accent = accent,
                onBack = {
                    onBack()
                },
                onPlayPause = viewModel::togglePlayPause
            )
        }

        // ── Gesture Indicators ───────────────────────────────────────────────
        if (uiState.showControls) {
            GestureIndicators(uiState = uiState, accent = accent)
        }

        // ── MiTV Watermark ───────────────────────────────────────────────────
        MiTVWatermark(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun PlayerControlsOverlay(
    channel: Channel,
    uiState: PlayerUiState,
    accent: Color,
    onBack: () -> Unit,
    onPlayPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.7f),
                    0.4f to Color.Transparent,
                    0.8f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.8f)
                )
            )
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    channel.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    channel.group,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.weight(1f))
            // Live badge
            if (channel.url.contains("m3u8", ignoreCase = true)) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color.Red) {
                    Text("● LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }

        // ── Center Play/Pause ────────────────────────────────────────────────
        Box(Modifier.align(Alignment.Center)) {
            val isPlaying = uiState.playbackState is PlaybackState.Playing
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // ── Bottom Info ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusText = when (val state = uiState.playbackState) {
                is PlaybackState.Playing -> "▶ Playing"
                is PlaybackState.Paused  -> "⏸ Paused"
                is PlaybackState.Buffering -> "⏳ Buffering ${state.percent}%"
                is PlaybackState.Loading -> "⏳ Loading…"
                is PlaybackState.Error   -> "⚠ Error"
                else -> ""
            }
            Text(statusText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.weight(1f))
            Text("MiTV Player", style = MaterialTheme.typography.labelSmall, color = accent.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun GestureIndicators(uiState: PlayerUiState, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brightness indicator
        GestureSlider(
            icon = Icons.Default.Brightness6,
            value = uiState.brightness,
            label = "Brightness",
            color = Color.Yellow.copy(alpha = 0.8f)
        )
        Spacer(Modifier.weight(1f))
        // Volume indicator
        GestureSlider(
            icon = if (uiState.volume > 0f) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            value = uiState.volume,
            label = "Volume",
            color = accent
        )
    }
}

@Composable
private fun GestureSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxHeight().wrapContentHeight(Alignment.CenterVertically)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(80.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value)
                    .align(Alignment.BottomCenter)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        Text(
            "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MiTVWatermark(modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data("https://i.ibb.co/5hPyzP10/1773218533375-removebg-preview.png")
            .crossfade(true)
            .build(),
        contentDescription = "MiTV Logo",
        modifier = modifier
            .size(80.dp)
            .alpha(0.35f)
    )
}

// ─── Gesture Modifier ─────────────────────────────────────────────────────────
private fun Modifier.gestureDetector(
    onTap: () -> Unit,
    onVerticalDrag: (x: Float, dy: Float, width: Float) -> Unit
): Modifier = this.pointerInput(Unit) {
    var totalDrag = 0f
    var startX = 0f
    var isSwiping = false

    detectDragGestures(
        onDragStart = { offset ->
            startX = offset.x
            totalDrag = 0f
            isSwiping = false
        },
        onDrag = { change, dragAmount ->
            change.consume()
            totalDrag += abs(dragAmount.y)
            if (totalDrag > 10f) {
                isSwiping = true
                onVerticalDrag(startX, dragAmount.y, size.width.toFloat())
            }
        },
        onDragEnd = {
            if (!isSwiping) onTap()
        }
    )
}
