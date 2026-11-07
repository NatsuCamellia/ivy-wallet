package com.ivy.legacy

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.appContext
import com.ivy.design.api.IvyDesign
import com.ivy.design.api.systems.IvyWalletDesign
import com.ivy.design.l0_system.IvyTheme
import com.ivy.design.l0_system.UI
import com.ivy.design.utils.IvyPreview
import com.ivy.domain.RootScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun ivyWalletCtx(): IvyWalletCtx = LocalWalletCtx.current

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun rootView(): View = LocalView.current

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun rootActivity(): AppCompatActivity = LocalContext.current as AppCompatActivity

@Composable
fun rootScreen(): RootScreen = LocalContext.current as RootScreen

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyWalletComponentPreview(
    theme: Theme = Theme.LIGHT,
    Content: @Composable BoxScope.() -> Unit
) {
    IvyWalletPreview(
        theme = theme
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.pure),
            contentAlignment = Alignment.Center
        ) {
            Content()
        }
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyWalletPreview(
    theme: Theme = Theme.LIGHT,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    appContext = rootView().context
    val ctx = IvyWalletCtx()
    CompositionLocalProvider(LocalWalletCtx provides ctx) {
        IvyPreview(
            theme = theme,
            design = appDesign(),
        ) {
            ctx.syncScreenSize(this, LocalDensity.current)
            NavigationRoot(navigation = Navigation()) {
                content()
            }
        }
    }
}

@Suppress("ModifierMissing")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyWalletUI(
    includeSurface: Boolean = true,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val ctx = LocalWalletCtx.current
    IvyTheme(
        theme = ctx.theme,
        design = appDesign(),
    ) {
        WrapWithSurface(includeSurface = includeSurface) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                ctx.syncScreenSize(this, LocalDensity.current)
                content()
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
        Surface { content() }
    } else {
        content()
    }
}

private fun IvyWalletCtx.syncScreenSize(scope: BoxWithConstraintsScope, density: Density) {
    screenWidth = with(density) { scope.maxWidth.roundToPx() }
    screenHeight = with(density) { scope.maxHeight.roundToPx() }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun appDesign(): IvyDesign = object : IvyWalletDesign() {}
