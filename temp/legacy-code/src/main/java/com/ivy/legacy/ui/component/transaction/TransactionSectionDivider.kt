package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
        },
        modifier = modifier,
        supportingContent = if (showIncomeExpenseRow) {
            { TransactionTotals(income = income, expenses = expenses, currency = baseCurrency) }
        } else {
            null
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()

                IconButton(onClick = { setExpanded(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/** Expenses as `-amount` and income as `+amount`, each only when it is not zero. */
@Composable
internal fun TransactionTotals(
    income: Double,
    expenses: Double,
    currency: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
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
