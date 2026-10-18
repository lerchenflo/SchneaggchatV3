package org.lerchenflo.schneaggchatv3mp.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import org.lerchenflo.schneaggchatv3mp.settings.domain.ThemeSetting

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val phTheme = darkColorScheme(
    primary = phOragne,
    onPrimary = phDarkGray,
    primaryContainer = phOrangeLightVariant,
    onPrimaryContainer = phDarkGray,
    secondary = phGray,
    onSecondary = phWhite,
    secondaryContainer = phDarkGray,
    onSecondaryContainer = phWhite,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = phBlack,
    onBackground = phWhite,
    surface = phDarkGray,
    onSurface = phWhite,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = phGray,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val darkVariant1 = darkColorScheme(
    primary = electricViolet,
    onPrimary = etherealMidnight,
    primaryContainer = electricVioletDim,
    onPrimaryContainer = etherealText,
    secondary = etherealTeal,
    onSecondary = etherealMidnight,
    background = etherealMidnight,
    onBackground = etherealText,
    surface = etherealMidnight,
    onSurface = etherealText,
    surfaceVariant = etherealMidnightHigh.copy(alpha = 0.6f), // Glassmorphism base
    onSurfaceVariant = etherealText.copy(alpha = 0.7f),       // For Label-SM/Timestamps
    surfaceContainerLowest = etherealMidnight,
    surfaceContainerLow = etherealMidnightLow,               // Sidebars/Contact lists
    surfaceContainer = etherealMidnight,
    surfaceContainerHigh = etherealMidnightHigh,             // Hover states/Active backgrounds
    surfaceContainerHighest = etherealMidnightHighest,       // Receiver message bubbles
    outlineVariant = etherealText.copy(alpha = 0.15f),       // The "Ghost Border"

    // Filled in 2026-10-17 so no role falls back to the Material defaults
    secondaryContainer = etherealTealContainer,
    onSecondaryContainer = etherealText,
    tertiary = etherealPink,
    onTertiary = etherealMidnight,
    tertiaryContainer = etherealPinkContainer,
    onTertiaryContainer = etherealText,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    scrim = scrimDark,
    outline = etherealOutline,
    inverseSurface = etherealText,
    inverseOnSurface = etherealMidnightHigh,
    inversePrimary = electricVioletDim,
    surfaceDim = etherealMidnight,
    surfaceBright = etherealMidnightHighest,
)

private val lightVariant2 = lightColorScheme(
    primary = curatorPrimary,
    onPrimary = curatorOnPrimary,
    primaryContainer = curatorPrimaryContainer,
    onPrimaryContainer = curatorPrimary,

    background = curatorSurface,
    onBackground = curatorOnSurface,

    surface = curatorSurface,
    onSurface = curatorOnSurface,
    onSurfaceVariant = curatorOnSurfaceVariant,

    // Tonal Stacking Logic
    surfaceContainerLowest = curatorSurfaceLowest,
    surfaceContainerLow = curatorSurfaceLow,
    surfaceContainer = curatorSurface,
    surfaceContainerHigh = curatorSurfaceHigh,
    surfaceContainerHighest = curatorSurfaceHighest,

    outlineVariant = curatorGhostBorder, // The "Ghost Border"

    // Filled in 2026-10-17 so no role falls back to the Material defaults
    secondary = curatorSecondary,
    onSecondary = curatorOnSecondary,
    secondaryContainer = curatorSecondaryContainer,
    onSecondaryContainer = curatorOnSecondaryContainer,
    tertiary = curatorTertiary,
    onTertiary = curatorOnTertiary,
    tertiaryContainer = curatorTertiaryContainer,
    onTertiaryContainer = curatorOnTertiaryContainer,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    scrim = scrimLight,
    surfaceVariant = curatorSurfaceHigh,
    outline = curatorOutline,
    inverseSurface = curatorOnSurface,
    inverseOnSurface = curatorSurfaceLow,
    inversePrimary = curatorPrimaryContainer,
    surfaceDim = curatorSurfaceHighest,
    surfaceBright = curatorSurfaceLowest,
)

private val neonPulseScheme = darkColorScheme(
    primary = neonPrimary,
    onPrimary = neonOnPrimary,
    primaryContainer = neonPrimaryContainer,
    onPrimaryContainer = neonOnPrimary,

    secondary = neonSecondary,
    onSecondary = neonOnSecondary,
    secondaryContainer = neonSecondaryContainer,
    onSecondaryContainer = neonOnSecondary,

    tertiary = neonTertiary,

    background = neonSurfaceLowest,
    onBackground = neonPrimary, // High-contrast headers

    surface = neonSurface,
    onSurface = neonPrimary,
    onSurfaceVariant = neonOnSurfaceVariant,

    // Tonal Layering Implementation
    surfaceContainerLowest = neonSurfaceLowest,
    surfaceContainerLow = neonSurfaceLow,
    surfaceContainer = neonSurfaceContainer,
    surfaceContainerHigh = neonSurfaceHigh,
    surfaceContainerHighest = neonSurfaceHighest,

    outlineVariant = neonGhostBorder, // Critical accessibility containment

    // Filled in 2026-10-17 so no role falls back to the Material defaults
    onTertiary = neonOnTertiary,
    tertiaryContainer = neonTertiaryContainer,
    onTertiaryContainer = neonTertiary,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    scrim = scrimDark,
    surfaceVariant = neonSurfaceHighest,
    outline = neonOutline,
    inverseSurface = neonInverseSurface,
    inverseOnSurface = neonSurface,
    inversePrimary = neonOnPrimary,
    surfaceDim = neonSurface,
    surfaceBright = neonSurfaceHighest,
)

private val annaScheme = darkColorScheme(
    primary = annaPrimary,
    onPrimary = annaOnPrimary,
    primaryContainer = annaPrimaryContainer,
    onPrimaryContainer = annaOnPrimaryContainer,

    secondary = annaSecondary,
    onSecondary = annaOnSecondary,
    secondaryContainer = annaSecondaryContainer,
    onSecondaryContainer = annaOnSecondaryContainer,

    tertiary = annaTertiary,
    onTertiary = annaOnTertiary,

    background = annaBackground,
    onBackground = annaOnBackground,

    surface = annaSurface,
    onSurface = annaOnSurface,
    surfaceVariant = annaSurfaceVariant,
    onSurfaceVariant = annaOnSurfaceVariant,

    surfaceContainerLowest = annaSurfaceContainerLowest,
    surfaceContainerLow = annaSurfaceContainerLow,
    surfaceContainer = annaSurfaceContainer,
    surfaceContainerHigh = annaSurfaceContainerHigh,
    surfaceContainerHighest = annaSurfaceContainerHighest,

    outline = annaOutline,
    outlineVariant = annaOutlineVariant,

    // Filled in 2026-10-17 so no role falls back to the Material defaults
    tertiaryContainer = annaTertiaryContainer,
    onTertiaryContainer = annaOnTertiary,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    scrim = scrimDark,
    inverseSurface = annaOnBackground,
    inverseOnSurface = annaSurface,
    inversePrimary = annaPrimary,
    surfaceDim = annaSurfaceContainerLowest,
    surfaceBright = annaSurfaceContainerHigh,
)

private val flosScheme = darkColorScheme(
    primary = flosPrimary,
    onPrimary = flosOnPrimary,
    primaryContainer = flosPrimaryContainer,
    onPrimaryContainer = flosOnPrimaryContainer,

    secondary = flosSecondary,
    onSecondary = flosOnSecondary,
    secondaryContainer = flosSecondaryContainer,
    onSecondaryContainer = flosOnSecondaryContainer,

    tertiary = flosTertiary,
    onTertiary = flosOnTertiary,

    background = flosBackground,
    onBackground = flosOnBackground,

    surface = flosSurface,
    onSurface = flosOnSurface,
    surfaceVariant = flosSurfaceVariant,
    onSurfaceVariant = flosOnSurfaceVariant,

    surfaceContainerLowest = flosSurfaceContainerLowest,
    surfaceContainerLow = flosSurfaceContainerLow,
    surfaceContainer = flosSurfaceContainer,
    surfaceContainerHigh = flosSurfaceContainerHigh,
    surfaceContainerHighest = flosSurfaceContainerHighest,

    outline = flosOutline,
    outlineVariant = flosOutlineVariant,

    // Filled in 2026-10-17 so no role falls back to the Material defaults
    tertiaryContainer = flosTertiaryContainer,
    onTertiaryContainer = flosOnTertiary,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    scrim = scrimDark,
    inverseSurface = flosOnBackground,
    inverseOnSurface = flosSurface,
    inversePrimary = flosPrimary,
    surfaceDim = flosBackground,
    surfaceBright = flosSurfaceContainerHighest,
)

@Composable
fun SchneaggchatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    content: @Composable() () -> Unit
) {
  val colorScheme = if(themeSetting == ThemeSetting.SYSTEM){
      when {
          darkTheme -> darkScheme
          else -> lightScheme
      }
  }else if(themeSetting == ThemeSetting.DARK){
      darkScheme
  }else if(themeSetting == ThemeSetting.VARIANT1){
      darkVariant1
  }
  else if(themeSetting == ThemeSetting.VARIANT2){
      lightVariant2
  }
  else if(themeSetting == ThemeSetting.NEONPULSE){
      neonPulseScheme
  }
  else if(themeSetting == ThemeSetting.ANNATHEME){
      annaScheme
  }
  else if(themeSetting == ThemeSetting.FLOSTHEME){
      flosScheme
  }
  /*else if(themeSetting == ThemeSetting.PHTHEME){
      phTheme
  }*/
  else{
      lightScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    content = content
  )
}

