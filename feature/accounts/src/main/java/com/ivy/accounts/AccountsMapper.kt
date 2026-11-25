package com.ivy.accounts

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Account
import com.ivy.legacy.data.model.AccountData

@Immutable
data class AccountRow(
    val account: Account,
    val balance: Double,
    /** The balance in the base currency; set only for a foreign currency account that has an exchange rate. */
    val baseCurrencyEquivalent: Double?,
    val monthlyCashflow: Double,
    val hasActivityThisMonth: Boolean,
)

/** The Balance banner: balances in the base currency and this month's sums over the counted accounts. */
@Immutable
data class AccountsSummary(
    val totalBalance: Double,
    val totalBalanceWithExcluded: Double,
    val monthlyIncome: Double,
    /** A positive sum of expenses. */
    val monthlyExpense: Double,
)

@Immutable
data class AccountsContent(
    val counted: List<AccountRow>,
    val notCounted: List<AccountRow>,
    val summary: AccountsSummary,
)

fun mapAccounts(
    accounts: List<AccountData>,
    baseCurrency: String,
    totalBalance: Double,
    totalBalanceWithExcluded: Double,
): AccountsContent {
    val (counted, notCounted) = accounts.partition { it.account.includeInBalance }
    return AccountsContent(
        counted = counted.map { toRow(it, baseCurrency) },
        notCounted = notCounted.map { toRow(it, baseCurrency) },
        summary = AccountsSummary(
            totalBalance = totalBalance,
            totalBalanceWithExcluded = totalBalanceWithExcluded,
            monthlyIncome = counted.sumOf {
                it.inBaseCurrency(it.monthlyIncome, it.monthlyIncomeBaseCurrency, baseCurrency)
            },
            monthlyExpense = counted.sumOf {
                it.inBaseCurrency(it.monthlyExpenses, it.monthlyExpensesBaseCurrency, baseCurrency)
            },
        ),
    )
}

private fun toRow(data: AccountData, baseCurrency: String) = AccountRow(
    account = data.account,
    balance = data.balance,
    baseCurrencyEquivalent = data.balanceBaseCurrency.takeIf { data.account.asset.code != baseCurrency },
    monthlyCashflow = data.monthlyIncome - data.monthlyExpenses,
    hasActivityThisMonth = data.monthlyIncome != 0.0 || data.monthlyExpenses != 0.0,
)

/** [own] for an account in the base currency, else [converted] (zero without an exchange rate). */
private fun AccountData.inBaseCurrency(own: Double, converted: Double?, baseCurrency: String): Double =
    if (account.asset.code == baseCurrency) own else converted ?: 0.0
