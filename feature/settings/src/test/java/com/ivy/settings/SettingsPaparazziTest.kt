package com.ivy.settings

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import com.ivy.ui.testing.WithFakeRootScreen
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SettingsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotSettingsScreen() {
        snapshot(theme) {
            WithFakeRootScreen {
                SettingsUiTest(theme == PaparazziTheme.Dark)
            }
        }
    }

    @Test
    fun snapshotSettingsAppSettings() {
        snapshot(theme) {
            WithFakeRootScreen {
                SettingsAppSettingsUiTest(theme == PaparazziTheme.Dark)
            }
        }
    }

    @Test
    fun snapshotSettingsOther() {
        snapshot(theme) {
            WithFakeRootScreen {
                SettingsOtherUiTest(theme == PaparazziTheme.Dark)
            }
        }
    }

    @Test
    fun snapshotSettingsEnd() {
        snapshot(theme) {
            WithFakeRootScreen {
                SettingsEndUiTest(theme == PaparazziTheme.Dark)
            }
        }
    }
}
