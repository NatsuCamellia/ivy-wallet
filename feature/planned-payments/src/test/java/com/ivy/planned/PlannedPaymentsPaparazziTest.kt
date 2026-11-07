package com.ivy.planned

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.planned.edit.EditPlannedScreenUiTest
import com.ivy.planned.list.PlannedPaymentsScreenUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PlannedPaymentsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotPlannedPaymentsScreen() {
        snapshot(theme) {
            PlannedPaymentsScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotEditPlannedScreen() {
        snapshot(theme) {
            EditPlannedScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
