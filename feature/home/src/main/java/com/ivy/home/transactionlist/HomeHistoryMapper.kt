package com.ivy.home.transactionlist

import androidx.compose.runtime.Immutable
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import java.time.LocalDate

/** Which transactions Home history shows. Lives in the ViewModel only and resets to [All]. */
enum class HomeTypeFilter { All, Expense, Income }

/**
 * One day of Home history. [income] and [expenses] are the day's totals in the base currency
 * (Transfers excluded), exactly as the date divider carries them.
 */
@Immutable
data class HomeDayGroup(
    val date: LocalDate,
    val income: Double,
    val expenses: Double,
    val transactions: List<Transaction>,
) {
    /** The day's Cashflow shown in its header. */
    val cashflow: Double get() = income - expenses
}

@Immutable
data class HomeHistory(
    val days: List<HomeDayGroup>,
    val isEmpty: Boolean,
)

/**
 * Applies the Type [filter] to the period's [days]: only matching transactions stay (Transfers only
 * under [HomeTypeFilter.All]), a day's subtotal keeps just the visible side, and days left without
 * rows are removed.
 */
fun mapHomeHistory(days: List<HomeDayGroup>, filter: HomeTypeFilter): HomeHistory {
    val visible = when (filter) {
        HomeTypeFilter.All -> days
        HomeTypeFilter.Expense -> days.mapNotNull { it.only(TransactionType.EXPENSE) }
        HomeTypeFilter.Income -> days.mapNotNull { it.only(TransactionType.INCOME) }
    }.filter { it.transactions.isNotEmpty() }
    return HomeHistory(days = visible, isEmpty = visible.isEmpty())
}

private fun HomeDayGroup.only(type: TransactionType): HomeDayGroup? {
    val rows = transactions.filter { it.type == type }
    if (rows.isEmpty()) return null
    return copy(
        income = if (type == TransactionType.INCOME) income else 0.0,
        expenses = if (type == TransactionType.EXPENSE) expenses else 0.0,
        transactions = rows,
    )
}

/** Splits the flat divider-then-rows history into [HomeDayGroup]s. */
fun List<TransactionHistoryItem>.toDayGroups(): List<HomeDayGroup> {
    val result = mutableListOf<HomeDayGroup>()
    var divider: TransactionHistoryDateDivider? = null
    val rows = mutableListOf<Transaction>()

    fun flush() {
        divider?.let { result += HomeDayGroup(it.date, it.income, it.expenses, rows.toList()) }
        rows.clear()
    }

    for (item in this) {
        when (item) {
            is TransactionHistoryDateDivider -> {
                flush()
                divider = item
            }

            is Transaction -> rows += item
        }
    }
    flush()
    return result
}

/** The inverse of [toDayGroups], for the existing row mapper. */
fun List<HomeDayGroup>.toHistoryItems(): List<TransactionHistoryItem> = flatMap { day ->
    listOf<TransactionHistoryItem>(TransactionHistoryDateDivider(day.date, day.income, day.expenses)) +
        day.transactions
}
