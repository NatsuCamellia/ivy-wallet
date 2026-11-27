package com.ivy.legacy.ui.component.transaction

import androidx.compose.ui.graphics.Color
import com.ivy.base.legacy.LegacyTag
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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class TransactionItemMapperTest {

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
            every { any<Instant>().toLocalDateTime() } returns LocalDateTime.of(2026, 3, 15, 14, 5)
        }
    }

    private fun mapper(
        accounts: List<Account> = listOf(cash),
        categories: List<Category> = listOf(food),
    ): TransactionItemMapper = TransactionItemMapper(
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
        billPaidFormat = "Bill for %1\$s %2\$s paid",
        formatAmount = { amount, currency -> "$amount $currency" },
    )

    private fun mapperWithDefaultFormatAmount(): TransactionItemMapper = TransactionItemMapper(
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
        billPaidFormat = "Bill for %1\$s %2\$s paid",
    )

    @Test
    fun `default formatAmount appends the currency code`() {
        // Given
        val mapper = mapperWithDefaultFormatAmount()

        // When
        val item = mapper.map(expenseTrn("a"))

        // Then
        item.ui.amountText shouldStartWith "-"
        item.ui.amountText shouldEndWith " USD"
    }

    @Test
    fun `untitled expense falls back to category name and keeps only account in supporting text`() {
        val trn = expenseTrn("a").copy(title = null)

        val item = mapper().map(trn)

        item.ui.title shouldBe "Food"
        item.ui.supportingText shouldBe "Cash"
        item.ui.time shouldBe "14:05"
    }

    @Test
    fun `titled expense shows category and account in supporting text`() {
        val item = mapper().map(expenseTrn("a"))

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

        val item = mapper(accounts = listOf(cash, revolut)).map(trn)

        item.ui.supportingText shouldBe "Cash → Revolut"
        item.ui.time shouldBe "14:05"
        item.ui.amountKind shouldBe TransactionAmountKind.Transfer
        item.ui.amountText shouldBe "40.0 USD"
        item.ui.secondaryText shouldBe "36.5 EUR"
    }

    @Test
    fun `due rows carry the due date text and flag only overdue ones`() {
        val due = expenseTrn("a").copy(dateTime = null, dueDate = Instant.EPOCH)

        val rows = listOf(TransactionDue.Overdue, TransactionDue.Upcoming).map { mapper().map(due, it) }

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

        val row = mapper().map(due, TransactionDue.Upcoming)

        row.ui.amountText shouldStartWith "+"
        row.ui.amountKind shouldBe TransactionAmountKind.Income
    }

    @Test
    fun `unknown account renders deleted text`() {
        val trn = expenseTrn("a").copy(accountId = UUID.randomUUID())

        val item = mapper().map(trn)

        item.ui.supportingText shouldBe "Food · deleted"
    }

    @Test
    fun `a description is shown as its own text`() {
        val trn = expenseTrn("a").copy(description = "Weekly shop at Lidl")

        val item = mapper().map(trn)

        item.ui.descriptionText shouldBe "Weekly shop at Lidl"
    }

    @Test
    fun `a missing or blank description shows nothing`() {
        listOf(null, "", "   ").forEach { description ->
            val item = mapper().map(expenseTrn("a").copy(description = description))

            item.ui.descriptionText shouldBe null
        }
    }

    @Test
    fun `a description is shown on an untitled row too and does not replace the title`() {
        val trn = expenseTrn("a").copy(title = null, description = "Weekly shop")

        val item = mapper().map(trn)

        item.ui.title shouldBe "Food"
        item.ui.descriptionText shouldBe "Weekly shop"
    }

    @Test
    fun `a paid recurring bill without a description gets the bill paid text`() {
        val trn = expenseTrn("a").copy(
            description = null,
            recurringRuleId = UUID.randomUUID(),
            dueDate = null,
            paidFor = Instant.EPOCH,
        )

        val item = mapper().map(trn)

        item.ui.descriptionText shouldBe "Bill for March 2026 paid"
    }

    @Test
    fun `the bill paid text never replaces a real description or shows on a still due payment`() {
        val paid = expenseTrn("a").copy(recurringRuleId = UUID.randomUUID(), paidFor = Instant.EPOCH)

        mapper().map(paid.copy(description = "Rent")).ui.descriptionText shouldBe "Rent"
        mapper().map(paid.copy(description = null, dueDate = Instant.EPOCH), TransactionDue.Upcoming)
            .ui.descriptionText shouldBe null
    }

    @Test
    fun `tags are shown as hash tags sorted by name and joined by a space`() {
        val trn = expenseTrn("a").copy(
            tags = persistentListOf(tag("work"), tag("Groceries"), tag("fun")),
        )

        val item = mapper().map(trn)

        item.ui.tagsText shouldBe "#fun #Groceries #work"
    }

    @Test
    fun `no tags shows nothing`() {
        val item = mapper().map(expenseTrn("a"))

        item.ui.tagsText shouldBe null
    }

    @Test
    fun `description and tags show on a due row too`() {
        val due = expenseTrn("a").copy(
            dateTime = null,
            dueDate = Instant.EPOCH,
            description = "Monthly",
            tags = persistentListOf(tag("bills")),
        )

        val row = mapper().map(due, TransactionDue.Overdue)

        row.ui.descriptionText shouldBe "Monthly"
        row.ui.tagsText shouldBe "#bills"
    }

    companion object {
        private fun tag(name: String) = LegacyTag(UUID.randomUUID(), name)

        private val cash = Account(name = "Cash", color = 0xFF00FF00.toInt())
        private val revolut = Account(name = "Revolut", currency = "EUR", color = 0xFF0000FF.toInt())
        private val food = Category(
            name = NotBlankTrimmedString.unsafe("Food"),
            color = ColorInt(0xFFFF9235.toInt()),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
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
