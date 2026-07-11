package com.ivy.ui.testing

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ivy.design.system.IvyColorSource
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.design.system.colors.IvyColors
import org.junit.Before
import org.junit.Rule
import java.util.Locale

open class PaparazziScreenshotTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @get:Rule(order = 0)
    val posixNicenessWorkaround = PosixNicenessWorkaroundRule()

    @get:Rule(order = 1)
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
                isTrueBlack = false,
                colorSource = IvyColorSource.BrandSeed(IvyColors.Purple.primary),
            ) {
                content()
            }
        }
    }
}

enum class PaparazziTheme {
    Light, Dark
}