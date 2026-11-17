package com.ivy.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.accounts.AccountsTab
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.home.HomeTab
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.sheet.AccountModal
import com.ivy.legacy.ui.sheet.AccountModalData
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData

private const val ScrimAlpha = 0.32f
private const val ToggleIconSwapProgress = 0.5f

@Composable
fun MainScreen(screen: MainScreen) {
    val viewModel: MainViewModel = viewModel()

    val currency by viewModel.currency.observeAsState("")

    onScreenStart {
        viewModel.start(screen)
    }

    val ivyContext = ivyWalletCtx()
    UI(
        tab = ivyContext.mainTab,
        baseCurrency = currency,
        selectTab = viewModel::selectTab,
        onCreateAccount = viewModel::createAccount
    )
}

@Composable
private fun UI(
    tab: MainTab,
    baseCurrency: String,
    selectTab: (MainTab) -> Unit,
    onCreateAccount: (CreateAccountData) -> Unit,
) {
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

    val nav = navigation()
    MainShell(
        tab = tab,
        selectTab = selectTab,
        onAddIncome = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.INCOME
                )
            )
        },
        onAddExpense = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.EXPENSE
                )
            )
        },
        onAddTransfer = {
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = TransactionType.TRANSFER
                )
            )
        },
        onAddPlannedPayment = {
            nav.navigateTo(
                EditPlannedScreen(
                    type = TransactionType.EXPENSE,
                    plannedPaymentRuleId = null
                )
            )
        },
        onAddAccount = {
            accountModalData = AccountModalData(
                account = null,
                balance = 0.0,
                baseCurrency = baseCurrency
            )
        }
    ) {
        when (tab) {
            MainTab.HOME -> HomeTab()
            MainTab.ACCOUNTS -> AccountsTab()
        }
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = { _, _ -> },
        dismiss = {
            accountModalData = null
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainShell(
    tab: MainTab,
    selectTab: (MainTab) -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(tab) { mutableStateOf(initiallyExpanded && tab == MainTab.HOME) }

    BackHandler(enabled = expanded) {
        expanded = false
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            ShortNavigationBar {
                ShortNavigationBarItem(
                    selected = tab == MainTab.HOME,
                    onClick = { selectTab(MainTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.home)) }
                )
                ShortNavigationBarItem(
                    selected = tab == MainTab.ACCOUNTS,
                    onClick = { selectTab(MainTab.ACCOUNTS) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text(stringResource(R.string.accounts)) }
                )
            }
        },
        floatingActionButton = {
            when (tab) {
                MainTab.HOME -> FloatingActionButtonMenu(
                    expanded = expanded,
                    button = {
                        ToggleFloatingActionButton(
                            checked = expanded,
                            onCheckedChange = { expanded = it }
                        ) {
                            val icon by remember {
                                derivedStateOf {
                                    if (checkedProgress > ToggleIconSwapProgress) {
                                        Icons.Default.Close
                                    } else {
                                        Icons.Default.Add
                                    }
                                }
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(
                                    if (expanded) R.string.close else R.string.add
                                )
                            )
                        }
                    }
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = {
                            expanded = false
                            onAddPlannedPayment()
                        },
                        text = { Text(stringResource(R.string.add_planned_payment)) },
                        icon = { Icon(Icons.Default.EventRepeat, contentDescription = null) }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            expanded = false
                            onAddTransfer()
                        },
                        text = { Text(stringResource(R.string.transfer)) },
                        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            expanded = false
                            onAddExpense()
                        },
                        text = { Text(stringResource(R.string.add_expense)) },
                        icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            expanded = false
                            onAddIncome()
                        },
                        text = { Text(stringResource(R.string.add_income)) },
                        icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                    )
                }

                MainTab.ACCOUNTS -> FloatingActionButton(onClick = onAddAccount) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_account))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            content()

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClickLabel = stringResource(R.string.close)
                        ) { expanded = false }
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMainScreen(
    theme: Theme = Theme.LIGHT,
    expanded: Boolean = false
) {
    WalletPreview(theme) {
        MainShell(
            tab = MainTab.HOME,
            selectTab = {},
            onAddIncome = {},
            onAddExpense = {},
            onAddTransfer = {},
            onAddPlannedPayment = {},
            onAddAccount = {},
            initiallyExpanded = expanded
        ) {}
    }
}

/** For screenshot testing */
@Composable
fun MainUiTest(isDark: Boolean, expanded: Boolean = false) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewMainScreen(theme, expanded)
}
