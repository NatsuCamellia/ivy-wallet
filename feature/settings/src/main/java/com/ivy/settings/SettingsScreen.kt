package com.ivy.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.Constants
import com.ivy.legacy.rootScreen
import com.ivy.navigation.AttributionsScreen
import com.ivy.navigation.ContributorsScreen
import com.ivy.navigation.ExchangeRatesScreen
import com.ivy.navigation.FeaturesScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.ReleasesScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.ui.component.dialog.ProgressDialog
import com.ivy.ui.component.dialog.RadioSelectionDialog
import com.ivy.ui.component.dialog.TextInputDialog
import com.ivy.ui.component.settings.ScreenDisplayTitle
import com.ivy.ui.component.settings.SettingsItem
import com.ivy.ui.component.settings.SettingsSectionTitle
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val DaysInMonth = 31

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    val rootScreen = rootScreen()

    SettingsUi(
        uiState = uiState,
        versionText = "${rootScreen.buildVersionName} (${rootScreen.buildVersionCode})",
        onEvent = viewModel::onEvent,
        onBackupData = { viewModel.onEvent(SettingsEvent.BackupData(rootScreen)) },
        onExportToCsv = { viewModel.onEvent(SettingsEvent.ExportToCsv(rootScreen)) },
        onRateUs = { rootScreen.reviewIvyWallet(dismissReviewCard = false) },
        onShareIvyWallet = { rootScreen.shareIvyWallet() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "LongParameterList")
private fun SettingsUi(
    uiState: SettingsState,
    versionText: String,
    onEvent: (SettingsEvent) -> Unit,
    onBackupData: () -> Unit,
    onExportToCsv: () -> Unit,
    onRateUs: () -> Unit,
    onShareIvyWallet: () -> Unit,
) {
    val nav = navigation()

    var nameDialogVisible by remember { mutableStateOf(false) }
    var currencyDialogVisible by remember { mutableStateOf(false) }
    var themeDialogVisible by remember { mutableStateOf(false) }
    var startDateDialogVisible by remember { mutableStateOf(false) }
    var deleteAllDataDialogVisible by remember { mutableStateOf(false) }
    var deleteAllDataFinalDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    BackButton(onClick = { nav.onBackPressed() })
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("settings_lazy_column"),
        ) {
            item {
                ScreenDisplayTitle(
                    text = stringResource(R.string.settings),
                    description = versionText,
                    onDescriptionClick = { nav.navigateTo(ReleasesScreen) },
                )
            }
            profileSection(
                uiState = uiState,
                onNameClick = { nameDialogVisible = true },
                onCurrencyClick = { currencyDialogVisible = true },
            )
            appearanceSection(
                uiState = uiState,
                onEvent = onEvent,
                onThemeClick = { themeDialogVisible = true },
            )
            behaviorSection(
                uiState = uiState,
                onEvent = onEvent,
                onStartDateClick = { startDateDialogVisible = true },
                onExchangeRatesClick = { nav.navigateTo(ExchangeRatesScreen) },
                onAdvancedFeaturesClick = { nav.navigateTo(FeaturesScreen) },
            )
            privacySection(uiState = uiState, onEvent = onEvent)
            importExportSection(
                onImportClick = {
                    nav.navigateTo(ImportScreen(launchedFromOnboarding = false))
                },
                onBackupData = onBackupData,
                onExportToCsv = onExportToCsv,
            )
            aboutSection(
                onRateUs = onRateUs,
                onShareIvyWallet = onShareIvyWallet,
                onReleasesClick = { nav.navigateTo(ReleasesScreen) },
                onContributorsClick = { nav.navigateTo(ContributorsScreen) },
                onAttributionsClick = { nav.navigateTo(AttributionsScreen) },
            )
            dangerZoneSection(onDeleteAllData = { deleteAllDataDialogVisible = true })
            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }

    SettingsDialogs(
        uiState = uiState,
        onEvent = onEvent,
        nameDialogVisible = nameDialogVisible,
        onNameDialogVisible = { nameDialogVisible = it },
        currencyDialogVisible = currencyDialogVisible,
        onCurrencyDialogVisible = { currencyDialogVisible = it },
        themeDialogVisible = themeDialogVisible,
        onThemeDialogVisible = { themeDialogVisible = it },
        startDateDialogVisible = startDateDialogVisible,
        onStartDateDialogVisible = { startDateDialogVisible = it },
        deleteAllDataDialogVisible = deleteAllDataDialogVisible,
        onDeleteAllDataDialogVisible = { deleteAllDataDialogVisible = it },
        deleteAllDataFinalDialogVisible = deleteAllDataFinalDialogVisible,
        onDeleteAllDataFinalDialogVisible = { deleteAllDataFinalDialogVisible = it },
    )
}

private fun LazyListScope.profileSection(
    uiState: SettingsState,
    onNameClick: () -> Unit,
    onCurrencyClick: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.profile))
    }
    item {
        SettingsItem(
            title = stringResource(R.string.name),
            description = uiState.name.ifBlank { stringResource(R.string.anonymous) },
            onClick = onNameClick,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.currency),
            description = uiState.currencyCode,
            onClick = onCurrencyClick,
        )
    }
}

private fun LazyListScope.appearanceSection(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onThemeClick: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.appearance))
    }
    item {
        SettingsItem(
            title = stringResource(R.string.theme),
            description = themeLabel(uiState.currentTheme),
            onClick = onThemeClick,
        )
    }
    if (uiState.dynamicColorAvailable) {
        item {
            SettingsItem(
                title = stringResource(R.string.dynamic_color),
                description = stringResource(R.string.dynamic_color_description),
                onClick = null,
                modifier = Modifier.toggleable(
                    value = uiState.dynamicColorEnabled,
                    role = Role.Switch,
                    onValueChange = { onEvent(SettingsEvent.SetDynamicColor(it)) },
                ),
            ) {
                Switch(
                    checked = uiState.dynamicColorEnabled,
                    onCheckedChange = null,
                )
            }
        }
    }
    if (uiState.languageOptionVisible) {
        item {
            SettingsItem(
                title = stringResource(R.string.language),
                description = Locale.getDefault().displayName,
                onClick = { onEvent(SettingsEvent.SwitchLanguage) },
            )
        }
    }
}

private fun LazyListScope.behaviorSection(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onStartDateClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onAdvancedFeaturesClick: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.behavior))
    }
    item {
        SettingsItem(
            title = stringResource(R.string.start_date_of_month),
            description = uiState.startDateOfMonth,
            onClick = onStartDateClick,
        )
    }
    item {
        SwitchItem(
            title = stringResource(R.string.transfers_as_income_expense),
            description = stringResource(R.string.transfers_as_income_expense_description),
            checked = uiState.treatTransfersAsIncomeExpense,
            onCheckedChange = { onEvent(SettingsEvent.SetTransfersAsIncomeExpense(it)) },
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.exchange_rates),
            onClick = onExchangeRatesClick,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.advanced_features),
            onClick = onAdvancedFeaturesClick,
        )
    }
}

private fun LazyListScope.privacySection(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.privacy))
    }
    item {
        SwitchItem(
            title = stringResource(R.string.lock_app),
            checked = uiState.lockApp,
            onCheckedChange = { onEvent(SettingsEvent.SetLockApp(it)) },
        )
    }
    item {
        SwitchItem(
            title = stringResource(R.string.show_notifications),
            checked = uiState.showNotifications,
            onCheckedChange = { onEvent(SettingsEvent.SetShowNotifications(it)) },
        )
    }
    item {
        SwitchItem(
            title = stringResource(R.string.hide_balance),
            description = stringResource(R.string.hide_balance_description),
            checked = uiState.hideCurrentBalance,
            onCheckedChange = { onEvent(SettingsEvent.SetHideCurrentBalance(it)) },
        )
    }
    item {
        SwitchItem(
            title = stringResource(R.string.hide_income),
            description = stringResource(R.string.hide_income_description),
            checked = uiState.hideIncome,
            onCheckedChange = { onEvent(SettingsEvent.SetHideIncome(it)) },
        )
    }
}

private fun LazyListScope.importExportSection(
    onImportClick: () -> Unit,
    onBackupData: () -> Unit,
    onExportToCsv: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.import_export))
    }
    item {
        SettingsItem(
            title = stringResource(R.string.import_data),
            onClick = onImportClick,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.backup_data),
            onClick = onBackupData,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.export_to_csv),
            description = stringResource(R.string.do_not_use_for_backup_purposes),
            onClick = onExportToCsv,
        )
    }
}

private fun LazyListScope.aboutSection(
    onRateUs: () -> Unit,
    onShareIvyWallet: () -> Unit,
    onReleasesClick: () -> Unit,
    onContributorsClick: () -> Unit,
    onAttributionsClick: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.about_and_support))
    }
    item {
        SettingsItem(title = stringResource(R.string.rate_us_on_google_play), onClick = onRateUs)
    }
    item {
        SettingsItem(title = stringResource(R.string.share_ivy_wallet), onClick = onShareIvyWallet)
    }
    item {
        UrlItem(
            title = stringResource(R.string.ivy_wallet_is_opensource),
            url = Constants.URL_IVY_WALLET_REPO,
        )
    }
    item {
        UrlItem(
            title = stringResource(R.string.ivy_telegram),
            url = Constants.URL_IVY_TELEGRAM_INVITE,
        )
    }
    item {
        UrlItem(
            title = stringResource(R.string.help_center),
            url = Constants.URL_HELP_CENTER,
        )
    }
    item {
        SettingsItem(title = stringResource(R.string.releases), onClick = onReleasesClick)
    }
    item {
        UrlItem(
            title = stringResource(R.string.report_bug),
            url = Constants.URL_GITHUB_NEW_ISSUE,
        )
    }
    item {
        UrlItem(
            title = stringResource(R.string.request_a_feature),
            url = Constants.URL_GITHUB_NEW_ISSUE,
        )
    }
    item {
        UrlItem(
            title = stringResource(R.string.contact_support),
            url = Constants.URL_IVY_TELEGRAM_INVITE,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.project_contributors),
            onClick = onContributorsClick,
        )
    }
    item {
        SettingsItem(title = stringResource(R.string.attributions), onClick = onAttributionsClick)
    }
    item {
        UrlItem(title = stringResource(R.string.terms_conditions), url = Constants.URL_TC)
    }
    item {
        UrlItem(title = stringResource(R.string.privacy_policy), url = Constants.URL_PRIVACY_POLICY)
    }
}

private fun LazyListScope.dangerZoneSection(onDeleteAllData: () -> Unit) {
    item {
        SettingsSectionTitle(
            text = stringResource(R.string.danger_zone),
            color = MaterialTheme.colorScheme.error,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.delete_all_user_data),
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onDeleteAllData,
        )
    }
}

@Composable
private fun SwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    SettingsItem(
        title = title,
        description = description,
        onClick = null,
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    ) {
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun UrlItem(title: String, url: String) {
    val uriHandler = LocalUriHandler.current
    SettingsItem(
        title = title,
        onClick = { uriHandler.openUri(url) },
    )
}

@Composable
private fun themeLabel(theme: Theme): String = stringResource(
    when (theme) {
        Theme.LIGHT -> R.string.light_mode
        Theme.DARK -> R.string.dark_mode
        Theme.AMOLED_DARK -> R.string.amoled_mode
        Theme.AUTO -> R.string.auto_mode
    }
)

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun SettingsDialogs(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    nameDialogVisible: Boolean,
    onNameDialogVisible: (Boolean) -> Unit,
    currencyDialogVisible: Boolean,
    onCurrencyDialogVisible: (Boolean) -> Unit,
    themeDialogVisible: Boolean,
    onThemeDialogVisible: (Boolean) -> Unit,
    startDateDialogVisible: Boolean,
    onStartDateDialogVisible: (Boolean) -> Unit,
    deleteAllDataDialogVisible: Boolean,
    onDeleteAllDataDialogVisible: (Boolean) -> Unit,
    deleteAllDataFinalDialogVisible: Boolean,
    onDeleteAllDataFinalDialogVisible: (Boolean) -> Unit,
) {
    if (nameDialogVisible) {
        TextInputDialog(
            title = stringResource(R.string.name),
            initialValue = uiState.name,
            onConfirm = {
                onEvent(SettingsEvent.SetName(it))
                onNameDialogVisible(false)
            },
            onDismiss = { onNameDialogVisible(false) },
        )
    }
    if (currencyDialogVisible) {
        CurrencyPickerDialog(
            selectedCode = uiState.currencyCode,
            onSelect = {
                onEvent(SettingsEvent.SetCurrency(it))
                onCurrencyDialogVisible(false)
            },
            onDismiss = { onCurrencyDialogVisible(false) },
        )
    }
    if (themeDialogVisible) {
        val themeOptions = remember {
            persistentListOf(Theme.AUTO, Theme.LIGHT, Theme.DARK, Theme.AMOLED_DARK)
        }
        RadioSelectionDialog(
            title = stringResource(R.string.theme),
            options = themeOptions.map { themeLabel(it) }.toImmutableList(),
            selectedIndex = themeOptions.indexOf(uiState.currentTheme),
            onSelect = { index ->
                onEvent(SettingsEvent.SetTheme(themeOptions[index]))
                onThemeDialogVisible(false)
            },
            onDismiss = { onThemeDialogVisible(false) },
        )
    }
    if (startDateDialogVisible) {
        RadioSelectionDialog(
            title = stringResource(R.string.choose_start_date_of_month),
            options = (1..DaysInMonth).map(Int::toString).toImmutableList(),
            selectedIndex = (uiState.startDateOfMonth.toIntOrNull() ?: 1) - 1,
            onSelect = { index ->
                onEvent(SettingsEvent.SetStartDateOfMonth(index + 1))
                onStartDateDialogVisible(false)
            },
            onDismiss = { onStartDateDialogVisible(false) },
        )
    }
    if (deleteAllDataDialogVisible) {
        AlertDialog(
            onDismissRequest = { onDeleteAllDataDialogVisible(false) },
            title = { Text(text = stringResource(R.string.delete_all_user_data_question)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_all_user_data_warning,
                        stringResource(R.string.your_account),
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllDataDialogVisible(false)
                        onDeleteAllDataFinalDialogVisible(true)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onDeleteAllDataDialogVisible(false) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
    if (deleteAllDataFinalDialogVisible) {
        AlertDialog(
            onDismissRequest = { onDeleteAllDataFinalDialogVisible(false) },
            title = {
                Text(
                    text = stringResource(
                        R.string.confirm_all_userd_data_deletion,
                        stringResource(R.string.all_of_your_data),
                    )
                )
            },
            text = { Text(text = stringResource(R.string.final_deletion_warning)) },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsEvent.DeleteAllUserData) }) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onDeleteAllDataFinalDialogVisible(false) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
    if (uiState.progressState) {
        ProgressDialog(
            title = stringResource(R.string.exporting_data),
            description = stringResource(R.string.exporting_data_description),
        )
    }
}

@Preview
@Composable
private fun Preview(dark: Boolean = false) {
    IvyPreview(dark = dark) {
        SettingsUi(
            uiState = SettingsState(
                currencyCode = "USD",
                name = "Ivy",
                currentTheme = Theme.AUTO,
                lockApp = false,
                showNotifications = true,
                hideCurrentBalance = false,
                hideIncome = false,
                treatTransfersAsIncomeExpense = false,
                startDateOfMonth = "1",
                progressState = false,
                languageOptionVisible = true,
                dynamicColorEnabled = true,
                dynamicColorAvailable = true,
            ),
            versionText = "1.0.0 (100)",
            onEvent = {},
            onBackupData = {},
            onExportToCsv = {},
            onRateUs = {},
            onShareIvyWallet = {},
        )
    }
}

/** For screenshot testing */
@Composable
fun SettingsUiTest(isDark: Boolean) {
    Preview(dark = isDark)
}
