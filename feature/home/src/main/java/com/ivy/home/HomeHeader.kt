package com.ivy.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.color
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import kotlin.math.absoluteValue

@Composable
internal fun HomeTopBar(
    name: String,
    period: TimePeriod,
    onShowPeriodModal: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    LargeFlexibleTopAppBar(
        title = {
            Text(
                text = if (name.isNotNullOrBlank()) {
                    stringResource(R.string.hi_name, name)
                } else {
                    stringResource(R.string.hi)
                },
                modifier = Modifier.testTag("home_greeting_text"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        actions = {
            PeriodButton(period = period, onClick = onShowPeriodModal)
            IconButton(
                onClick = onOpenMoreMenu,
                modifier = Modifier.testTag("home_more_menu_button"),
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun PeriodButton(period: TimePeriod, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Text(
            text = period.toDisplayShort(
                startDateOfMonth = ivyWalletCtx().startDayOfMonth,
                timeConverter = LocalTimeConverter.current,
                timeProvider = LocalTimeProvider.current,
                timeFormatter = LocalTimeFormatter.current,
            ),
            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
        )
    }
}

@Composable
fun CashFlowInfo(
    currency: String,
    balance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideBalance: Boolean,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
    onBalanceClick: () -> Unit,
    onHiddenBalanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Balance(
            currency = currency,
            balance = balance,
            cashflow = monthlyIncome - monthlyExpenses,
            hideBalance = hideBalance,
            onClick = if (hideBalance) onHiddenBalanceClick else onBalanceClick,
        )

        IncomeExpenses(
            currency = currency,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            hideIncome = hideIncome,
            onHiddenIncomeClick = onHiddenIncomeClick,
        )
    }
}

@Composable
private fun Balance(
    currency: String,
    balance: Double,
    cashflow: Double,
    hideBalance: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(
                onClickLabel = stringResource(
                    if (hideBalance) R.string.show_balance else R.string.open_balance
                ),
                role = Role.Button,
                onClick = onClick
            )
            .testTag("home_balance")
    ) {
        val style = MaterialTheme.typography.displaySmallEmphasized
        if (hideBalance) {
            Text(text = HiddenBalance, style = style)
        } else {
            AmountText(amount = balance, currency = currency, style = style)
        }

        if (cashflow != 0.0 && !hideBalance) {
            Text(
                text = stringResource(
                    R.string.cashflow,
                    (if (cashflow > 0) "+" else ""),
                    cashflow.format(currency),
                    currency,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cashflow > 0) {
                    TransactionType.INCOME.color()
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun IncomeExpenses(
    currency: String,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(
            type = TransactionType.INCOME,
            label = stringResource(R.string.income),
            currency = currency,
            amount = monthlyIncome,
            onClick = {
                if (hideIncome) {
                    onHiddenIncomeClick()
                } else {
                    nav.navigateTo(PieChartStatisticScreen(type = TransactionType.INCOME))
                }
            },
            modifier = Modifier.weight(1f),
            testTag = "home_card_income",
        )

        SummaryCard(
            type = TransactionType.EXPENSE,
            label = stringResource(R.string.expenses),
            currency = currency,
            amount = 0.0 - monthlyExpenses.absoluteValue,
            onClick = { nav.navigateTo(PieChartStatisticScreen(type = TransactionType.EXPENSE)) },
            modifier = Modifier.weight(1f),
            testTag = "home_card_expense",
        )
    }
}

@Composable
private fun SummaryCard(
    type: TransactionType,
    label: String,
    currency: String,
    amount: Double,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.testTag(testTag)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (type == TransactionType.INCOME) {
                        Icons.Default.ArrowDownward
                    } else {
                        Icons.Default.ArrowUpward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    tint = type.color(),
                )
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }

            AmountText(
                amount = amount,
                currency = currency,
                style = MaterialTheme.typography.titleMediumEmphasized.copy(color = type.color()),
                showPlusSign = type == TransactionType.INCOME,
            )
        }
    }
}

private const val HiddenBalance = "****"
