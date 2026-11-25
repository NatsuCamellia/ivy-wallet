package com.ivy.categories

import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.wallet.domain.data.SortOrder
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class CategoriesMapperTest {

    @Test
    fun `expense rows carry the amount, the share of the expense total and a bar relative to the largest`() {
        val result = map(
            CategoryType.Expense,
            category("Rent", expenses = 600.0),
            category("Food", expenses = 300.0),
            category("Fun", expenses = 100.0),
        )

        result.active.map { it.data.category.name.value } shouldBe listOf("Rent", "Food", "Fun")
        result.active.map { it.amount } shouldBe listOf(600.0, 300.0, 100.0)
        result.active.map { it.percentage } shouldBe listOf(60.0, 30.0, 10.0)
        result.active.map { it.barFraction } shouldBe listOf(1.0, 0.5, 100.0 / 600.0)
    }

    @Test
    fun `income rows use the income amounts and the income total`() {
        val result = map(
            CategoryType.Income,
            category("Salary", income = 900.0, expenses = 50.0),
            category("Gifts", income = 100.0),
        )

        result.active.map { it.amount } shouldBe listOf(900.0, 100.0)
        result.active.map { it.percentage } shouldBe listOf(90.0, 10.0)
        result.active.map { it.barFraction } shouldBe listOf(1.0, 100.0 / 900.0)
    }

    @Test
    fun `categories with nothing for the chosen type are listed as no records, not as rows`() {
        val expenseView = map(
            CategoryType.Expense,
            category("Food", expenses = 200.0),
            category("Salary", income = 900.0),
            category("Unused"),
        )
        val incomeView = map(
            CategoryType.Income,
            category("Food", expenses = 200.0),
            category("Salary", income = 900.0),
            category("Unused"),
        )

        expenseView.active.map { it.data.category.name.value } shouldBe listOf("Food")
        expenseView.noRecords.map { it.category.name.value } shouldBe listOf("Salary", "Unused")
        incomeView.active.map { it.data.category.name.value } shouldBe listOf("Salary")
        incomeView.noRecords.map { it.category.name.value } shouldBe listOf("Food", "Unused")
    }

    @Test
    fun `both chip totals are present whichever type is chosen`() {
        val categories = arrayOf(
            category("Food", expenses = 200.0),
            category("Salary", income = 900.0, expenses = 25.5),
            category("Unused"),
        )

        for (type in CategoryType.entries) {
            val result = map(type, *categories)

            result.expenseTotal shouldBe 225.5
            result.incomeTotal shouldBe 900.0
        }
    }

    @Test
    fun `a type with no records at all has no rows, every category under no records and a zero total`() {
        val result = map(
            CategoryType.Income,
            category("Food", expenses = 200.0),
            category("Fun", expenses = 40.0),
        )

        result.active shouldBe emptyList()
        result.noRecords.map { it.category.name.value } shouldBe listOf("Food", "Fun")
        result.incomeTotal shouldBe 0.0
        result.expenseTotal shouldBe 240.0
    }

    @Test
    fun `no categories at all gives an empty result`() {
        val result = map(CategoryType.Expense)

        result.active shouldBe emptyList()
        result.noRecords shouldBe emptyList()
        result.expenseTotal shouldBe 0.0
        result.incomeTotal shouldBe 0.0
    }

    @Test
    fun `student scenario with expenses above income keeps the shares of each type meaningful`() {
        val categories = arrayOf(
            category("Rent", expenses = 700.0),
            category("Food", expenses = 300.0),
            category("Allowance", income = 400.0),
            category("Tutoring", income = 100.0),
        )

        val expenses = map(CategoryType.Expense, *categories)
        val income = map(CategoryType.Income, *categories)

        expenses.expenseTotal shouldBe 1000.0
        income.incomeTotal shouldBe 500.0
        expenses.active.map { it.percentage } shouldBe listOf(70.0, 30.0)
        income.active.map { it.percentage } shouldBe listOf(80.0, 20.0)
        expenses.active.sumOf { it.percentage } shouldBe 100.0
        income.active.sumOf { it.percentage } shouldBe 100.0
    }

    @Test
    fun `a single category fills the bar and the whole total`() {
        val row = map(CategoryType.Expense, category("Food", expenses = 12.34)).active.single()

        row.barFraction shouldBe 1.0
        row.percentage shouldBe 100.0
    }

    @Test
    fun `the default sort orders rows by the saved order`() {
        val result = map(
            CategoryType.Expense,
            category("C", expenses = 10.0, orderNum = 2.0),
            category("A", expenses = 30.0, orderNum = 0.0),
            category("B", expenses = 20.0, orderNum = 1.0),
            sortOrder = SortOrder.DEFAULT,
        )

        result.active.map { it.data.category.name.value } shouldBe listOf("A", "B", "C")
    }

    @Test
    fun `the alphabetical sort orders rows by name`() {
        val result = map(
            CategoryType.Expense,
            category("Fun", expenses = 10.0),
            category("Rent", expenses = 30.0),
            category("Food", expenses = 20.0),
            sortOrder = SortOrder.ALPHABETICAL,
        )

        result.active.map { it.data.category.name.value } shouldBe listOf("Food", "Fun", "Rent")
    }

    @Test
    fun `the expenses sort orders rows by expenses, largest first`() {
        val result = map(
            CategoryType.Expense,
            category("Fun", expenses = 10.0),
            category("Rent", expenses = 30.0),
            category("Food", expenses = 20.0),
            sortOrder = SortOrder.EXPENSES,
        )

        result.active.map { it.data.category.name.value } shouldBe listOf("Rent", "Food", "Fun")
    }

    @Test
    fun `the balance sort orders rows by balance, largest first, and puts balances that round to zero last`() {
        val result = map(
            CategoryType.Expense,
            category("Tiny", income = 10.4, expenses = 10.0),
            category("Debt", expenses = 20.0),
            category("Even", income = 30.0, expenses = 30.0),
            category("Saved", income = 80.0, expenses = 30.0),
            sortOrder = SortOrder.BALANCE_AMOUNT,
        )

        result.active.map { it.data.category.name.value } shouldBe listOf("Saved", "Debt", "Tiny", "Even")
    }

    @Test
    fun `the chosen sort also orders the no records rows`() {
        val result = map(
            CategoryType.Expense,
            category("Zoo", income = 1.0, orderNum = 0.0),
            category("Bank", income = 1.0, orderNum = 1.0),
            category("Food", expenses = 5.0, orderNum = 2.0),
            sortOrder = SortOrder.ALPHABETICAL,
        )

        result.noRecords.map { it.category.name.value } shouldBe listOf("Bank", "Zoo")
    }

    private fun map(
        type: CategoryType,
        vararg categories: CategoryData,
        sortOrder: SortOrder = SortOrder.DEFAULT,
    ) = mapCategories(categories = categories.toList(), type = type, sortOrder = sortOrder)

    private fun category(
        name: String,
        income: Double = 0.0,
        expenses: Double = 0.0,
        orderNum: Double = 0.0,
    ) = CategoryData(
        category = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(0),
            icon = null,
            orderNum = orderNum,
        ),
        monthlyBalance = income - expenses,
        monthlyExpenses = expenses,
        monthlyIncome = income,
    )
}
