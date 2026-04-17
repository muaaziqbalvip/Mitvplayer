package com.mitv.player.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.mitv.player.data.AppTheme

// ─── Color Schemes ────────────────────────────────────────────────────────────
private val DarkGoldColorScheme = darkColorScheme(
    primary          = DarkGold_Primary,
    onPrimary        = DarkGold_OnPrimary,
    primaryContainer = DarkGold_SurfaceVar,
    background       = DarkGold_Background,
    surface          = DarkGold_Surface,
    surfaceVariant   = DarkGold_SurfaceVar,
    onBackground     = DarkGold_OnBackground,
    onSurface        = DarkGold_OnSurface,
    outline          = DarkGold_Outline,
    error            = ColorError
)

private val MidnightBlueColorScheme = darkColorScheme(
    primary          = MidBlue_Primary,
    onPrimary        = DarkGold_OnPrimary,
    primaryContainer = MidBlue_SurfaceVar,
    background       = MidBlue_Background,
    surface          = MidBlue_Surface,
    surfaceVariant   = MidBlue_SurfaceVar,
    onBackground     = MidBlue_OnBackground,
    onSurface        = MidBlue_OnSurface,
    outline          = MidBlue_Outline,
    error            = ColorError
)

private val AmoledBlackColorScheme = darkColorScheme(
    primary          = Amoled_Primary,
    onPrimary        = Amoled_Background,
    primaryContainer = Amoled_SurfaceVar,
    background       = Amoled_Background,
    surface          = Amoled_Surface,
    surfaceVariant   = Amoled_SurfaceVar,
    onBackground     = Amoled_OnBackground,
    onSurface        = Amoled_OnSurface,
    outline          = Amoled_Outline,
    error            = ColorError
)

private val CrimsonDarkColorScheme = darkColorScheme(
    primary          = Crimson_Primary,
    onPrimary        = Crimson_Background,
    primaryContainer = Crimson_SurfaceVar,
    background       = Crimson_Background,
    surface          = Crimson_Surface,
    surfaceVariant   = Crimson_SurfaceVar,
    onBackground     = Crimson_OnBackground,
    onSurface        = Crimson_OnSurface,
    outline          = Crimson_Outline,
    error            = ColorError
)

fun getColorScheme(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.DARK_GOLD      -> DarkGoldColorScheme
    AppTheme.MIDNIGHT_BLUE  -> MidnightBlueColorScheme
    AppTheme.AMOLED_BLACK   -> AmoledBlackColorScheme
    AppTheme.CRIMSON_DARK   -> CrimsonDarkColorScheme
}

// ─── Local CompositionLocal for accent ────────────────────────────────────────
val LocalAccentColor = compositionLocalOf { MiTVGold }

@Composable
fun MiTVTheme(
    appTheme: AppTheme = AppTheme.DARK_GOLD,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(appTheme)
    val accentColor = when (appTheme) {
        AppTheme.DARK_GOLD     -> MiTVGold
        AppTheme.MIDNIGHT_BLUE -> MidBlue_Primary
        AppTheme.AMOLED_BLACK  -> Amoled_Primary
        AppTheme.CRIMSON_DARK  -> Crimson_Primary
    }

    CompositionLocalProvider(LocalAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography   = MiTVTypography,
            content      = content
        )
    }
}
