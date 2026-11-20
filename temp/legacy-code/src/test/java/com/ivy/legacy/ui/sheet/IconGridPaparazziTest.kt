package com.ivy.legacy.ui.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class IconGridPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun snapshotIvyAndVueIcons() {
        snapshot(theme) { Grid(theme) }
    }
}

@Composable
private fun Grid(theme: PaparazziTheme) {
    WalletPreview(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
        IconGrid(
            selectedIcon = "ic_vue_building_bank",
            color = Color(0xFF6B4DFF),
            onIconSelect = {},
            icons = persistentListOf<Any>(
                IconPickerSection("Ivy"),
                "bank", "house", "education", "doctor", "farmacy",
                "cash", "groceries", "gift", "pet", "work",
                IconPickerSection("Building"),
                "ic_vue_building_bank", "ic_vue_building_house", "ic_vue_building_building",
                "ic_vue_building_buildings", "ic_vue_building_courthouse", "ic_vue_building_hospital",
                "ic_vue_main_cake", "ic_vue_main_coffee", "ic_vue_main_clock", "ic_vue_main_crown",
            ),
        )
    }
}
