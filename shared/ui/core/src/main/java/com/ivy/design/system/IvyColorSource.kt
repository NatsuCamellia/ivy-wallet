package com.ivy.design.system

import androidx.compose.ui.graphics.Color

sealed interface IvyColorSource {
    data object Dynamic : IvyColorSource

    data class BrandSeed(
        val seedColor: Color
    ) : IvyColorSource
}
