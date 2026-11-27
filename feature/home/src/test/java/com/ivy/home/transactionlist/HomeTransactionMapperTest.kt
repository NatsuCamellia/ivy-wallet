package com.ivy.home.transactionlist

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.component.transaction.TransactionItemMapper
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class HomeTransactionMapperTest {

    private val timeFormatter = mockk<TimeFormatter>()
    private val timeConverter = mockk<TimeConverter>()

    init {
        with(timeFormatter) {
            every {
                any<java.time.LocalDateTime>().format(any<TimeFormatter.Style>())
            } returns "Today"
            every { any<Instant>().formatLocal(any()) } returns "Fri, Jul 24"
            every { any<LocalTime>().format() } returns "14:05"
        }
        with(timeConverter) {
            every { any<Instant>().toLocalTime() } returns LocalTime.of(14, 5)
        }
    }

    private fun mapper(): HomeTransactionMapper = HomeTransactionMapper(
        items = TransactionItemMapper(
            baseData = AppBaseData(
                baseCurrency = "USD",
                accounts = persistentListOf(cash),
                categories = persistentListOf(),
            ),
            timeConverter = timeConverter,
            timeFormatter = timeFormatter,
            deletedText = "deleted",
            dueOnFormat = "Due on %1\$s",
            expenseFallback = "Expense",
            incomeFallback = "Income",
            transferFallback = "Transfer",
            billPaidFormat = "Bill for %1\$s %2\$s paid",
            formatAmount = { amount, currency -> "$amount $currency" },
        ),
        baseCurrency = "USD",
        timeFormatter = timeFormatter,
    )

    @Test
    fun `day header carries formatted date and signed net total`() {
        val items = mapper().mapHistory(listOf(divider(income = 100.0, expenses = 40.0)))

        val header = items.first() as HomeTrnListItem.DayHeader
        header.title shouldBe "Today"
        header.netText shouldBe "+60.0 USD"
    }

    @Test
    fun `history keeps the divider order and maps transactions without a due date`() {
        val items = mapper().mapHistory(listOf(divider(0.0, 10.0), expenseTrn()))

        items.size shouldBe 2
        val row = (items[1] as HomeTrnListItem.Trn).item
        row.ui.time shouldBe "14:05"
        row.ui.dueText shouldBe null
        row.ui.dueOverdue shouldBe false
    }

    @Test
    fun `due rows flag only overdue ones`() {
        val due = expenseTrn().copy(dateTime = null, dueDate = Instant.EPOCH)

        val rows = mapper().mapDueRows(listOf(HomeDueRow(due, overdue = true), HomeDueRow(due, overdue = false)))

        rows.map { it.item.ui.dueText } shouldBe listOf("Due on Fri, Jul 24", "Due on Fri, Jul 24")
        rows.map { it.item.ui.dueOverdue } shouldBe listOf(true, false)
        rows.forEach { it.item.ui.time shouldBe null }
    }

    companion object {
        private val cash = Account(name = "Cash", color = 0xFF00FF00.toInt())

        private fun divider(income: Double, expenses: Double): TransactionHistoryDateDivider =
            TransactionHistoryDateDivider(
                date = LocalDate.of(2026, 7, 19),
                income = income,
                expenses = expenses,
            )

        private fun expenseTrn(): Transaction = Transaction(
            accountId = cash.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("32.51"),
            title = "Trn",
            dateTime = Instant.EPOCH,
        )
    }
}
