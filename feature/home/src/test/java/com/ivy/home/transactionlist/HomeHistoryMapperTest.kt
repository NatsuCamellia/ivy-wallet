package com.ivy.home.transactionlist

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Test

class HomeHistoryMapperTest {

    private val monday = day(
        date = LocalDate.of(2026, 11, 23),
        income = 100.0,
        expenses = 40.0,
        expenseTrn("coffee"),
        incomeTrn("salary"),
        transferTrn("top-up"),
    )
    private val sunday = day(
        date = LocalDate.of(2026, 11, 22),
        income = 0.0,
        expenses = 25.0,
        expenseTrn("lunch"),
    )
    private val saturday = day(
        date = LocalDate.of(2026, 11, 21),
        income = 15.0,
        expenses = 0.0,
        incomeTrn("refund"),
    )
    private val friday = day(
        date = LocalDate.of(2026, 11, 20),
        income = 0.0,
        expenses = 0.0,
        transferTrn("move"),
    )

    private fun titles(group: HomeDayGroup) = group.transactions.map { it.title }

    @Test
    fun `All keeps every day, every transaction and the original subtotals`() {
        val result = mapHomeHistory(listOf(monday, sunday, saturday, friday), HomeTypeFilter.All)

        result.days shouldBe listOf(monday, sunday, saturday, friday)
        result.isEmpty shouldBe false
    }

    @Test
    fun `Expense keeps only expenses and drops days without any`() {
        val result = mapHomeHistory(listOf(monday, sunday, saturday, friday), HomeTypeFilter.Expense)

        result.days.map { it.date } shouldBe listOf(monday.date, sunday.date)
        result.days.map(::titles) shouldBe listOf(listOf("coffee"), listOf("lunch"))
    }

    @Test
    fun `Income keeps only incomes and drops days without any`() {
        val result = mapHomeHistory(listOf(monday, sunday, saturday, friday), HomeTypeFilter.Income)

        result.days.map { it.date } shouldBe listOf(monday.date, saturday.date)
        result.days.map(::titles) shouldBe listOf(listOf("salary"), listOf("refund"))
    }

    @Test
    fun `Transfers appear only under All`() {
        val days = listOf(monday, friday)

        val transfers = { filter: HomeTypeFilter ->
            mapHomeHistory(days, filter).days.flatMap { it.transactions }
                .count { it.type == TransactionType.TRANSFER }
        }

        transfers(HomeTypeFilter.All) shouldBe 2
        transfers(HomeTypeFilter.Expense) shouldBe 0
        transfers(HomeTypeFilter.Income) shouldBe 0
    }

    @Test
    fun `subtotals are recalculated for the visible rows only`() {
        val expense = mapHomeHistory(listOf(monday), HomeTypeFilter.Expense).days.single()
        val income = mapHomeHistory(listOf(monday), HomeTypeFilter.Income).days.single()

        expense.income shouldBe 0.0
        expense.expenses shouldBe 40.0
        income.income shouldBe 100.0
        income.expenses shouldBe 0.0
    }

    @Test
    fun `a filter that matches nothing leaves no days and flags the empty state`() {
        val result = mapHomeHistory(listOf(sunday, friday), HomeTypeFilter.Income)

        result.days shouldBe emptyList()
        result.isEmpty shouldBe true
    }

    @Test
    fun `no transactions at all is empty under every filter`() {
        HomeTypeFilter.entries.forEach {
            mapHomeHistory(emptyList(), it).isEmpty shouldBe true
        }
    }

    @Test
    fun `student scenario - expenses exceed income gives a negative cashflow under All`() {
        val payday = day(
            date = LocalDate.of(2026, 11, 1),
            income = 300.0,
            expenses = 520.0,
            incomeTrn("allowance"),
            expenseTrn("rent"),
        )

        val all = mapHomeHistory(listOf(payday), HomeTypeFilter.All).days.single()
        val expense = mapHomeHistory(listOf(payday), HomeTypeFilter.Expense).days.single()
        val income = mapHomeHistory(listOf(payday), HomeTypeFilter.Income).days.single()

        all.cashflow shouldBe -220.0
        expense.cashflow shouldBe -520.0
        income.cashflow shouldBe 300.0
    }

    @Test
    fun `groups round trip through the flat history list`() {
        val days = listOf(monday, sunday)

        days.toHistoryItems().toDayGroups() shouldBe days
    }

    companion object {
        private fun day(
            date: LocalDate,
            income: Double,
            expenses: Double,
            vararg trns: Transaction,
        ) = HomeDayGroup(date, income, expenses, trns.toList())

        private fun trn(title: String, type: TransactionType) = Transaction(
            accountId = java.util.UUID.randomUUID(),
            type = type,
            amount = BigDecimal("10.0"),
            title = title,
        )

        private fun expenseTrn(title: String) = trn(title, TransactionType.EXPENSE)
        private fun incomeTrn(title: String) = trn(title, TransactionType.INCOME)
        private fun transferTrn(title: String) = trn(title, TransactionType.TRANSFER)
    }
}
