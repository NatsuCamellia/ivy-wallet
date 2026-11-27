package com.ivy.planned.list

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
import com.ivy.legacy.ui.component.label
import com.ivy.legacy.ui.component.transaction.TransactionBadge
import com.ivy.legacy.ui.component.transaction.amountKind
import com.ivy.legacy.ui.component.transaction.amountSign
import com.ivy.legacy.ui.component.transaction.formatAmountWithCurrency
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.ui.R
import com.ivy.ui.color.Blue
import com.ivy.ui.color.Green
import com.ivy.ui.component.FlatListItem
import com.ivy.ui.component.transaction.amountColor
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.TimeFormatter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * One planned payment in a list as a flat row: its title and the category and account as text, the
 * amount colored by its type and the recurrence under them.
 */
@Composable
fun PlannedPaymentCard(
    baseCurrency: String,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    plannedPayment: PlannedPaymentRule,
    onClick: (PlannedPaymentRule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = accounts.find { it.id == plannedPayment.accountId }
    val category = plannedPayment.categoryId?.let { targetId -> categories.find { it.id.value == targetId } }
    val title = plannedPayment.title.takeIf { it.isNotNullOrBlank() }
    val type = plannedPayment.type
    val currency = account?.currency ?: baseCurrency
    val categoryName = category?.name?.value
    val accountName = account?.name ?: stringResource(R.string.deleted)

    FlatListItem(
        title = title ?: categoryName ?: type.label(),
        onClick = { if (account != null) onClick(plannedPayment) },
        modifier = modifier.testTag("planned_payment_card"),
        supportingText = if (type == TransactionType.TRANSFER) {
            null
        } else {
            listOfNotNull(categoryName.takeIf { title != null }, accountName).joinToString(" \u00B7 ")
        },
        primaryText = "${type.amountSign()}${formatAmountWithCurrency(plannedPayment.amount, currency)}",
        primaryColor = type.amountKind().amountColor(),
        leading = {
            TransactionBadge(
                color = category?.color?.value?.let(::Color),
                iconAsset = category?.icon?.id,
                isTransfer = type == TransactionType.TRANSFER,
            )
        },
        supportingContent = { RuleText(plannedPayment) },
    )
}

@Composable
private fun RuleText(rule: PlannedPaymentRule) {
    val startDate = rule.startDate?.formatted() ?: stringResource(R.string.null_text)
    val intervalN = rule.intervalN
    val intervalType = rule.intervalType
    Column {
        if (rule.oneTime) {
            RuleLine(stringResource(R.string.planned_for_date, startDate))
        } else if (intervalN != null && intervalType != null) {
            RuleLine(
                stringResource(
                    R.string.planned_starts_repeats_every,
                    startDate,
                    intervalN,
                    intervalType.forDisplay(intervalN),
                ),
            )
        } else {
            RuleLine(stringResource(R.string.planned_starts_date, startDate))
            RuleLine(stringResource(R.string.invalid_planned_rule), MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun RuleLine(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
private fun Instant.formatted(): String = with(LocalTimeFormatter.current) {
    formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = false))
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
