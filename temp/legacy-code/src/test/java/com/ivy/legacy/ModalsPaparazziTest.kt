package com.ivy.legacy

import androidx.compose.runtime.Composable
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.legacy.ui.component.tags.AddOrEditTagModalUiTest
import com.ivy.legacy.ui.component.tags.EditTagModalUiTest
import com.ivy.legacy.ui.component.tags.ShowTagModalUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import com.ivy.wallet.ui.theme.components.ChangeTransactionTypeModalUiTest
import com.ivy.wallet.ui.theme.components.ReorderModalUiTest
import com.ivy.wallet.ui.theme.modal.AddKeywordModalUiTest
import com.ivy.wallet.ui.theme.modal.BufferModalUiTest
import com.ivy.wallet.ui.theme.modal.ChooseIconModalUiTest
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalFromToUiTest
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalLastNUiTest
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalMonthUiTest
import com.ivy.wallet.ui.theme.modal.ChooseStartDateOfMonthModalUiTest
import com.ivy.wallet.ui.theme.modal.CurrencyModalUiTest
import com.ivy.wallet.ui.theme.modal.DeleteConfirmationModalUiTest
import com.ivy.wallet.ui.theme.modal.DeleteModalUiTest
import com.ivy.wallet.ui.theme.modal.EditLoanModalUiTest
import com.ivy.wallet.ui.theme.modal.EditLoanRecordModalUiTest
import com.ivy.wallet.ui.theme.modal.IvyModalUiTest
import com.ivy.wallet.ui.theme.modal.LoanModalUiTest
import com.ivy.wallet.ui.theme.modal.LoanRecordModalUiTest
import com.ivy.wallet.ui.theme.modal.MonthPickerModalUiTest
import com.ivy.wallet.ui.theme.modal.NameModalUiTest
import com.ivy.wallet.ui.theme.modal.ProgressModalUiTest
import com.ivy.wallet.ui.theme.modal.RecurringRuleModalMultipleTimesUiTest
import com.ivy.wallet.ui.theme.modal.RecurringRuleModalOneTimeUiTest
import com.ivy.wallet.ui.theme.modal.edit.AccountModalUiTest
import com.ivy.wallet.ui.theme.modal.edit.AmountModalUiTest
import com.ivy.wallet.ui.theme.modal.edit.CalculatorModalUiTest
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalUiTest
import com.ivy.wallet.ui.theme.modal.edit.ChooseCategoryModalUiTest
import com.ivy.wallet.ui.theme.modal.edit.DescriptionModalUiTest
import com.ivy.wallet.ui.theme.modal.edit.EditAccountModalUiTest
import com.ivy.wallet.ui.theme.modal.edit.EditCategoryModalUiTest
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
    IvyModal({ IvyModalUiTest(it) }),
    Loan({ LoanModalUiTest(it) }),
    LoanRecord({ LoanRecordModalUiTest(it) }),
    MonthPicker({ MonthPickerModalUiTest(it) }),
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
