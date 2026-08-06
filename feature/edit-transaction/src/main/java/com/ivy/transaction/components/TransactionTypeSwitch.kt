package com.ivy.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.design.system.LocalIvyExtendedColors
import com.ivy.ui.R

private val TypeOrder = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTypeSwitch(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val income = LocalIvyExtendedColors.current.income
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        TypeOrder.forEachIndexed { index, type ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = TypeOrder.size),
                icon = {
                    val dotColor = when (type) {
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                        TransactionType.INCOME -> income
                        TransactionType.TRANSFER -> null
                    }
                    if (dotColor != null) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                },
            ) {
                Text(text = stringResource(labelFor(type)))
            }
        }
    }
}

private fun labelFor(type: TransactionType): Int = when (type) {
    TransactionType.EXPENSE -> R.string.expense
    TransactionType.INCOME -> R.string.income
    TransactionType.TRANSFER -> R.string.transfer
}
