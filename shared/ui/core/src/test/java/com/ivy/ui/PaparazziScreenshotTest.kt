package com.ivy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ivy.base.legacy.Theme
import com.ivy.ui.theme.WalletTheme
import org.junit.Rule

open class PaparazziScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        showSystemUi = true,
        maxPercentDifference = 0.001
    )

    protected fun snapshot(theme: PaparazziTheme, content: @Composable () -> Unit) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WalletTheme(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                    content()
                }
            }
        }
    }
}

enum class PaparazziTheme {
    Light, Dark
}