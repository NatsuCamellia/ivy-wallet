package com.ivy.budgets

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class BudgetPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotBudgetScreen() {
        snapshot(theme) {
            BudgetScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotBudgetScreenProgress() {
        snapshot(theme) {
            BudgetScreenProgressUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotBudgetScreenEmpty() {
        snapshot(theme) {
            BudgetScreenEmptyUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotBudgetScreenHugeAmount() {
        snapshot(theme) {
            BudgetScreenHugeAmountUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
