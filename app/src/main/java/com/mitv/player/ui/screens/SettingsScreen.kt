package com.mitv.player.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitv.player.data.*
import com.mitv.player.ui.theme.LocalAccentColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MiTVRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.settingsFlow, repository.playlists) { settings, playlists ->
                SettingsUiState(settings = settings, playlists = playlists)
            }.collect { state -> _uiState.value = state }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.updateSettings(_uiState.value.settings.copy(theme = theme))
        }
    }

    fun toggleGestures(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(_uiState.value.settings.copy(playerGestures = enabled))
        }
    }

    fun toggleAutoRetry(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(_uiState.value.settings.copy(autoRetry = enabled))
        }
    }

    fun setBufferSize(size: BufferSize) {
        viewModelScope.launch {
            repository.updateSettings(_uiState.value.settings.copy(bufferSize = size))
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.clearCache()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

// ─── Settings Screen ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accent = LocalAccentColor.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Theme Section ────────────────────────────────────────────────
            item {
                SettingsSectionHeader("🎨  Appearance")
                ThemeSelector(
                    currentTheme = uiState.settings.theme,
                    onThemeSelected = { theme ->
                        viewModel.setTheme(theme)
                        onThemeChanged(theme)
                    }
                )
            }

            // ── Player Section ───────────────────────────────────────────────
            item {
                SettingsSectionHeader("▶️  Player")

                SettingsToggleItem(
                    title = "Gesture Controls",
                    subtitle = "Swipe to adjust brightness & volume",
                    checked = uiState.settings.playerGestures,
                    onCheckedChange = viewModel::toggleGestures,
                    icon = Icons.Default.TouchApp
                )

                SettingsToggleItem(
                    title = "Auto Retry",
                    subtitle = "Automatically retry on stream failure",
                    checked = uiState.settings.autoRetry,
                    onCheckedChange = viewModel::toggleAutoRetry,
                    icon = Icons.Default.Autorenew
                )

                SettingsSectionHeader("Buffer Size")
                BufferSizeSelector(
                    currentSize = uiState.settings.bufferSize,
                    onSizeSelected = viewModel::setBufferSize
                )
            }

            // ── Playlists Section ─────────────────────────────────────────────
            item {
                SettingsSectionHeader("📋  Playlists")
            }
            if (uiState.playlists.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No playlists. Add one from the home screen.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    }
                }
            } else {
                items(uiState.playlists) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onDelete = { viewModel.deletePlaylist(playlist) }
                    )
                }
            }

            // ── Storage Section ───────────────────────────────────────────────
            item {
                SettingsSectionHeader("🗂  Storage")
                SettingsActionItem(
                    title = "Clear Cache",
                    subtitle = "Free up cached media data",
                    icon = Icons.Default.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    isLoading = uiState.isLoading,
                    onClick = { showClearConfirm = true }
                )
            }

            // ── About Section ────────────────────────────────────────────────
            item {
                SettingsSectionHeader("ℹ️  About")
                AboutCard()
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Cache?") },
            text = { Text("All cached media data will be deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCache(); showClearConfirm = false }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────
@Composable
private fun SettingsSectionHeader(title: String) {
    val accent = LocalAccentColor.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = accent,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    val accent = LocalAccentColor.current
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTheme.entries.forEach { theme ->
            val isSelected = theme == currentTheme
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) accent else MaterialTheme.colorScheme.outline,
                label = "theme_border"
            )
            Surface(
                onClick = { onThemeSelected(theme) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onThemeSelected(theme) },
                        colors = RadioButtonDefaults.colors(selectedColor = accent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        theme.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun BufferSizeSelector(currentSize: BufferSize, onSizeSelected: (BufferSize) -> Unit) {
    val accent = LocalAccentColor.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BufferSize.entries.forEach { size ->
            FilterChip(
                selected = size == currentSize,
                onClick = { onSizeSelected(size) },
                label = { Text(size.displayName) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = Color.Black
                )
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val accent = LocalAccentColor.current
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
        leadingContent = { Icon(icon, null, tint = accent) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accent)
            )
        }
    )
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium, color = tint) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
        leadingContent = {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = tint, strokeWidth = 2.dp)
            else Icon(icon, null, tint = tint)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun PlaylistItem(playlist: Playlist, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(playlist.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                playlist.url.take(50) + if (playlist.url.length > 50) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        },
        leadingContent = { Icon(Icons.Default.PlaylistPlay, null, tint = LocalAccentColor.current) },
        trailingContent = {
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    )
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Remove Playlist?") },
            text = { Text("Remove \"${playlist.name}\" from your playlists?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AboutCard() {
    val accent = LocalAccentColor.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("MiTV Player", style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Black)
            Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(Modifier.height(4.dp))
            Text("Premium IPTV Experience", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}
