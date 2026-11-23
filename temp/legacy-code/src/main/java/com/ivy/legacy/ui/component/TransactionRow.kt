package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.LegacyTag
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.component.transaction.account
import com.ivy.legacy.ui.component.transaction.category
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.navigation.Navigation
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.TimeFormatter
import kotlinx.collections.immutable.ImmutableList
import java.time.Instant
import java.time.LocalDateTime

private enum class DueState { Upcoming, Overdue, Due }

/**
 * One transaction in a list. [index] and [count] place it in its group of rows so the group gets
 * rounded outer corners; the gap between rows is the caller's.
 */
@Composable
fun TransactionRow(
    baseData: AppBaseData,
    transaction: Transaction,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    onPayOrGet: (Transaction) -> Unit,
    onClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
    onSkipTransaction: (Transaction) -> Unit = {},
) {
    val account = account(transaction.accountId, baseData.accounts)
    val toAccount = account(transaction.toAccountId, baseData.accounts)
    val category = category(transaction.categoryId, baseData.categories)

    val title = transaction.title.takeIf { it.isNotNullOrBlank() }
    val description = transactionDescription(transaction)
    val categoryName = category?.name?.value
    val headline = title ?: description ?: categoryName ?: transaction.type.label()
    val time = transaction.dateTime?.let { dateTime ->
        val localTime = with(LocalTimeConverter.current) { dateTime.toLocalDateTime() }.toLocalTime()
        with(LocalTimeFormatter.current) { localTime.format() }
    }

    SegmentedListItem(
        onClick = { if (account != null) onClick(transaction) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.testTag("transaction_card"),
        leadingContent = { TransactionBadge(category, transaction.type) },
        overlineContent = transaction.dueDate?.let { dueDate -> { DueOverline(dueDate, dueState(transaction)) } },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (title != null && description != null) {
                    Text(text = description, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                NavigationChips(
                    category = category,
                    account = account,
                    toAccount = toAccount,
                    isTransfer = transaction.type == TransactionType.TRANSFER,
                    showAccountColor = shouldShowAccountSpecificColorInTransactions,
                    time = time,
                )
                Tags(transaction.tags)
                if (transaction.dueDate != null && transaction.dateTime == null) {
                    PayOrSkipButtons(
                        isExpense = transaction.type == TransactionType.EXPENSE,
                        onSkip = { onSkipTransaction(transaction) },
                        onPayOrGet = { onPayOrGet(transaction) },
                    )
                }
            }
        },
    ) {
        Headline(
            headline = headline,
            transaction = transaction,
            currency = account?.currency ?: baseData.baseCurrency,
            toCurrency = toAccount?.currency ?: baseData.baseCurrency,
        )
    }
}

/** The name and the amount share a line; when the amount is too wide for that it moves to its own line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Headline(headline: String, transaction: Transaction, currency: String, toCurrency: String) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = headline,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Amounts(transaction, currency, toCurrency)
    }
}

/** The category as a badge in its colour; without a category a transfer or a neutral default badge. */
@Composable
fun TransactionBadge(category: Category?, type: TransactionType, modifier: Modifier = Modifier) {
    when {
        category != null -> IconBadge(
            color = Color(category.color.value),
            iconName = category.icon?.id,
            defaultIcon = R.drawable.ic_custom_category_m,
            modifier = modifier,
        )

        type == TransactionType.TRANSFER -> IconBadge(
            color = MaterialTheme.colorScheme.primaryContainer,
            icon = Icons.Default.SwapHoriz,
            modifier = modifier,
        )

        else -> IconBadge(
            color = MaterialTheme.colorScheme.secondaryContainer,
            iconName = null,
            defaultIcon = R.drawable.ic_custom_category_m,
            modifier = modifier,
        )
    }
}

@Composable
private fun DueOverline(dueDate: Instant, due: DueState?) {
    val dueOn = with(LocalTimeFormatter.current) {
        dueDate.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = true))
    }
    val dueOnText = stringResource(R.string.due_on, dueOn)
    when (due) {
        DueState.Upcoming -> Text(
            text = "${stringResource(R.string.upcoming)} · $dueOnText",
            color = MaterialTheme.colorScheme.secondary,
        )

        DueState.Overdue -> Text(
            text = "${stringResource(R.string.overdue)} · $dueOnText",
            color = MaterialTheme.colorScheme.error,
        )

        else -> Text(dueOnText)
    }
}

/**
 * Chips that open the category's and the account's transactions; a transfer shows both accounts.
 * The optional [time] ends the last line, after the chips, so it never sits alone above them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavigationChips(
    category: Category?,
    account: Account?,
    toAccount: Account?,
    isTransfer: Boolean,
    showAccountColor: Boolean,
    modifier: Modifier = Modifier,
    time: String? = null,
) {
    val nav = navigation()
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (category != null) {
            AssistChip(
                onClick = {
                    nav.navigateTo(TransactionsScreen(accountId = null, categoryId = category.id.value))
                },
                label = { Text(category.name.value) },
            )
        }
        AccountChip(account, showAccountColor, nav)
        if (isTransfer) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
                AccountChip(toAccount, showAccountColor, nav)
            }
        }
        if (time != null) {
            Text(
                text = time,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun AccountChip(account: Account?, showColor: Boolean, nav: Navigation) {
    AssistChip(
        onClick = {
            account?.let { nav.navigateTo(TransactionsScreen(accountId = it.id, categoryId = null)) }
        },
        label = { Text(account?.name ?: stringResource(R.string.deleted)) },
        leadingIcon = if (showColor && account != null) {
            {
                IconBadge(
                    color = Color(account.color),
                    iconName = account.icon,
                    defaultIcon = R.drawable.ic_custom_account_m,
                    size = AssistChipDefaults.IconSize,
                )
            }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(tags: ImmutableList<LegacyTag>) {
    if (tags.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Text(
                text = "#${tag.name}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Amounts(transaction: Transaction, currency: String, toCurrency: String) {
    val type = transaction.type
    val amount = transaction.amount.toDouble()
    Column(horizontalAlignment = Alignment.End) {
        TypeAmount(type = type, amount = amount, currency = currency)
        if (type == TransactionType.TRANSFER && toCurrency != currency) {
            AmountText(
                amount = transaction.toAmount.toDouble(),
                currency = toCurrency,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PayOrSkipButtons(
    isExpense: Boolean,
    onSkip: () -> Unit,
    onPayOrGet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onSkip) {
            Text(stringResource(R.string.skip))
        }
        Button(onClick = onPayOrGet) {
            Text(stringResource(if (isExpense) R.string.pay else R.string.get))
        }
    }
}

@Composable
private fun dueState(transaction: Transaction): DueState? {
    val dueDate: LocalDateTime = with(LocalTimeConverter.current) {
        transaction.dueDate?.toLocalDateTime()
    } ?: return null
    return when {
        dueDate.isAfter(timeNowUTC()) -> DueState.Upcoming
        dueDate.isBefore(dateNowUTC().atStartOfDay()) -> DueState.Overdue
        else -> DueState.Due
    }
}

@Composable
private fun transactionDescription(transaction: Transaction): String? {
    val paidFor = with(LocalTimeConverter.current) {
        transaction.paidFor?.toLocalDateTime()
    }
    return when {
        transaction.description.isNotNullOrBlank() -> transaction.description
        transaction.recurringRuleId != null &&
                transaction.dueDate == null &&
                paidFor != null -> {
            stringResource(
                R.string.bill_paid,
                paidFor.month.name.lowercase().capitalizeLocal(),
                paidFor.year.toString()
            )
        }

        else -> null
    }
}
