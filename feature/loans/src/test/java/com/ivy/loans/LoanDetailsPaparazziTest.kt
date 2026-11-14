package com.ivy.loans

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.loans.loandetails.LoanDetailScreenUiTest
import com.ivy.loans.loandetails.LoanDetailsEmptyUiTest
import com.ivy.loans.loandetails.LoanDetailsHugeAmountUiTest
import com.ivy.loans.loandetails.LoanDetailsPaidOffUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class LoanDetailsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotLoanDetails() {
        snapshot(theme) {
            LoanDetailScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotLoanDetailsEmpty() {
        snapshot(theme) {
            LoanDetailsEmptyUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotLoanDetailsPaidOff() {
        snapshot(theme) {
            LoanDetailsPaidOffUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotLoanDetailsHugeAmount() {
        snapshot(theme) {
            LoanDetailsHugeAmountUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
