package com.ivy.accounts

import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.model.AccountData
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class AccountsMapperTest {

    @Test
    fun `splits accounts into counted and not counted sections keeping their order`() {
        val result = map(
            accountData("Cash"),
            accountData("Old bank", included = false),
            accountData("Card"),
            accountData("Savings", included = false),
        )

        result.counted.map { it.account.name.value } shouldBe listOf("Cash", "Card")
        result.notCounted.map { it.account.name.value } shouldBe listOf("Old bank", "Savings")
        result.counted.size shouldBe 2
        result.notCounted.size shouldBe 2
    }

    @Test
    fun `an account's Cashflow is its monthly income minus expenses`() {
        val result = map(accountData("Cash", income = 1200.0, expenses = 450.5))

        val row = result.counted.single()
        row.monthlyCashflow shouldBe 749.5
        row.hasActivityThisMonth shouldBe true
    }

    @Test
    fun `an account with expenses only has a negative Cashflow`() {
        val row = map(accountData("Cash", expenses = 80.0)).counted.single()

        row.monthlyCashflow shouldBe -80.0
        row.hasActivityThisMonth shouldBe true
    }

    @Test
    fun `an account with no income or expenses this month has no activity`() {
        val row = map(accountData("Cash", balance = 500.0)).counted.single()

        row.hasActivityThisMonth shouldBe false
        row.monthlyCashflow shouldBe 0.0
    }

    @Test
    fun `income equal to expenses is activity with a zero Cashflow`() {
        val row = map(accountData("Cash", income = 50.0, expenses = 50.0)).counted.single()

        row.hasActivityThisMonth shouldBe true
        row.monthlyCashflow shouldBe 0.0
    }

    @Test
    fun `a foreign currency account carries its base currency equivalent`() {
        val row = map(
            accountData("Travel", currency = "USD", balance = 1200.0, balanceBaseCurrency = 1100.5),
        ).counted.single()

        row.balance shouldBe 1200.0
        row.baseCurrencyEquivalent shouldBe 1100.5
    }

    @Test
    fun `a base currency account has no equivalent to show`() {
        val row = map(accountData("Cash", balance = 300.0)).counted.single()

        row.baseCurrencyEquivalent shouldBe null
    }

    @Test
    fun `a foreign currency account without an exchange rate has no equivalent to show`() {
        val row = map(accountData("Travel", currency = "USD", balance = 1200.0)).counted.single()

        row.baseCurrencyEquivalent shouldBe null
    }

    @Test
    fun `banner totals are passed through`() {
        val summary = map(totalBalance = 900.0, totalBalanceWithExcluded = 1400.0).summary

        summary.totalBalance shouldBe 900.0
        summary.totalBalanceWithExcluded shouldBe 1400.0
    }

    @Test
    fun `banner income and expense sum counted accounts only`() {
        val summary = map(
            accountData("Cash", income = 1000.0, expenses = 300.0),
            accountData("Card", income = 200.0, expenses = 50.0),
            accountData("Old bank", included = false, income = 9000.0, expenses = 7000.0),
        ).summary

        summary.monthlyIncome shouldBe 1200.0
        summary.monthlyExpense shouldBe 350.0
    }

    @Test
    fun `banner income and expense convert foreign accounts to the base currency`() {
        val summary = map(
            accountData("Cash", income = 100.0, expenses = 40.0),
            accountData(
                "Travel",
                currency = "USD",
                income = 200.0,
                expenses = 100.0,
                incomeBaseCurrency = 180.0,
                expensesBaseCurrency = 90.0,
            ),
        ).summary

        summary.monthlyIncome shouldBe 280.0
        summary.monthlyExpense shouldBe 130.0
    }

    @Test
    fun `a foreign account without an exchange rate adds nothing to the banner income and expense`() {
        val summary = map(
            accountData("Cash", income = 100.0, expenses = 40.0),
            accountData("Travel", currency = "USD", income = 200.0, expenses = 100.0),
        ).summary

        summary.monthlyIncome shouldBe 100.0
        summary.monthlyExpense shouldBe 40.0
    }

    @Test
    fun `student scenario with expenses above income shows a negative Cashflow and positive sums`() {
        val result = map(
            accountData("Wallet", balance = 35.0, income = 400.0, expenses = 950.0),
            totalBalance = 35.0,
            totalBalanceWithExcluded = 35.0,
        )

        result.counted.single().monthlyCashflow shouldBe -550.0
        result.summary.monthlyIncome shouldBe 400.0
        result.summary.monthlyExpense shouldBe 950.0
    }

    @Test
    fun `no accounts gives empty sections and a zero banner`() {
        val result = map()

        result.counted shouldBe emptyList()
        result.notCounted shouldBe emptyList()
        result.summary.monthlyIncome shouldBe 0.0
        result.summary.monthlyExpense shouldBe 0.0
    }

    private fun map(
        vararg accounts: AccountData,
        baseCurrency: String = BASE,
        totalBalance: Double = 0.0,
        totalBalanceWithExcluded: Double = 0.0,
    ) = mapAccounts(
        accounts = accounts.toList(),
        baseCurrency = baseCurrency,
        totalBalance = totalBalance,
        totalBalanceWithExcluded = totalBalanceWithExcluded,
    )

    private fun accountData(
        name: String,
        currency: String = BASE,
        included: Boolean = true,
        balance: Double = 0.0,
        balanceBaseCurrency: Double? = null,
        income: Double = 0.0,
        expenses: Double = 0.0,
        incomeBaseCurrency: Double? = null,
        expensesBaseCurrency: Double? = null,
    ) = AccountData(
        account = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            asset = AssetCode.unsafe(currency),
            color = ColorInt(0),
            icon = null,
            includeInBalance = included,
            orderNum = 0.0,
        ),
        balance = balance,
        balanceBaseCurrency = balanceBaseCurrency,
        monthlyExpenses = expenses,
        monthlyIncome = income,
        monthlyIncomeBaseCurrency = incomeBaseCurrency,
        monthlyExpensesBaseCurrency = expensesBaseCurrency,
    )

    companion object {
        const val BASE = "EUR"
    }
}
