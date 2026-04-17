package com.mitv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.mitv.player.data.AppTheme
import com.mitv.player.data.Channel
import com.mitv.player.ui.screens.*
import com.mitv.player.ui.theme.MiTVTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiTVApp()
        }
    }
}

@Composable
fun MiTVApp() {
    var currentTheme by remember { mutableStateOf(AppTheme.DARK_GOLD) }
    var currentChannel by remember { mutableStateOf<Channel?>(null) }
    val navController = rememberNavController()

    MiTVTheme(appTheme = currentTheme) {
        NavHost(
            navController = navController,
            startDestination = "dashboard"
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onChannelClick = { channel ->
                        currentChannel = channel
                        val encoded = URLEncoder.encode(
                            Json.encodeToString(channel.toSerializable()),
                            "UTF-8"
                        )
                        navController.navigate("player/$encoded")
                    },
                    onSettingsClick = { navController.navigate("settings") },
                    currentChannel = currentChannel
                )
            }

            composable(
                route = "player/{channelJson}",
                arguments = listOf(navArgument("channelJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("channelJson") ?: return@composable
                val decoded = URLDecoder.decode(encoded, "UTF-8")
                val channel = try {
                    Json.decodeFromString<ChannelSerializable>(decoded).toChannel()
                } catch (e: Exception) {
                    currentChannel ?: return@composable
                }
                PlayerScreen(
                    channel = channel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onThemeChanged = { theme -> currentTheme = theme }
                )
            }
        }
    }
}

// ─── Serializable Channel wrapper ────────────────────────────────────────────
@kotlinx.serialization.Serializable
data class ChannelSerializable(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String,
    val group: String,
    val tvgId: String,
    val tvgName: String,
    val playlistId: String
)

fun Channel.toSerializable() = ChannelSerializable(id, name, url, logoUrl, group, tvgId, tvgName, playlistId)
fun ChannelSerializable.toChannel() = Channel(id, name, url, logoUrl, group, tvgId, tvgName, playlistId)
