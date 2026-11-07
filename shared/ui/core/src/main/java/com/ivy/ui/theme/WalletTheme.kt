package com.ivy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ivy.base.legacy.Theme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WalletTheme(
    theme: Theme,
    content: @Composable () -> Unit
) {
    val dark = theme.isDark(isSystemInDarkTheme())
    val context = LocalContext.current
    val scheme = when {
        LocalInspectionMode.current -> if (dark) SeedDarkColorScheme else SeedLightColorScheme
        dark -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }.let { if (theme == Theme.AMOLED_DARK) it.trueBlack() else it }

    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

fun Theme.isDark(systemDark: Boolean): Boolean = when (this) {
    Theme.LIGHT -> false
    Theme.DARK, Theme.AMOLED_DARK -> true
    Theme.AUTO -> systemDark
}

fun ColorScheme.trueBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
)
