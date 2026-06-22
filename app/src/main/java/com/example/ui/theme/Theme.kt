package com.example.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF3A1206),
    primaryContainer = Color(0xFF5A2A1A),
    onPrimaryContainer = Color(0xFFF8DDD0),
    secondary = Color(0xFFD2A491),
    background = DarkBackground,
    surface = Color(0xFF262019),
    surfaceVariant = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = Color(0xFFC3B2A4),
    outline = Color(0xFF5A4A40)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = DarkPrimaryText,
    secondary = Color(0xFF9C6B5C),
    background = PolishBg,
    surface = PolishSurface,
    surfaceVariant = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = PolishTextBlack,
    onSurface = PolishTextBlack,
    onSurfaceVariant = PolishTextGrey,
    outline = PolishBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set false to prioritize custom Professional Polish design values
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
