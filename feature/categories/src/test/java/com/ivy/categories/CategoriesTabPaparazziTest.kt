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
    fun `snapshot Categories expense`() {
        snapshot(theme) {
            CategoriesTabUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Categories income`() {
        snapshot(theme) {
            CategoriesTabIncomeUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotCategoriesEmpty() {
        snapshot(theme) {
            CategoriesTabEmptyUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotCategoriesHugeAmount() {
        snapshot(theme) {
            CategoriesTabHugeAmountUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
