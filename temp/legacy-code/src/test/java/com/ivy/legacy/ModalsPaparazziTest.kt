package com.ivy.legacy

import androidx.compose.runtime.Composable
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.legacy.ui.component.tags.AddOrEditTagModalUiTest
import com.ivy.legacy.ui.component.tags.EditTagModalUiTest
import com.ivy.legacy.ui.component.tags.ShowTagModalUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import com.ivy.legacy.ui.sheet.ChangeTransactionTypeModalUiTest
import com.ivy.legacy.ui.sheet.AddKeywordModalUiTest
import com.ivy.legacy.ui.sheet.BufferModalUiTest
import com.ivy.legacy.ui.sheet.ChoosePeriodModalFromToUiTest
import com.ivy.legacy.ui.sheet.ChoosePeriodModalLastNUiTest
import com.ivy.legacy.ui.sheet.ChoosePeriodModalMonthUiTest
import com.ivy.legacy.ui.sheet.ChooseStartDateOfMonthModalUiTest
import com.ivy.legacy.ui.sheet.DeleteConfirmationModalUiTest
import com.ivy.legacy.ui.sheet.DeleteModalUiTest
import com.ivy.legacy.ui.sheet.EditLoanModalUiTest
import com.ivy.legacy.ui.sheet.EditLoanRecordModalUiTest
import com.ivy.legacy.ui.sheet.LoanModalUiTest
import com.ivy.legacy.ui.sheet.LoanRecordModalUiTest
import com.ivy.legacy.ui.sheet.NameModalUiTest
import com.ivy.legacy.ui.sheet.ProgressModalUiTest
import com.ivy.legacy.ui.sheet.RecurringRuleModalMultipleTimesUiTest
import com.ivy.legacy.ui.sheet.RecurringRuleModalOneTimeUiTest
import com.ivy.legacy.ui.sheet.AccountModalUiTest
import com.ivy.legacy.ui.sheet.AmountModalUiTest
import com.ivy.legacy.ui.sheet.CalculatorModalUiTest
import com.ivy.legacy.ui.sheet.CategoryModalUiTest
import com.ivy.legacy.ui.sheet.ChooseCategoryModalUiTest
import com.ivy.legacy.ui.sheet.ChooseIconModalUiTest
import com.ivy.legacy.ui.sheet.CurrencyModalUiTest
import com.ivy.legacy.ui.sheet.DescriptionModalUiTest
import com.ivy.legacy.ui.sheet.EditAccountModalUiTest
import com.ivy.legacy.ui.sheet.EditCategoryModalUiTest
import com.ivy.legacy.ui.sheet.ReorderModalUiTest
import org.junit.Test
import org.junit.runner.RunWith

enum class ModalUnderTest(val content: @Composable (isDark: Boolean) -> Unit) {
    AddKeyword({ AddKeywordModalUiTest(it) }),
    Buffer({ BufferModalUiTest(it) }),
    ChooseIcon({ ChooseIconModalUiTest(it) }),
    ChoosePeriodMonth({ ChoosePeriodModalMonthUiTest(it) }),
    ChoosePeriodFromTo({ ChoosePeriodModalFromToUiTest(it) }),
    ChoosePeriodLastN({ ChoosePeriodModalLastNUiTest(it) }),
    ChooseStartDateOfMonth({ ChooseStartDateOfMonthModalUiTest(it) }),
    Currency({ CurrencyModalUiTest(it) }),
    Delete({ DeleteModalUiTest(it) }),
    DeleteConfirmation({ DeleteConfirmationModalUiTest(it) }),
    Loan({ LoanModalUiTest(it) }),
    LoanRecord({ LoanRecordModalUiTest(it) }),
    Name({ NameModalUiTest(it) }),
    Progress({ ProgressModalUiTest(it) }),
    RecurringRuleOneTime({ RecurringRuleModalOneTimeUiTest(it) }),
    RecurringRuleMultipleTimes({ RecurringRuleModalMultipleTimesUiTest(it) }),
    Account({ AccountModalUiTest(it) }),
    Amount({ AmountModalUiTest(it) }),
    Calculator({ CalculatorModalUiTest(it) }),
    Category({ CategoryModalUiTest(it) }),
    ChooseCategory({ ChooseCategoryModalUiTest(it) }),
    Description({ DescriptionModalUiTest(it) }),
    ChangeTransactionType({ ChangeTransactionTypeModalUiTest(it) }),
    ReorderModal({ ReorderModalUiTest(it) }),
    AddTag({ AddOrEditTagModalUiTest(it) }),
    EditTag({ EditTagModalUiTest(it) }),
    ShowTag({ ShowTagModalUiTest(it) }),
    AccountEdit({ EditAccountModalUiTest(it) }),
    CategoryEdit({ EditCategoryModalUiTest(it) }),
    LoanEdit({ EditLoanModalUiTest(it) }),
    LoanRecordEdit({ EditLoanRecordModalUiTest(it) })
}

@RunWith(TestParameterInjector::class)
class ModalsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val modal: ModalUnderTest,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotModal() {
        snapshot(theme) {
            modal.content(theme == PaparazziTheme.Dark)
        }
    }
}
