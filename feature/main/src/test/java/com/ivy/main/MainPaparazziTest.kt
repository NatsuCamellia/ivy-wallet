package com.ivy.main

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class MainPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotMainScreen() {
        snapshot(theme) {
            MainUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotMainScreenFabExpanded() {
        snapshot(theme) {
            MainUiTest(theme == PaparazziTheme.Dark, expanded = true)
        }
    }
}
