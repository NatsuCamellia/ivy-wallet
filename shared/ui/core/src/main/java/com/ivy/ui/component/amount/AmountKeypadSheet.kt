package com.ivy.ui.component.amount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols

private const val KeyHeight = 52
private const val KeyGap = 8
private const val HorizontalPadding = 16

private val KeypadRows = listOf(
    listOf(AmountKeypadKey.Digit7, AmountKeypadKey.Digit8, AmountKeypadKey.Digit9, AmountKeypadKey.Divide),
    listOf(AmountKeypadKey.Digit4, AmountKeypadKey.Digit5, AmountKeypadKey.Digit6, AmountKeypadKey.Times),
    listOf(AmountKeypadKey.Digit1, AmountKeypadKey.Digit2, AmountKeypadKey.Digit3, AmountKeypadKey.Minus),
    listOf(AmountKeypadKey.Decimal, AmountKeypadKey.Digit0, AmountKeypadKey.Backspace, AmountKeypadKey.Plus),
)

private val OperatorKeys = setOf(
    AmountKeypadKey.Plus, AmountKeypadKey.Minus, AmountKeypadKey.Times, AmountKeypadKey.Divide,
)

@Immutable
@Suppress("DataClassTypedIDs")
data class KeypadAccountUi(
    val id: String,
    val name: String,
    val selected: Boolean,
)

@Composable
fun AmountKeypadContent(
    currency: String,
    initialAmount: Double?,
    accounts: ImmutableList<KeypadAccountUi>,
    onAccountClick: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onDone: (Double) -> Unit,
    modifier: Modifier = Modifier,
    decimalCountMax: Int = 2,
) {
    val decimalSeparator = remember { DecimalFormatSymbols.getInstance().decimalSeparator }
    var input by remember(initialAmount) {
        mutableStateOf(amountKeypadInputOf(initialAmount, decimalSeparator))
    }
    AmountKeypadContent(
        currency = currency,
        input = input,
        onInputChange = { input = it },
        accounts = accounts,
        onAccountClick = onAccountClick,
        onAddAccountClick = onAddAccountClick,
        onDone = onDone,
        decimalCountMax = decimalCountMax,
        decimalSeparator = decimalSeparator,
        modifier = modifier,
    )
}

/** Stateless core, reused by [AmountKeypadContent] and by the mid-expression preview below. */
@Composable
private fun AmountKeypadContent(
    currency: String,
    input: AmountKeypadInput,
    onInputChange: (AmountKeypadInput) -> Unit,
    accounts: ImmutableList<KeypadAccountUi>,
    onAccountClick: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onDone: (Double) -> Unit,
    decimalCountMax: Int,
    decimalSeparator: Char,
    modifier: Modifier = Modifier,
) {
    val evaluated = input.evaluate(decimalSeparator)

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = HorizontalPadding.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = input.text.ifEmpty { "0" },
                    style = MaterialTheme.typography.displayMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = currency,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (input.isExpression) {
                Text(
                    text = evaluated?.let { formatEvaluated(it, decimalCountMax, decimalSeparator) }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (accounts.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.id }) { account ->
                    FilterChip(
                        selected = account.selected,
                        onClick = { onAccountClick(account.id) },
                        label = { Text(account.name) },
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = onAddAccountClick,
                        label = { Text(stringResource(R.string.new_account)) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(KeyGap.dp)) {
            KeypadRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap.dp)) {
                    row.forEach { key ->
                        AmountKeypadKeyButton(
                            key = key,
                            onClick = { onInputChange(input.press(key, decimalCountMax, decimalSeparator)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Button(
            onClick = { evaluated?.let(onDone) },
            enabled = evaluated != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.done))
        }
    }
}

@Composable
private fun AmountKeypadKeyButton(
    key: AmountKeypadKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (key in OperatorKeys) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(KeyHeight.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (key == AmountKeypadKey.Backspace) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(R.string.backspace),
                )
            } else {
                Text(
                    text = key.label,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}

private fun formatEvaluated(value: Double, decimalCountMax: Int, decimalSeparator: Char): String =
    BigDecimal.valueOf(value)
        .setScale(decimalCountMax, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', decimalSeparator)

private val AmountKeypadKey.label: String
    get() = when (this) {
        AmountKeypadKey.Digit0 -> "0"
        AmountKeypadKey.Digit1 -> "1"
        AmountKeypadKey.Digit2 -> "2"
        AmountKeypadKey.Digit3 -> "3"
        AmountKeypadKey.Digit4 -> "4"
        AmountKeypadKey.Digit5 -> "5"
        AmountKeypadKey.Digit6 -> "6"
        AmountKeypadKey.Digit7 -> "7"
        AmountKeypadKey.Digit8 -> "8"
        AmountKeypadKey.Digit9 -> "9"
        AmountKeypadKey.Decimal -> DecimalFormatSymbols.getInstance().decimalSeparator.toString()
        AmountKeypadKey.Backspace -> ""
        AmountKeypadKey.Plus -> "+"
        AmountKeypadKey.Minus -> "−"
        AmountKeypadKey.Times -> "×"
        AmountKeypadKey.Divide -> "÷"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountKeypadSheet(
    currency: String,
    initialAmount: Double?,
    accounts: ImmutableList<KeypadAccountUi>,
    onAccountClick: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onDone: (Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    decimalCountMax: Int = 2,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        AmountKeypadContent(
            currency = currency,
            initialAmount = initialAmount,
            accounts = accounts,
            onAccountClick = onAccountClick,
            onAddAccountClick = onAddAccountClick,
            onDone = onDone,
            decimalCountMax = decimalCountMax,
        )
    }
}

/** For screenshot testing */
@Composable
fun AmountKeypadUiTest(withAccounts: Boolean) {
    AmountKeypadContent(
        currency = "USD",
        initialAmount = 42.5,
        accounts = if (withAccounts) PreviewAccounts else persistentListOf(),
        onAccountClick = {},
        onAddAccountClick = {},
        onDone = {},
    )
}

/**
 * For screenshot testing of the keypad's expression mode — the whole justification for deleting
 * `CalculatorModal`. [complete] picks between an expression that evaluates (sub-line filled, *Done*
 * enabled) and one left mid-operator (no sub-line, *Done* disabled).
 */
@Composable
fun AmountKeypadExpressionUiTest(complete: Boolean) {
    AmountKeypadContent(
        currency = "USD",
        input = AmountKeypadInput(if (complete) "12.5+3" else "12.5+"),
        onInputChange = {},
        accounts = persistentListOf(),
        onAccountClick = {},
        onAddAccountClick = {},
        onDone = {},
        decimalCountMax = 2,
        decimalSeparator = '.',
    )
}

private val PreviewAccounts = persistentListOf(
    KeypadAccountUi(id = "1", name = "Cash", selected = true),
    KeypadAccountUi(id = "2", name = "Revolut", selected = false),
    KeypadAccountUi(id = "3", name = "DSK Bank", selected = false),
)

@Preview
@Composable
private fun AmountKeypadContentWithAccountsPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AmountKeypadContent(
                currency = "USD",
                initialAmount = 42.5,
                accounts = PreviewAccounts,
                onAccountClick = {},
                onAddAccountClick = {},
                onDone = {},
            )
        }
    }
}

@Preview
@Composable
private fun AmountKeypadContentExpressionPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AmountKeypadContent(
                currency = "EUR",
                input = AmountKeypadInput("12+3"),
                onInputChange = {},
                accounts = persistentListOf(),
                onAccountClick = {},
                onAddAccountClick = {},
                onDone = {},
                decimalCountMax = 2,
                decimalSeparator = '.',
            )
        }
    }
}
