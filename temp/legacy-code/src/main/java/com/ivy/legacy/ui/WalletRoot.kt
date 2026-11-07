package com.ivy.legacy.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.LocalWalletCtx
import com.ivy.ui.theme.WalletTheme
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.time.TimeFormatter

@Suppress("ModifierMissing")
@Composable
fun WalletRoot(
    ctx: IvyWalletCtx,
    timeConverter: TimeConverter,
    timeProvider: TimeProvider,
    timeFormatter: TimeFormatter,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalWalletCtx provides ctx,
        LocalTimeConverter provides timeConverter,
        LocalTimeProvider provides timeProvider,
        LocalTimeFormatter provides timeFormatter,
    ) {
        WalletTheme(theme = ctx.theme) {
            Surface(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}
