package com.ivy.legacy.ui.component.transaction

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ivy.legacy.utils.dateNowLocal
import com.ivy.legacy.utils.formatLocal
import com.ivy.ui.R
import java.time.LocalDate

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun HistoryDateDivider(
    date: LocalDate,
    baseCurrency: String,
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier,
) {
    val today = dateNowLocal()

    ListItem(
        headlineContent = {
            Text(
                text = date.formatLocal(
                    if (today.year == date.year) "MMMM dd." else "MMM dd. yyy"
                ),
                style = MaterialTheme.typography.titleSmall
            )
        },
        modifier = modifier,
        supportingContent = {
            Text(
                text = when (date) {
                    today -> stringResource(R.string.today)
                    today.minusDays(1) -> stringResource(R.string.yesterday)
                    today.plusDays(1) -> stringResource(R.string.tomorrow)
                    else -> date.formatLocal("EEEE")
                },
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            TransactionTotals(
                income = income,
                expenses = expenses,
                currency = baseCurrency,
                horizontalAlignment = Alignment.End
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
