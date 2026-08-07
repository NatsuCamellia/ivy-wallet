package com.ivy.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.design.system.LocalIvyExtendedColors
import com.ivy.navigation.IvyPreview
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.SUGGESTIONS_LIMIT
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/** Expense-first: the overwhelmingly common case on this screen. */
private val TypeSelectorOrder = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTypeSelector(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = TypeSelectorOrder
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = entries.size),
                selected = type == entry,
                onClick = { onTypeChange(entry) },
                label = { Text(text = stringResource(entry.labelRes)) },
            )
        }
    }
}

private val TransactionType.labelRes: Int
    get() = when (this) {
        TransactionType.EXPENSE -> R.string.expense
        TransactionType.INCOME -> R.string.income
        TransactionType.TRANSFER -> R.string.transfer
    }

@Composable
fun AmountHeadline(
    amountText: String,
    currency: String,
    type: TransactionType,
    supportingText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountColor = when (type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
        TransactionType.INCOME -> LocalIvyExtendedColors.current.income
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.edit_amount),
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.displayLarge,
                color = amountColor,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = currency,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    type: TransactionType,
    suggestions: ImmutableSet<String>,
    onSuggestionClick: (String) -> Unit,
    onNext: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                // 8dp here plus the field's own 16dp of content padding lands the text on the
                // screen's 24dp start edge, shared with the amount headline and the rows below.
                .padding(horizontal = 8.dp)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.headlineSmall,
            placeholder = { Text(text = stringResource(type.titleRes)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                // The rule beneath is drawn separately, so its inset stays independent of the
                // text's and doesn't thicken on focus.
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.take(SUGGESTIONS_LIMIT).forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = { Text(suggestion) },
                    )
                }
            }
        }
    }
}

private val TransactionType.titleRes: Int
    get() = when (this) {
        TransactionType.EXPENSE -> R.string.expense_title
        TransactionType.INCOME -> R.string.income_title
        TransactionType.TRANSFER -> R.string.transfer_title
    }

@Preview
@Composable
private fun ExpenseHeaderPreview() {
    IvyPreview(dark = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                TransactionTypeSelector(type = TransactionType.EXPENSE, onTypeChange = {})
                AmountHeadline(
                    amountText = "42.50",
                    currency = "USD",
                    type = TransactionType.EXPENSE,
                    supportingText = "≈ 39.10 EUR",
                    onClick = {},
                )
                TitleField(
                    value = TextFieldValue("Groceries"),
                    onValueChange = {},
                    type = TransactionType.EXPENSE,
                    suggestions = persistentSetOf("Groceries", "Supermarket", "Weekly shop"),
                    onSuggestionClick = {},
                    onNext = {},
                    focusRequester = FocusRequester(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun IncomeHeaderPreview() {
    IvyPreview(dark = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                TransactionTypeSelector(type = TransactionType.INCOME, onTypeChange = {})
                AmountHeadline(
                    amountText = "2,500.00",
                    currency = "USD",
                    type = TransactionType.INCOME,
                    supportingText = null,
                    onClick = {},
                )
                TitleField(
                    value = TextFieldValue(""),
                    onValueChange = {},
                    type = TransactionType.INCOME,
                    suggestions = persistentSetOf(),
                    onSuggestionClick = {},
                    onNext = {},
                    focusRequester = FocusRequester(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TransferHeaderPreview() {
    IvyPreview(dark = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                TransactionTypeSelector(type = TransactionType.TRANSFER, onTypeChange = {})
                AmountHeadline(
                    amountText = "100.00",
                    currency = "EUR",
                    type = TransactionType.TRANSFER,
                    supportingText = "≈ 108.50 USD",
                    onClick = {},
                )
                TitleField(
                    value = TextFieldValue(""),
                    onValueChange = {},
                    type = TransactionType.TRANSFER,
                    suggestions = persistentSetOf(),
                    onSuggestionClick = {},
                    onNext = {},
                    focusRequester = FocusRequester(),
                )
            }
        }
    }
}
