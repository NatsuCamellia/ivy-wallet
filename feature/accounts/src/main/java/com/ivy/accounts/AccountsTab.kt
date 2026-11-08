package com.ivy.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.color
import com.ivy.legacy.ui.sheet.ReorderModalSingleType
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.GreenLight
import com.ivy.ui.rememberScrollPositionListState
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
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + LocalMinimumInteractiveComponentSize.current * 2
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.accountsData.isEmpty()) {
            item {
                EmptyState()
            }
        } else if (!state.hideTotalBalance) {
            item {
                TotalBalanceCard(state)
            }
        }
        items(state.accountsData, key = { it.account.id.value }) {
            AccountCard(
                baseCurrency = state.baseCurrency,
                accountData = it,
                compactModeEnabled = state.compactAccountsModeEnabled,
                onClick = {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = it.account.id.value,
                            categoryId = null
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun TotalBalanceCard(state: AccountsState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        LabelledAmount(
            label = stringResource(R.string.total_balance),
            amount = state.totalBalanceWithoutExcluded.toDoubleOrNull() ?: 0.0,
            currency = state.baseCurrency,
            amountStyle = MaterialTheme.typography.headlineSmallEmphasized,
        )
        LabelledAmount(
            label = stringResource(R.string.total_balance_excluded),
            amount = state.totalBalanceWithExcluded.toDoubleOrNull() ?: 0.0,
            currency = state.baseCurrency,
            amountStyle = MaterialTheme.typography.titleMediumEmphasized,
        )
    }
}

@Composable
private fun AccountCard(
    baseCurrency: String,
    accountData: AccountData,
    compactModeEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = accountData.account
    val currency = account.asset.code

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { AccountBadge(account) },
            supportingContent = if (!account.includeInBalance) {
                {
                    Text(
                        text = stringResource(R.string.excluded),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            NameAndBalance(accountData, currency, baseCurrency)
        }

        if (!compactModeEnabled) {
            Row {
                LabelledAmount(
                    label = stringResource(R.string.month_income),
                    amount = accountData.monthlyIncome,
                    currency = currency,
                    amountStyle = MaterialTheme.typography.titleSmall,
                    amountColor = TransactionType.INCOME.color(),
                    showPlusSign = true,
                    modifier = Modifier.weight(1f)
                )
                LabelledAmount(
                    label = stringResource(R.string.month_expenses),
                    amount = 0.0 - accountData.monthlyExpenses.absoluteValue,
                    currency = currency,
                    amountStyle = MaterialTheme.typography.titleSmall,
                    amountColor = TransactionType.EXPENSE.color(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** The name and the balance share a line; when the balance is too wide for that it moves to its own line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameAndBalance(accountData: AccountData, currency: String, baseCurrency: String) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = accountData.account.name.value,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            AmountText(
                amount = accountData.balance,
                currency = currency,
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            val baseBalance = accountData.balanceBaseCurrency
            if (currency != baseCurrency && baseBalance != null) {
                AmountText(
                    amount = baseBalance,
                    currency = baseCurrency,
                    modifier = Modifier.testTag("baseCurrencyEquivalent"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                )
            }
        }
    }
}

@Composable
private fun LabelledAmount(
    label: String,
    amount: Double,
    currency: String,
    amountStyle: TextStyle,
    modifier: Modifier = Modifier,
    amountColor: Color = Color.Unspecified,
    showPlusSign: Boolean = false,
) {
    ListItem(
        modifier = modifier,
        overlineContent = { Text(label) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        AmountText(
            amount = amount,
            currency = currency,
            style = amountStyle.copy(color = amountColor),
            showPlusSign = showPlusSign,
        )
    }
}

@Composable
private fun AccountBadge(account: Account) {
    IconBadge(
        color = Color(account.color.value),
        iconName = account.icon?.id,
        defaultIcon = R.drawable.ic_custom_account_m,
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.no_accounts),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.no_accounts_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun PreviewAccountsTab(
    theme: Theme = Theme.LIGHT,
    compact: Boolean = false,
    empty: Boolean = false,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            AccountsUi(state = previewState(compact = compact, empty = empty))
        }
    }
}

@Suppress("MagicNumber", "LongMethod")
private fun previewState(compact: Boolean, empty: Boolean): AccountsState {
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

    val accounts = if (empty) {
        persistentListOf()
    } else {
        persistentListOf(
            AccountData(account("Phyre", Green), 2125.0, null, 920.0, 3045.0),
            AccountData(account("DSK", GreenLight, included = false), 12125.21, null, 1350.50, 8000.48),
            AccountData(account("Revolut", Color(0xFFFFF59D), "USD", "revolut"), 1200.0, 1979.64, 750.0, 1000.30),
            AccountData(account("Cash", Color(0xFF212121), icon = "cash"), 820.0, null, 340.0, 400.0),
            AccountData(
                account("Long-term savings and investments for the whole family", Color(0xFF6200EE)),
                balance = 123456789012.34,
                balanceBaseCurrency = null,
                monthlyExpenses = 98765432.1,
                monthlyIncome = 123456789.01
            ),
        )
    }
    return AccountsState(
        baseCurrency = "BGN",
        accountsData = accounts,
        totalBalanceWithExcluded = if (empty) "0.0" else "123470000000.0",
        totalBalanceWithExcludedText = "",
        totalBalanceWithoutExcluded = if (empty) "0.0" else "123457000000.0",
        totalBalanceWithoutExcludedText = "",
        reorderVisible = false,
        compactAccountsModeEnabled = compact,
        hideTotalBalance = false
    )
}

/** For screen shot testing **/
@Composable
fun AccountsTabNonCompactUITest(dark: Boolean) {
    PreviewAccountsTab(theme = dark.theme(), compact = false)
}

/** For screen shot testing **/
@Composable
fun AccountsTabCompactUITest(dark: Boolean) {
    PreviewAccountsTab(theme = dark.theme(), compact = true)
}

/** For screen shot testing **/
@Composable
fun AccountsTabEmptyUITest(dark: Boolean) {
    PreviewAccountsTab(theme = dark.theme(), empty = true)
}

private fun Boolean.theme(): Theme = if (this) Theme.DARK else Theme.LIGHT
