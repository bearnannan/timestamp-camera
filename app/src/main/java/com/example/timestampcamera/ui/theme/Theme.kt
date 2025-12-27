package com.example.timestampcamera.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// Premium Color Palette
val GoldAccent = Color(0xFFFFCC00)
val GoldDark = Color(0xFFFFA500)
val DarkOverlay = Color(0x99000000)
val GlassWhite = Color(0x40FFFFFF)
val GlassDark = Color(0x60000000)
val PremiumBlack = Color(0xFF1A1A1A)

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = GoldDark,
    background = PremiumBlack,
    surface = PremiumBlack,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GoldAccent,
    secondary = GoldDark
)

@Composable
fun TimestampCameraTheme(
    darkTheme: Boolean = true, // Always dark for camera app
    // Disable dynamic color for consistent black UI
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to BLACK
            window.statusBarColor = android.graphics.Color.BLACK
            // Set navigation bar to BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            // Light icons on dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
