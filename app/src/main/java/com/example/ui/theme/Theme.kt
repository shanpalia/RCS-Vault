package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = SurfacePureWhite,
    primaryContainer = MintPrimaryLight,
    onPrimaryContainer = MintPrimaryDark,
    secondary = MintSecondary,
    onSecondary = SurfacePureWhite,
    secondaryContainer = MintPrimaryLight,
    onSecondaryContainer = MintPrimaryDark,
    tertiary = MintTertiary,
    onTertiary = SurfacePureWhite,
    background = BackgroundWhite,
    onBackground = TextPrimaryDark,
    surface = SurfacePureWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineBorder,
    outlineVariant = OutlineVariant,
    error = StatusError,
    onError = SurfacePureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Light UI by default as required
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BackgroundWhite.toArgb()
                window.navigationBarColor = SurfacePureWhite.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
