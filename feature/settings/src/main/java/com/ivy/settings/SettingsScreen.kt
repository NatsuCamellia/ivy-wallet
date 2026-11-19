package com.ivy.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Attribution
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.Constants
import com.ivy.legacy.rootScreen
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.icon
import com.ivy.legacy.ui.component.label
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
import com.ivy.ui.component.dialog.ProgressDialog
import com.ivy.ui.component.dialog.RadioSelectionDialog
import com.ivy.ui.component.dialog.TextInputDialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale

private const val DaysInMonth = 31

/** The categories list and, under it, one page per category; the pages share one ViewModel. */
private enum class SettingsPage(@StringRes val title: Int) {
    Categories(R.string.settings),
    Profile(R.string.profile),
    Appearance(R.string.appearance),
    Behavior(R.string.behavior),
    Privacy(R.string.privacy),
    ImportExport(R.string.import_export),
    AboutSupport(R.string.about_and_support),
    DangerZone(R.string.danger_zone),
}

private enum class SettingsDialog {
    Name, Currency, Theme, StartDate, DeleteAll, DeleteAllFinal
}

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    val rootScreen = rootScreen()

    UI(
        state = uiState,
        versionName = "${rootScreen.buildVersionName} (${rootScreen.buildVersionCode})",
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun UI(
    state: SettingsState,
    versionName: String,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
    initialPage: SettingsPage = SettingsPage.Categories,
) {
    val nav = navigation()
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    BackHandler(enabled = page != SettingsPage.Categories) { page = SettingsPage.Categories }

    key(page) {
        SettingsPageScaffold(
            page = page,
            onBack = { if (page == SettingsPage.Categories) nav.back() else page = SettingsPage.Categories },
            modifier = modifier,
        ) {
            when (page) {
                SettingsPage.Categories -> CategoriesGroups(onPage = { page = it })
                SettingsPage.Profile -> ProfileGroup(state, onDialog = { dialog = it })
                SettingsPage.Appearance -> AppearanceGroup(state, onDialog = { dialog = it }, onEvent = onEvent)
                SettingsPage.Behavior -> BehaviorGroup(state, onDialog = { dialog = it }, onEvent = onEvent)
                SettingsPage.Privacy -> PrivacyGroup(state, onEvent)
                SettingsPage.ImportExport -> ImportExportGroup(onEvent)
                SettingsPage.AboutSupport -> AboutSupportGroup(versionName)
                SettingsPage.DangerZone -> DangerGroup(onDeleteClick = { dialog = SettingsDialog.DeleteAll })
            }
        }
    }

    SettingsDialogs(
        state = state,
        dialog = dialog,
        onDialog = { dialog = it },
        onEvent = onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsPageScaffold(
    page: SettingsPage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(page.title)) },
                navigationIcon = { BackButton(onClick = onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("settings_lazy_column"),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = page) { content() }
        }
    }
}

@Composable
private fun CategoriesGroups(onPage: (SettingsPage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsGroup(
            items = persistentListOf(
                SettingsItem(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.profile),
                    supporting = stringResource(R.string.profile_settings_desc),
                    onClick = { onPage(SettingsPage.Profile) },
                ),
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.appearance),
                    supporting = stringResource(R.string.appearance_settings_desc),
                    onClick = { onPage(SettingsPage.Appearance) },
                ),
                SettingsItem(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(R.string.behavior),
                    supporting = stringResource(R.string.behavior_settings_desc),
                    onClick = { onPage(SettingsPage.Behavior) },
                ),
                SettingsItem(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.privacy),
                    supporting = stringResource(R.string.privacy_settings_desc),
                    onClick = { onPage(SettingsPage.Privacy) },
                ),
                SettingsItem(
                    icon = Icons.Outlined.ImportExport,
                    title = stringResource(R.string.import_export),
                    supporting = stringResource(R.string.import_export_settings_desc),
                    onClick = { onPage(SettingsPage.ImportExport) },
                ),
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_and_support),
                    supporting = stringResource(R.string.about_and_support_desc),
                    onClick = { onPage(SettingsPage.AboutSupport) },
                ),
            )
        )
        SettingsGroup(
            items = persistentListOf(
                SettingsItem(
                    icon = Icons.Outlined.Delete,
                    title = stringResource(R.string.danger_zone),
                    supporting = stringResource(R.string.danger_zone_desc),
                    destructive = true,
                    onClick = { onPage(SettingsPage.DangerZone) },
                ),
            )
        )
    }
}

@Composable
private fun ProfileGroup(
    state: SettingsState,
    onDialog: (SettingsDialog) -> Unit,
) {
    SettingsGroup(
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Outlined.Badge,
                title = stringResource(R.string.name),
                supporting = state.name.ifBlank { stringResource(R.string.anonymous) },
                onClick = { onDialog(SettingsDialog.Name) },
                modifier = Modifier.testTag("settings_profile_card"),
            ),
            SettingsItem(
                icon = Icons.Outlined.AttachMoney,
                title = stringResource(R.string.currency),
                supporting = state.currencyCode,
                onClick = { onDialog(SettingsDialog.Currency) },
            ),
        )
    )
}

@Composable
private fun AppearanceGroup(
    state: SettingsState,
    onDialog: (SettingsDialog) -> Unit,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsGroup(
        items = listOfNotNull(
            SettingsItem(
                icon = state.currentTheme.icon(),
                title = stringResource(R.string.theme),
                supporting = state.currentTheme.label(),
                onClick = { onDialog(SettingsDialog.Theme) },
            ),
            SettingsItem(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.language),
                supporting = Locale.getDefault().displayName,
                onClick = { onEvent(SettingsEvent.SwitchLanguage) },
            ).takeIf { state.languageOptionVisible },
        ).toImmutableList()
    )
}

@Composable
private fun BehaviorGroup(
    state: SettingsState,
    onDialog: (SettingsDialog) -> Unit,
    onEvent: (SettingsEvent) -> Unit,
) {
    val nav = navigation()
    SettingsGroup(
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Outlined.CalendarMonth,
                title = stringResource(R.string.start_date_of_month),
                supporting = state.startDateOfMonth,
                onClick = { onDialog(SettingsDialog.StartDate) },
            ),
            SettingsItem(
                icon = Icons.Outlined.SwapHoriz,
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
                icon = Icons.Outlined.CurrencyExchange,
                title = stringResource(R.string.exchange_rates),
                onClick = { nav.navigateTo(ExchangeRatesScreen) },
            ),
            SettingsItem(
                icon = Icons.Outlined.Science,
                title = stringResource(R.string.advanced_features),
                onClick = { nav.navigateTo(FeaturesScreen) },
            ),
        )
    )
}

@Composable
private fun PrivacyGroup(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    SettingsGroup(
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Outlined.Fingerprint,
                title = stringResource(R.string.lock_app),
                checked = state.lockApp,
                onClick = { onEvent(SettingsEvent.SetLockApp(!state.lockApp)) },
            ),
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.show_notifications),
                checked = state.showNotifications,
                onClick = { onEvent(SettingsEvent.SetShowNotifications(!state.showNotifications)) },
            ),
            SettingsItem(
                icon = Icons.Outlined.VisibilityOff,
                title = stringResource(R.string.hide_balance),
                supporting = stringResource(R.string.hide_balance_description),
                checked = state.hideCurrentBalance,
                onClick = { onEvent(SettingsEvent.SetHideCurrentBalance(!state.hideCurrentBalance)) },
            ),
            SettingsItem(
                icon = Icons.Outlined.VisibilityOff,
                title = stringResource(R.string.hide_income),
                supporting = stringResource(R.string.hide_income_description),
                checked = state.hideIncome,
                onClick = { onEvent(SettingsEvent.SetHideIncome(!state.hideIncome)) },
            ),
        )
    )
}

@Composable
private fun ImportExportGroup(onEvent: (SettingsEvent) -> Unit) {
    val nav = navigation()
    val rootScreen = rootScreen()
    SettingsGroup(
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Outlined.FileDownload,
                title = stringResource(R.string.import_data),
                onClick = { nav.navigateTo(ImportScreen(launchedFromOnboarding = false)) },
            ),
            SettingsItem(
                icon = Icons.Outlined.Backup,
                title = stringResource(R.string.backup_data),
                onClick = { onEvent(SettingsEvent.BackupData(rootScreen)) },
            ),
            SettingsItem(
                icon = Icons.Outlined.FileUpload,
                title = stringResource(R.string.export_to_csv),
                supporting = stringResource(R.string.do_not_use_for_backup_purposes),
                onClick = { onEvent(SettingsEvent.ExportToCsv(rootScreen)) },
            ),
        )
    )
}

@Composable
private fun AboutSupportGroup(versionName: String) {
    val nav = navigation()
    val uriHandler = LocalUriHandler.current
    SettingsGroup(
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.version),
                supporting = versionName,
                onClick = { nav.navigateTo(ReleasesScreen) },
            ),
            SettingsItem(
                icon = Icons.Outlined.Code,
                title = stringResource(R.string.github),
                onClick = { uriHandler.openUri(Constants.URL_IVY_WALLET_REPO) },
            ),
            SettingsItem(
                icon = Icons.Outlined.BugReport,
                title = stringResource(R.string.report_bug),
                onClick = { uriHandler.openUri(Constants.URL_GITHUB_NEW_ISSUE) },
            ),
            SettingsItem(
                icon = Icons.Outlined.Lightbulb,
                title = stringResource(R.string.request_a_feature),
                onClick = { uriHandler.openUri(Constants.URL_GITHUB_NEW_ISSUE) },
            ),
            SettingsItem(
                icon = Icons.AutoMirrored.Outlined.Help,
                title = stringResource(R.string.help_center),
                onClick = { uriHandler.openUri(Constants.URL_HELP_CENTER) },
            ),
            SettingsItem(
                icon = Icons.Outlined.People,
                title = stringResource(R.string.project_contributors),
                onClick = { nav.navigateTo(ContributorsScreen) },
            ),
            SettingsItem(
                icon = Icons.Outlined.Attribution,
                title = stringResource(R.string.attributions),
                onClick = { nav.navigateTo(AttributionsScreen) },
            ),
            SettingsItem(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.terms_conditions),
                onClick = { uriHandler.openUri(Constants.URL_TC) },
            ),
            SettingsItem(
                icon = Icons.Outlined.PrivacyTip,
                title = stringResource(R.string.privacy_policy),
                onClick = { uriHandler.openUri(Constants.URL_PRIVACY_POLICY) },
            ),
        )
    )
}

@Composable
private fun DangerGroup(onDeleteClick: () -> Unit) {
    SettingsGroup(
        items = persistentListOf(
            SettingsItem(
                icon = Icons.Outlined.Delete,
                title = stringResource(R.string.delete_all_user_data),
                destructive = true,
                onClick = onDeleteClick,
            ),
        )
    )
}

@Composable
private fun SettingsDialogs(
    state: SettingsState,
    dialog: SettingsDialog?,
    onDialog: (SettingsDialog?) -> Unit,
    onEvent: (SettingsEvent) -> Unit,
) {
    val dismiss = { onDialog(null) }
    when (dialog) {
        SettingsDialog.Name -> TextInputDialog(
            title = stringResource(R.string.name),
            initialValue = state.name,
            onConfirm = {
                onEvent(SettingsEvent.SetName(it))
                dismiss()
            },
            onDismiss = dismiss,
        )

        SettingsDialog.Currency -> CurrencyPickerDialog(
            selectedCode = state.currencyCode,
            onSelect = {
                onEvent(SettingsEvent.SetCurrency(it))
                dismiss()
            },
            onDismiss = dismiss,
        )

        SettingsDialog.Theme -> {
            val themes = remember { persistentListOf(Theme.AUTO, Theme.LIGHT, Theme.DARK, Theme.AMOLED_DARK) }
            RadioSelectionDialog(
                title = stringResource(R.string.theme),
                options = themes.map { it.label() }.toImmutableList(),
                selectedIndex = themes.indexOf(state.currentTheme),
                onSelect = {
                    onEvent(SettingsEvent.SetTheme(themes[it]))
                    dismiss()
                },
                onDismiss = dismiss,
            )
        }

        SettingsDialog.StartDate -> RadioSelectionDialog(
            title = stringResource(R.string.choose_start_date_of_month),
            options = (1..DaysInMonth).map(Int::toString).toImmutableList(),
            selectedIndex = (state.startDateOfMonth.toIntOrNull() ?: 1) - 1,
            onSelect = {
                onEvent(SettingsEvent.SetStartDateOfMonth(it + 1))
                dismiss()
            },
            onDismiss = dismiss,
        )

        SettingsDialog.DeleteAll -> ConfirmDeleteDialog(
            title = stringResource(R.string.delete_all_user_data_question),
            text = stringResource(R.string.delete_all_user_data_warning, stringResource(R.string.your_account)),
            onConfirm = { onDialog(SettingsDialog.DeleteAllFinal) },
            onDismiss = dismiss,
        )

        SettingsDialog.DeleteAllFinal -> ConfirmDeleteDialog(
            title = stringResource(
                R.string.confirm_all_userd_data_deletion,
                stringResource(R.string.all_of_your_data),
            ),
            text = stringResource(R.string.final_deletion_warning),
            onConfirm = { onEvent(SettingsEvent.DeleteAllUserData) },
            onDismiss = dismiss,
        )

        null -> Unit
    }

    if (state.progressState) {
        ProgressDialog(
            title = stringResource(R.string.exporting_data),
            description = stringResource(R.string.exporting_data_description),
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = { Text(text = title) },
        text = { Text(text = text) },
    )
}

@Immutable
private class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
    val supporting: String? = null,
    val checked: Boolean? = null,
    val destructive: Boolean = false,
    val modifier: Modifier = Modifier,
)

@Composable
private fun SettingsGroup(
    items: ImmutableList<SettingsItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        items.forEachIndexed { index, item ->
            SettingsRow(item = item, index = index, count = items.size)
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
        Text(text = item.title, color = contentColor)
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
            supportingContent = supporting,
            content = headline,
        )
    }
}

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    page: SettingsPage = SettingsPage.Categories,
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
                versionName = "1.0.0 (100)",
                onEvent = {},
                initialPage = page,
            )
        }
    }
}

/** For screenshot testing: the categories list. */
@Composable
fun SettingsUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing: the About & Support page. */
@Composable
fun AboutSupportUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, page = SettingsPage.AboutSupport)
}

/** For screenshot testing: the Behavior page. */
@Composable
fun SettingsAppSettingsUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, page = SettingsPage.Behavior, switches = true)
}

/** For screenshot testing: the Privacy page with every switch on. */
@Composable
fun SettingsOtherUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, page = SettingsPage.Privacy, switches = true)
}

/** For screenshot testing: the Danger zone page. */
@Composable
fun SettingsEndUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, page = SettingsPage.DangerZone)
}
