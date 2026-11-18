package com.ivy.ui.component.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.ui.R

/**
 * One transaction as a row of a segmented group: [position] gives it rounded outer corners and the gap
 * between rows is the caller's. A planned payment passes [onSkip] and [onPayOrGet] for its buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionItem(
    ui: TransactionItemUi,
    position: TransactionItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onPayOrGet: (() -> Unit)? = null,
    payOrGetText: String? = null,
    icon: @Composable () -> Unit,
) {
    val hasButtons = onSkip != null || onPayOrGet != null
    SegmentedListItem(
        onClick = onClick,
        shapes = position.toShapes(),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.testTag("transaction_item"),
        leadingContent = icon,
        overlineContent = ui.dueText?.let { dueText ->
            {
                Text(text = dueText, color = ui.amountKind.amountColor())
            }
        },
        supportingContent = if (ui.supportingText != null || hasButtons) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ui.supportingText != null) {
                        Text(text = ui.supportingText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (hasButtons) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onSkip != null) {
                                OutlinedButton(onClick = onSkip) { Text(stringResource(R.string.skip)) }
                            }
                            if (onPayOrGet != null) {
                                Button(onClick = onPayOrGet) { Text(payOrGetText ?: stringResource(R.string.pay)) }
                            }
                        }
                    }
                }
            }
        } else {
            null
        },
    ) {
        Headline(ui)
    }
}

/** The title and the amount share a line; when the amount is too wide for that it moves to its own line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Headline(ui: TransactionItemUi) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = ui.title,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = ui.amountText,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = ui.amountKind.amountColor(),
            )
            if (ui.secondaryText != null) {
                Text(
                    text = ui.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TransactionItemPosition.toShapes(): ListItemShapes = when (this) {
    TransactionItemPosition.Single -> ListItemDefaults.segmentedShapes(index = 0, count = 1)
    TransactionItemPosition.First -> ListItemDefaults.segmentedShapes(index = 0, count = 2)
    TransactionItemPosition.Middle -> ListItemDefaults.segmentedShapes(index = 1, count = 3)
    TransactionItemPosition.Last -> ListItemDefaults.segmentedShapes(index = 1, count = 2)
}

@Composable
private fun TransactionAmountKind.amountColor(): Color = when (this) {
    TransactionAmountKind.Expense -> MaterialTheme.colorScheme.error
    TransactionAmountKind.Income -> MaterialTheme.colorScheme.tertiary
    TransactionAmountKind.Transfer -> MaterialTheme.colorScheme.primary
    TransactionAmountKind.Upcoming -> MaterialTheme.colorScheme.secondary
    TransactionAmountKind.Overdue -> MaterialTheme.colorScheme.error
}
