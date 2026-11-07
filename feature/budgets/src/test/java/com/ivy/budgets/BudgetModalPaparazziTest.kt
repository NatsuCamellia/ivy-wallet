package com.ivy.budgets

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class BudgetModalPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotBudgetModalCreate() {
        snapshot(theme) {
            BudgetModalCreateUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotBudgetModalEdit() {
        snapshot(theme) {
            BudgetModalEditUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
