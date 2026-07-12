package com.ivy.ui.component.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SettingsComponentsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot settings components`() {
        snapshot(theme) {
            Column {
                ScreenDisplayTitle(text = "Settings", description = "1.0.0 (100)")
                SettingsSectionTitle(text = "Appearance")
                SettingsItem(title = "Theme", description = "Auto", onClick = {})
                SettingsItem(
                    title = "Dynamic color",
                    description = "Use colors from your wallpaper",
                    onClick = {},
                ) {
                    Switch(checked = true, onCheckedChange = {})
                }
                SettingsItem(title = "Disabled row", enabled = false, onClick = {})
            }
        }
    }
}
