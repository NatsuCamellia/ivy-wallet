package com.ivy.wallet.domain.action.viewmodel.account

import arrow.core.toOption
import com.ivy.frp.action.FPAction
import com.ivy.frp.action.thenMap
import com.ivy.frp.then
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.account.CalcAccIncomeExpenseAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

class AccountDataAct @Inject constructor(
    private val exchangeAct: ExchangeAct,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val calcAccIncomeExpenseAct: CalcAccIncomeExpenseAct
) : FPAction<AccountDataAct.Input, ImmutableList<com.ivy.legacy.data.model.AccountData>>() {

    override suspend fun Input.compose(): suspend () -> ImmutableList<com.ivy.legacy.data.model.AccountData> = suspend {
        accounts
    } thenMap { acc ->
        val balance = calcAccBalanceAct(
            CalcAccBalanceAct.Input(
                account = acc
            )
        ).balance

        val isForeign = acc.asset.code != baseCurrency
        val toBaseCurrency: suspend (BigDecimal) -> BigDecimal? = { amount ->
            if (isForeign) {
                exchangeAct(
                    ExchangeAct.Input(
                        data = ExchangeData(
                            baseCurrency = baseCurrency,
                            fromCurrency = acc.asset.code.toOption()
                        ),
                        amount = amount
                    )
                ).orNull()
            } else {
                null
            }
        }
        val balanceBaseCurrency = toBaseCurrency(balance)

        val incomeExpensePair = calcAccIncomeExpenseAct(
            CalcAccIncomeExpenseAct.Input(
                account = acc,
                range = range,
                includeTransfersInCalc = includeTransfersInCalc
            )
        ).incomeExpensePair

        com.ivy.legacy.data.model.AccountData(
            account = acc,
            balance = balance.toDouble(),
            balanceBaseCurrency = balanceBaseCurrency?.toDouble(),
            monthlyIncome = incomeExpensePair.income.toDouble(),
            monthlyExpenses = incomeExpensePair.expense.toDouble(),
            monthlyExpensesBaseCurrency = toBaseCurrency(incomeExpensePair.expense)?.toDouble(),
            monthlyIncomeBaseCurrency = toBaseCurrency(incomeExpensePair.income)?.toDouble(),
        )
    } then {
        it.toImmutableList()
    }

    data class Input(
        val accounts: ImmutableList<com.ivy.data.model.Account>,
        val baseCurrency: String,
        val range: ClosedTimeRange,
        val includeTransfersInCalc: Boolean = false
    )
}
