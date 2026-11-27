package com.ivy.home.transactionlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.ui.R
import com.ivy.ui.component.StickyListHeader
import com.ivy.ui.component.screenMargin
import com.ivy.ui.component.TypeFilterChips
import com.ivy.ui.component.TypeFilterOption
import com.ivy.ui.component.transaction.FlatTransactionItem
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
    historyEmpty: Boolean,
    typeFilter: HomeTypeFilter,
    onTypeFilterChange: (HomeTypeFilter) -> Unit,
    due: HomeDue,
    dueRows: ImmutableList<HomeTrnListItem.Trn>,
    dueExpanded: Boolean,
    emptyStateTitle: String,
    emptyStateText: String,
    onTransactionClick: (Transaction) -> Unit,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: () -> Unit,
    setDueExpanded: (Boolean) -> Unit,
) {
    if (due.rows.isNotEmpty()) {
        item(key = "due_header", contentType = "due_header") {
            DueHeader(showSkipAll = due.showSkipAll, onSkipAll = onSkipAllTransactions)
        }
        // Prefixed so a row never shares its key with the same transaction in the history below: HomeViewModel
        // publishes the reloaded history before the reloaded Due rows, so a paid or skipped one is in both for a frame.
        items(items = dueRows, key = { "due_${it.ui.id}" }, contentType = { "due_trn" }) { item ->
            FlatTransactionItem(
                ui = item.ui,
                onClick = { onTransactionClick(item.trn) },
                onSkip = { onSkipTransaction(item.trn) },
                onPayOrGet = { onPayOrGet(item.trn) },
                payOrGetText = stringResource(
                    if (item.trn.type == TransactionType.EXPENSE) R.string.pay else R.string.get,
                ),
            ) {
                TrnBadge(item)
            }
        }
        if (due.showAllCount != null) {
            item(key = "due_toggle", contentType = "due_toggle") {
                TextButton(
                    onClick = { setDueExpanded(!dueExpanded) },
                    modifier = Modifier.screenMargin(),
                ) {
                    Text(
                        text = if (dueExpanded) {
                            stringResource(R.string.due_show_less)
                        } else {
                            stringResource(R.string.due_show_all, due.showAllCount)
                        },
                    )
                }
            }
        }
    }

    if (!historyEmpty || typeFilter != HomeTypeFilter.All) {
        item(key = "type_filter", contentType = "type_filter") {
            TypeFilterChips(
                options = persistentListOf(
                    TypeFilterOption(HomeTypeFilter.All, stringResource(R.string.type_filter_all), null),
                    TypeFilterOption(HomeTypeFilter.Expense, stringResource(R.string.expense), null),
                    TypeFilterOption(HomeTypeFilter.Income, stringResource(R.string.income), null),
                ),
                selected = typeFilter,
                onSelect = onTypeFilterChange,
            )
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

    if (historyEmpty && typeFilter != HomeTypeFilter.All) {
        item(key = "type_filter_empty") {
            Text(
                text = stringResource(R.string.no_transactions_for_your_filter),
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else if (historyItems.isEmpty() && due.rows.isEmpty()) {
        item {
            EmptyState(
                emptyStateTitle = emptyStateTitle,
                emptyStateText = emptyStateText,
                modifier = Modifier.screenMargin(),
            )
        }
    }
}

@Composable
private fun DueHeader(showSkipAll: Boolean, onSkipAll: () -> Unit) {
    Row(
        modifier = Modifier.screenMargin().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.due_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        if (showSkipAll) {
            TextButton(onClick = onSkipAll) {
                Text(text = stringResource(R.string.skip_all))
            }
        }
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
