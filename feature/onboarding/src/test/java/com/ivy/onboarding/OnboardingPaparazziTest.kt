package com.ivy.onboarding

import app.cash.paparazzi.DeviceConfig
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.onboarding.steps.OnboardingAccountsEmptyUiTest
import com.ivy.onboarding.steps.OnboardingAccountsUiTest
import com.ivy.onboarding.steps.OnboardingCategoriesEmptyUiTest
import com.ivy.onboarding.steps.OnboardingCategoriesUiTest
import com.ivy.onboarding.steps.OnboardingLoginUiTest
import com.ivy.onboarding.steps.OnboardingSetCurrencyUiTest
import com.ivy.onboarding.steps.OnboardingSplashUiTest
import com.ivy.onboarding.steps.OnboardingTypeUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class OnboardingPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotOnboardingSplashScreen() {
        snapshot(theme) {
            OnboardingSplashUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotOnboardingLoginScreen() {
        snapshot(theme) {
            OnboardingLoginUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotOnboardingTypeScreen() {
        snapshot(theme) {
            OnboardingTypeUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotOnboardingCurrencyScreen() {
        snapshot(theme) {
            OnboardingSetCurrencyUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotOnboardingAccountsScreen() {
        snapshot(theme) {
            OnboardingAccountsUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotOnboardingCategoriesScreen() {
        snapshot(theme) {
            OnboardingCategoriesUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotOnboardingAccountsEmptyScreen() {
        snapshot(theme) {
            OnboardingAccountsEmptyUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotOnboardingCategoriesEmptyScreen() {
        snapshot(theme) {
            OnboardingCategoriesEmptyUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotOnboardingLoginLargeFontScale() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_6_PRO.copy(fontScale = 1.5f))
        snapshot(theme) {
            OnboardingLoginUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
