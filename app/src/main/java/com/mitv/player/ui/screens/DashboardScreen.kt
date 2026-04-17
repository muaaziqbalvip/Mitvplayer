package com.mitv.player.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitv.player.data.*
import com.mitv.player.ui.components.*
import com.mitv.player.ui.theme.LocalAccentColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MiTVRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observePlaylists()
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            repository.playlists.collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
                if (playlists.isNotEmpty()) loadChannels()
            }
        }
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.loadAllActiveCategories()
            result.fold(
                onSuccess = { categories ->
                    _uiState.update { it.copy(categories = categories, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch {
            repository.addPlaylist(name, url)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun getFilteredChannels(): List<Channel> {
        val state = _uiState.value
        val allChannels = if (state.selectedCategory != null) {
            state.categories.find { it.name == state.selectedCategory }?.channels ?: emptyList()
        } else {
            state.categories.flatMap { it.channels }
        }
        return if (state.searchQuery.isBlank()) allChannels
        else allChannels.filter {
            it.name.contains(state.searchQuery, ignoreCase = true) ||
            it.group.contains(state.searchQuery, ignoreCase = true)
        }
    }
}

// ─── Dashboard Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onChannelClick: (Channel) -> Unit,
    onSettingsClick: () -> Unit,
    currentChannel: Channel? = null,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accent = LocalAccentColor.current
    val focusManager = LocalFocusManager.current
    var showAddPlaylist by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mi",
                            style = MaterialTheme.typography.headlineMedium,
                            color = accent,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "TV",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " Player",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPlaylist = true }) {
                        Icon(Icons.Default.Add, "Add Playlist", tint = accent)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Search Bar ───────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search channels…", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateSearch("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // ── Category Chips ───────────────────────────────────────────────
            if (uiState.categories.isNotEmpty()) {
                CategoryChipRow(
                    categories = uiState.categories.map { it.name },
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::selectCategory,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(Modifier.height(4.dp))
            }

            // ── Channel Count ────────────────────────────────────────────────
            val filteredChannels = viewModel.getFilteredChannels()
            if (!uiState.isLoading && filteredChannels.isNotEmpty()) {
                Text(
                    text = "${filteredChannels.size} channels",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // ── Content Area ─────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = accent)
                            Spacer(Modifier.height(12.dp))
                            Text("Loading channels…", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(uiState.error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = viewModel::loadChannels) { Text("Retry") }
                        }
                    }
                    uiState.playlists.isEmpty() -> {
                        EmptyPlaylistView(onAddPlaylist = { showAddPlaylist = true })
                    }
                    filteredChannels.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No channels found", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredChannels, key = { it.id }) { channel ->
                                ChannelCard(
                                    channel = channel,
                                    isPlaying = currentChannel?.id == channel.id,
                                    onClick = { onChannelClick(channel) }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    // ── Add Playlist Dialog ──────────────────────────────────────────────────
    if (showAddPlaylist) {
        AddPlaylistDialog(
            onDismiss = { showAddPlaylist = false },
            onAdd = { name, url ->
                viewModel.addPlaylist(name, url)
                showAddPlaylist = false
            }
        )
    }
}

@Composable
private fun EmptyPlaylistView(onAddPlaylist: () -> Unit) {
    val accent = LocalAccentColor.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📺", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("No playlists added", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Add an M3U playlist to start watching", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddPlaylist,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("Add Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    val accent = LocalAccentColor.current
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add M3U Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("M3U URL") },
                    placeholder = { Text("http://…") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && url.isNotBlank()) onAdd(name.trim(), url.trim()) },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("Add", color = accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
