package com.ivy.legacy.ui.component

import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.ivy.legacy.utils.format

@Composable
fun AmountItem(
    label: String,
    currency: String,
    amount: Double,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = modifier.testTag("amount_balance"),
        overlineContent = { Text(label) },
    ) {
        Text(
            text = "${amount.format(currency)} $currency",
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
    }
}
