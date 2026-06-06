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

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF1E2436),
    onPrimary = Color(0xFF00305F),
    onPrimaryContainer = Color(0xFFD3E3FD),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC4C7C5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    background = BackgroundLight,
    surface = Color.White,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF001D35),
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = GrayText,
    outline = Color(0xFFC4C7C5),
    outlineVariant = Color(0xFFE3E3E3)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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
