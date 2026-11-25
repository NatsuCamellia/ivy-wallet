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
import com.ivy.ui.component.transaction.TransactionItemUi
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
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

        for (item in history) {
            when (item) {
                is TransactionHistoryDateDivider -> result += mapDayHeader(item)
                is Transaction -> result += mapTransaction(item, due = null)
            }
        }
        return result.toImmutableList()
    }

    fun mapDueRows(rows: List<HomeDueRow>): ImmutableList<HomeTrnListItem.Trn> =
        rows.map { mapTransaction(trn = it.transaction, due = it) }.toImmutableList()

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
        due: HomeDueRow?,
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
        val time = if (due == null) trn.dateTime?.let(::formatTransactionTime) else null

        val amountKind = when (trn.type) {
            TransactionType.INCOME -> TransactionAmountKind.Income
            TransactionType.EXPENSE -> TransactionAmountKind.Expense
            TransactionType.TRANSFER -> TransactionAmountKind.Transfer
        }
        val amountSign = when (trn.type) {
            TransactionType.EXPENSE -> "-"
            TransactionType.INCOME -> "+"
            TransactionType.TRANSFER -> ""
        }
        val amountText = "$amountSign${formatAmount(trn.amount.toDouble(), currency)}"

        val secondaryText = if (due == null && isTransfer && toCurrency != currency) {
            formatAmount(trn.toAmount.toDouble(), toCurrency)
        } else {
            null
        }

        val dueText = if (due != null && trn.dueDate != null) {
            String.format(dueOnFormat, formatDueDate(trn.dueDate!!))
        } else {
            null
        }

        return HomeTrnListItem.Trn(
            ui = TransactionItemUi(
                id = trn.id.toString(),
                title = title,
                supportingText = supporting,
                time = time,
                categoryColor = categoryColor(category?.color?.value, account, isTransfer),
                amountText = amountText,
                amountKind = amountKind,
                secondaryText = secondaryText,
                dueText = dueText,
                dueOverdue = due?.overdue == true,
            ),
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
}
