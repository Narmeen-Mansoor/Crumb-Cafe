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

private val DarkColorScheme = darkColorScheme(
    primary = WarmSage,
    secondary = DaisyGold,
    tertiary = SoftPink,
    background = WarmEarth,
    surface = CozyBrown,
    onPrimary = ButtercreamSand,
    onSecondary = ButtercreamSand,
    onTertiary = ButtercreamSand,
    onBackground = ButtercreamSand,
    onSurface = ButtercreamSand
)

private val LightColorScheme = lightColorScheme(
    primary = WarmSage,
    secondary = DaisyGold,
    tertiary = LightCoral,
    background = ButtercreamSand,
    surface = TonalSand,
    onPrimary = Color.White,
    onSecondary = WarmEarth,
    onTertiary = Color.White,
    onBackground = WarmEarth,
    onSurface = WarmEarth
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We override dynamicColor with false to preserve the custom cozy cafe colors exactly
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
