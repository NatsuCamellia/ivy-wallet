package com.ivy.home.transactionlist

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.ui.R
import com.ivy.ui.component.StickyListHeader
import com.ivy.ui.component.transaction.FlatTransactionItem
import com.ivy.ui.component.transaction.TransactionItem
import com.ivy.ui.component.transaction.TransactionSectionHeader
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import kotlinx.collections.immutable.ImmutableList

@Composable
fun rememberHomeTransactionMapper(baseData: AppBaseData): HomeTransactionMapper {
    val timeConverter = LocalTimeConverter.current
    val timeFormatter = LocalTimeFormatter.current
    val deletedText = stringResource(R.string.deleted)
    val dueOnFormat = stringResource(R.string.due_on)
    val expenseFallback = stringResource(R.string.expense)
    val incomeFallback = stringResource(R.string.income)
    val transferFallback = stringResource(R.string.transfer)
    return remember(baseData, timeConverter, timeFormatter) {
        HomeTransactionMapper(
            baseData = baseData,
            timeConverter = timeConverter,
            timeFormatter = timeFormatter,
            deletedText = deletedText,
            dueOnFormat = dueOnFormat,
            expenseFallback = expenseFallback,
            incomeFallback = incomeFallback,
            transferFallback = transferFallback,
        )
    }
}

@Suppress("LongParameterList", "LongMethod")
fun LazyListScope.homeTransactionsList(
    historyItems: ImmutableList<HomeTrnListItem>,
    upcoming: LegacyDueSection?,
    upcomingRows: ImmutableList<HomeTrnListItem.Trn>,
    upcomingSubtitle: String?,
    overdue: LegacyDueSection?,
    overdueRows: ImmutableList<HomeTrnListItem.Trn>,
    overdueSubtitle: String?,
    emptyStateTitle: String,
    emptyStateText: String,
    onTransactionClick: (Transaction) -> Unit,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    setUpcomingExpanded: (Boolean) -> Unit,
    setOverdueExpanded: (Boolean) -> Unit,
) {
    if (upcoming != null && upcoming.trns.isNotEmpty()) {
        item(key = "upcoming_section") {
            TransactionSectionHeader(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.upcoming),
                titleColor = MaterialTheme.colorScheme.secondary,
                subtitle = upcomingSubtitle,
                expanded = upcoming.expanded,
                onExpandedChange = setUpcomingExpanded,
            )
        }
        if (upcoming.expanded) {
            dueRows(upcomingRows, onTransactionClick, onPayOrGet, onSkipTransaction)
        }
    }

    if (overdue != null && overdue.trns.isNotEmpty()) {
        item(key = "overdue_section") {
            TransactionSectionHeader(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.overdue),
                titleColor = MaterialTheme.colorScheme.error,
                subtitle = overdueSubtitle,
                expanded = overdue.expanded,
                onExpandedChange = setOverdueExpanded,
                trailing = {
                    TextButton(onClick = { onSkipAllTransactions(overdue.trns) }) {
                        Text(text = stringResource(R.string.skip_all))
                    }
                },
            )
        }
        if (overdue.expanded) {
            dueRows(overdueRows, onTransactionClick, onPayOrGet, onSkipTransaction)
        }
    }

    historyItems.forEach { item ->
        when (item) {
            is HomeTrnListItem.DayHeader -> stickyHeader(key = item.key, contentType = "day_header") {
                StickyListHeader(title = item.title, subtotal = item.netText)
            }

            is HomeTrnListItem.Trn -> item(key = item.ui.id, contentType = "history_trn") {
                FlatTransactionItem(ui = item.ui, onClick = { onTransactionClick(item.trn) }) {
                    TrnBadge(item)
                }
            }
        }
    }

    if (historyItems.isEmpty() && upcoming.isEmpty() && overdue.isEmpty()) {
        item {
            EmptyState(
                emptyStateTitle = emptyStateTitle,
                emptyStateText = emptyStateText,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private fun LegacyDueSection?.isEmpty(): Boolean = this == null || trns.isEmpty()

private fun LazyListScope.dueRows(
    rows: ImmutableList<HomeTrnListItem.Trn>,
    onTransactionClick: (Transaction) -> Unit,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
) {
    items(items = rows, key = { it.ui.id }) { item ->
        TrnRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = ListItemDefaults.SegmentedGap),
            item = item,
            onClick = { onTransactionClick(item.trn) },
            onSkip = { onSkipTransaction(item.trn) },
            onPayOrGet = { onPayOrGet(item.trn) },
            payOrGetText = stringResource(if (item.trn.type == TransactionType.EXPENSE) R.string.pay else R.string.get),
        )
    }
}

@Composable
private fun TrnRow(
    item: HomeTrnListItem.Trn,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onPayOrGet: (() -> Unit)? = null,
    payOrGetText: String? = null,
) {
    TransactionItem(
        ui = item.ui,
        position = item.position,
        modifier = modifier,
        onClick = onClick,
        onSkip = onSkip,
        onPayOrGet = onPayOrGet,
        payOrGetText = payOrGetText,
    ) {
        TrnBadge(item)
    }
}

/** The category (or account) colour with its icon, a transfer without one shows the transfer glyph. */
@Composable
private fun TrnBadge(item: HomeTrnListItem.Trn) {
    val color = item.ui.categoryColor
    val isTransfer = item.trn.type == TransactionType.TRANSFER
    when {
        color != null -> IconBadge(
            color = color,
            iconName = item.iconAsset,
            defaultIcon = if (isTransfer) R.drawable.ic_transfer else R.drawable.ic_custom_category_m,
        )

        isTransfer -> IconBadge(
            color = MaterialTheme.colorScheme.primaryContainer,
            icon = Icons.Default.SwapHoriz,
        )

        else -> IconBadge(
            color = MaterialTheme.colorScheme.secondaryContainer,
            iconName = item.iconAsset,
            defaultIcon = R.drawable.ic_custom_category_m,
        )
    }
}
