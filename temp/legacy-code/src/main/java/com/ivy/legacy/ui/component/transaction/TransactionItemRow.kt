package com.ivy.legacy.ui.component.transaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ivy.base.model.TransactionType
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.ui.R
import com.ivy.ui.component.transaction.FlatTransactionItem
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter

@Composable
fun rememberTransactionItemMapper(baseData: AppBaseData): TransactionItemMapper {
    val timeConverter = LocalTimeConverter.current
    val timeFormatter = LocalTimeFormatter.current
    val deletedText = stringResource(R.string.deleted)
    val dueOnFormat = stringResource(R.string.due_on)
    val expenseFallback = stringResource(R.string.expense)
    val incomeFallback = stringResource(R.string.income)
    val transferFallback = stringResource(R.string.transfer)
    return remember(baseData, timeConverter, timeFormatter) {
        TransactionItemMapper(
            baseData = baseData,
            timeConverter = timeConverter,
            timeFormatter = timeFormatter,
            deletedText = deletedText,
            dueOnFormat = dueOnFormat,
            expenseFallback = expenseFallback,
            incomeFallback = incomeFallback,
            transferFallback = transferFallback,
        )
    }
}

/**
 * One transaction as a flat row with its badge. Passing [onSkip] and [onPayOrGet] shows the Skip and
 * Pay (an expense) or Get buttons of a planned payment.
 */
@Composable
fun TransactionItemRow(
    item: TransactionRowItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onPayOrGet: (() -> Unit)? = null,
) {
    FlatTransactionItem(
        ui = item.ui,
        onClick = onClick,
        modifier = modifier,
        onSkip = onSkip,
        onPayOrGet = onPayOrGet,
        payOrGetText = stringResource(
            if (item.trn.type == TransactionType.EXPENSE) R.string.pay else R.string.get,
        ),
    ) {
        TransactionBadge(
            color = item.ui.categoryColor,
            iconAsset = item.iconAsset,
            isTransfer = item.trn.type == TransactionType.TRANSFER,
        )
    }
}

/** The category (or account) [color] with its icon, a transfer without one shows the transfer glyph. */
@Composable
fun TransactionBadge(
    color: Color?,
    iconAsset: String?,
    isTransfer: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        color != null -> IconBadge(
            color = color,
            iconName = iconAsset,
            defaultIcon = if (isTransfer) R.drawable.ic_transfer else R.drawable.ic_custom_category_m,
            modifier = modifier,
        )

        isTransfer -> IconBadge(
            color = MaterialTheme.colorScheme.primaryContainer,
            icon = Icons.Default.SwapHoriz,
            modifier = modifier,
        )

        else -> IconBadge(
            color = MaterialTheme.colorScheme.secondaryContainer,
            iconName = iconAsset,
            defaultIcon = R.drawable.ic_custom_category_m,
            modifier = modifier,
        )
    }
}
