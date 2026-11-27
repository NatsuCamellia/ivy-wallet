package com.ivy.accounts

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Account
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.ui.component.amountLabel
import com.ivy.ui.component.HiddenBalance
import kotlin.math.absoluteValue

@Immutable
data class AccountRow(
    val account: Account,
    val balance: Double,
    /** The balance in the base currency; set only for a foreign currency account that has an exchange rate. */
    val baseCurrencyEquivalent: Double?,
    val monthlyCashflow: Double,
    val hasActivityThisMonth: Boolean,
    /** The balance as shown, or [HiddenBalance] when the amounts are hidden. */
    val balanceText: String,
    /** The base currency equivalent as shown (or [HiddenBalance]); the currency code without an equivalent. */
    val supportingText: String,
    /** This month's Cashflow as shown (or [HiddenBalance]); null for an account with no activity. */
    val cashflowText: String?,
)

/** The Balance banner: balances in the base currency and this month's sums over the counted accounts. */
@Immutable
data class AccountsSummary(
    val totalBalance: Double,
    val totalBalanceWithExcluded: Double,
    val monthlyIncome: Double,
    /** A positive sum of expenses. */
    val monthlyExpense: Double,
    /** The banner amounts as shown, each [HiddenBalance] when the amounts are hidden. */
    val totalBalanceText: String,
    val totalBalanceWithExcludedText: String,
    val monthlyIncomeText: String,
    val monthlyExpenseText: String,
)

@Immutable
data class AccountsContent(
    val counted: List<AccountRow>,
    val notCounted: List<AccountRow>,
    val summary: AccountsSummary,
)

/** With [hideAmounts] every money figure of the banner and the rows reads [HiddenBalance]. */
fun mapAccounts(
    accounts: List<AccountData>,
    baseCurrency: String,
    totalBalance: Double,
    totalBalanceWithExcluded: Double,
    hideAmounts: Boolean = false,
): AccountsContent {
    val (counted, notCounted) = accounts.partition { it.account.includeInBalance }
    val monthlyIncome = counted.sumOf {
        it.inBaseCurrency(it.monthlyIncome, it.monthlyIncomeBaseCurrency, baseCurrency)
    }
    val monthlyExpense = counted.sumOf {
        it.inBaseCurrency(it.monthlyExpenses, it.monthlyExpensesBaseCurrency, baseCurrency)
    }
    fun shown(amount: String): String = if (hideAmounts) HiddenBalance else amount
    return AccountsContent(
        counted = counted.map { toRow(it, baseCurrency, hideAmounts) },
        notCounted = notCounted.map { toRow(it, baseCurrency, hideAmounts) },
        summary = AccountsSummary(
            totalBalance = totalBalance,
            totalBalanceWithExcluded = totalBalanceWithExcluded,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            totalBalanceText = shown(amountLabel(totalBalance, baseCurrency)),
            totalBalanceWithExcludedText = shown(amountLabel(totalBalanceWithExcluded, baseCurrency)),
            monthlyIncomeText = shown(
                amountLabel(monthlyIncome, baseCurrency, showPlusSign = true, shortenBigNumbers = true),
            ),
            monthlyExpenseText = shown(
                amountLabel(0.0 - monthlyExpense.absoluteValue, baseCurrency, shortenBigNumbers = true),
            ),
        ),
    )
}

private fun toRow(data: AccountData, baseCurrency: String, hideAmounts: Boolean): AccountRow {
    val currency = data.account.asset.code
    val baseCurrencyEquivalent = data.balanceBaseCurrency.takeIf { currency != baseCurrency }
    val monthlyCashflow = data.monthlyIncome - data.monthlyExpenses
    val hasActivityThisMonth = data.monthlyIncome != 0.0 || data.monthlyExpenses != 0.0
    fun shown(amount: String): String = if (hideAmounts) HiddenBalance else amount
    return AccountRow(
        account = data.account,
        balance = data.balance,
        baseCurrencyEquivalent = baseCurrencyEquivalent,
        monthlyCashflow = monthlyCashflow,
        hasActivityThisMonth = hasActivityThisMonth,
        balanceText = shown(amountLabel(data.balance, currency)),
        supportingText = baseCurrencyEquivalent
            ?.let { shown(amountLabel(it, baseCurrency)) }
            ?: currency,
        cashflowText = if (hasActivityThisMonth) {
            shown(amountLabel(monthlyCashflow, currency, showPlusSign = true))
        } else {
            null
        },
    )
}

/** [own] for an account in the base currency, else [converted] (zero without an exchange rate). */
private fun AccountData.inBaseCurrency(own: Double, converted: Double?, baseCurrency: String): Double =
    if (account.asset.code == baseCurrency) own else converted ?: 0.0
