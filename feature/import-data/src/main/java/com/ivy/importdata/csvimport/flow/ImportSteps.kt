package com.ivy.importdata.csvimport.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.importdata.csvimport.flow.instructions.DefaultImportSteps
import com.ivy.importdata.csvimport.flow.instructions.FinancistoSteps
import com.ivy.importdata.csvimport.flow.instructions.FortuneCitySteps
import com.ivy.importdata.csvimport.flow.instructions.IvyWalletSteps
import com.ivy.importdata.csvimport.flow.instructions.KTWMoneyManagerSteps
import com.ivy.importdata.csvimport.flow.instructions.MonefySteps
import com.ivy.importdata.csvimport.flow.instructions.MoneyManagerPraseSteps
import com.ivy.importdata.csvimport.flow.instructions.OneMoneySteps
import com.ivy.importdata.csvimport.flow.instructions.SpendeeSteps
import com.ivy.importdata.csvimport.flow.instructions.WalletByBudgetBakersSteps
import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType

@Composable
fun ImportType.ImportSteps(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ImportStepsContent()
    }
}

@Composable
private fun ImportType.ImportStepsContent() {
    when (this) {
        ImportType.IVY -> IvyWalletSteps()
        ImportType.MONEY_MANAGER -> MoneyManagerPraseSteps()
        ImportType.WALLET_BY_BUDGET_BAKERS -> WalletByBudgetBakersSteps()
        ImportType.SPENDEE -> SpendeeSteps()
        ImportType.MONEFY -> MonefySteps()
        ImportType.ONE_MONEY -> OneMoneySteps()
        ImportType.BLUE_COINS -> DefaultImportSteps()
        ImportType.KTW_MONEY_MANAGER -> KTWMoneyManagerSteps()
        ImportType.FORTUNE_CITY -> FortuneCitySteps()
        ImportType.FINANCISTO -> FinancistoSteps()
    }
}
