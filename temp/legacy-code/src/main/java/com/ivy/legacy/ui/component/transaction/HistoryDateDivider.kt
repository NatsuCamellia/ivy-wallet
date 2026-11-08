package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.legacy.utils.dateNowLocal
import com.ivy.legacy.utils.formatLocal
import com.ivy.ui.R
import java.time.LocalDate

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun HistoryDateDivider(
    date: LocalDate,
    spacerTop: Dp,
    baseCurrency: String,
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier,
) {
    val today = dateNowLocal()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacerTop, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date.formatLocal(
                    if (today.year == date.year) "MMMM dd." else "MMM dd. yyy"
                ),
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = when (date) {
                    today -> stringResource(R.string.today)
                    today.minusDays(1) -> stringResource(R.string.yesterday)
                    today.plusDays(1) -> stringResource(R.string.tomorrow)
                    else -> date.formatLocal("EEEE")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TransactionTotals(
            income = income,
            expenses = expenses,
            currency = baseCurrency
        )
    }
}
