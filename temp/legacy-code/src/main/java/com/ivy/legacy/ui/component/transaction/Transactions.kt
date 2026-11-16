package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.legacy.stringRes
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.ui.component.TransactionRow
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import java.util.UUID

@Deprecated("Legacy code. Don't use it, please.")
fun LazyListScope.transactions(
    baseData: AppBaseData,

    upcoming: LegacyDueSection?,
    overdue: LegacyDueSection?,
    history: List<TransactionHistoryItem>,

    emptyStateTitle: String = stringRes(R.string.no_transactions),
    emptyStateText: String,

    dateDividerMarginTop: Dp? = null,
    lastItemSpacer: Dp? = null,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    setUpcomingExpanded: (Boolean) -> Unit,
    setOverdueExpanded: (Boolean) -> Unit,
    onSkipTransaction: (Transaction) -> Unit = {},
    onSkipAllTransactions: (List<Transaction>) -> Unit = {}
) {
    upcomingSection(
        baseData = baseData,
        upcoming = upcoming,
        shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
        onPayOrGet = onPayOrGet,
        onSkipTransaction = onSkipTransaction,
        setExpanded = setUpcomingExpanded
    )

    overdueSection(
        baseData = baseData,
        overdue = overdue,

        onPayOrGet = onPayOrGet,
        onSkipTransaction = onSkipTransaction,
        onSkipAllTransactions = onSkipAllTransactions,
        shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
        setExpanded = setOverdueExpanded
    )

    historySection(
        baseData = baseData,

        history = history,
        shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
        dateDividerMarginTop = dateDividerMarginTop,
        onPayOrGet = onPayOrGet
    )

    if (
        (upcoming == null || upcoming.trns.isEmpty()) &&
        (overdue == null || overdue.trns.isEmpty()) &&
        history.isEmpty()
    ) {
        item {
            EmptyState(
                emptyStateTitle = emptyStateTitle,
                emptyStateText = emptyStateText
            )
        }
    }

    scrollHackSpacer(
        history = history,
        upcoming = upcoming,
        overdue = overdue,
        lastItemSpacer = lastItemSpacer
    )
}

private fun LazyListScope.upcomingSection(
    baseData: AppBaseData,

    upcoming: LegacyDueSection?,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    setExpanded: (Boolean) -> Unit
) {
    if (upcoming == null) return // guard

    if (upcoming.trns.isNotEmpty()) {
        item(
            key = "upcoming_section"
        ) {
            SectionDivider(
                expanded = upcoming.expanded,
                setExpanded = setExpanded,
                title = stringRes(R.string.upcoming),
                titleColor = MaterialTheme.colorScheme.secondary,
                baseCurrency = baseData.baseCurrency,
                income = upcoming.stats.income.toDouble(),
                expenses = upcoming.stats.expense.abs().toDouble()
            )
        }

        if (upcoming.expanded) {
            trnItems(
                baseData = baseData,

                transactions = upcoming.trns,
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
                onPayOrGet = onPayOrGet,
                onSkipTransaction = onSkipTransaction
            )
        }
    }
}

private fun LazyListScope.overdueSection(
    baseData: AppBaseData,

    overdue: LegacyDueSection?,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    setExpanded: (Boolean) -> Unit
) {
    if (overdue == null) return

    if (overdue.trns.isNotEmpty()) {
        item(
            key = "overdue_section"
        ) {
            SectionDivider(
                expanded = overdue.expanded,
                setExpanded = setExpanded,
                title = stringRes(R.string.overdue),
                titleColor = MaterialTheme.colorScheme.error,
                baseCurrency = baseData.baseCurrency,
                income = overdue.stats.income.toDouble(),
                expenses = overdue.stats.expense.abs().toDouble(),
                actions = {
                    if (overdue.expanded) {
                        TextButton(onClick = { onSkipAllTransactions(overdue.trns) }) {
                            Text(text = stringResource(R.string.skip_all))
                        }
                    }
                }
            )
        }

        if (overdue.expanded) {
            trnItems(
                baseData = baseData,

                transactions = overdue.trns,
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
                onPayOrGet = onPayOrGet,
                onSkipTransaction = onSkipTransaction
            )
        }
    }
}

private fun LazyListScope.trnItems(
    baseData: AppBaseData,

    transactions: List<Transaction>,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
) {
    itemsIndexed(
        items = transactions,
        key = { _, trn -> trn.id }
    ) { index, trn ->
        val nav = navigation()
        TransactionRow(
            baseData = baseData,
            transaction = trn,
            shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
            onPayOrGet = onPayOrGet,
            onClick = { onTransactionClick(nav = nav, transaction = it) },
            modifier = Modifier.rowPadding(),
            index = index,
            count = transactions.size,
            onSkipTransaction = onSkipTransaction,
        )
    }
}

private fun Modifier.rowPadding(): Modifier =
    padding(horizontal = 16.dp).padding(top = ListItemDefaults.SegmentedGap)

private fun LazyListScope.historySection(
    baseData: AppBaseData,

    history: List<TransactionHistoryItem>,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    dateDividerMarginTop: Dp? = null,

    onPayOrGet: (Transaction) -> Unit
) {
    if (history.isNotEmpty()) {
        val groups = history.transactionGroups()
        items(
            items = history,
            key = {
                when (it) {
                    is Transaction -> it.id.toString()
                    is TransactionHistoryDateDivider -> it.date.toString()
                    else -> "unknown"
                }
            }
        ) {
            when (it) {
                is Transaction -> {
                    val nav = navigation()
                    val (index, count) = groups.getValue(it.id)

                    TransactionRow(
                        baseData = baseData,
                        transaction = it,
                        shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
                        onPayOrGet = onPayOrGet,
                        onClick = { trn -> onTransactionClick(nav = nav, transaction = trn) },
                        modifier = Modifier.rowPadding(),
                        index = index,
                        count = count,
                    )
                }

                is TransactionHistoryDateDivider -> {
                    HistoryDateDivider(
                        date = it.date,
                        modifier = dateDividerMarginTop?.let { top -> Modifier.padding(top = top) } ?: Modifier,
                        baseCurrency = baseData.baseCurrency,
                        income = it.income,
                        expenses = it.expenses
                    )
                }
            }
        }
    }
}

/** Each transaction's position and the size of the run of transactions (between date dividers) it belongs to. */
internal fun List<TransactionHistoryItem>.transactionGroups(): Map<UUID, Pair<Int, Int>> {
    val groups = HashMap<UUID, Pair<Int, Int>>()
    var start = 0
    while (start < size) {
        var end = start
        while (end < size && this[end] is Transaction) end++
        for (i in start until end) groups[(this[i] as Transaction).id] = (i - start) to (end - start)
        start = end + 1
    }
    return groups
}

private fun onTransactionClick(
    nav: Navigation,
    transaction: Transaction
) {
    nav.navigateTo(
        EditTransactionScreen(
            initialTransactionId = transaction.id,
            type = transaction.type
        )
    )
}

/** A centred icon, title and hint for a list with nothing in it. */
@Composable
fun EmptyState(
    emptyStateTitle: String,
    emptyStateText: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = emptyStateTitle,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = emptyStateText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun LazyListScope.scrollHackSpacer(
    history: List<TransactionHistoryItem>,
    upcoming: LegacyDueSection?,
    overdue: LegacyDueSection?,

    lastItemSpacer: Dp?,
) {
    item {
        if (lastItemSpacer != null) {
            Spacer(Modifier.height(lastItemSpacer))
        } else {
            // last spacer - scroll hack
            val trnCount = history.size.plus(
                if (upcoming != null && upcoming.expanded) upcoming.trns.size else 0
            ).plus(
                if (overdue != null && overdue.expanded) overdue.trns.size else 0
            )
            if (trnCount <= 5) {
                Spacer(Modifier.height(300.dp))
            } else {
                Spacer(Modifier.height(150.dp))
            }
        }
    }
}
