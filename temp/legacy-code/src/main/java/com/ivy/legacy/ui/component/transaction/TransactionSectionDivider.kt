package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.color
import com.ivy.ui.R

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun SectionDivider(
    expanded: Boolean,
    title: String,
    titleColor: Color,
    baseCurrency: String,
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier,
    showIncomeExpenseRow: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    setExpanded: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )

            if (showIncomeExpenseRow) {
                TransactionTotals(
                    income = income,
                    expenses = expenses,
                    currency = baseCurrency
                )
            }
        }

        actions()

        IconButton(onClick = { setExpanded(!expanded) }) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand)
            )
        }
    }
}

/** Income as `+amount` and expenses as `-amount`, each only when it is not zero. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransactionTotals(
    income: Double,
    expenses: Double,
    currency: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (expenses > 0) {
            AmountText(
                amount = -expenses,
                currency = currency,
                style = MaterialTheme.typography.labelLarge.copy(color = TransactionType.EXPENSE.color())
            )
        }
        if (income > 0) {
            AmountText(
                amount = income,
                currency = currency,
                style = MaterialTheme.typography.labelLarge.copy(color = TransactionType.INCOME.color()),
                showPlusSign = true
            )
        }
    }
}
