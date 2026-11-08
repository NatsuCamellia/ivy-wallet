package com.ivy.legacy.ui.component.edit.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AccountsRow
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.sheet.AmountModal
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.GreenDark
import com.ivy.ui.color.GreenLight
import com.ivy.wallet.domain.data.IvyCurrency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

/**
 * The bottom bar of the edit screens: account chips, the amount (tap to open the amount sheet) and the
 * primary [actions]. While the keyboard is open the account chips are hidden to leave room for the form.
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
fun EditBottomBar(
    type: TransactionType,
    accounts: ImmutableList<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    amount: Double,
    currency: String,
    amountModalShown: Boolean,
    setAmountModalShown: (Boolean) -> Unit,
    onAmountChange: (Double) -> Unit,
    onSelectedAccountChange: (Account) -> Unit,
    onToAccountChange: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
    modifier: Modifier = Modifier,
    initialTransactionId: UUID? = null,
    convertedAmount: Double? = null,
    convertedAmountCurrencyCode: String? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    val convertedAmountText = if (
        type == TransactionType.TRANSFER && convertedAmount != null && convertedAmountCurrencyCode != null
    ) {
        val decimals = IvyCurrency.getDecimalPlaces(convertedAmountCurrencyCode)
        "${convertedAmount.format(decimals)} $convertedAmountCurrencyCode"
    } else {
        null
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!WindowInsets.isImeVisible) {
                AccountSelection(
                    type = type,
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    toAccount = toAccount,
                    onSelectedAccountChange = onSelectedAccountChange,
                    onToAccountChange = onToAccountChange,
                    onAddNewAccount = onAddNewAccount,
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { setAmountModalShown(true) }
                        .testTag("edit_amount_balance_row"),
                ) {
                    AmountText(
                        amount = amount,
                        currency = currency,
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                    )
                    if (convertedAmountText != null) {
                        Text(
                            text = convertedAmountText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                actions()
            }
        }
    }

    val amountModalId = remember(initialTransactionId, amountModalShown) { UUID.randomUUID() }
    AmountModal(
        id = amountModalId,
        visible = amountModalShown,
        currency = currency,
        initialAmount = amount.takeIf { it > 0 },
        Header = {
            Text(
                text = stringResource(R.string.account),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            AccountsRow(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onSelectedAccountChange = onSelectedAccountChange,
                onAddNewAccount = onAddNewAccount,
                childrenTestTag = "amount_modal_account",
            )
        },
        dismiss = { setAmountModalShown(false) },
        onAmountChanged = onAmountChange,
    )
}

@Composable
private fun AccountSelection(
    type: TransactionType,
    accounts: ImmutableList<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    onSelectedAccountChange: (Account) -> Unit,
    onToAccountChange: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
) {
    val label = when (type) {
        TransactionType.INCOME -> stringResource(R.string.add_money_to)
        TransactionType.EXPENSE -> stringResource(R.string.pay_with)
        TransactionType.TRANSFER -> stringResource(R.string.from)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        AccountsRow(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onSelectedAccountChange = onSelectedAccountChange,
            onAddNewAccount = onAddNewAccount,
            childrenTestTag = "from_account",
        )

        if (type == TransactionType.TRANSFER) {
            Text(
                text = stringResource(R.string.to),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            AccountsRow(
                accounts = accounts,
                selectedAccount = toAccount,
                onSelectedAccountChange = onToAccountChange,
                onAddNewAccount = onAddNewAccount,
                childrenTestTag = "to_account",
            )
        }
    }
}

@Preview
@Composable
private fun PreviewEditBottomBar() {
    PreviewBar(TransactionType.INCOME)
}

@Preview
@Composable
private fun PreviewEditBottomBarTransfer() {
    PreviewBar(TransactionType.TRANSFER)
}

@Composable
private fun PreviewBar(type: TransactionType) {
    WalletPreview(Theme.LIGHT) {
        val cash = Account("Cash", color = Green.toArgb())
        val dsk = Account("DSK", color = GreenDark.toArgb())
        EditBottomBar(
            type = type,
            accounts = persistentListOf(cash, dsk, Account("phyre", color = GreenLight.toArgb(), icon = "cash")),
            selectedAccount = cash,
            toAccount = dsk,
            amount = 12350.0,
            currency = "BGN",
            amountModalShown = false,
            setAmountModalShown = {},
            onAmountChange = {},
            onSelectedAccountChange = {},
            onToAccountChange = {},
            onAddNewAccount = {},
        ) {
            Button(onClick = {}) { Text(stringResource(R.string.save)) }
        }
    }
}
