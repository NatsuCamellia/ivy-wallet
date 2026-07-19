package com.ivy.design.system

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.ivy.design.system.colors.IvyColors
import com.materialkolor.rememberDynamicColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IvyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    colorSource: IvyColorSource = LocalIvyColorSource.current,
    content: @Composable () -> Unit
) {
    val colorScheme = ivyColorScheme(colorSource, dark).applyTrueBlack(isTrueBlack)
    CompositionLocalProvider(
        LocalIvyExtendedColors provides if (dark) DarkExtendedColors else LightExtendedColors
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = Shapes(),
            typography = ivyExpressiveTypography(),
            content = content,
        )
    }
}

@Composable
private fun ivyColorScheme(colorSource: IvyColorSource, dark: Boolean): ColorScheme =
    when (colorSource) {
        is IvyColorSource.Dynamic -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            ivyBrandColorScheme(IvyColorSource.BrandSeed(IvyColors.Purple.primary), dark)
        }

        is IvyColorSource.BrandSeed -> ivyBrandColorScheme(colorSource, dark)
    }

@Composable
private fun ivyBrandColorScheme(brandSeed: IvyColorSource.BrandSeed, dark: Boolean): ColorScheme =
    rememberDynamicColorScheme(seedColor = brandSeed.seedColor, isDark = dark)
