package com.ivy.transaction

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class EditTransactionPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun `snapshot new expense`() {
        snapshot(theme) {
            EditTransactionUiTest(
                isDark = theme == PaparazziTheme.Dark,
                state = EditTransactionPreviewState.NewExpense,
            )
        }
    }

    @Test
    fun `snapshot filled transaction`() {
        snapshot(theme) {
            EditTransactionUiTest(
                isDark = theme == PaparazziTheme.Dark,
                state = EditTransactionPreviewState.EditFilled,
            )
        }
    }

    @Test
    fun `snapshot transfer with custom rate`() {
        snapshot(theme) {
            EditTransactionUiTest(
                isDark = theme == PaparazziTheme.Dark,
                state = EditTransactionPreviewState.TransferWithRate,
            )
        }
    }
}
