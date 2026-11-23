package com.ivy.legacy.ui.component.edit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.time.TimeFormatter
import java.time.Instant

@Composable
fun TransactionDateTime(
    dateTime: Instant?,
    dueDateTime: Instant?,
    onEditDate: () -> Unit,
    onEditTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (dueDateTime != null && dateTime == null) return

    val localDateTime = with(LocalTimeConverter.current) {
        (dateTime ?: LocalTimeProvider.current.utcNow()).toLocalDateTime()
    }
    val timeFormatter = LocalTimeFormatter.current
    Card(modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            supportingContent = {
                Row {
                    TextButton(onClick = onEditDate) {
                        Text(
                            with(timeFormatter) {
                                localDateTime.format(TimeFormatter.Style.DateOnly(includeWeekDay = false))
                            }
                        )
                    }
                    TextButton(onClick = onEditTime) {
                        Text(with(timeFormatter) { localDateTime.toLocalTime().format() })
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(stringResource(R.string.created_on))
        }
    }
}

@Preview
@Composable
private fun PreviewTransactionDateTime() {
    WalletPreview(Theme.LIGHT) {
        TransactionDateTime(
            dateTime = LocalTimeProvider.current.utcNow(),
            dueDateTime = null,
            onEditDate = {},
            onEditTime = {},
        )
    }
}
