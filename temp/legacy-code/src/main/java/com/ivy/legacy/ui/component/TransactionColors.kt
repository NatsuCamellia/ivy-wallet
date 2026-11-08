package com.ivy.legacy.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ivy.base.model.TransactionType
import com.ivy.ui.R

@Composable
@ReadOnlyComposable
fun TransactionType.color(): Color = when (this) {
    TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
    TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
}

@Composable
fun TransactionType.label(): String = stringResource(
    when (this) {
        TransactionType.INCOME -> R.string.income
        TransactionType.EXPENSE -> R.string.expense
        TransactionType.TRANSFER -> R.string.transfer
    }
)

fun TransactionType.icon(): ImageVector = when (this) {
    TransactionType.INCOME -> Icons.Default.ArrowDownward
    TransactionType.EXPENSE -> Icons.Default.ArrowUpward
    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
}
