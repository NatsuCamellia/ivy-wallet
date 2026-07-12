package com.ivy.settings

import com.ivy.base.legacy.Theme
import com.ivy.domain.RootScreen

sealed interface SettingsEvent {
    data class SetCurrency(val newCurrency: String) : SettingsEvent
    data class SetName(val newName: String) : SettingsEvent
    data class ExportToCsv(val rootScreen: RootScreen) : SettingsEvent
    data class BackupData(val rootScreen: RootScreen) : SettingsEvent
    data class SetTheme(val theme: Theme) : SettingsEvent
    data class SetDynamicColor(val enabled: Boolean) : SettingsEvent
    data class SetLockApp(val lockApp: Boolean) : SettingsEvent
    data class SetShowNotifications(val showNotifications: Boolean) : SettingsEvent
    data class SetHideCurrentBalance(val hideCurrentBalance: Boolean) : SettingsEvent
    data class SetHideIncome(val hideIncome: Boolean) : SettingsEvent
    data class SetTransfersAsIncomeExpense(val treatTransfersAsIncomeExpense: Boolean) :
        SettingsEvent

    data class SetStartDateOfMonth(val startDate: Int) : SettingsEvent
    data object DeleteCloudUserData : SettingsEvent
    data object DeleteAllUserData : SettingsEvent
    data object SwitchLanguage : SettingsEvent
}
