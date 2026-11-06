package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = TribeGreenPrimaryDark,
  onPrimary = Color(0xFF052E16),
  primaryContainer = TribeGreenDarkContainer,
  onPrimaryContainer = TribeGreenOnContainerDark,
  secondary = TribeTealSecondary,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF134E4A),
  onSecondaryContainer = Color(0xFFCCFBF1),
  tertiary = TribeAmberTertiary,
  onTertiary = Color.White,
  background = TribeBackgroundDark,
  surface = TribeSurfaceDark,
  surfaceVariant = TribeSurfaceVariantDark,
  onBackground = TribeTextPrimaryDark,
  onSurface = TribeTextPrimaryDark,
  onSurfaceVariant = TribeTextSecondaryDark,
  outline = Color(0xFF374151)
)

private val LightColorScheme = lightColorScheme(
  primary = TribeGreenPrimary,
  onPrimary = Color.White,
  primaryContainer = TribeGreenContainer,
  onPrimaryContainer = TribeGreenOnContainer,
  secondary = TribeTealSecondary,
  onSecondary = Color.White,
  secondaryContainer = TribeTealContainer,
  onSecondaryContainer = TribeTealOnContainer,
  tertiary = TribeAmberTertiary,
  onTertiary = Color.White,
  background = TribeBackgroundLight,
  surface = TribeSurfaceLight,
  surfaceVariant = TribeSurfaceVariantLight,
  onBackground = TribeTextPrimaryLight,
  onSurface = TribeTextPrimaryLight,
  onSurfaceVariant = TribeTextSecondaryLight,
  outline = TribeOutlineLight
)

@Composable
fun TribeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
