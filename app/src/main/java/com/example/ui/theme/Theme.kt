package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Dark Natural Tones scheme
private val DarkColorScheme = darkColorScheme(
    primary = MauveRose,
    secondary = EarthChocolate,
    tertiary = SafariOlive,
    background = Color(0xFF2B2523), // Dark clay
    surface = Color(0xFF38312F),
    surfaceVariant = Color(0xFF4E4340),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SandBg,
    onSurface = SandBg
)

// Light Natural Tones scheme
private val LightColorScheme = lightColorScheme(
    primary = MauveRose,         // Earthy mauve/rose-brown
    secondary = EarthChocolate,   // Deep warm cocoa/chocolate
    tertiary = SafariOlive,       // Soft forest green
    background = SandBg,          // Warm cream background
    surface = Color.White,
    surfaceVariant = PaleSand,    // Soft sand beige container
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = EarthChocolate,
    onSurface = EarthChocolate,
    outlineVariant = ClayBeige
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color support if Android 12+, but prioritize Natural Tones beautifully
    dynamicColor: Boolean = false, // Set to false to enforce our beautiful custom palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
