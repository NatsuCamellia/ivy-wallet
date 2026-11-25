package com.ivy.ui.component.transaction

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The one amount color rule: Expense onSurface, Income tertiary, Transfer onSurfaceVariant.
 */
@Composable
fun TransactionAmountKind.amountColor(): Color = when (this) {
    TransactionAmountKind.Expense -> MaterialTheme.colorScheme.onSurface
    TransactionAmountKind.Income -> MaterialTheme.colorScheme.tertiary
    TransactionAmountKind.Transfer -> MaterialTheme.colorScheme.onSurfaceVariant
}
