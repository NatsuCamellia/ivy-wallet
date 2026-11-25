package com.ivy.home.transactionlist

import androidx.compose.ui.graphics.Color
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.component.transaction.TransactionAmountKind
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
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

    private fun mapper(
        accounts: List<Account> = listOf(cash),
        categories: List<Category> = listOf(food),
    ): HomeTransactionMapper = HomeTransactionMapper(
        baseData = AppBaseData(
            baseCurrency = "USD",
            accounts = persistentListOf(*accounts.toTypedArray()),
            categories = persistentListOf(*categories.toTypedArray()),
        ),
        timeConverter = timeConverter,
        timeFormatter = timeFormatter,
        deletedText = "deleted",
        dueOnFormat = "Due on %1\$s",
        expenseFallback = "Expense",
        incomeFallback = "Income",
        transferFallback = "Transfer",
        formatAmount = { amount, currency -> "$amount $currency" },
    )

    private fun mapperWithDefaultFormatAmount(): HomeTransactionMapper = HomeTransactionMapper(
        baseData = AppBaseData(
            baseCurrency = "USD",
            accounts = persistentListOf(cash),
            categories = persistentListOf(food),
        ),
        timeConverter = timeConverter,
        timeFormatter = timeFormatter,
        deletedText = "deleted",
        dueOnFormat = "Due on %1\$s",
        expenseFallback = "Expense",
        incomeFallback = "Income",
        transferFallback = "Transfer",
    )

    @Test
    fun `default formatAmount appends the currency code`() {
        // Given
        val mapper = mapperWithDefaultFormatAmount()

        // When
        val item = mapper.mapHistory(listOf(divider(0.0, 10.0), expenseTrn("a")))[1]
            as HomeTrnListItem.Trn

        // Then
        item.ui.amountText shouldStartWith "-"
        item.ui.amountText shouldEndWith " USD"
    }

    @Test
    fun `day header carries formatted date and signed net total`() {
        val items = mapper().mapHistory(listOf(divider(income = 100.0, expenses = 40.0)))

        val header = items.first() as HomeTrnListItem.DayHeader
        header.title shouldBe "Today"
        header.netText shouldBe "+60.0 USD"
    }

    @Test
    fun `untitled expense falls back to category name and keeps only account in supporting text`() {
        val trn = expenseTrn("a").copy(title = null)

        val item = mapper().mapHistory(listOf(divider(0.0, 10.0), trn))[1] as HomeTrnListItem.Trn

        item.ui.title shouldBe "Food"
        item.ui.supportingText shouldBe "Cash"
        item.ui.time shouldBe "14:05"
    }

    @Test
    fun `titled expense shows category and account in supporting text`() {
        val item = mapper()
            .mapHistory(listOf(divider(0.0, 10.0), expenseTrn("a")))[1] as HomeTrnListItem.Trn

        item.ui.supportingText shouldBe "Food · Cash"
        item.ui.time shouldBe "14:05"
        item.ui.amountKind shouldBe TransactionAmountKind.Expense
        item.ui.amountText shouldBe "-32.51 USD"
        item.ui.secondaryText shouldBe null
        item.ui.categoryColor shouldBe Color(food.color.value)
        item.iconAsset shouldBe null
    }

    @Test
    fun `transfer builds from-to supporting text and cross-currency secondary text`() {
        val trn = Transaction(
            accountId = cash.id,
            toAccountId = revolut.id,
            type = TransactionType.TRANSFER,
            amount = BigDecimal("40.0"),
            toAmount = BigDecimal("36.5"),
            title = "Top-up",
            dateTime = Instant.EPOCH,
        )

        val item = mapper(accounts = listOf(cash, revolut))
            .mapHistory(listOf(divider(0.0, 0.0), trn))[1] as HomeTrnListItem.Trn

        item.ui.supportingText shouldBe "Cash → Revolut"
        item.ui.time shouldBe "14:05"
        item.ui.amountKind shouldBe TransactionAmountKind.Transfer
        item.ui.amountText shouldBe "40.0 USD"
        item.ui.secondaryText shouldBe "36.5 EUR"
    }

    @Test
    fun `due rows carry the due date text and flag only overdue ones`() {
        val due = expenseTrn("a").copy(dateTime = null, dueDate = Instant.EPOCH)

        val rows = mapper().mapDueRows(listOf(HomeDueRow(due, overdue = true), HomeDueRow(due, overdue = false)))

        rows.map { it.ui.dueText } shouldBe listOf("Due on Fri, Jul 24", "Due on Fri, Jul 24")
        rows.map { it.ui.dueOverdue } shouldBe listOf(true, false)
        rows.forEach {
            it.ui.amountKind shouldBe TransactionAmountKind.Expense
            it.ui.secondaryText shouldBe null
            it.ui.time shouldBe null
        }
    }

    @Test
    fun `due rows for income carry a plus sign and the income kind`() {
        val due = incomeTrn("a").copy(dateTime = null, dueDate = Instant.EPOCH)

        val row = mapper().mapDueRows(listOf(HomeDueRow(due, overdue = false))).first()

        row.ui.amountText shouldStartWith "+"
        row.ui.amountKind shouldBe TransactionAmountKind.Income
    }

    @Test
    fun `unknown account renders deleted text`() {
        val trn = expenseTrn("a").copy(accountId = UUID.randomUUID())

        val item = mapper().mapHistory(listOf(divider(0.0, 10.0), trn))[1] as HomeTrnListItem.Trn

        item.ui.supportingText shouldBe "Food · deleted"
    }

    companion object {
        private val cash = Account(name = "Cash", color = 0xFF00FF00.toInt())
        private val revolut = Account(name = "Revolut", currency = "EUR", color = 0xFF0000FF.toInt())
        private val food = Category(
            name = NotBlankTrimmedString.unsafe("Food"),
            color = ColorInt(0xFFFF9235.toInt()),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

        private fun divider(income: Double, expenses: Double): TransactionHistoryDateDivider =
            TransactionHistoryDateDivider(
                date = LocalDate.of(2026, 7, 19),
                income = income,
                expenses = expenses,
            )

        private fun expenseTrn(seed: String): Transaction = Transaction(
            accountId = cash.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("32.51"),
            title = "Trn $seed",
            categoryId = food.id.value,
            dateTime = Instant.EPOCH,
        )

        private fun incomeTrn(seed: String): Transaction = Transaction(
            accountId = cash.id,
            type = TransactionType.INCOME,
            amount = BigDecimal("32.51"),
            title = "Trn $seed",
            categoryId = food.id.value,
            dateTime = Instant.EPOCH,
        )
    }
}
