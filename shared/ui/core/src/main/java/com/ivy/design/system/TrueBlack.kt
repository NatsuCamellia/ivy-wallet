package com.ivy.design.system

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

private val PureBlack = Color(0xFF000000)

internal fun ColorScheme.applyTrueBlack(isTrueBlack: Boolean): ColorScheme {
    if (!isTrueBlack) return this
    return copy(background = PureBlack, surface = PureBlack)
}
