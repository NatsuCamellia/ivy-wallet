package com.ivy.design.system

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

sealed interface IvyColorSource {
    data object Dynamic : IvyColorSource

    data class BrandSeed(
        val seedColor: Color
    ) : IvyColorSource
}

/**
 * App-wide color source for [IvyMaterial3Theme]. Provided once in RootActivity
 * from the persisted dynamic-color preference so legacy theme call sites
 * don't need to plumb it through.
 */
val LocalIvyColorSource: ProvidableCompositionLocal<IvyColorSource> =
    compositionLocalOf { IvyColorSource.Dynamic }
