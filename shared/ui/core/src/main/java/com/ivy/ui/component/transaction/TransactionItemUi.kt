package com.ivy.ui.component.transaction

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
@Suppress("DataClassTypedIDs")
data class TransactionItemUi(
    val id: String,
    val title: String,
    val supportingText: String?,
    val time: String?,
    val categoryColor: Color?,
    val amountText: String,
    val amountKind: TransactionAmountKind,
    val secondaryText: String?,
    val dueText: String?,
    val dueOverdue: Boolean,
)

enum class TransactionAmountKind { Expense, Income, Transfer }
