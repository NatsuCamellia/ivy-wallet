package com.ivy.planned.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.IntervalType
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.forDisplay
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.NavigationChips
import com.ivy.legacy.ui.component.TransactionBadge
import com.ivy.legacy.ui.component.color
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.ui.R
import com.ivy.ui.color.Blue
import com.ivy.ui.color.Green
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.TimeFormatter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.abs

/**
 * One planned payment in a list. [index] and [count] place it in its group of rows so the group gets
 * rounded outer corners; the gap between rows is the caller's.
 */
@Composable
fun PlannedPaymentCard(
    baseCurrency: String,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    plannedPayment: PlannedPaymentRule,
    onClick: (PlannedPaymentRule) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
) {
    val account = accounts.find { it.id == plannedPayment.accountId }
    val category = plannedPayment.categoryId?.let { targetId -> categories.find { it.id.value == targetId } }
    val title = plannedPayment.title.takeIf { it.isNotNullOrBlank() }
    val type = plannedPayment.type

    SegmentedListItem(
        onClick = { if (account != null) onClick(plannedPayment) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.testTag("planned_payment_card"),
        leadingContent = { TransactionBadge(category, type) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RuleText(plannedPayment)
                if (type != TransactionType.TRANSFER) {
                    NavigationChips(
                        category = category,
                        account = account,
                        toAccount = null,
                        isTransfer = false,
                        showAccountColor = true,
                    )
                }
            }
        },
    ) {
        Headline(
            headline = title ?: category?.name?.value ?: type.label(),
            type = type,
            amount = plannedPayment.amount,
            currency = account?.currency ?: baseCurrency,
        )
    }
}

@Composable
private fun Headline(headline: String, type: TransactionType, amount: Double, currency: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = headline,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = type.icon(),
                contentDescription = type.label(),
                modifier = Modifier.size(AssistChipDefaults.IconSize),
                tint = type.color(),
            )
            AmountText(
                amount = if (type == TransactionType.EXPENSE) -abs(amount) else amount,
                currency = currency,
                style = MaterialTheme.typography.titleMediumEmphasized.copy(color = type.color()),
                showPlusSign = type == TransactionType.INCOME,
            )
        }
    }
}

@Composable
private fun RuleText(rule: PlannedPaymentRule) {
    val startDate = rule.startDate?.formatted() ?: stringResource(R.string.null_text)
    val intervalN = rule.intervalN
    val intervalType = rule.intervalType
    if (rule.oneTime) {
        Text(stringResource(R.string.planned_for_date, startDate))
    } else if (intervalN != null && intervalType != null) {
        Text(
            text = stringResource(R.string.planned_starts_date, startDate) + " · " + stringResource(
                R.string.planned_repeats_every,
                intervalN,
                intervalType.forDisplay(intervalN)
            )
        )
    } else {
        Text(stringResource(R.string.planned_starts_date, startDate))
        Text(
            text = stringResource(R.string.invalid_planned_rule),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun Instant.formatted(): String = with(LocalTimeFormatter.current) {
    formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = false))
}

@Composable
private fun TransactionType.label(): String = stringResource(
    when (this) {
        TransactionType.INCOME -> R.string.income
        TransactionType.EXPENSE -> R.string.expense
        TransactionType.TRANSFER -> R.string.transfer
    }
)

private fun TransactionType.icon(): ImageVector = when (this) {
    TransactionType.INCOME -> Icons.Default.ArrowDownward
    TransactionType.EXPENSE -> Icons.Default.ArrowUpward
    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
}

@Suppress("MagicNumber")
private val previewStart = LocalDateTime.of(2023, 4, 27, 12, 0).toInstant(ZoneOffset.UTC)

@Composable
private fun PreviewCard(
    rule: (accountId: UUID, categoryId: UUID) -> PlannedPaymentRule,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        NavigationRoot(navigation = Navigation()) {
            val cash = Account(name = "Cash", Green.toArgb())
            val food = Category(
                name = NotBlankTrimmedString.unsafe("Food"),
                color = ColorInt(Blue.toArgb()),
                icon = null,
                id = CategoryId(UUID(0, 1)),
                orderNum = 0.0,
            )
            PlannedPaymentCard(
                baseCurrency = "BGN",
                categories = persistentListOf(food),
                accounts = persistentListOf(cash),
                plannedPayment = rule(cash.id, food.id.value),
                onClick = {},
                modifier = modifier,
            )
        }
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_oneTime() {
    PreviewCard(
        rule = { accountId, categoryId ->
            PlannedPaymentRule(
                accountId = accountId,
                title = "Lidl pazar",
                categoryId = categoryId,
                amount = 250.75,
                startDate = previewStart,
                oneTime = true,
                intervalType = null,
                intervalN = null,
                type = TransactionType.EXPENSE
            )
        }
    )
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_recurring() {
    PreviewCard(
        rule = { accountId, categoryId ->
            PlannedPaymentRule(
                accountId = accountId,
                title = "Tabu",
                categoryId = categoryId,
                amount = 250.75,
                startDate = previewStart,
                oneTime = false,
                intervalType = IntervalType.MONTH,
                intervalN = 1,
                type = TransactionType.INCOME
            )
        }
    )
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_recurringError() {
    PreviewCard(
        rule = { accountId, categoryId ->
            PlannedPaymentRule(
                accountId = accountId,
                title = "Tabu",
                categoryId = categoryId,
                amount = 250.75,
                startDate = previewStart,
                oneTime = false,
                intervalType = null,
                intervalN = null,
                type = TransactionType.EXPENSE
            )
        }
    )
}
