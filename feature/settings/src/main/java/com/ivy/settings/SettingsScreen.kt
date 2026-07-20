package com.ivy.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.ivy.navigation.ExchangeRatesScreen
import com.ivy.navigation.FeaturesScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.ui.component.dialog.ProgressDialog
import com.ivy.ui.component.dialog.RadioSelectionDialog
import com.ivy.ui.component.dialog.TextInputDialog
import com.ivy.ui.component.settings.ScreenDisplayTitle
import com.ivy.ui.component.settings.SettingsItem
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
        versionName = rootScreen.buildVersionName,
        onEvent = viewModel::onEvent,
        onBackupData = { viewModel.onEvent(SettingsEvent.BackupData(rootScreen)) },
        onExportToCsv = { viewModel.onEvent(SettingsEvent.ExportToCsv(rootScreen)) },
    )
}

/**
 * ReadYou-style settings: the first page only lists categories, each drilling down into its own
 * page. This is UI-only navigation kept local to the screen (not the app's [com.ivy.navigation]
 * back stack) since every category shares the same [SettingsState]/[SettingsViewModel].
 */
private enum class SettingsPage {
    Categories,
    Profile,
    Appearance,
    Behavior,
    Privacy,
    ImportExport,
    AboutSupport,
    DangerZone,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "LongParameterList")
private fun SettingsUi(
    uiState: SettingsState,
    versionName: String,
    onEvent: (SettingsEvent) -> Unit,
    onBackupData: () -> Unit,
    onExportToCsv: () -> Unit,
) {
    val nav = navigation()
    val uriHandler = LocalUriHandler.current

    var page by remember { mutableStateOf(SettingsPage.Categories) }

    var nameDialogVisible by remember { mutableStateOf(false) }
    var currencyDialogVisible by remember { mutableStateOf(false) }
    var themeDialogVisible by remember { mutableStateOf(false) }
    var startDateDialogVisible by remember { mutableStateOf(false) }
    var deleteAllDataDialogVisible by remember { mutableStateOf(false) }
    var deleteAllDataFinalDialogVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = page != SettingsPage.Categories) {
        page = SettingsPage.Categories
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    BackButton(
                        onClick = {
                            if (page != SettingsPage.Categories) {
                                page = SettingsPage.Categories
                            } else {
                                nav.onBackPressed()
                            }
                        },
                    )
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (page) {
                SettingsPage.Categories -> CategoriesPage(onNavigate = { page = it })
                SettingsPage.Profile -> ProfilePage(
                    uiState = uiState,
                    onNameClick = { nameDialogVisible = true },
                    onCurrencyClick = { currencyDialogVisible = true },
                )

                SettingsPage.Appearance -> AppearancePage(
                    uiState = uiState,
                    onEvent = onEvent,
                    onThemeClick = { themeDialogVisible = true },
                )

                SettingsPage.Behavior -> BehaviorPage(
                    uiState = uiState,
                    onEvent = onEvent,
                    onStartDateClick = { startDateDialogVisible = true },
                    onExchangeRatesClick = { nav.navigateTo(ExchangeRatesScreen) },
                    onAdvancedFeaturesClick = { nav.navigateTo(FeaturesScreen) },
                )

                SettingsPage.Privacy -> PrivacyPage(uiState = uiState, onEvent = onEvent)
                SettingsPage.ImportExport -> ImportExportPage(
                    onImportClick = {
                        nav.navigateTo(ImportScreen(launchedFromOnboarding = false))
                    },
                    onBackupData = onBackupData,
                    onExportToCsv = onExportToCsv,
                )

                SettingsPage.AboutSupport -> AboutSupportPage(
                    versionName = versionName,
                    onGitHubClick = { uriHandler.openUri(Constants.URL_IVY_WALLET_REPO) },
                    onAttributionsClick = { nav.navigateTo(AttributionsScreen) },
                    onTermsClick = { uriHandler.openUri(Constants.URL_TC) },
                    onPrivacyClick = { uriHandler.openUri(Constants.URL_PRIVACY_POLICY) },
                )

                SettingsPage.DangerZone -> DangerZonePage(
                    onDeleteAllData = { deleteAllDataDialogVisible = true },
                )
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

@Composable
private fun CategoriesPage(onNavigate: (SettingsPage) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_lazy_column"),
    ) {
        item {
            ScreenDisplayTitle(text = stringResource(R.string.settings))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.profile),
                description = stringResource(R.string.profile_settings_desc),
                icon = Icons.Filled.Person,
                onClick = { onNavigate(SettingsPage.Profile) },
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.appearance),
                description = stringResource(R.string.appearance_settings_desc),
                icon = Icons.Filled.Palette,
                onClick = { onNavigate(SettingsPage.Appearance) },
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.behavior),
                description = stringResource(R.string.behavior_settings_desc),
                icon = Icons.Filled.Tune,
                onClick = { onNavigate(SettingsPage.Behavior) },
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.privacy),
                description = stringResource(R.string.privacy_settings_desc),
                icon = Icons.Filled.Lock,
                onClick = { onNavigate(SettingsPage.Privacy) },
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.import_export),
                description = stringResource(R.string.import_export_settings_desc),
                icon = Icons.Filled.ImportExport,
                onClick = { onNavigate(SettingsPage.ImportExport) },
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.about_and_support),
                description = stringResource(R.string.about_and_support_desc),
                icon = Icons.Filled.Info,
                onClick = { onNavigate(SettingsPage.AboutSupport) },
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.danger_zone),
                description = stringResource(R.string.danger_zone_desc),
                icon = Icons.Filled.Delete,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { onNavigate(SettingsPage.DangerZone) },
            )
        }
        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ProfilePage(
    uiState: SettingsState,
    onNameClick: () -> Unit,
    onCurrencyClick: () -> Unit,
) {
    DetailPageColumn {
        item {
            ScreenDisplayTitle(text = stringResource(R.string.profile))
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
}

@Composable
private fun AppearancePage(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onThemeClick: () -> Unit,
) {
    DetailPageColumn {
        item {
            ScreenDisplayTitle(text = stringResource(R.string.appearance))
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
}

@Composable
private fun BehaviorPage(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onStartDateClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onAdvancedFeaturesClick: () -> Unit,
) {
    DetailPageColumn {
        item {
            ScreenDisplayTitle(text = stringResource(R.string.behavior))
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
}

@Composable
private fun PrivacyPage(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    DetailPageColumn {
        item {
            ScreenDisplayTitle(text = stringResource(R.string.privacy))
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
}

@Composable
private fun ImportExportPage(
    onImportClick: () -> Unit,
    onBackupData: () -> Unit,
    onExportToCsv: () -> Unit,
) {
    DetailPageColumn {
        item {
            ScreenDisplayTitle(text = stringResource(R.string.import_export))
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
}

@Composable
private fun DangerZonePage(onDeleteAllData: () -> Unit) {
    DetailPageColumn {
        item {
            ScreenDisplayTitle(text = stringResource(R.string.danger_zone))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.delete_all_user_data),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onDeleteAllData,
            )
        }
    }
}

@Composable
private fun DetailPageColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

@Composable
private fun AboutSupportPage(
    versionName: String,
    onGitHubClick: () -> Unit,
    onAttributionsClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    DetailPageColumn {
        item {
            ScreenDisplayTitle(text = stringResource(R.string.about_and_support))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.version),
                description = versionName,
                icon = Icons.Filled.Info,
                onClick = null,
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.github),
                icon = Icons.Filled.Code,
                onClick = onGitHubClick,
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.attributions),
                icon = Icons.Filled.MenuBook,
                onClick = onAttributionsClick,
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.terms_conditions),
                icon = Icons.Filled.Description,
                onClick = onTermsClick,
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.privacy_policy),
                icon = Icons.Filled.PrivacyTip,
                onClick = onPrivacyClick,
            )
        }
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
            versionName = "1.0.0",
            onEvent = {},
            onBackupData = {},
            onExportToCsv = {},
        )
    }
}

/** For screenshot testing */
@Composable
fun SettingsUiTest(isDark: Boolean) {
    Preview(dark = isDark)
}

@Preview
@Composable
private fun AboutSupportPreview(dark: Boolean = false) {
    IvyPreview(dark = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AboutSupportPage(
                versionName = "1.0.0",
                onGitHubClick = {},
                onAttributionsClick = {},
                onTermsClick = {},
                onPrivacyClick = {},
            )
        }
    }
}

/** For screenshot testing */
@Composable
fun AboutSupportUiTest(isDark: Boolean) {
    AboutSupportPreview(dark = isDark)
}
