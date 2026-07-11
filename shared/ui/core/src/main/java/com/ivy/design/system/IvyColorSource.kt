package com.ivy.design.system

import androidx.compose.ui.graphics.Color
import com.ivy.design.system.colors.IvyColors

sealed interface IvyColorSource {
    data object Dynamic : IvyColorSource

    data class BrandSeed(
        val seedColor: Color = IvyColors.Purple.primary
    ) : IvyColorSource
}
