package com.ivy.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.domain.RootScreen
import com.ivy.legacy.Constants
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.openUrl
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.component.settings.SettingsSectionTitle
import com.ivy.wallet.ui.theme.components.BufferBattery
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.MoreMenu(
    expanded: Boolean,

    balance: Double,
    buffer: Double,
    currency: String,
    theme: Theme,

    setExpanded: (Boolean) -> Unit,
    onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = { setExpanded(true) },
        modifier = modifier
            .statusBarsPadding()
            .align(Alignment.TopEnd)
            .padding(top = 20.dp, end = 24.dp)
            .testTag("home_more_menu_arrow"),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_expandarrow),
            contentDescription = stringResource(R.string.more_options),
        )
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { setExpanded(false) },
        ) {
            MoreMenuContent(
                balance = balance,
                buffer = buffer,
                currency = currency,
                theme = theme,
                onSwitchTheme = onSwitchTheme,
                onBufferClick = onBufferClick,
            )
        }
    }
}

@Composable
private fun MoreMenuContent(
    balance: Double,
    buffer: Double,
    currency: String,
    theme: Theme,
    onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit,
) {
    val nav = navigation()
    // rootScreen() unsafely casts LocalContext.current — capture the context here (always
    // safe) and defer the RootScreen check into the click lambda so this composable can still
    // be Paparazzi-tested without a real RootScreen host.
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .testTag("home_more_menu_content"),
    ) {
        SearchButton {
            nav.navigateTo(SearchScreen)
        }

        Spacer(Modifier.height(24.dp))

        SettingsSectionTitle(text = stringResource(R.string.quick_access))

        QuickAccessGrid(
            theme = theme,
            onSwitchTheme = onSwitchTheme,
            onSettingsClick = { nav.navigateTo(SettingsScreen) },
            onCategoriesClick = { nav.navigateTo(CategoriesScreen) },
            onPlannedPaymentsClick = { nav.navigateTo(PlannedPaymentsScreen) },
            onShareClick = {
                if (context is RootScreen) {
                    context.shareIvyWallet()
                }
            },
            onReportsClick = { nav.navigateTo(ReportScreen) },
            onBudgetsClick = { nav.navigateTo(BudgetScreen) },
            onLoansClick = { nav.navigateTo(LoansScreen) },
        )

        Spacer(Modifier.height(24.dp))

        Buffer(
            buffer = buffer,
            currency = currency,
            balance = balance,
            onBufferClick = onBufferClick,
        )

        Spacer(Modifier.height(16.dp))

        OpenSource()

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SearchButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = stringResource(R.string.search_transactions),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun QuickAccessGrid(
    theme: Theme,
    onSwitchTheme: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onPlannedPaymentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onLoansClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickAccessButton(
                icon = R.drawable.home_more_menu_settings,
                label = stringResource(R.string.settings),
                onClick = onSettingsClick,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_categories,
                label = stringResource(R.string.categories),
                onClick = onCategoriesClick,
            )
            QuickAccessButton(
                icon = when (theme) {
                    Theme.LIGHT -> R.drawable.home_more_menu_light_mode
                    Theme.DARK -> R.drawable.home_more_menu_dark_mode
                    Theme.AMOLED_DARK -> R.drawable.home_more_menu_amoled_dark_mode
                    Theme.AUTO -> R.drawable.home_more_menu_auto_mode
                },
                label = when (theme) {
                    Theme.LIGHT -> stringResource(R.string.light_mode)
                    Theme.DARK -> stringResource(R.string.dark_mode)
                    Theme.AMOLED_DARK -> stringResource(R.string.amoled_mode)
                    Theme.AUTO -> stringResource(R.string.auto_mode)
                },
                onClick = onSwitchTheme,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_planned_payments,
                label = stringResource(R.string.planned_payments),
                onClick = onPlannedPaymentsClick,
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickAccessButton(
                icon = R.drawable.home_more_menu_share,
                label = stringResource(R.string.share_ivy),
                onClick = onShareClick,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_reports,
                label = stringResource(R.string.reports),
                onClick = onReportsClick,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_budgets,
                label = stringResource(R.string.budgets),
                onClick = onBudgetsClick,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_loans,
                label = stringResource(R.string.loans),
                onClick = onLoansClick,
            )
        }
    }
}

@Composable
private fun QuickAccessButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(painter = painterResource(icon), contentDescription = label)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun Buffer(
    buffer: Double,
    currency: String,
    balance: Double,
    onBufferClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoIndication(rememberInteractionSource()) {
                    onBufferClick()
                }
                .padding(horizontal = 24.dp)
                .testTag("savings_goal_row"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.savings_goal),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.weight(1f))

            AmountCurrencyB1(
                amount = buffer,
                currency = currency,
            )
        }

        Spacer(Modifier.height(12.dp))

        BufferBattery(
            modifier = Modifier.padding(horizontal = 16.dp),
            buffer = buffer,
            currency = currency,
            balance = balance,
        ) {
            onBufferClick()
        }
    }
}

@Composable
private fun OpenSource() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                openUrl(
                    uriHandler = uriHandler,
                    url = Constants.URL_IVY_WALLET_REPO
                )
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))

        Icon(
            painter = painterResource(R.drawable.github_logo),
            contentDescription = null,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.ivy_wallet_open_source),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = Constants.URL_IVY_WALLET_REPO,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewCollapsed() {
    IvyWalletPreview {
        MoreMenu(
            expanded = false,
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            setExpanded = {},
            onSwitchTheme = {},
            onBufferClick = {},
            onCurrencyClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewExpanded() {
    IvyWalletPreview {
        MoreMenu(
            expanded = true,
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            setExpanded = {},
            onSwitchTheme = {},
            onBufferClick = {},
            onCurrencyClick = {},
        )
    }
}

/** For screenshot testing */
@Composable
fun MoreMenuContentUiTest(isDark: Boolean) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    IvyWalletPreview(theme) {
        MoreMenuContent(
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            onSwitchTheme = {},
            onBufferClick = {},
        )
    }
}
