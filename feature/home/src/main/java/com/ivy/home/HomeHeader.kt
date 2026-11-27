package com.ivy.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.ivy.base.model.TransactionType
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.amountLabel
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.component.BalanceBanner
import com.ivy.ui.component.BalanceBannerColumn
import com.ivy.ui.component.HiddenBalance
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.absoluteValue

@Composable
internal fun HomeTopBar(
    name: String,
    period: TimePeriod,
    onShowPeriodModal: () -> Unit,
    onSearch: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    LargeFlexibleTopAppBar(
        title = {
            Column {
                Text(
                    text = if (name.isNotNullOrBlank()) {
                        stringResource(R.string.hi_name, name)
                    } else {
                        stringResource(R.string.hi)
                    },
                    modifier = Modifier.testTag("home_greeting_text"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PeriodTitle(period = period, onClick = onShowPeriodModal)
            }
        },
        modifier = modifier,
        actions = {
            IconButton(onClick = onSearch, modifier = Modifier.testTag("home_search_button")) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_transactions_action))
            }
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

/** The Period as the page title; tapping it opens the period picker. */
@Composable
private fun PeriodTitle(period: TimePeriod, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("home_period_title"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = period.toDisplayShort(
                startDateOfMonth = ivyWalletCtx().startDayOfMonth,
                timeConverter = LocalTimeConverter.current,
                timeProvider = LocalTimeProvider.current,
                timeFormatter = LocalTimeFormatter.current,
            ),
            style = LocalTextStyle.current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
    }
}

/** The Home Balance banner: Total balance on top, then Income, Expense and Cashflow of the Period. */
@Composable
fun HomeBalanceBanner(
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
    val nav = navigation()
    val cashflow = monthlyIncome - monthlyExpenses
    val columns = persistentListOf(
        BalanceBannerColumn(
            label = stringResource(R.string.income),
            value = amountLabel(monthlyIncome, currency, showPlusSign = true, shortenBigNumbers = true),
            onClick = {
                if (hideIncome) {
                    onHiddenIncomeClick()
                } else {
                    nav.navigateTo(PieChartStatisticScreen(type = TransactionType.INCOME))
                }
            },
            testTag = "home_card_income",
        ),
        BalanceBannerColumn(
            label = stringResource(R.string.expenses),
            value = amountLabel(0.0 - monthlyExpenses.absoluteValue, currency, shortenBigNumbers = true),
            onClick = { nav.navigateTo(PieChartStatisticScreen(type = TransactionType.EXPENSE)) },
            testTag = "home_card_expense",
        ),
        BalanceBannerColumn(
            label = stringResource(R.string.cashflow_label),
            value = if (hideBalance) {
                HiddenBalance
            } else {
                amountLabel(cashflow, currency, showPlusSign = true, shortenBigNumbers = true)
            },
            onClick = null,
            testTag = "home_cashflow",
        ),
    )
    BalanceBanner(
        label = stringResource(R.string.total_balance),
        value = if (hideBalance) HiddenBalance else amountLabel(balance, currency),
        columns = columns,
        modifier = modifier,
        onClick = if (hideBalance) onHiddenBalanceClick else onBalanceClick,
        onClickLabel = stringResource(if (hideBalance) R.string.show_balance else R.string.open_balance),
        valueTestTag = "home_balance",
    )
}
