package com.ivy.design.api

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.ivy.base.legacy.Theme
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.design.l0_system.IvyTheme
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.time.TimeFormatter

@SuppressLint("ComposeModifierMissing")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyUI(
    theme: Theme,
    timeConverter: TimeConverter,
    timeProvider: TimeProvider,
    timeFormatter: TimeFormatter,
    design: IvyDesign,
    includeSurface: Boolean = true,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalTimeConverter provides timeConverter,
        LocalTimeProvider provides timeProvider,
        LocalTimeFormatter provides timeFormatter,
    ) {
        IvyTheme(
            theme = theme,
            design = design
        ) {
            WrapWithSurface(includeSurface = includeSurface) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun WrapWithSurface(
    includeSurface: Boolean,
    content: @Composable () -> Unit,
) {
    if (includeSurface) {
        Surface {
            content()
        }
    } else {
        content()
    }
}
