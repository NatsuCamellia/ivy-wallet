package com.ivy.legacy.ui.component.transaction

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.format
import com.ivy.ui.component.transaction.TransactionAmountKind
import com.ivy.ui.component.transaction.TransactionItemUi
import com.ivy.ui.time.TimeFormatter
import java.time.Instant

/** A planned payment that is still to be paid: [Overdue] once its date has passed, [Upcoming] before. */
enum class TransactionDue { Overdue, Upcoming }

/** A [Transaction] with the [ui] a flat transaction row shows and the icon of its badge. */
@Immutable
data class TransactionRowItem(
    val ui: TransactionItemUi,
    val iconAsset: String?,
    val trn: Transaction,
)

/** The amount followed by its currency code, e.g. "250.75 USD". */
fun formatAmountWithCurrency(amount: Double, currency: String): String = "${amount.format(currency)} $currency"

/** The sign an amount of this type shows: an expense is negative, an income gets a plus. */
fun TransactionType.amountSign(): String = when (this) {
    TransactionType.EXPENSE -> "-"
    TransactionType.INCOME -> "+"
    TransactionType.TRANSFER -> ""
}

fun TransactionType.amountKind(): TransactionAmountKind = when (this) {
    TransactionType.INCOME -> TransactionAmountKind.Income
    TransactionType.EXPENSE -> TransactionAmountKind.Expense
    TransactionType.TRANSFER -> TransactionAmountKind.Transfer
}

/**
 * Maps a [Transaction] to the [TransactionRowItem] that Home, the account and category screens, the
 * Transactions screen, Reports and Search all show as a flat row.
 */
class TransactionItemMapper(
    private val baseData: AppBaseData,
    private val timeConverter: TimeConverter,
    private val timeFormatter: TimeFormatter,
    private val deletedText: String,
    private val dueOnFormat: String,
    private val expenseFallback: String,
    private val incomeFallback: String,
    private val transferFallback: String,
    private val billPaidFormat: String,
    val formatAmount: (Double, String) -> String = ::formatAmountWithCurrency,
) {
    /** [due] is set for a planned payment in a Due list and `null` for a transaction in the history. */
    @Suppress("CyclomaticComplexMethod")
    fun map(trn: Transaction, due: TransactionDue? = null): TransactionRowItem {
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

        val amountText = "${trn.type.amountSign()}${formatAmount(trn.amount.toDouble(), currency)}"

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

        return TransactionRowItem(
            ui = TransactionItemUi(
                id = trn.id.toString(),
                title = title,
                supportingText = supporting,
                descriptionText = description(trn),
                tagsText = tagsText(trn),
                time = time,
                categoryColor = categoryColor(category?.color?.value, account, isTransfer),
                amountText = amountText,
                amountKind = trn.type.amountKind(),
                secondaryText = secondaryText,
                dueText = dueText,
                dueOverdue = due == TransactionDue.Overdue,
            ),
            iconAsset = category?.icon?.id ?: if (isTransfer) null else account?.icon,
            trn = trn,
        )
    }

    /**
     * The description, else for a paid recurring bill the "bill paid" text, like the transaction card
     * did; `null` when there is neither.
     */
    private fun description(trn: Transaction): String? {
        val paidFor = trn.paidFor
        return when {
            !trn.description.isNullOrBlank() -> trn.description
            trn.recurringRuleId != null && trn.dueDate == null && paidFor != null -> {
                val paidOn = with(timeConverter) { paidFor.toLocalDateTime() }
                String.format(
                    billPaidFormat,
                    paidOn.month.name.lowercase().capitalizeLocal(),
                    paidOn.year.toString(),
                )
            }

            else -> null
        }
    }

    /** The tags as "#groceries #work", sorted by name so the order does not depend on the database. */
    private fun tagsText(trn: Transaction): String? = trn.tags
        .map { it.name }
        .sortedBy { it.lowercase() }
        .joinToString(" ") { "#$it" }
        .ifBlank { null }

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
