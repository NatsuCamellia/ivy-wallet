package com.ivy.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Attribution
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.Constants
import com.ivy.legacy.rootScreen
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.icon
import com.ivy.legacy.ui.component.label
import com.ivy.legacy.ui.sheet.ChooseStartDateOfMonthModal
import com.ivy.legacy.ui.sheet.CurrencyModal
import com.ivy.legacy.ui.sheet.DeleteModal
import com.ivy.legacy.ui.sheet.NameModal
import com.ivy.legacy.ui.sheet.ProgressModal
import com.ivy.navigation.AttributionsScreen
import com.ivy.navigation.ContributorsScreen
import com.ivy.navigation.ExchangeRatesScreen
import com.ivy.navigation.FeaturesScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.ReleasesScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.wallet.domain.data.IvyCurrency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UI(
    state: SettingsState,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onEvent: (SettingsEvent) -> Unit,
) {
    var currencyModalVisible by remember { mutableStateOf(false) }
    var nameModalVisible by remember { mutableStateOf(false) }
    var chooseStartDateOfMonthVisible by remember { mutableStateOf(false) }
    var deleteAllDataModalVisible by remember { mutableStateOf(false) }
    var deleteAllDataModalFinalVisible by remember { mutableStateOf(false) }
    val nav = navigation()
    val rootScreen = rootScreen()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("settings_lazy_column"),
            state = listState,
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "account") {
                AccountGroup(
                    currencyCode = state.currencyCode,
                    name = state.name,
                    onCurrencyClick = { currencyModalVisible = true },
                    onNameClick = { nameModalVisible = true },
                )
            }
            item(key = "import_export") {
                ImportExportGroup(onEvent = onEvent)
            }
            item(key = "app_settings") {
                AppSettingsGroup(
                    state = state,
                    onStartDateClick = { chooseStartDateOfMonthVisible = true },
                    onEvent = onEvent,
                )
            }
            item(key = "other") {
                OtherGroup()
            }
            item(key = "product") {
                ProductGroup()
            }
            item(key = "danger_zone") {
                DangerGroup(onDeleteClick = { deleteAllDataModalVisible = true })
            }

            item(key = "version") {
                TextButton(
                    onClick = { nav.navigateTo(ReleasesScreen) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${rootScreen.buildVersionName} (${rootScreen.buildVersionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    CurrencyModal(
        title = stringResource(R.string.set_currency),
        initialCurrency = IvyCurrency.fromCode(state.currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        onEvent(SettingsEvent.SetCurrency(it))
    }

    NameModal(
        visible = nameModalVisible,
        name = state.name,
        dismiss = { nameModalVisible = false }
    ) {
        onEvent(SettingsEvent.SetName(it))
    }

    ChooseStartDateOfMonthModal(
        visible = chooseStartDateOfMonthVisible,
        selectedStartDateOfMonth = state.startDateOfMonth.toInt(),
        dismiss = { chooseStartDateOfMonthVisible = false }
    ) {
        onEvent(SettingsEvent.SetStartDateOfMonth(it))
    }

    DeleteModal(
        title = stringResource(R.string.delete_all_user_data_question),
        description = stringResource(
            R.string.delete_all_user_data_warning,
            stringResource(R.string.your_account)
        ),
        visible = deleteAllDataModalVisible,
        dismiss = { deleteAllDataModalVisible = false },
        onDelete = {
            deleteAllDataModalVisible = false
            deleteAllDataModalFinalVisible = true
        }
    )

    DeleteModal(
        title = stringResource(
            R.string.confirm_all_userd_data_deletion,
            stringResource(R.string.all_of_your_data)
        ),
        description = stringResource(R.string.final_deletion_warning),
        visible = deleteAllDataModalFinalVisible,
        dismiss = { deleteAllDataModalFinalVisible = false },
        onDelete = {
            onEvent(SettingsEvent.DeleteAllUserData)
        }
    )

    ProgressModal(
        title = stringResource(R.string.exporting_data),
        description = stringResource(R.string.exporting_data_description),
        visible = state.progressState
    )
}

@Composable
private fun AccountGroup(
    currencyCode: String,
    name: String,
    onCurrencyClick: () -> Unit,
    onNameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(
        modifier = modifier,
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Default.AttachMoney,
                title = stringResource(R.string.set_currency),
                trailing = currencyCode,
                onClick = onCurrencyClick,
            ),
            SettingsItem(
                icon = Icons.Default.Person,
                title = name.ifBlank { stringResource(R.string.anonymous) },
                supporting = stringResource(R.string.account),
                onClick = onNameClick,
                modifier = Modifier.testTag("settings_profile_card"),
                titleModifier = Modifier.testTag("local_account_name"),
            ),
        )
    )
}

@Composable
private fun ImportExportGroup(
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val rootScreen = rootScreen()
    SettingsGroup(
        modifier = modifier,
        title = stringResource(R.string.import_export),
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Default.FileUpload,
                title = stringResource(R.string.export_to_csv),
                supporting = stringResource(R.string.do_not_use_for_backup_purposes),
                onClick = { onEvent(SettingsEvent.ExportToCsv(rootScreen)) },
            ),
            SettingsItem(
                icon = Icons.Default.Backup,
                title = stringResource(R.string.backup_data),
                onClick = { onEvent(SettingsEvent.BackupData(rootScreen)) },
            ),
            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = stringResource(R.string.import_data),
                onClick = { nav.navigateTo(ImportScreen(launchedFromOnboarding = false)) },
            ),
        )
    )
}

private fun Theme.next(): Theme = when (this) {
    Theme.LIGHT -> Theme.DARK
    Theme.DARK -> Theme.AMOLED_DARK
    Theme.AMOLED_DARK -> Theme.AUTO
    Theme.AUTO -> Theme.LIGHT
}

@Composable
private fun AppSettingsGroup(
    state: SettingsState,
    onStartDateClick: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    SettingsGroup(
        modifier = modifier,
        title = stringResource(R.string.app_settings),
        items = listOfNotNull(
            SettingsItem(
                icon = state.currentTheme.icon(),
                title = state.currentTheme.label(),
                supporting = stringResource(R.string.tap_to_switch_theme),
                onClick = { onEvent(SettingsEvent.SetTheme(state.currentTheme.next())) },
            ),
            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.language),
                supporting = Locale.getDefault().displayName,
                onClick = { onEvent(SettingsEvent.SwitchLanguage) },
            ).takeIf { state.languageOptionVisible },
            SettingsItem(
                icon = Icons.Default.CurrencyExchange,
                title = stringResource(R.string.exchange_rates),
                onClick = { nav.navigateTo(ExchangeRatesScreen) },
            ),
            SettingsItem(
                icon = Icons.Default.Fingerprint,
                title = stringResource(R.string.lock_app),
                checked = state.lockApp,
                onClick = { onEvent(SettingsEvent.SetLockApp(!state.lockApp)) },
            ),
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.show_notifications),
                checked = state.showNotifications,
                onClick = { onEvent(SettingsEvent.SetShowNotifications(!state.showNotifications)) },
            ),
            SettingsItem(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.hide_balance),
                supporting = stringResource(R.string.hide_balance_description),
                checked = state.hideCurrentBalance,
                onClick = { onEvent(SettingsEvent.SetHideCurrentBalance(!state.hideCurrentBalance)) },
            ),
            SettingsItem(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.hide_income),
                supporting = stringResource(R.string.hide_income_description),
                checked = state.hideIncome,
                onClick = { onEvent(SettingsEvent.SetHideIncome(!state.hideIncome)) },
            ),
            SettingsItem(
                icon = Icons.Default.SwapHoriz,
                title = stringResource(R.string.transfers_as_income_expense),
                supporting = stringResource(R.string.transfers_as_income_expense_description),
                checked = state.treatTransfersAsIncomeExpense,
                onClick = {
                    onEvent(
                        SettingsEvent.SetTransfersAsIncomeExpense(!state.treatTransfersAsIncomeExpense)
                    )
                },
            ),
            SettingsItem(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.start_date_of_month),
                trailing = state.startDateOfMonth,
                onClick = onStartDateClick,
            ),
            SettingsItem(
                icon = Icons.Default.Tune,
                title = stringResource(R.string.advanced_features),
                onClick = { nav.navigateTo(FeaturesScreen) },
            ),
        ).toImmutableList()
    )
}

@Composable
private fun OtherGroup(modifier: Modifier = Modifier) {
    val rootScreen = rootScreen()
    SettingsGroup(
        modifier = modifier,
        title = stringResource(R.string.other),
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Default.Star,
                title = stringResource(R.string.rate_us_on_google_play),
                onClick = { rootScreen.reviewIvyWallet(dismissReviewCard = false) },
            ),
            SettingsItem(
                icon = Icons.Default.Share,
                title = stringResource(R.string.share_ivy_wallet),
                onClick = { rootScreen.shareIvyWallet() },
            ),
            SettingsItem(
                icon = Icons.Default.Code,
                title = stringResource(R.string.ivy_wallet_is_opensource),
                supporting = stringResource(R.string.github),
                onClick = { rootScreen.openUrlInBrowser(url = Constants.URL_IVY_WALLET_REPO) },
            ),
        )
    )
}

@Composable
private fun ProductGroup(modifier: Modifier = Modifier) {
    val nav = navigation()
    val rootScreen = rootScreen()
    val uriHandler = LocalUriHandler.current
    SettingsGroup(
        modifier = modifier,
        title = stringResource(R.string.product),
        items = persistentListOf(
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Send,
                title = stringResource(R.string.ivy_telegram),
                onClick = { rootScreen.openUrlInBrowser(Constants.URL_IVY_TELEGRAM_INVITE) },
            ),
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = stringResource(R.string.help_center),
                onClick = { uriHandler.openUri(Constants.URL_HELP_CENTER) },
            ),
            SettingsItem(
                icon = Icons.Default.NewReleases,
                title = stringResource(R.string.releases),
                onClick = { nav.navigateTo(ReleasesScreen) },
            ),
            SettingsItem(
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.report_bug),
                onClick = { uriHandler.openUri(Constants.URL_GITHUB_NEW_ISSUE) },
            ),
            SettingsItem(
                icon = Icons.Default.Lightbulb,
                title = stringResource(R.string.request_a_feature),
                onClick = { rootScreen.openUrlInBrowser(Constants.URL_GITHUB_NEW_ISSUE) },
            ),
            SettingsItem(
                icon = Icons.Default.SupportAgent,
                title = stringResource(R.string.contact_support),
                onClick = { rootScreen.openUrlInBrowser(Constants.URL_IVY_TELEGRAM_INVITE) },
            ),
            SettingsItem(
                icon = Icons.Default.People,
                title = stringResource(R.string.project_contributors),
                onClick = { nav.navigateTo(ContributorsScreen) },
            ),
            SettingsItem(
                icon = Icons.Default.Attribution,
                title = stringResource(R.string.attributions),
                onClick = { nav.navigateTo(AttributionsScreen) },
            ),
            SettingsItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.terms_conditions),
                onClick = { uriHandler.openUri(Constants.URL_TC) },
            ),
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.privacy_policy),
                onClick = { uriHandler.openUri(Constants.URL_PRIVACY_POLICY) },
            ),
        )
    )
}

@Composable
private fun DangerGroup(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(
        modifier = modifier,
        title = stringResource(R.string.danger_zone),
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Default.Delete,
                title = stringResource(R.string.delete_all_user_data),
                destructive = true,
                onClick = onDeleteClick,
            ),
        )
    )
}

@Immutable
private class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
    val supporting: String? = null,
    val trailing: String? = null,
    val checked: Boolean? = null,
    val destructive: Boolean = false,
    val modifier: Modifier = Modifier,
    val titleModifier: Modifier = Modifier,
)

@Composable
private fun SettingsGroup(
    items: ImmutableList<SettingsItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            items.forEachIndexed { index, item ->
                SettingsRow(item = item, index = index, count = items.size)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsRow(
    item: SettingsItem,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (item.destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val leading: @Composable () -> Unit = {
        Icon(imageVector = item.icon, contentDescription = null, tint = contentColor)
    }
    val supporting: (@Composable () -> Unit)? = item.supporting?.let { { Text(it) } }
    val headline: @Composable () -> Unit = {
        Text(text = item.title, modifier = item.titleModifier, color = contentColor)
    }
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

    if (item.checked != null) {
        SegmentedListItem(
            checked = item.checked,
            onCheckedChange = { item.onClick() },
            shapes = shapes,
            colors = colors,
            modifier = modifier.semantics { role = Role.Switch }.then(item.modifier),
            leadingContent = leading,
            trailingContent = { Switch(checked = item.checked, onCheckedChange = null) },
            supportingContent = supporting,
            content = headline,
        )
    } else {
        SegmentedListItem(
            onClick = item.onClick,
            shapes = shapes,
            colors = colors,
            modifier = modifier.then(item.modifier),
            leadingContent = leading,
            trailingContent = item.trailing?.let { trailing ->
                {
                    Text(text = trailing, style = MaterialTheme.typography.titleMedium)
                }
            },
            supportingContent = supporting,
            content = headline,
        )
    }
}

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    firstVisibleItem: Int = 0,
    switches: Boolean = false,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(
                state = SettingsState(
                    currencyCode = "BGN",
                    name = "Ivy",
                    currentTheme = Theme.AUTO,
                    lockApp = switches,
                    showNotifications = switches,
                    hideCurrentBalance = switches,
                    hideIncome = switches,
                    treatTransfersAsIncomeExpense = switches,
                    startDateOfMonth = "1",
                    progressState = false,
                    languageOptionVisible = true,
                ),
                listState = rememberLazyListState(initialFirstVisibleItemIndex = firstVisibleItem),
                onEvent = {}
            )
        }
    }
}

/** For screenshot testing */
@Composable
fun SettingsUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing: the app settings group with every switch on. */
@Composable
fun SettingsAppSettingsUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, firstVisibleItem = 2, switches = true)
}

/** For screenshot testing: the other group. */
@Composable
fun SettingsOtherUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, firstVisibleItem = 3)
}

/** For screenshot testing: the end of the list, down to the danger zone and the version. */
@Composable
fun SettingsEndUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, firstVisibleItem = Int.MAX_VALUE)
}
