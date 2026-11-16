package com.ivy.legacy.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ivy.base.legacy.Theme
import com.ivy.ui.R

fun Theme.icon(): ImageVector = when (this) {
    Theme.LIGHT -> Icons.Default.LightMode
    Theme.DARK -> Icons.Default.DarkMode
    Theme.AMOLED_DARK -> Icons.Default.Contrast
    Theme.AUTO -> Icons.Default.BrightnessAuto
}

@Composable
fun Theme.label(): String = stringResource(
    when (this) {
        Theme.LIGHT -> R.string.light_mode
        Theme.DARK -> R.string.dark_mode
        Theme.AMOLED_DARK -> R.string.amoled_mode
        Theme.AUTO -> R.string.auto_mode
    }
)
