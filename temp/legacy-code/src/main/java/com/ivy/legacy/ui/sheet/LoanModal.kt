package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.LoanType
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AccountsRow
import com.ivy.legacy.ui.component.AmountItem
import com.ivy.legacy.ui.component.ColorPicker
import com.ivy.legacy.ui.component.DateTimeRow
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.getDefaultFIATCurrency
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.ui.R
import com.ivy.ui.color.Ivy
import com.ivy.ui.color.Orange
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.collections.immutable.toImmutableList

data class LoanModalData(
    val loan: Loan?,
    val baseCurrency: String,
    val selectedAccount: Account? = null,
    val autoFocusKeyboard: Boolean = true,
    val autoOpenAmountModal: Boolean = false,
    val createLoanTransaction: Boolean = false,
    val id: UUID = UUID.randomUUID()
)

@Suppress("CyclomaticComplexMethod", "LongMethod")
@SuppressLint("ComposeModifierMissing")
@Composable
fun LoanModal(
    modal: LoanModalData?,
    dateTime: Instant,
    onSetDate: () -> Unit,
    onSetTime: () -> Unit,
    onCreateLoan: (CreateLoanData) -> Unit,
    onEditLoan: (Loan, Boolean) -> Unit,
    accounts: List<Account> = emptyList(),
    onCreateAccount: (CreateAccountData) -> Unit = {},
    onPerformCalculations: () -> Unit = {},
    dismiss: () -> Unit,
) {
    if (modal == null) return

    key(modal.id) {
        val loan = modal.loan
        val timeConverter = LocalTimeConverter.current

        var nameTextFieldValue by remember { mutableStateOf(selectEndTextFieldValue(loan?.name)) }
        val loanDateTime = loan?.dateTime ?: with(timeConverter) { dateTime.toLocalDateTime() }
        var type by remember { mutableStateOf(loan?.type ?: LoanType.BORROW) }
        var amount by remember { mutableStateOf(loan?.amount ?: 0.0) }
        var color by remember { mutableStateOf(loan?.color?.let { Color(it) } ?: Ivy) }
        var icon by remember { mutableStateOf(loan?.icon) }
        var noteTextFieldValue by remember { mutableStateOf(selectEndTextFieldValue(loan?.note)) }
        var currencyCode by remember { mutableStateOf(modal.baseCurrency) }
        var selectedAcc by remember { mutableStateOf(modal.selectedAccount) }
        var createLoanTrans by remember { mutableStateOf(modal.createLoanTransaction) }

        var accountChangeModal by remember { mutableStateOf(false) }
        var amountModalVisible by remember { mutableStateOf(false) }
        var currencyModalVisible by remember { mutableStateOf(false) }
        var chooseIconModalVisible by remember { mutableStateOf(false) }
        var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            if (modal.autoOpenAmountModal) {
                amountModalVisible = true
            }
        }

        val amountLabel = stringResource(R.string.enter_loan_amount_uppercase).toLowerCaseLocal().capitalizeLocal()
        val gutter = Modifier.padding(horizontal = 16.dp)

        ActionSheet(
            onDismiss = dismiss,
            title = stringResource(if (loan != null) R.string.edit_loan else R.string.new_loan),
            actions = { dismissSheet ->
                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        accountChangeModal =
                            loan != null && modal.selectedAccount != null &&
                                currencyCode != (modal.selectedAccount.currency ?: modal.baseCurrency)

                        if (!accountChangeModal) {
                            save(
                                loan = loan,
                                nameTextFieldValue = nameTextFieldValue,
                                dateTime = loanDateTime,
                                noteTextFieldValue = noteTextFieldValue,
                                type = type,
                                color = color,
                                icon = icon,
                                amount = amount,
                                selectedAccount = selectedAcc,
                                createLoanTransaction = createLoanTrans,

                                onCreateLoan = onCreateLoan,
                                onEditLoan = onEditLoan,
                                dismiss = dismissSheet
                            )
                        }
                    },
                    enabled = nameTextFieldValue.text.isNotNullOrBlank() && amount > 0 && selectedAcc != null,
                    modifier = if (loan == null) Modifier.testTag("modal_add") else Modifier,
                ) {
                    Text(stringResource(if (loan != null) R.string.save else R.string.add))
                }
            }
        ) { dismissSheet ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconNameRow(
                    hint = stringResource(R.string.loan_name),
                    defaultIcon = R.drawable.ic_custom_loan_m,
                    color = color,
                    icon = icon,

                    autoFocusKeyboard = modal.autoFocusKeyboard,

                    nameTextFieldValue = nameTextFieldValue,
                    setNameTextFieldValue = { nameTextFieldValue = it },
                    showChooseIconModal = { chooseIconModalVisible = true },
                    modifier = gutter,
                )

                DateTimeRow(
                    dateTime = loanDateTime,
                    onEditDate = onSetDate,
                    onEditTime = onSetTime,
                    modifier = gutter,
                )

                Column(modifier = gutter, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.loan_type),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        LoanType.entries.forEachIndexed { index, loanType ->
                            SegmentedButton(
                                selected = type == loanType,
                                onClick = { type = loanType },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = LoanType.entries.size,
                                ),
                            ) {
                                Text(
                                    stringResource(
                                        if (loanType == LoanType.BORROW) R.string.borrow_money else R.string.lend_money
                                    )
                                )
                            }
                        }
                    }
                }

                ColorPicker(
                    selectedColor = color,
                    onColorSelect = { color = it }
                )

                OutlinedTextField(
                    value = noteTextFieldValue,
                    onValueChange = { noteTextFieldValue = it },
                    modifier = gutter.fillMaxWidth(),
                    label = { Text(stringResource(R.string.note)) },
                    singleLine = true,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.associated_account),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = gutter,
                    )

                    AccountsRow(
                        accounts = remember(accounts) { accounts.toImmutableList() },
                        selectedAccount = selectedAcc,
                        onSelectedAccountChange = {
                            selectedAcc = it
                            currencyCode = it.currency ?: getDefaultFIATCurrency().currencyCode
                        },
                        onAddNewAccount = {
                            accountModalData = AccountModalData(
                                account = null,
                                baseCurrency = selectedAcc?.currency ?: "USD",
                                balance = 0.0
                            )
                        },
                        childrenTestTag = "amount_modal_account"
                    )
                }

                Column(
                    modifier = gutter,
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    SegmentedListItem(
                        checked = createLoanTrans,
                        onCheckedChange = { createLoanTrans = it },
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                        leadingContent = { Checkbox(checked = createLoanTrans, onCheckedChange = null) },
                    ) {
                        Text(stringResource(R.string.create_main_transaction))
                    }

                    AmountItem(
                        label = amountLabel,
                        currency = currencyCode,
                        amount = amount,
                        index = 1,
                        count = 2,
                        onClick = { amountModalVisible = true },
                    )
                }
            }

            ConfirmModal(
                visible = accountChangeModal,
                title = stringResource(R.string.confirm_account_change),
                description = stringResource(R.string.confirm_account_change_warning),
                dismiss = {
                    selectedAcc = modal.selectedAccount ?: selectedAcc
                    accountChangeModal = false
                }
            ) {
                onPerformCalculations()
                save(
                    loan = loan,
                    nameTextFieldValue = nameTextFieldValue,
                    dateTime = loanDateTime,
                    noteTextFieldValue = noteTextFieldValue,
                    type = type,
                    color = color,
                    icon = icon,
                    amount = amount,
                    selectedAccount = selectedAcc,
                    createLoanTransaction = createLoanTrans,

                    onCreateLoan = onCreateLoan,
                    onEditLoan = onEditLoan,
                    dismiss = dismissSheet
                )
                accountChangeModal = false
            }
        }

        val amountModalId = remember(amount) { UUID.randomUUID() }
        AmountModal(
            id = amountModalId,
            visible = amountModalVisible,
            currency = currencyCode,
            initialAmount = amount,
            dismiss = { amountModalVisible = false }
        ) { newAmount ->
            amount = newAmount
        }

        CurrencyModal(
            title = stringResource(R.string.choose_currency),
            initialCurrency = IvyCurrency.fromCode(currencyCode),
            visible = currencyModalVisible,
            dismiss = { currencyModalVisible = false }
        ) {
            currencyCode = it
        }

        AccountModal(
            modal = accountModalData,
            onCreateAccount = onCreateAccount,
            onEditAccount = { _, _ -> },
            dismiss = { accountModalData = null }
        )

        ChooseIconModal(
            visible = chooseIconModalVisible,
            initialIcon = icon ?: "loan",
            color = color,
            dismiss = { chooseIconModalVisible = false }
        ) {
            icon = it
        }
    }
}

private fun save(
    loan: Loan?,
    nameTextFieldValue: TextFieldValue,
    dateTime: LocalDateTime,
    noteTextFieldValue: TextFieldValue,
    type: LoanType,
    color: Color,
    icon: String?,
    amount: Double,
    selectedAccount: Account? = null,
    createLoanTransaction: Boolean = false,

    onCreateLoan: (CreateLoanData) -> Unit,
    onEditLoan: (Loan, Boolean) -> Unit,
    dismiss: () -> Unit
) {
    if (loan != null) {
        onEditLoan(
            loan.copy(
                name = nameTextFieldValue.text.trim(),
                dateTime = dateTime,
                note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull()?.value,
                type = type,
                amount = amount,
                color = color.toArgb(),
                icon = icon,
                accountId = selectedAccount?.id
            ),
            createLoanTransaction
        )
    } else {
        onCreateLoan(
            CreateLoanData(
                name = nameTextFieldValue.text.trim(),
                type = type,
                amount = amount,
                color = color,
                icon = icon,
                account = selectedAccount,
                createLoanTransaction = createLoanTransaction,
                dateTime = dateTime,
                note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull()?.value
            )
        )
    }

    dismiss()
}

internal fun previewAccounts(): List<Account> = listOf(
    Account(name = "Cash", color = Ivy.toArgb(), currency = "BGN", id = UUID(0, 1)),
    Account(name = "Bank", color = Orange.toArgb(), currency = "BGN", id = UUID(0, 2)),
)

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        LoanModal(
            modal = LoanModalData(
                loan = null,
                baseCurrency = "BGN",
                selectedAccount = previewAccounts().first(),
            ),
            accounts = previewAccounts(),
            onCreateLoan = { },
            onEditLoan = { _, _ -> },
            onSetTime = { },
            onSetDate = { },
            dateTime = Instant.parse("2026-01-15T12:00:00Z")
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun LoanModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Suppress("MagicNumber")
@Composable
fun EditLoanModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        LoanModal(
            modal = LoanModalData(
                loan = Loan(
                    name = "Car loan",
                    amount = 5000.0,
                    type = LoanType.LEND,
                    color = Ivy.toArgb(),
                    note = "Paid back in installments",
                    dateTime = LocalDateTime.of(2026, 1, 15, 12, 0),
                    id = UUID.fromString("00000000-0000-0000-0000-000000000001")
                ),
                baseCurrency = "BGN",
                selectedAccount = previewAccounts().first(),
            ),
            accounts = previewAccounts(),
            onCreateLoan = { },
            onEditLoan = { _, _ -> },
            onSetTime = { },
            onSetDate = { },
            dateTime = Instant.parse("2026-01-15T12:00:00Z")
        ) {
        }
    }
}
