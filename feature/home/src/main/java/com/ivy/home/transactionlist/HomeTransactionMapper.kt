package com.ivy.home.transactionlist

import androidx.compose.ui.graphics.Color
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.format
import com.ivy.ui.component.transaction.TransactionAmountKind
import com.ivy.ui.component.transaction.TransactionItemPosition
import com.ivy.ui.component.transaction.TransactionItemUi
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import java.math.BigDecimal
import java.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class HomeTransactionMapper(
    private val baseData: AppBaseData,
    private val timeConverter: TimeConverter,
    private val timeFormatter: TimeFormatter,
    private val deletedText: String,
    private val dueOnFormat: String,
    private val expenseFallback: String,
    private val incomeFallback: String,
    private val transferFallback: String,
    private val formatAmount: (Double, String) -> String = { amount, currency ->
        "${amount.format(currency)} $currency"
    },
) {
    fun mapHistory(history: List<TransactionHistoryItem>): ImmutableList<HomeTrnListItem> {
        val result = mutableListOf<HomeTrnListItem>()
        val run = mutableListOf<Transaction>()

        fun flushRun() {
            run.forEachIndexed { index, trn ->
                result += mapTransaction(trn, position(index, run.size), dueKind = null)
            }
            run.clear()
        }

        for (item in history) {
            when (item) {
                is TransactionHistoryDateDivider -> {
                    flushRun()
                    result += mapDayHeader(item)
                }

                is Transaction -> run += item
            }
        }
        flushRun()
        return result.toImmutableList()
    }

    fun mapDueSection(
        trns: List<Transaction>,
        overdue: Boolean,
    ): ImmutableList<HomeTrnListItem.Trn> = trns.mapIndexed { index, trn ->
        mapTransaction(
            trn = trn,
            position = position(index, trns.size),
            dueKind = if (overdue) TransactionAmountKind.Overdue else TransactionAmountKind.Upcoming,
        )
    }.toImmutableList()

    fun sectionSubtitle(stats: IncomeExpensePair, currency: String): String? {
        val parts = buildList {
            if (stats.income > BigDecimal.ZERO) {
                add("+${formatAmount(stats.income.toDouble(), currency)}")
            }
            if (stats.expense > BigDecimal.ZERO) {
                add("-${formatAmount(stats.expense.abs().toDouble(), currency)}")
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun mapDayHeader(divider: TransactionHistoryDateDivider): HomeTrnListItem.DayHeader {
        val net = divider.income - divider.expenses
        val sign = if (net >= 0) "+" else "-"
        return HomeTrnListItem.DayHeader(
            key = divider.date.toString(),
            title = with(timeFormatter) {
                divider.date.atStartOfDay().format(
                    TimeFormatter.Style.DateOnly(includeWeekDay = true),
                )
            },
            netText = "$sign${formatAmount(kotlin.math.abs(net), baseData.baseCurrency)}",
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mapTransaction(
        trn: Transaction,
        position: TransactionItemPosition,
        dueKind: TransactionAmountKind?,
    ): HomeTrnListItem.Trn {
        val account = baseData.accounts.find { it.id == trn.accountId }
        val toAccount = baseData.accounts.find { it.id == trn.toAccountId }
        val currency = account?.currency ?: baseData.baseCurrency
        val toCurrency = toAccount?.currency ?: baseData.baseCurrency
        val category = baseData.categories.find { it.id.value == trn.categoryId }

        val accountName = account?.name ?: deletedText
        val isTransfer = trn.type == TransactionType.TRANSFER
        val titledByCategory = trn.title.isNullOrBlank() && category != null
        val title = when {
            !trn.title.isNullOrBlank() -> trn.title!!
            category != null -> category.name.value
            else -> when (trn.type) {
                TransactionType.INCOME -> incomeFallback
                TransactionType.EXPENSE -> expenseFallback
                TransactionType.TRANSFER -> transferFallback
            }
        }
        val supporting = if (isTransfer) {
            "$accountName → ${toAccount?.name ?: deletedText}"
        } else {
            listOfNotNull(
                category?.name?.value?.takeIf { !titledByCategory },
                accountName,
            ).joinToString(" · ").ifBlank { null }
        }

        val amountKind = dueKind ?: when (trn.type) {
            TransactionType.INCOME -> TransactionAmountKind.Income
            TransactionType.EXPENSE -> TransactionAmountKind.Expense
            TransactionType.TRANSFER -> TransactionAmountKind.Transfer
        }
        val amountSign = when {
            dueKind != null || trn.type == TransactionType.EXPENSE -> "-"
            trn.type == TransactionType.INCOME -> "+"
            else -> ""
        }
        val amountText = "$amountSign${formatAmount(trn.amount.toDouble(), currency)}"

        val secondaryText = when {
            dueKind != null -> null
            isTransfer && toCurrency != currency ->
                formatAmount(trn.toAmount.toDouble(), toCurrency)
            else -> trn.dateTime?.let(::formatTransactionTime)
        }

        val dueText = if (dueKind != null && trn.dueDate != null) {
            String.format(dueOnFormat, formatDueDate(trn.dueDate!!))
        } else {
            null
        }

        return HomeTrnListItem.Trn(
            ui = TransactionItemUi(
                id = trn.id.toString(),
                title = title,
                supportingText = supporting,
                categoryColor = categoryColor(category?.color?.value, account, isTransfer),
                amountText = amountText,
                amountKind = amountKind,
                secondaryText = secondaryText,
                dueText = dueText,
            ),
            position = position,
            iconAsset = category?.icon?.id ?: if (isTransfer) null else account?.icon,
            trn = trn,
        )
    }

    private fun formatTransactionTime(instant: Instant): String = with(timeFormatter) {
        with(timeConverter) { instant.toLocalTime() }.format()
    }

    private fun formatDueDate(instant: Instant): String = with(timeFormatter) {
        instant.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = true))
    }

    private fun categoryColor(categoryColor: Int?, account: Account?, isTransfer: Boolean): Color? =
        when {
            categoryColor != null -> Color(categoryColor)
            isTransfer -> null
            else -> account?.color?.let(::Color)
        }

    private fun position(index: Int, size: Int): TransactionItemPosition = when {
        size == 1 -> TransactionItemPosition.Single
        index == 0 -> TransactionItemPosition.First
        index == size - 1 -> TransactionItemPosition.Last
        else -> TransactionItemPosition.Middle
    }
}
