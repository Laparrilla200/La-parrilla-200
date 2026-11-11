package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ParrillaColorScheme = darkColorScheme(
  primary = GrillCrimson,
  onPrimary = PureWhite,
  primaryContainer = GrillRedDark,
  onPrimaryContainer = PureWhite,
  secondary = GrillFireRed,
  onSecondary = PureWhite,
  secondaryContainer = SurfaceVariantDark,
  onSecondaryContainer = PureWhite,
  tertiary = AmberWarning,
  onTertiary = CharcoalBlack,
  background = CharcoalBlack,
  onBackground = PureWhite,
  surface = DeepCharcoal,
  onSurface = PureWhite,
  surfaceVariant = SurfaceCharcoal,
  onSurfaceVariant = OffWhite,
  outline = OutlineDark,
  error = GrillRedLight,
  onError = CharcoalBlack
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = ParrillaColorScheme,
    typography = Typography,
    content = content
  )
}
