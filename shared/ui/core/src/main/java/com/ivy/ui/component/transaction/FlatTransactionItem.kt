package com.ivy.ui.component.transaction

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R
import com.ivy.ui.component.FlatListItem

/**
 * One transaction as a [FlatListItem]: the amount is stacked over the time (and a converted
 * transfer amount) and is colored by [amountColor]. A planned payment (with [TransactionItemUi.dueText])
 * shows its date above the title, in the error color when overdue, and passes [onSkip] and
 * [onPayOrGet] for its buttons.
 */
@Composable
fun FlatTransactionItem(
    ui: TransactionItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onPayOrGet: (() -> Unit)? = null,
    payOrGetText: String? = null,
    icon: @Composable () -> Unit,
) {
    val hasButtons = onSkip != null || onPayOrGet != null
    FlatListItem(
        title = ui.title,
        onClick = onClick,
        modifier = modifier.testTag("transaction_item"),
        supportingText = ui.supportingText,
        primaryText = ui.amountText,
        primaryColor = ui.amountKind.amountColor(),
        secondaryText = listOfNotNull(ui.secondaryText, ui.time).joinToString(" · ").ifBlank { null },
        leading = icon,
        overlineText = ui.dueText,
        overlineColor = if (ui.dueOverdue) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        actions = if (hasButtons) {
            {
                if (onSkip != null) {
                    OutlinedButton(onClick = onSkip) { Text(stringResource(R.string.skip)) }
                }
                if (onPayOrGet != null) {
                    Button(onClick = onPayOrGet) { Text(payOrGetText ?: stringResource(R.string.pay)) }
                }
            }
        } else {
            null
        },
    )
}
