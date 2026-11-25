package com.ivy.home.transactionlist

import androidx.compose.runtime.Immutable
import com.ivy.base.legacy.Transaction

/** A planned [transaction] in the Due section; [overdue] rows show their date in the error color. */
@Immutable
data class HomeDueRow(
    val transaction: Transaction,
    val overdue: Boolean,
)

@Immutable
data class HomeDue(
    val rows: List<HomeDueRow>,
    val showAllCount: Int?,
    val showSkipAll: Boolean,
)

/** Collapsed Due shows at most this many rows. */
const val HomeDueCollapsedRows = 3

/**
 * The Due section: [overdue] rows first, then [upcoming]. Collapsed it keeps the first
 * [HomeDueCollapsedRows]; [HomeDue.showAllCount] is the total and is set whenever there are more
 * rows than that (the toggle reads "Show all" collapsed and collapses again when [expanded]).
 * Skip all appears while anything is Overdue and skips just the Overdue ones.
 */
fun mapHomeDue(
    overdue: List<Transaction>,
    upcoming: List<Transaction>,
    expanded: Boolean,
): HomeDue {
    val all = overdue.map { HomeDueRow(it, overdue = true) } + upcoming.map { HomeDueRow(it, overdue = false) }
    val collapsible = all.size > HomeDueCollapsedRows
    return HomeDue(
        rows = if (collapsible && !expanded) all.take(HomeDueCollapsedRows) else all,
        showAllCount = all.size.takeIf { collapsible },
        showSkipAll = overdue.isNotEmpty(),
    )
}
