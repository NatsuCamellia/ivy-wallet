package com.ivy.accounts

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.amountLabel
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.sheet.ReorderModalSingleType
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.GreenLight
import com.ivy.ui.component.BalanceBanner
import com.ivy.ui.component.BalanceBannerColumn
import com.ivy.ui.component.FlatListItem
import com.ivy.ui.component.StickyListHeader
import com.ivy.ui.component.screenMargin
import com.ivy.ui.component.transaction.TransactionAmountKind
import com.ivy.ui.component.transaction.amountColor
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.withFabClearance
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID
import kotlin.math.absoluteValue

@Composable
fun AccountsTab() {
    val viewModel: AccountsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    AccountsUi(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun AccountsUi(
    state: AccountsState,
    modifier: Modifier = Modifier,
    onEvent: (AccountsEvent) -> Unit = {}
) {
    val ivyContext = ivyWalletCtx()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var listState = rememberLazyListState()
    if (state.accountsData.isNotEmpty()) {
        listState = rememberScrollPositionListState(
            key = "accounts_lazy_column",
            initialFirstVisibleItemIndex = ivyContext.accountsListState?.firstVisibleItemIndex ?: 0,
            initialFirstVisibleItemScrollOffset = ivyContext.accountsListState?.firstVisibleItemScrollOffset
                ?: 0
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.accounts)) },
                actions = {
                    IconButton(
                        onClick = { onEvent(AccountsEvent.OnReorderModalVisible(reorderVisible = true)) },
                        modifier = Modifier.testTag("reorder_button")
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.reorder))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        AccountsList(
            state = state,
            listState = listState,
            contentPadding = innerPadding.withFabClearance(),
        )
    }

    ReorderModalSingleType(
        visible = state.reorderVisible,
        initialItems = state.accountsData,
        dismiss = {
            onEvent(AccountsEvent.OnReorderModalVisible(reorderVisible = false))
        },
        onReordered = {
            onEvent(AccountsEvent.OnReorder(reorderedList = it))
        }
    ) { _, item ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountBadge(item.account)
            Text(text = item.account.name.value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AccountsList(
    state: AccountsState,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val content = state.content
    val onAccountClick = { row: AccountRow ->
        nav.navigateTo(TransactionsScreen(accountId = row.account.id.value, categoryId = null))
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        if (state.accountsData.isEmpty()) {
            item {
                EmptyState(
                    emptyStateTitle = stringResource(R.string.no_accounts),
                    emptyStateText = stringResource(R.string.no_accounts_description),
                    modifier = Modifier.screenMargin(),
                    icon = Icons.Outlined.AccountBalanceWallet,
                )
            }
        } else if (!state.hideTotalBalance) {
            item {
                AccountsBanner(
                    summary = content.summary,
                    currency = state.baseCurrency,
                    modifier = Modifier.screenMargin().padding(bottom = 8.dp),
                )
            }
        }
        accountsSection(
            title = R.string.accounts_counted_in_total,
            rows = content.counted,
            baseCurrency = state.baseCurrency,
            onClick = onAccountClick,
        )
        accountsSection(
            title = R.string.accounts_not_counted_in_total,
            rows = content.notCounted,
            baseCurrency = state.baseCurrency,
            onClick = onAccountClick,
        )
    }
}

/** A sticky header with the account count and its rows; nothing for an empty section. */
private fun LazyListScope.accountsSection(
    @StringRes title: Int,
    rows: List<AccountRow>,
    baseCurrency: String,
    onClick: (AccountRow) -> Unit,
) {
    if (rows.isEmpty()) return
    stickyHeader(key = "header_$title", contentType = "accounts_header") {
        StickyListHeader(title = stringResource(title), subtotal = rows.size.toString())
    }
    items(rows, key = { it.account.id.value }, contentType = { "account_row" }) { row ->
        AccountItem(row = row, baseCurrency = baseCurrency, onClick = { onClick(row) })
    }
}

/** The Balance banner: Total balance, the total with Excluded accounts and this month's Income and Expense. */
@Composable
private fun AccountsBanner(summary: AccountsSummary, currency: String, modifier: Modifier = Modifier) {
    BalanceBanner(
        label = stringResource(R.string.total_balance),
        value = amountLabel(summary.totalBalance, currency),
        columns = persistentListOf(
            BalanceBannerColumn(
                label = stringResource(R.string.month_income),
                value = amountLabel(summary.monthlyIncome, currency, showPlusSign = true, shortenBigNumbers = true),
                onClick = null,
                testTag = "accounts_income",
            ),
            BalanceBannerColumn(
                label = stringResource(R.string.month_expenses),
                value = amountLabel(0.0 - summary.monthlyExpense.absoluteValue, currency, shortenBigNumbers = true),
                onClick = null,
                testTag = "accounts_expense",
            ),
        ),
        modifier = modifier,
        supportingText = stringResource(
            R.string.total_including_excluded,
            amountLabel(summary.totalBalanceWithExcluded, currency),
        ),
        valueTestTag = "accounts_balance",
    )
}

@Composable
private fun AccountItem(
    row: AccountRow,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = row.account
    val currency = account.asset.code
    FlatListItem(
        title = account.name.value,
        onClick = onClick,
        modifier = modifier.testTag("account_item"),
        supportingText = row.baseCurrencyEquivalent
            ?.let { amountLabel(it, baseCurrency) }
            ?: currency,
        primaryText = amountLabel(row.balance, currency),
        secondaryText = if (row.hasActivityThisMonth) {
            amountLabel(row.monthlyCashflow, currency, showPlusSign = true)
        } else {
            stringResource(R.string.account_no_activity_this_month)
        },
        secondaryColor = if (row.hasActivityThisMonth) {
            cashflowKind(row.monthlyCashflow).amountColor()
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        leading = { AccountBadge(account) },
    )
}

private fun cashflowKind(cashflow: Double): TransactionAmountKind =
    if (cashflow > 0) TransactionAmountKind.Income else TransactionAmountKind.Expense

@Composable
private fun AccountBadge(account: Account) {
    IconBadge(
        color = Color(account.color.value),
        iconName = account.icon?.id,
        defaultIcon = R.drawable.ic_custom_account_m,
    )
}

@Preview
@Composable
private fun PreviewAccountsTab(
    theme: Theme = Theme.LIGHT,
    empty: Boolean = false,
    hugeAmountOnly: Boolean = false,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            AccountsUi(state = previewState(empty = empty, hugeAmountOnly = hugeAmountOnly))
        }
    }
}

@Suppress("MagicNumber", "LongMethod")
private fun previewState(empty: Boolean, hugeAmountOnly: Boolean): AccountsState {
    fun account(name: String, color: Color, currency: String = "BGN", icon: String? = null, included: Boolean = true) =
        Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(color.toArgb()),
            asset = AssetCode.unsafe(currency),
            icon = icon?.let { IconAsset.unsafe(it) },
            includeInBalance = included,
            orderNum = 0.0,
        )

    val hugeAmount = AccountData(
        account("Long-term savings and investments for the whole family", Color(0xFF6200EE)),
        balance = 123456789012.34,
        balanceBaseCurrency = null,
        monthlyExpenses = 98765432.1,
        monthlyIncome = 123456789.01,
        monthlyExpensesBaseCurrency = null,
        monthlyIncomeBaseCurrency = null,
    )
    val accounts = if (empty) {
        persistentListOf()
    } else if (hugeAmountOnly) {
        persistentListOf(hugeAmount)
    } else {
        persistentListOf(
            AccountData(account("Phyre", Green), 2125.0, null, 920.0, 3045.0, null, null),
            AccountData(account("DSK", GreenLight, included = false), 12125.21, null, 1350.50, 8000.48, null, null),
            AccountData(
                account("Revolut", Color(0xFFFFF59D), "USD", "revolut"),
                balance = 1200.0,
                balanceBaseCurrency = 1979.64,
                monthlyExpenses = 750.0,
                monthlyIncome = 1000.30,
                monthlyExpensesBaseCurrency = 1237.5,
                monthlyIncomeBaseCurrency = 1650.5,
            ),
            AccountData(account("Cash", Color(0xFF212121), icon = "cash"), 820.0, null, 0.0, 0.0, null, null),
            hugeAmount,
        )
    }
    return AccountsState(
        baseCurrency = "BGN",
        accountsData = accounts,
        content = mapAccounts(
            accounts = accounts,
            baseCurrency = "BGN",
            totalBalance = if (empty) 0.0 else 123457000000.0,
            totalBalanceWithExcluded = if (empty) 0.0 else 123470000000.0,
        ),
        reorderVisible = false,
        hideTotalBalance = hugeAmountOnly
    )
}

/** For screen shot testing **/
@Composable
fun AccountsTabUITest(dark: Boolean) {
    PreviewAccountsTab(theme = dark.theme())
}

/** For screen shot testing **/
@Composable
fun AccountsTabEmptyUITest(dark: Boolean) {
    PreviewAccountsTab(theme = dark.theme(), empty = true)
}

/** For screen shot testing **/
@Composable
fun AccountsTabHugeAmountUITest(dark: Boolean) {
    PreviewAccountsTab(theme = dark.theme(), hugeAmountOnly = true)
}

private fun Boolean.theme(): Theme = if (this) Theme.DARK else Theme.LIGHT
