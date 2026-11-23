package com.ivy.exchangerates.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.legacy.Theme
import com.ivy.exchangerates.data.RateUi
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.ui.R

@Composable
fun RateItem(
    rate: RateUi,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.testTag("rate_item"),
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AmountText(
                    amount = rate.rate,
                    currency = rate.to,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_rate_pair, rate.from, rate.to)
                        )
                    }
                }
            }
        },
    ) {
        Text("${rate.from}–${rate.to}")
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        RateItem(rate = RateUi("BGN", "EUR", 1.95583), index = 0, count = 1, onClick = {}, onDelete = {})
    }
}
