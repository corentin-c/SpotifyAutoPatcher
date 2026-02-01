package com.github.corentinc.httpcodescats.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.corentinc.screens.backgroundDark
import com.corentinc.screens.backgroundLight
import com.corentinc.screens.errorContainerDark
import com.corentinc.screens.errorContainerLight
import com.corentinc.screens.errorDark
import com.corentinc.screens.errorLight
import com.corentinc.screens.inverseOnSurfaceDark
import com.corentinc.screens.inverseOnSurfaceLight
import com.corentinc.screens.inversePrimaryDark
import com.corentinc.screens.inversePrimaryLight
import com.corentinc.screens.inverseSurfaceDark
import com.corentinc.screens.inverseSurfaceLight
import com.corentinc.screens.onBackgroundDark
import com.corentinc.screens.onBackgroundLight
import com.corentinc.screens.onErrorContainerDark
import com.corentinc.screens.onErrorContainerLight
import com.corentinc.screens.onErrorDark
import com.corentinc.screens.onErrorLight
import com.corentinc.screens.onPrimaryContainerDark
import com.corentinc.screens.onPrimaryContainerLight
import com.corentinc.screens.onPrimaryDark
import com.corentinc.screens.onPrimaryLight
import com.corentinc.screens.onSecondaryContainerDark
import com.corentinc.screens.onSecondaryContainerLight
import com.corentinc.screens.onSecondaryDark
import com.corentinc.screens.onSecondaryLight
import com.corentinc.screens.onSurfaceDark
import com.corentinc.screens.onSurfaceLight
import com.corentinc.screens.onSurfaceVariantDark
import com.corentinc.screens.onSurfaceVariantLight
import com.corentinc.screens.onTertiaryContainerDark
import com.corentinc.screens.onTertiaryContainerLight
import com.corentinc.screens.onTertiaryDark
import com.corentinc.screens.onTertiaryLight
import com.corentinc.screens.outlineDark
import com.corentinc.screens.outlineLight
import com.corentinc.screens.outlineVariantDark
import com.corentinc.screens.outlineVariantLight
import com.corentinc.screens.primaryContainerDark
import com.corentinc.screens.primaryContainerLight
import com.corentinc.screens.primaryDark
import com.corentinc.screens.primaryLight
import com.corentinc.screens.scrimDark
import com.corentinc.screens.scrimLight
import com.corentinc.screens.secondaryContainerDark
import com.corentinc.screens.secondaryContainerLight
import com.corentinc.screens.secondaryDark
import com.corentinc.screens.secondaryLight
import com.corentinc.screens.surfaceBrightDark
import com.corentinc.screens.surfaceBrightLight
import com.corentinc.screens.surfaceContainerDark
import com.corentinc.screens.surfaceContainerHighDark
import com.corentinc.screens.surfaceContainerHighLight
import com.corentinc.screens.surfaceContainerHighestDark
import com.corentinc.screens.surfaceContainerHighestLight
import com.corentinc.screens.surfaceContainerLight
import com.corentinc.screens.surfaceContainerLowDark
import com.corentinc.screens.surfaceContainerLowLight
import com.corentinc.screens.surfaceContainerLowestDark
import com.corentinc.screens.surfaceContainerLowestLight
import com.corentinc.screens.surfaceDark
import com.corentinc.screens.surfaceDimDark
import com.corentinc.screens.surfaceDimLight
import com.corentinc.screens.surfaceLight
import com.corentinc.screens.surfaceVariantDark
import com.corentinc.screens.surfaceVariantLight
import com.corentinc.screens.tertiaryContainerDark
import com.corentinc.screens.tertiaryContainerLight
import com.corentinc.screens.tertiaryDark
import com.corentinc.screens.tertiaryLight


private val LightColorScheme = lightColorScheme(
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

private val DarkColorScheme = darkColorScheme(
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

@Composable
fun AutoPatcherTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val colorScheme = when {
		darkTheme -> DarkColorScheme
		else -> LightColorScheme
	}

	MaterialTheme(
		colorScheme = colorScheme,
		content = content
	)
}