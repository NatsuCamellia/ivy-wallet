package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.legacy.utils.format
import com.ivy.ui.R

/** Income or expense total with the number of transactions, and an optional add button. */
@Composable
fun SummaryCard(
    type: TransactionType,
    label: String,
    currency: String,
    amount: Double,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    addLabel: String? = null,
    onAdd: (() -> Unit)? = null,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (type == TransactionType.INCOME) {
                        Icons.Default.ArrowDownward
                    } else {
                        Icons.Default.ArrowUpward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    tint = type.color(),
                )
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }

            Text(
                text = "${if (type == TransactionType.INCOME && amount > 0) "+" else ""}" +
                    "${amount.format(currency)} $currency",
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = type.color(),
            )

            Column {
                Text(text = count.toString(), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.transactions),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (addLabel != null && onAdd != null) {
                FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Text(text = addLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, content = content)
    } else {
        Card(modifier = modifier, content = content)
    }
}
