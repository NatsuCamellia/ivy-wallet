package com.ivy.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

// An approximation of the space the Scaffold's FAB takes over the end of a list.
@Composable
fun fabClearance(): Dp = LocalMinimumInteractiveComponentSize.current * 2

/** The Scaffold insets on all four sides, with the bottom one extended by [fabClearance]. */
@Composable
fun PaddingValues.withFabClearance(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + fabClearance()
    )
}
