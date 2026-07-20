package com.ivy.home.transactionlist

import androidx.compose.runtime.Immutable
import com.ivy.base.legacy.Transaction
import com.ivy.ui.component.transaction.TransactionItemPosition
import com.ivy.ui.component.transaction.TransactionItemUi

@Immutable
sealed interface HomeTrnListItem {
    @Immutable
    data class DayHeader(
        val key: String,
        val title: String,
        val netText: String,
    ) : HomeTrnListItem

    @Immutable
    data class Trn(
        val ui: TransactionItemUi,
        val position: TransactionItemPosition,
        val iconAsset: String?,
        val trn: Transaction,
    ) : HomeTrnListItem
}
