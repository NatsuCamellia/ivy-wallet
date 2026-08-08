package com.ivy.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.design.system.LocalIvyExtendedColors
import com.ivy.navigation.IvyPreview
import com.ivy.ui.R
import com.ivy.ui.component.settings.SettingsItem
import com.ivy.ui.component.transaction.CategoryIconBubble
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon

/** [SettingsItem]'s own metrics, mirrored by [CategoryRow] below. */
private val RowStartPadding = 24.dp
private val RowTopPadding = 16.dp
private val RowEndPadding = 16.dp
private val RowBottomPadding = 16.dp
private val RowIconGap = 24.dp
private val RowIconSize = 24.dp

@Composable
fun CategoryRow(
    categoryName: String?,
    categoryColor: Color?,
    categoryIcon: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Not built on SettingsItem: SettingsItem's `icon` slot is an ImageVector, which can't carry
    // the tonal CategoryIconBubble. Widening that shared component's API for this single caller
    // isn't worth it, so this Row hand-mirrors SettingsItem's metrics instead.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = RowStartPadding,
                top = RowTopPadding,
                end = RowEndPadding,
                bottom = RowBottomPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconBubble(
            categoryColor = categoryColor,
            modifier = Modifier.padding(end = RowIconGap),
            size = RowIconSize,
        ) {
            ItemIconSDefaultIcon(
                iconName = categoryIcon,
                defaultIcon = R.drawable.ic_custom_category_s,
                tint = LocalContentColor.current,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryName ?: stringResource(R.string.choose_category),
                color = if (categoryName == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color.Unspecified
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            )
            Text(
                text = stringResource(R.string.category),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun AccountRow(
    accountName: String?,
    currency: String?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsItem(
        title = accountName ?: stringResource(R.string.select_account),
        onClick = onClick,
        modifier = modifier,
        description = if (currency != null) "$label · $currency" else label,
        titleColor = if (accountName == null) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color.Unspecified
        },
        icon = Icons.Outlined.AccountBalanceWallet,
    )
}

/**
 * The row body picks the date; the trailing clock picks the time. Two targets, because the
 * screen has two separate events for them ([EditTransactionViewEvent.OnChangeDate] and
 * [EditTransactionViewEvent.OnChangeTime]) and the legacy row they replace was likewise
 * split into a date half and a time half.
 */
@Composable
fun DateTimeRow(
    dateTimeText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTimeClick: (() -> Unit)? = null,
) {
    SettingsItem(
        title = dateTimeText ?: stringResource(R.string.set_date_and_time),
        onClick = onClick,
        modifier = modifier,
        description = stringResource(R.string.date_and_time),
        titleColor = if (dateTimeText == null) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color.Unspecified
        },
        icon = Icons.Outlined.CalendarMonth,
        trailing = onTimeClick?.let { onTime ->
            {
                IconButton(onClick = onTime) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = stringResource(R.string.change_time),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
fun DueDateRow(
    dueDateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsItem(
        title = dueDateText,
        onClick = onClick,
        modifier = modifier,
        description = stringResource(R.string.due_date),
        icon = Icons.Outlined.Event,
    )
}

@Composable
fun DescriptionRow(
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsItem(
        title = description ?: stringResource(R.string.add_description),
        onClick = onClick,
        modifier = modifier,
        description = stringResource(R.string.description),
        titleColor = if (description == null) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color.Unspecified
        },
        icon = Icons.AutoMirrored.Outlined.Notes,
    )
}

@Composable
fun TagsRow(
    tagCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsItem(
        title = if (tagCount == 0) {
            stringResource(R.string.add_tags)
        } else {
            pluralStringResource(R.plurals.tag_count, tagCount, tagCount)
        },
        onClick = onClick,
        modifier = modifier,
        description = stringResource(R.string.tags),
        titleColor = if (tagCount == 0) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color.Unspecified
        },
        icon = Icons.Outlined.Sell,
    )
}

@Composable
fun ExchangeRateRow(
    rateText: String,
    onClick: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extendedColors = LocalIvyExtendedColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(extendedColors.warningContainer)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CurrencyExchange,
            contentDescription = null,
            tint = extendedColors.onWarningContainer,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.custom_exchange_rate),
                color = extendedColors.onWarningContainer,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = rateText,
                color = extendedColors.onWarningContainer,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onReset) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.reset_exchange_rate),
                tint = extendedColors.onWarningContainer,
            )
        }
    }
}

@Preview
@Composable
private fun RowsFilledPreview() {
    IvyPreview(dark = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                CategoryRow(
                    categoryName = "Groceries",
                    categoryColor = Color(0xFF25B26A),
                    categoryIcon = null,
                    onClick = {},
                )
                AccountRow(
                    accountName = "Cash",
                    currency = "USD",
                    label = stringResource(R.string.account),
                    onClick = {},
                )
                DateTimeRow(dateTimeText = "Today, 14:20", onClick = {}, onTimeClick = {})
                DueDateRow(dueDateText = "Aug 12, 2026", onClick = {})
                DescriptionRow(description = "Weekly shop", onClick = {})
                TagsRow(tagCount = 3, onClick = {})
                ExchangeRateRow(rateText = "1 USD = 0.92 EUR", onClick = {}, onReset = {})
            }
        }
    }
}

@Preview
@Composable
private fun RowsEmptyPreview() {
    IvyPreview(dark = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                CategoryRow(
                    categoryName = null,
                    categoryColor = null,
                    categoryIcon = null,
                    onClick = {},
                )
                AccountRow(
                    accountName = null,
                    currency = null,
                    label = stringResource(R.string.account),
                    onClick = {},
                )
                DateTimeRow(dateTimeText = null, onClick = {}, onTimeClick = {})
                DescriptionRow(description = null, onClick = {})
                TagsRow(tagCount = 0, onClick = {})
            }
        }
    }
}
