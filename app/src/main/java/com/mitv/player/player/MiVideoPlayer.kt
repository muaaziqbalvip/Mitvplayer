package com.mitv.player.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.*
import androidx.media3.datasource.cache.*
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.*
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.mitv.player.data.Channel
import com.mitv.player.data.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MiVideoPlayer"
private const val CACHE_SIZE_BYTES = 100L * 1024 * 1024 // 100 MB

@UnstableApi
@Singleton
class MiVideoPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ── ExoPlayer & Cache ────────────────────────────────────────────────────
    private val cache: SimpleCache by lazy {
        val cacheDir = File(context.cacheDir, "mitv_media_cache")
        SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES), StandaloneDatabaseProvider(context))
    }

    private val httpDataSourceFactory: HttpDataSource.Factory by lazy {
        DefaultHttpDataSource.Factory()
            .setUserAgent("MiTV-Player/1.0")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
    }

    private val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val player: ExoPlayer by lazy {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs */ 5_000,
                        /* maxBufferMs */ 30_000,
                        /* bufferForPlaybackMs */ 2_000,
                        /* bufferForPlaybackAfterRebufferMs */ 5_000
                    )
                    .build()
            )
            .build()
            .also { exo ->
                exo.addListener(playerListener)
                exo.playWhenReady = true
            }
    }

    // ── State Flow ───────────────────────────────────────────────────────────
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentChannel: Channel? = null
    private var retryCount = 0
    private val maxRetries = 3

    // ── Player Listener ───────────────────────────────────────────────────────
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val ch = currentChannel ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playbackState.value = PlaybackState.Buffering(ch, 0)
                }
                Player.STATE_READY -> {
                    retryCount = 0
                    _playbackState.value = if (player.playWhenReady)
                        PlaybackState.Playing(ch) else PlaybackState.Paused(ch)
                }
                Player.STATE_ENDED  -> _playbackState.value = PlaybackState.Idle
                Player.STATE_IDLE   -> { /* handled in onPlayerError */ }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val ch = currentChannel ?: return
            if (_playbackState.value !is PlaybackState.Buffering) {
                _playbackState.value = if (isPlaying)
                    PlaybackState.Playing(ch) else PlaybackState.Paused(ch)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val ch = currentChannel ?: return
            Log.e(TAG, "Player error: ${error.message}", error)
            if (retryCount < maxRetries) {
                retryCount++
                Log.d(TAG, "Retrying… attempt $retryCount")
                player.prepare()
            } else {
                _playbackState.value = PlaybackState.Error(ch, error.message ?: "Playback failed")
            }
        }

        override fun onBufferedPercentageChanged(bufferedPercentage: Int) {
            val ch = currentChannel ?: return
            if (_playbackState.value is PlaybackState.Buffering) {
                _playbackState.value = PlaybackState.Buffering(ch, bufferedPercentage)
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────
    fun play(channel: Channel) {
        if (currentChannel?.url == channel.url &&
            _playbackState.value is PlaybackState.Playing) return

        currentChannel = channel
        retryCount = 0
        _playbackState.value = PlaybackState.Loading

        val mediaItem = buildMediaItem(channel.url)
        val mediaSource = buildMediaSource(channel.url, mediaItem)

        player.stop()
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        player.playWhenReady = !player.playWhenReady
    }

    fun seekForward(ms: Long = 10_000) {
        player.seekTo(player.currentPosition + ms)
    }

    fun seekBackward(ms: Long = 10_000) {
        player.seekTo(maxOf(0, player.currentPosition - ms))
    }

    fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    fun retry() {
        retryCount = 0
        currentChannel?.let { play(it) }
    }

    fun release() {
        player.removeListener(playerListener)
        player.release()
        cache.release()
    }

    // ── Media Source Builder ─────────────────────────────────────────────────
    private fun buildMediaItem(url: String): MediaItem =
        MediaItem.Builder()
            .setUri(Uri.parse(url))
            .build()

    private fun buildMediaSource(url: String, mediaItem: MediaItem): MediaSource {
        return when {
            url.contains(".m3u8", ignoreCase = true) ||
            url.contains("hls", ignoreCase = true) -> {
                HlsMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            url.contains(".mpd", ignoreCase = true) ||
            url.contains("dash", ignoreCase = true) -> {
                DashMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            url.contains("youtube.com") ||
            url.contains("youtu.be") -> {
                // YouTube requires yt-dlp extraction; use a proxy URL if configured
                ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }
}
