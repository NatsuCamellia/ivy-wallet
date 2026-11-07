package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.LoanRecordType
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.legacy.legacy.ui.theme.components.DateTimeRow
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AccountsRow
import com.ivy.legacy.ui.component.AmountItem
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.getDefaultFIATCurrency
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanRecordData
import com.ivy.wallet.domain.deprecated.logic.model.EditLoanRecordData
import java.time.Instant
import java.util.UUID
import kotlinx.collections.immutable.toImmutableList

data class LoanRecordModalData(
    val loanRecord: LoanRecord?,
    val baseCurrency: String,
    val loanAccountCurrencyCode: String? = null,
    val selectedAccount: Account? = null,
    val createLoanRecordTransaction: Boolean = false,
    val isLoanInterest: Boolean = false,
    val id: UUID = UUID.randomUUID(),
)

@Suppress("CyclomaticComplexMethod", "LongMethod")
@SuppressLint("ComposeModifierMissing")
@Composable
fun LoanRecordModal(
    modal: LoanRecordModalData?,
    dateTime: Instant,
    onSetDate: () -> Unit,
    onSetTime: () -> Unit,
    onCreate: (CreateLoanRecordData) -> Unit,
    onEdit: (EditLoanRecordData) -> Unit,
    onDelete: (LoanRecord) -> Unit,
    dismiss: () -> Unit,
    accounts: List<Account> = emptyList(),
    onCreateAccount: (CreateAccountData) -> Unit = {},
) {
    if (modal == null) return

    key(modal.id) {
        val initialRecord = modal.loanRecord

        var noteTextFieldValue by remember { mutableStateOf(selectEndTextFieldValue(initialRecord?.note)) }
        var currencyCode by remember { mutableStateOf(modal.baseCurrency) }
        var amount by remember { mutableStateOf(initialRecord?.amount ?: 0.0) }
        var selectedAcc by remember { mutableStateOf(modal.selectedAccount) }
        var createLoanRecordTrans by remember { mutableStateOf(modal.createLoanRecordTransaction) }
        var loanInterest by remember { mutableStateOf(modal.isLoanInterest) }
        var reCalculate by remember { mutableStateOf(false) }
        var reCalculateVisible by remember {
            mutableStateOf(
                modal.loanAccountCurrencyCode != null && modal.loanAccountCurrencyCode != modal.baseCurrency
            )
        }
        var loanRecordType by remember {
            mutableStateOf(initialRecord?.loanRecordType ?: LoanRecordType.DECREASE)
        }

        val recordDateTime = initialRecord?.dateTime ?: dateTime
        var amountModalVisible by remember { mutableStateOf(false) }
        var deleteModalVisible by remember { mutableStateOf(false) }
        var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
        var accountChangeConformationModal by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (initialRecord == null) {
                amountModalVisible = true
            }
        }

        val amountLabel = stringResource(R.string.enter_record_amount_uppercase).toLowerCaseLocal().capitalizeLocal()
        val gutter = Modifier.padding(horizontal = 16.dp)
        val timeConverter = LocalTimeConverter.current

        ActionSheet(
            onDismiss = dismiss,
            title = stringResource(if (initialRecord != null) R.string.edit_record else R.string.new_record),
            actions = { dismissSheet ->
                if (initialRecord != null) {
                    IconButton(
                        onClick = { deleteModalVisible = true },
                        modifier = Modifier.testTag("modal_delete"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        accountChangeConformationModal =
                            initialRecord != null && modal.selectedAccount != null &&
                                modal.baseCurrency != currencyCode && currencyCode != modal.loanAccountCurrencyCode

                        if (!accountChangeConformationModal) {
                            save(
                                loanRecord = initialRecord,
                                noteTextFieldValue = noteTextFieldValue,
                                amount = amount,
                                dateTime = recordDateTime,
                                loanRecordInterest = loanInterest,
                                selectedAccount = selectedAcc,
                                createLoanRecordTransaction = createLoanRecordTrans,
                                reCalculateAmount = reCalculate,
                                loanRecordType = loanRecordType,

                                onCreate = onCreate,
                                onEdit = onEdit,
                                dismiss = dismissSheet,
                            )
                        }
                    },
                    enabled = amount > 0 && selectedAcc != null,
                    modifier = if (initialRecord == null) Modifier.testTag("modal_add") else Modifier,
                ) {
                    Text(stringResource(if (initialRecord != null) R.string.save else R.string.add))
                }
            }
        ) { dismissSheet ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = noteTextFieldValue,
                    onValueChange = { noteTextFieldValue = it },
                    modifier = gutter.fillMaxWidth(),
                    label = { Text(stringResource(R.string.note)) },
                    singleLine = true,
                )

                DateTimeRow(
                    dateTime = with(timeConverter) { recordDateTime.toLocalDateTime() },
                    onEditDate = onSetDate,
                    onEditTime = onSetTime,
                    modifier = gutter,
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
                            currencyCode = it.currency ?: getDefaultFIATCurrency().currencyCode

                            reCalculateVisible =
                                initialRecord?.convertedAmount != null && selectedAcc != null &&
                                    currencyCode == modal.baseCurrency
                            // Unchecks the Recalculate Option if Recalculate Checkbox is not visible
                            reCalculate = !reCalculateVisible

                            selectedAcc = it
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

                Column(modifier = gutter, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.loan_record_type),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        recordTypes.forEachIndexed { index, recordType ->
                            SegmentedButton(
                                selected = loanRecordType == recordType,
                                onClick = {
                                    if (recordType == LoanRecordType.INCREASE) loanInterest = false
                                    loanRecordType = recordType
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = recordTypes.size,
                                ),
                            ) {
                                Text(
                                    stringResource(
                                        if (recordType == LoanRecordType.INCREASE) {
                                            R.string.increase_loan
                                        } else {
                                            R.string.decrease_loan
                                        }
                                    )
                                )
                            }
                        }
                    }
                }

                val showInterest = loanRecordType == LoanRecordType.DECREASE
                val itemCount = 2 + (if (showInterest) 1 else 0) + (if (reCalculateVisible) 1 else 0)
                Column(
                    modifier = gutter,
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    var index = 0

                    SegmentedListItem(
                        checked = createLoanRecordTrans,
                        onCheckedChange = { createLoanRecordTrans = it },
                        shapes = ListItemDefaults.segmentedShapes(index = index++, count = itemCount),
                        leadingContent = { Checkbox(checked = createLoanRecordTrans, onCheckedChange = null) },
                    ) {
                        Text(stringResource(R.string.create_main_transaction))
                    }

                    if (showInterest) {
                        SegmentedListItem(
                            checked = loanInterest,
                            onCheckedChange = { loanInterest = it },
                            shapes = ListItemDefaults.segmentedShapes(index = index++, count = itemCount),
                            leadingContent = { Checkbox(checked = loanInterest, onCheckedChange = null) },
                        ) {
                            Text(stringResource(R.string.mark_as_interest))
                        }
                    }

                    if (reCalculateVisible) {
                        SegmentedListItem(
                            checked = reCalculate,
                            onCheckedChange = { reCalculate = it },
                            shapes = ListItemDefaults.segmentedShapes(index = index++, count = itemCount),
                            leadingContent = { Checkbox(checked = reCalculate, onCheckedChange = null) },
                        ) {
                            Text(stringResource(R.string.recalculate_amount_with_today_exchange_rates))
                        }
                    }

                    AmountItem(
                        label = amountLabel,
                        currency = currencyCode,
                        amount = amount,
                        index = index,
                        count = itemCount,
                        onClick = { amountModalVisible = true },
                    )
                }
            }

            DeleteModal(
                visible = deleteModalVisible,
                title = stringResource(R.string.confirm_deletion),
                description = stringResource(R.string.record_deletion_warning, noteTextFieldValue.text),
                dismiss = { deleteModalVisible = false }
            ) {
                if (initialRecord != null) {
                    onDelete(initialRecord)
                }
                deleteModalVisible = false
                reCalculate = false
                dismissSheet()
            }

            ConfirmModal(
                visible = accountChangeConformationModal,
                title = stringResource(R.string.confirm_account_change),
                description = stringResource(R.string.account_change_warning),
                dismiss = {
                    selectedAcc = modal.selectedAccount ?: selectedAcc
                    accountChangeConformationModal = false
                }
            ) {
                save(
                    loanRecord = initialRecord,
                    noteTextFieldValue = noteTextFieldValue,
                    amount = amount,
                    dateTime = recordDateTime,
                    loanRecordInterest = loanInterest,
                    selectedAccount = selectedAcc,
                    createLoanRecordTransaction = createLoanRecordTrans,
                    reCalculateAmount = reCalculate,
                    loanRecordType = loanRecordType,

                    onCreate = onCreate,
                    onEdit = onEdit,
                    dismiss = dismissSheet,
                )

                accountChangeConformationModal = false
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

        AccountModal(
            modal = accountModalData,
            onCreateAccount = onCreateAccount,
            onEditAccount = { _, _ -> },
            dismiss = { accountModalData = null }
        )
    }
}

private val recordTypes = listOf(LoanRecordType.DECREASE, LoanRecordType.INCREASE)

private fun save(
    loanRecord: LoanRecord?,
    noteTextFieldValue: TextFieldValue,
    amount: Double,
    dateTime: Instant,
    loanRecordInterest: Boolean = false,
    createLoanRecordTransaction: Boolean = false,
    selectedAccount: Account? = null,
    reCalculateAmount: Boolean = false,
    loanRecordType: LoanRecordType,

    onCreate: (CreateLoanRecordData) -> Unit,
    onEdit: (EditLoanRecordData) -> Unit,
    dismiss: () -> Unit
) {
    if (loanRecord != null) {
        val record = loanRecord.copy(
            note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull()?.value,
            amount = amount,
            dateTime = dateTime,
            interest = loanRecordInterest,
            accountId = selectedAccount?.id,
            loanRecordType = loanRecordType
        )
        onEdit(
            EditLoanRecordData(
                newLoanRecord = record,
                originalLoanRecord = loanRecord,
                createLoanRecordTransaction = createLoanRecordTransaction,
                reCalculateLoanAmount = reCalculateAmount,
            )
        )
    } else {
        onCreate(
            CreateLoanRecordData(
                note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull()?.value,
                amount = amount,
                dateTime = dateTime,
                interest = loanRecordInterest,
                account = selectedAccount,
                createLoanRecordTransaction = createLoanRecordTransaction,
                loanRecordType = loanRecordType
            )
        )
    }

    dismiss()
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        LoanRecordModal(
            modal = LoanRecordModalData(
                loanRecord = null,
                baseCurrency = "BGN"
            ),
            onCreate = {},
            onEdit = {},
            onDelete = {},
            dismiss = {},
            onSetDate = {},
            onSetTime = {},
            dateTime = Instant.parse("2026-01-15T12:00:00Z")
        )
    }
}

/** For screenshot testing */
@Composable
fun LoanRecordModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun EditLoanRecordModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        LoanRecordModal(
            modal = LoanRecordModalData(
                loanRecord = LoanRecord(
                    loanId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    amount = 300.0,
                    note = "First installment",
                    dateTime = Instant.parse("2026-01-15T12:00:00Z"),
                    loanRecordType = LoanRecordType.DECREASE,
                    id = UUID.fromString("00000000-0000-0000-0000-000000000001")
                ),
                baseCurrency = "BGN"
            ),
            onCreate = {},
            onEdit = {},
            onDelete = {},
            dismiss = {},
            onSetDate = {},
            onSetTime = {},
            dateTime = Instant.parse("2026-01-15T12:00:00Z")
        )
    }
}
