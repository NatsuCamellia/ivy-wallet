package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
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
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.ColorPicker
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.legacy.utils.toUpperCaseLocal
import com.ivy.ui.R
import com.ivy.ui.color.Ivy
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import java.util.UUID

data class AccountModalData(
    val account: Account?,
    val baseCurrency: String,
    val balance: Double,
    val adjustBalanceMode: Boolean = false,
    val forceNonZeroBalance: Boolean = false,
    val autoFocusKeyboard: Boolean = true,
    val id: UUID = UUID.randomUUID()
)

@SuppressLint("ComposeModifierMissing")
@Composable
fun AccountModal(
    modal: AccountModalData?,
    onCreateAccount: (CreateAccountData) -> Unit,
    onEditAccount: (Account, balance: Double) -> Unit,
    dismiss: () -> Unit,
) {
    val account = modal?.account
    var nameTextFieldValue by remember(modal) {
        mutableStateOf(selectEndTextFieldValue(account?.name))
    }
    var color by remember(modal) {
        mutableStateOf(account?.color?.let { Color(it) } ?: Ivy)
    }
    var amount by remember(modal) {
        mutableStateOf(modal?.balance ?: 0.0)
    }
    var currencyCode by remember(modal) {
        mutableStateOf(account?.currency ?: modal?.baseCurrency ?: "")
    }
    var icon by remember(modal) {
        mutableStateOf(account?.icon)
    }
    var includeInBalance by remember(modal) {
        mutableStateOf(account?.includeInBalance ?: true)
    }

    var amountModalVisible by remember { mutableStateOf(false) }
    var currencyModalVisible by remember { mutableStateOf(false) }
    var chooseIconModalVisible by remember(modal) {
        mutableStateOf(false)
    }

    val forceNonZeroBalance = modal?.forceNonZeroBalance ?: false

    if (modal != null) {
        key(modal.id) {
            LaunchedEffect(Unit) {
                if (modal.adjustBalanceMode) {
                    amountModalVisible = true
                }
            }

            ActionSheet(
                onDismiss = dismiss,
                title = if (account != null) {
                    stringResource(R.string.edit_account)
                } else {
                    stringResource(R.string.new_account)
                },
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            save(
                                account = account,
                                nameTextFieldValue = nameTextFieldValue,
                                currency = currencyCode,
                                color = color,
                                icon = icon,
                                amount = amount,
                                includeInBalance = includeInBalance,

                                onCreateAccount = onCreateAccount,
                                onEditAccount = onEditAccount,
                                dismiss = dismissSheet
                            )
                        },
                        enabled = nameTextFieldValue.text.isNotNullOrBlank() &&
                            (!forceNonZeroBalance || amount > 0),
                        modifier = if (account == null) Modifier.testTag("modal_add") else Modifier,
                    ) {
                        Text(stringResource(if (account != null) R.string.save else R.string.add))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    IconNameRow(
                        hint = stringResource(R.string.account_name),
                        defaultIcon = R.drawable.ic_custom_account_m,
                        color = color,
                        icon = icon,

                        autoFocusKeyboard = modal.autoFocusKeyboard,

                        nameTextFieldValue = nameTextFieldValue,
                        setNameTextFieldValue = { nameTextFieldValue = it },
                        showChooseIconModal = {
                            chooseIconModalVisible = true
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    ColorPicker(
                        selectedColor = color,
                        onColorSelect = { color = it }
                    )

                    AccountSettings(
                        currencyCode = currencyCode,
                        amount = amount,
                        includeInBalance = includeInBalance,
                        onCurrencyClick = { currencyModalVisible = true },
                        onIncludeInBalanceChange = { includeInBalance = it },
                        onAmountClick = { amountModalVisible = true },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }

    val amountModalId = remember(modal, amount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = currencyCode,
        initialAmount = amount,
        showPlusMinus = true,
        dismiss = { amountModalVisible = false }
    ) { newAmount ->
        amount = newAmount

        if (modal?.adjustBalanceMode == true) {
            save(
                account = account,
                nameTextFieldValue = nameTextFieldValue,
                currency = currencyCode,
                color = color,
                icon = icon,
                amount = newAmount,
                includeInBalance = includeInBalance,

                onCreateAccount = onCreateAccount,
                onEditAccount = onEditAccount,
                dismiss = dismiss
            )
        }
    }

    CurrencyModal(
        title = stringResource(R.string.choose_currency),
        initialCurrency = IvyCurrency.fromCode(currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        currencyCode = it
    }

    ChooseIconModal(
        visible = chooseIconModalVisible,
        initialIcon = icon ?: "account",
        color = color,
        dismiss = { chooseIconModalVisible = false }
    ) {
        icon = it
    }
}

@Composable
private fun AccountSettings(
    currencyCode: String,
    amount: Double,
    includeInBalance: Boolean,
    onCurrencyClick: () -> Unit,
    onIncludeInBalanceChange: (Boolean) -> Unit,
    onAmountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        SegmentedListItem(
            onClick = onCurrencyClick,
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 3),
            modifier = Modifier.testTag("account_modal_currency"),
            supportingContent = {
                Text(IvyCurrency.fromCode(currencyCode)?.name?.toLowerCaseLocal() ?: "")
            },
        ) {
            Text(currencyCode.toUpperCaseLocal())
        }

        SegmentedListItem(
            checked = includeInBalance,
            onCheckedChange = onIncludeInBalanceChange,
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = 3),
            trailingContent = { Switch(checked = includeInBalance, onCheckedChange = null) },
        ) {
            Text(stringResource(R.string.include_account))
        }

        SegmentedListItem(
            onClick = onAmountClick,
            shapes = ListItemDefaults.segmentedShapes(index = 2, count = 3),
            modifier = Modifier.testTag("amount_balance"),
            overlineContent = { Text(stringResource(R.string.enter_account_balance)) },
        ) {
            Text(
                text = "${amount.format(currencyCode)} $currencyCode",
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
        }
    }
}

private fun save(
    account: Account?,
    nameTextFieldValue: TextFieldValue,
    currency: String,
    color: Color,
    icon: String?,
    amount: Double,
    includeInBalance: Boolean,

    onCreateAccount: (CreateAccountData) -> Unit,
    onEditAccount: (Account, balance: Double) -> Unit,
    dismiss: () -> Unit
) {
    if (account != null) {
        onEditAccount(
            account.copy(
                name = nameTextFieldValue.text.trim(),
                currency = currency,
                includeInBalance = includeInBalance,
                icon = icon,
                color = color.toArgb()
            ),
            amount
        )
    } else {
        onCreateAccount(
            CreateAccountData(
                name = nameTextFieldValue.text.trim(),
                currency = currency,
                color = color,
                icon = icon,
                balance = amount,
                includeBalance = includeInBalance
            )
        )
    }

    dismiss()
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        AccountModal(
            modal = AccountModalData(
                account = null,
                baseCurrency = "BGN",
                balance = 0.0
            ),
            onCreateAccount = { },
            onEditAccount = { _, _ -> }
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun AccountModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun EditAccountModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        AccountModal(
            modal = AccountModalData(
                account = Account(
                    name = "Cash",
                    color = Ivy.toArgb(),
                    currency = "BGN",
                    id = UUID.fromString("00000000-0000-0000-0000-000000000001")
                ),
                baseCurrency = "BGN",
                balance = 1250.0
            ),
            onCreateAccount = { },
            onEditAccount = { _, _ -> }
        ) {
        }
    }
}
