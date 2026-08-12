package com.ivy.categories

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CategoriesTabPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot Categories nonCompact Screen`() {
        snapshot(theme) {
            CategoriesTabUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Categories nonCompact Screen with search bar`() {
        snapshot(theme) {
            CategoriesTabWithSearchBarUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Categories compact Screen`() {
        snapshot(theme) {
            CategoriesTabCompactUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Categories compact Screen with search bar`() {
        snapshot(theme) {
            CategoriesTabWithSearchBarCompactUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
