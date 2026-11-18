package com.ivy.ui.component.transaction

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** The day of a run of transactions with its net amount, signed. */
@Composable
fun TransactionDayHeader(
    title: String,
    netText: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(text = title, style = MaterialTheme.typography.titleSmall) },
        modifier = modifier,
        trailingContent = {
            Text(
                text = netText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
