package com.ivy.categories

import androidx.compose.runtime.Immutable
import com.ivy.wallet.domain.data.SortOrder

/** The type of transactions the Categories tab shows amounts for. */
enum class CategoryType { Expense, Income }

@Immutable
data class CategoryRow(
    val data: CategoryData,
    /** The category's amount for the chosen type, in the base currency. */
    val amount: Double,
    /** 0 to 1, relative to the largest category of the chosen type. */
    val barFraction: Double,
    /** 0 to 100, of the chosen type's total. */
    val percentage: Double,
)

@Immutable
data class CategoriesContent(
    val active: List<CategoryRow>,
    /** Categories with nothing recorded for the chosen type. */
    val noRecords: List<CategoryData>,
    val expenseTotal: Double,
    val incomeTotal: Double,
)

fun mapCategories(
    categories: List<CategoryData>,
    type: CategoryType,
    sortOrder: SortOrder,
): CategoriesContent {
    val amountOf = { data: CategoryData ->
        when (type) {
            CategoryType.Expense -> data.monthlyExpenses
            CategoryType.Income -> data.monthlyIncome
        }
    }
    val (activeData, noRecords) = sortCategories(categories, sortOrder).partition { amountOf(it) != 0.0 }
    val total = activeData.sumOf(amountOf)
    val largest = activeData.maxOfOrNull(amountOf) ?: 0.0
    return CategoriesContent(
        active = activeData.map {
            CategoryRow(
                data = it,
                amount = amountOf(it),
                barFraction = amountOf(it) / largest,
                percentage = amountOf(it) / total * 100,
            )
        },
        noRecords = noRecords,
        expenseTotal = categories.sumOf { it.monthlyExpenses },
        incomeTotal = categories.sumOf { it.monthlyIncome },
    )
}

/** The four Sort by options, as the Categories tab has always applied them. */
fun sortCategories(categories: List<CategoryData>, sortOrder: SortOrder): List<CategoryData> =
    when (sortOrder) {
        SortOrder.DEFAULT -> categories.sortedBy { it.category.orderNum }

        SortOrder.BALANCE_AMOUNT -> categories.sortedByDescending { it.monthlyBalance }
            // balances that round to zero go last
            .partition { it.monthlyBalance.toInt() != 0 }
            .let { (nonZero, zero) -> nonZero + zero }

        SortOrder.ALPHABETICAL -> categories.sortedBy { it.category.name.value }

        SortOrder.EXPENSES -> categories.sortedByDescending { it.monthlyExpenses }
    }
