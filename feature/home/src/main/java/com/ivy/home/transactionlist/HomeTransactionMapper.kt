package com.ivy.home.transactionlist

import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.legacy.ui.component.transaction.TransactionDue
import com.ivy.legacy.ui.component.transaction.TransactionItemMapper
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Maps Home's history and Due rows; the rows themselves come from the shared [items] mapper. */
class HomeTransactionMapper(
    private val items: TransactionItemMapper,
    private val baseCurrency: String,
    private val timeFormatter: TimeFormatter,
) {
    fun mapHistory(history: List<TransactionHistoryItem>): ImmutableList<HomeTrnListItem> {
        val result = mutableListOf<HomeTrnListItem>()

        for (item in history) {
            when (item) {
                is TransactionHistoryDateDivider -> result += mapDayHeader(item)
                is Transaction -> result += HomeTrnListItem.Trn(items.map(item))
            }
        }
        return result.toImmutableList()
    }

    fun mapDueRows(rows: List<HomeDueRow>): ImmutableList<HomeTrnListItem.Trn> = rows.map {
        val due = if (it.overdue) TransactionDue.Overdue else TransactionDue.Upcoming
        HomeTrnListItem.Trn(items.map(it.transaction, due))
    }.toImmutableList()

    private fun mapDayHeader(divider: TransactionHistoryDateDivider): HomeTrnListItem.DayHeader {
        val net = divider.income - divider.expenses
        val sign = if (net >= 0) "+" else "-"
        return HomeTrnListItem.DayHeader(
            key = divider.date.toString(),
            title = with(timeFormatter) {
                divider.date.atStartOfDay().format(
                    TimeFormatter.Style.DateOnly(includeWeekDay = true),
                )
            },
            netText = "$sign${items.formatAmount(kotlin.math.abs(net), baseCurrency)}",
        )
    }
}
