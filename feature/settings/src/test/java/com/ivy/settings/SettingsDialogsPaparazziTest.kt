package com.ivy.settings

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.component.dialog.ProgressDialog
import com.ivy.ui.component.dialog.RadioSelectionDialog
import com.ivy.ui.component.dialog.TextInputDialog
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SettingsDialogsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotRadioSelectionDialog() {
        snapshot(theme) {
            RadioSelectionDialog(
                title = "Theme",
                options = persistentListOf("Auto mode", "Light mode", "Dark mode", "AMOLED mode"),
                selectedIndex = 1,
                onSelect = {},
                onDismiss = {},
            )
        }
    }

    @Test
    fun snapshotTextInputDialog() {
        snapshot(theme) {
            TextInputDialog(title = "Name", initialValue = "Ivy", onConfirm = {}, onDismiss = {})
        }
    }

    @Test
    fun snapshotProgressDialog() {
        snapshot(theme) {
            ProgressDialog(title = "Exporting data", description = "Please wait.")
        }
    }

    @Test
    fun snapshotCurrencyPickerDialog() {
        snapshot(theme) {
            CurrencyPickerDialog(selectedCode = "EUR", onSelect = {}, onDismiss = {})
        }
    }
}
