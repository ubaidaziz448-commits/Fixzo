package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TrustTealDark,
    secondary = DeepTealDark,
    tertiary = AmberAccentDark,
    background = SlateDarkBackground,
    surface = CardSurfaceDark,
    onPrimary = SlateDarkBackground,
    onSecondary = SlateDarkBackground,
    onBackground = OffWhiteBackground,
    onSurface = OffWhiteBackground
)

private val LightColorScheme = lightColorScheme(
    primary = TrustTealLight,
    secondary = DeepTealLight,
    tertiary = AmberAccentLight,
    background = OffWhiteBackground,
    surface = CardSurfaceLight,
    onPrimary = CardSurfaceLight,
    onSecondary = CardSurfaceLight,
    onBackground = SlateDarkBackground,
    onSurface = SlateDarkBackground
)

@Composable
fun FixzoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to enforce our elegant brand teal theme
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
