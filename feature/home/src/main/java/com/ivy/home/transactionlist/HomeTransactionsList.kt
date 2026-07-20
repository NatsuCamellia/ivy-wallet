package com.ivy.home.transactionlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.system.LocalIvyExtendedColors
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.ui.R
import com.ivy.ui.component.transaction.TransactionAmountKind
import com.ivy.ui.component.transaction.TransactionDayHeader
import com.ivy.ui.component.transaction.TransactionItem
import com.ivy.ui.component.transaction.TransactionItemPosition
import com.ivy.ui.component.transaction.TransactionSectionHeader
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import kotlinx.collections.immutable.ImmutableList

private const val FewTransactionsThreshold = 5

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
                title = stringResource(R.string.upcoming),
                titleColor = LocalIvyExtendedColors.current.warning,
                subtitle = upcomingSubtitle,
                expanded = upcoming.expanded,
                onExpandedChange = setUpcomingExpanded,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
            )
        }
        if (upcoming.expanded) {
            dueRows(upcomingRows, onTransactionClick, onPayOrGet, onSkipTransaction)
        }
    }

    if (overdue != null && overdue.trns.isNotEmpty()) {
        item(key = "overdue_section") {
            val overdueTrns = overdue.trns
            TransactionSectionHeader(
                title = stringResource(R.string.overdue),
                titleColor = MaterialTheme.colorScheme.error,
                subtitle = overdueSubtitle,
                expanded = overdue.expanded,
                onExpandedChange = setOverdueExpanded,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
                trailing = {
                    TextButton(onClick = { onSkipAllTransactions(overdueTrns) }) {
                        Text(text = stringResource(R.string.skip_all))
                    }
                },
            )
        }
        if (overdue.expanded) {
            dueRows(overdueRows, onTransactionClick, onPayOrGet, onSkipTransaction)
        }
    }

    items(
        items = historyItems,
        key = {
            when (it) {
                is HomeTrnListItem.DayHeader -> it.key
                is HomeTrnListItem.Trn -> it.ui.id
            }
        },
    ) { item ->
        when (item) {
            is HomeTrnListItem.DayHeader -> TransactionDayHeader(
                title = item.title,
                netText = item.netText,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
            )

            is HomeTrnListItem.Trn -> TrnRow(
                item = item,
                onClick = { onTransactionClick(item.trn) },
            )
        }
    }

    val trnCount = historyItems.count { it is HomeTrnListItem.Trn }
        .plus(if (upcoming?.expanded == true) upcomingRows.size else 0)
        .plus(if (overdue?.expanded == true) overdueRows.size else 0)
    val upcomingEmpty = upcoming == null || upcoming.trns.isEmpty()
    val overdueEmpty = overdue == null || overdue.trns.isEmpty()
    if (trnCount == 0 && upcomingEmpty && overdueEmpty) {
        item {
            HomeTransactionsEmptyState(title = emptyStateTitle, text = emptyStateText)
        }
    }

    item {
        // scroll hack: keep the last items reachable above the FAB/bottom bar
        Spacer(Modifier.height(if (trnCount <= FewTransactionsThreshold) 300.dp else 150.dp))
    }
}

private fun LazyListScope.dueRows(
    rows: ImmutableList<HomeTrnListItem.Trn>,
    onTransactionClick: (Transaction) -> Unit,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
) {
    items(items = rows, key = { it.ui.id }) { item ->
        val isExpense = item.trn.type == TransactionType.EXPENSE
        TrnRow(
            item = item,
            onClick = { onTransactionClick(item.trn) },
            onSkip = { onSkipTransaction(item.trn) },
            onPayOrGet = { onPayOrGet(item.trn) },
            payOrGetText = stringResource(if (isExpense) R.string.pay else R.string.get),
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
    val topGap = when (item.position) {
        TransactionItemPosition.Single, TransactionItemPosition.First -> 8.dp
        TransactionItemPosition.Middle, TransactionItemPosition.Last -> 4.dp
    }
    TransactionItem(
        ui = item.ui,
        position = item.position,
        onClick = onClick,
        onSkip = onSkip,
        onPayOrGet = onPayOrGet,
        payOrGetText = payOrGetText,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = topGap),
        icon = {
            ItemIconSDefaultIcon(
                iconName = item.iconAsset,
                defaultIcon = if (item.ui.amountKind == TransactionAmountKind.Transfer) {
                    R.drawable.ic_transfer
                } else {
                    R.drawable.ic_custom_category_s
                },
                tint = LocalContentColor.current,
            )
        },
    )
}

@Composable
private fun HomeTransactionsEmptyState(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
