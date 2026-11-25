package com.ivy.ui.component.transaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.ivy.ui.component.FlatListItem

/**
 * One transaction as a [FlatListItem]: the amount is stacked over the time (and a converted
 * transfer amount) and is colored by [amountColor].
 */
@Composable
fun FlatTransactionItem(
    ui: TransactionItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    FlatListItem(
        title = ui.title,
        onClick = onClick,
        modifier = modifier.testTag("transaction_item"),
        supportingText = ui.supportingText,
        primaryText = ui.amountText,
        primaryColor = ui.amountKind.amountColor(),
        secondaryText = listOfNotNull(ui.secondaryText, ui.time).joinToString(" · ").ifBlank { null },
        leading = icon,
    )
}
