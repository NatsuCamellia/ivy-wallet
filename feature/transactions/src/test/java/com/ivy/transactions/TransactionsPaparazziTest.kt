package com.ivy.transactions

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class TransactionsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot Transactions Screen`() {
        snapshot(theme) {
            TransactionsUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotTransactionsCategory() {
        snapshot(theme) {
            TransactionsCategoryUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotTransactionsEmptyPeriod() {
        snapshot(theme) {
            TransactionsEmptyPeriodUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotTransactionsHugeBalance() {
        snapshot(theme) {
            TransactionsHugeBalanceUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
