package com.ivy.ui.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ivy.base.legacy.Theme
import com.ivy.ui.theme.WalletTheme
import org.junit.Before
import org.junit.Rule
import java.util.Locale
import java.util.TimeZone

open class PaparazziScreenshotTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        showSystemUi = true,
        maxPercentDifference = 0.005
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