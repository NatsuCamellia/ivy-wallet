package com.ivy.home.transactionlist

import androidx.compose.runtime.Immutable
import com.ivy.legacy.ui.component.transaction.TransactionRowItem

@Immutable
sealed interface HomeTrnListItem {
    @Immutable
    data class DayHeader(
        val key: String,
        val title: String,
        val netText: String,
    ) : HomeTrnListItem

    @Immutable
    data class Trn(val item: TransactionRowItem) : HomeTrnListItem
}
