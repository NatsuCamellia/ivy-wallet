package com.ivy.ui.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ivy.design.system.IvyMaterial3Theme
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
            IvyMaterial3Theme(
                dark = when (theme) {
                    PaparazziTheme.Light -> false
                    PaparazziTheme.Dark -> true
                },
                isTrueBlack = false
            ) {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    content()
                }
            }
        }
    }
}

enum class PaparazziTheme {
    Light, Dark
}