package com.ivy.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.sheet.ChoosePeriodModal
import com.ivy.legacy.ui.sheet.ChoosePeriodModalData
import com.ivy.navigation.BalanceScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.legacy.ui.component.PeriodSelector

@Composable
fun BalanceScreen(screen: BalanceScreen) {
    val viewModel: BalanceViewModel = viewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun UI(
    state: BalanceState,
    modifier: Modifier = Modifier,
    onEvent: (BalanceEvent) -> Unit = {}
) {
    val nav = navigation()
    var choosePeriodModal: ChoosePeriodModalData? by remember { mutableStateOf(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.balance_title)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSelector(
                period = state.period,
                onPreviousMonth = { onEvent(BalanceEvent.OnPreviousMonth) },
                onNextMonth = { onEvent(BalanceEvent.OnNextMonth) },
                onShowChoosePeriodModal = {
                    choosePeriodModal = ChoosePeriodModalData(period = state.period)
                }
            )

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                BalanceRow(
                    label = stringResource(R.string.current_balance),
                    amount = state.currentBalance,
                    currency = state.baseCurrencyCode,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.testTag("current_balance"),
                )
                BalanceRow(
                    label = stringResource(R.string.planned_payments_inline),
                    amount = state.plannedPaymentsAmount,
                    currency = state.baseCurrencyCode,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    showPlusSign = true,
                    modifier = Modifier.testTag("planned_payments"),
                )
                HorizontalDivider()
                BalanceRow(
                    label = stringResource(R.string.balance_after_payments),
                    amount = state.balanceAfterPlannedPayments,
                    currency = state.baseCurrencyCode,
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    modifier = Modifier.testTag("balance_after_payments"),
                )
            }
        }
    }

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = { choosePeriodModal = null }
    ) {
        onEvent(BalanceEvent.OnSetPeriod(it))
    }
}

@Composable
private fun BalanceRow(
    label: String,
    amount: Double,
    currency: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    showPlusSign: Boolean = false,
) {
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        overlineContent = { Text(label) },
    ) {
        AmountText(
            amount = amount,
            currency = currency,
            style = style,
            showPlusSign = showPlusSign,
            textAlign = TextAlign.Start,
        )
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT, huge: Boolean = false) {
    WalletPreview(theme) {
        val state = BalanceState(
            period = TimePeriod(month = Month.monthsList().first()),
            baseCurrencyCode = "BGN",
            currentBalance = if (huge) 123456789012.34 else 9326.55,
            balanceAfterPlannedPayments = if (huge) 222222221122.21 else 8426.0,
            plannedPaymentsAmount = if (huge) 98765432109.87 else -900.55,
        )
        NavigationRoot(navigation = Navigation()) {
            UI(state = state)
        }
    }
}

/** For screenshot testing */
@Composable
fun BalanceScreenUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun BalanceHugeAmountUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, huge = true)
}
