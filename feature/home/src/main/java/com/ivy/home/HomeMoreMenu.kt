package com.ivy.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.Constants
import com.ivy.legacy.rootScreen
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.sheet.ActionSheet
import com.ivy.legacy.ui.sheet.BufferStatus
import com.ivy.legacy.utils.openUrl
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R

@Composable
fun MoreMenu(
    balance: Double,
    buffer: Double,
    currency: String,
    theme: Theme,
    onDismiss: () -> Unit,
    onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val rootScreen = rootScreen()
    val uriHandler = LocalUriHandler.current

    ActionSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        title = stringResource(R.string.more),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuGroup {
                MenuItem(
                    icon = Icons.Default.Search,
                    label = stringResource(R.string.search_transactions),
                    onClick = { nav.navigateTo(SearchScreen) }
                )
            }

            val quickAccess = listOf(
                MenuEntry(Icons.Default.Settings, stringResource(R.string.settings)) {
                    nav.navigateTo(SettingsScreen)
                },
                MenuEntry(Icons.Default.Category, stringResource(R.string.categories)) {
                    nav.navigateTo(CategoriesScreen)
                },
                MenuEntry(theme.icon(), theme.label(), onSwitchTheme),
                MenuEntry(Icons.Default.EventRepeat, stringResource(R.string.planned_payments_inline)) {
                    nav.navigateTo(PlannedPaymentsScreen)
                },
                MenuEntry(Icons.Default.Share, stringResource(R.string.share_ivy)) {
                    rootScreen.shareIvyWallet()
                },
                MenuEntry(Icons.Default.Assessment, stringResource(R.string.reports)) {
                    nav.navigateTo(ReportScreen)
                },
                MenuEntry(Icons.Default.PieChart, stringResource(R.string.budgets)) {
                    nav.navigateTo(BudgetScreen)
                },
                MenuEntry(Icons.Default.AccountBalance, stringResource(R.string.loans)) {
                    nav.navigateTo(LoansScreen)
                },
            )
            MenuGroup(title = stringResource(R.string.quick_access)) {
                quickAccess.forEachIndexed { index, entry ->
                    MenuItem(
                        icon = entry.icon,
                        label = entry.label,
                        index = index,
                        count = quickAccess.size,
                        onClick = entry.onClick
                    )
                }
            }

            MenuGroup {
                SegmentedListItem(
                    onClick = onBufferClick,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    modifier = Modifier.testTag("savings_goal_row"),
                    trailingContent = {
                        AmountText(
                            amount = buffer,
                            currency = currency,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                        )
                    },
                    supportingContent = {
                        BufferStatus(buffer = buffer, balance = balance, currency = currency)
                    },
                ) {
                    Text(stringResource(R.string.savings_goal))
                }
            }

            MenuGroup {
                MenuItem(
                    icon = Icons.Default.Code,
                    label = stringResource(R.string.ivy_wallet_open_source),
                    supporting = Constants.URL_IVY_WALLET_REPO,
                    onClick = { openUrl(uriHandler = uriHandler, url = Constants.URL_IVY_WALLET_REPO) }
                )
            }
        }
    }
}

private class MenuEntry(val icon: ImageVector, val label: String, val onClick: () -> Unit)

@Composable
private fun MenuGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            content()
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    index: Int = 0,
    count: Int = 1,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = modifier,
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        supportingContent = supporting?.let { { Text(it) } },
    ) {
        Text(label)
    }
}

private fun Theme.icon(): ImageVector = when (this) {
    Theme.LIGHT -> Icons.Default.LightMode
    Theme.DARK -> Icons.Default.DarkMode
    Theme.AMOLED_DARK -> Icons.Default.Contrast
    Theme.AUTO -> Icons.Default.BrightnessAuto
}

@Composable
private fun Theme.label(): String = stringResource(
    when (this) {
        Theme.LIGHT -> R.string.light_mode
        Theme.DARK -> R.string.dark_mode
        Theme.AMOLED_DARK -> R.string.amoled_mode
        Theme.AUTO -> R.string.auto_mode
    }
)

@Preview
@Composable
private fun PreviewMoreMenu() {
    WalletPreview {
        MoreMenu(
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            onDismiss = {},
            onSwitchTheme = {},
            onBufferClick = {},
            onCurrencyClick = {}
        )
    }
}
