package com.ivy.legacy.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.ivy.base.model.TransactionType

@Composable
@ReadOnlyComposable
fun TransactionType.color(): Color = when (this) {
    TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
    TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
}
