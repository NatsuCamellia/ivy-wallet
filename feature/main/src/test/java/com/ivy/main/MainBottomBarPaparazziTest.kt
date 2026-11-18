package com.ivy.main

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.legacy.data.model.MainTab
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class MainBottomBarPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot bottom bar - home tab`() {
        snapshot(theme) {
            MainUiTest(theme == PaparazziTheme.Dark, MainTab.HOME)
        }
    }

    @Test
    fun `snapshot bottom bar - accounts tab`() {
        snapshot(theme) {
            MainUiTest(theme == PaparazziTheme.Dark, MainTab.ACCOUNTS)
        }
    }

    @Test
    fun `snapshot bottom bar - categories tab`() {
        snapshot(theme) {
            MainUiTest(theme == PaparazziTheme.Dark, MainTab.CATEGORIES)
        }
    }
}
