package com.ivy.onboarding.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.data.model.AccountBalance
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.sheet.AccountModal
import com.ivy.legacy.ui.sheet.AccountModalData
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingProgressSlider
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.onboarding.components.Suggestions
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.Ivy
import com.ivy.ui.color.IvyDark
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData

@Composable
fun OnboardingAccounts(
    baseCurrency: String,
    suggestions: List<CreateAccountData>,
    accounts: List<AccountBalance>,
    modifier: Modifier = Modifier,
    onCreateAccount: (CreateAccountData) -> Unit = { },
    onEditAccount: (Account, Double) -> Unit = { _, _ -> },
    onSkip: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    val nav = navigation()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingToolbar(
                hasSkip = accounts.isEmpty(),
                onBack = { nav.onBackPressed() },
                onSkip = onSkip
            )
        },
        bottomBar = {
            if (accounts.isNotEmpty()) {
                Button(
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.next))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.add_accounts),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            if (accounts.isEmpty()) {
                item {
                    Image(
                        painter = painterResource(id = R.drawable.onboarding_illustration_accounts),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                OnboardingProgressSlider(
                    selectedStep = 2,
                    stepsCount = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(accounts, key = { it.account.id }) { accountBalance ->
                AccountCard(
                    baseCurrency = baseCurrency,
                    accountBalance = accountBalance,
                    onClick = {
                        accountModalData = AccountModalData(
                            account = accountBalance.account,
                            baseCurrency = baseCurrency,
                            balance = accountBalance.balance,
                            autoFocusKeyboard = false
                        )
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.suggestions),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                Suggestions(
                    suggestions = suggestions.filter { suggestion ->
                        accounts.map { it.account.name.toLowerCaseLocal() }
                            .contains(suggestion.name.toLowerCaseLocal()).not()
                    },
                    onAddSuggestion = { onCreateAccount(it as CreateAccountData) },
                    onAddNew = {
                        accountModalData = AccountModalData(
                            account = null,
                            baseCurrency = baseCurrency,
                            balance = 0.0
                        )
                    }
                )
            }
        }
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = onEditAccount,
        dismiss = { accountModalData = null }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountCard(
    baseCurrency: String,
    accountBalance: AccountBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = accountBalance.account
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                IconBadge(
                    color = Color(account.color),
                    iconName = account.icon,
                    defaultIcon = R.drawable.ic_custom_account_m,
                )
            },
            trailingContent = {
                AmountText(
                    amount = accountBalance.balance,
                    currency = account.currency ?: baseCurrency,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(account.name)
        }
    }
}

private val previewSuggestions = listOf(
    CreateAccountData(name = "Cash", currency = "BGN", color = Green, icon = "cash", balance = 0.0),
    CreateAccountData(name = "Bank", currency = "BGN", color = Ivy, icon = "bank", balance = 0.0),
    CreateAccountData(name = "Revolut", currency = "BGN", color = Color(0xFF4DCAFF), icon = "revolut", balance = 0.0),
)

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT, accounts: List<AccountBalance> = emptyList()) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            OnboardingAccounts(
                baseCurrency = "BGN",
                suggestions = previewSuggestions,
                accounts = accounts,
            )
        }
    }
}

private val previewAccounts = listOf(
    AccountBalance(Account(name = "Cash", color = Green.toArgb(), icon = "cash"), balance = 120.5),
    AccountBalance(Account(name = "Savings", color = IvyDark.toArgb(), currency = "EUR"), balance = 1_250_000.0),
)

/** For screenshot testing */
@Composable
fun OnboardingAccountsUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, previewAccounts)
}

/** For screenshot testing */
@Composable
fun OnboardingAccountsEmptyUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
