package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.time.TimeFormatter
import java.time.Instant
import java.util.concurrent.TimeUnit

@Composable
fun DueDate(
    dueDate: Instant,
    onPickDueDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onPickDueDate, modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
            overlineContent = { Text(stringResource(R.string.planned_for)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(
                text = with(LocalTimeFormatter.current) {
                    dueDate.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = false))
                },
            )
        }
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun PreviewDueDate() {
    WalletPreview(Theme.LIGHT) {
        DueDate(
            dueDate = LocalTimeProvider.current.utcNow().plusSeconds(TimeUnit.DAYS.toSeconds(6)),
            onPickDueDate = {},
        )
    }
}
