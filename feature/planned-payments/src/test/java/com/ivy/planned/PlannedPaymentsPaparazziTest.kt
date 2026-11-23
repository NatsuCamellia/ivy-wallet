package com.ivy.planned

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.planned.edit.EditPlannedRepeatUiTest
import com.ivy.planned.edit.EditPlannedScreenUiTest
import com.ivy.planned.list.PlannedPaymentsEmptyUiTest
import com.ivy.planned.list.PlannedPaymentsInvalidRuleUiTest
import com.ivy.planned.list.PlannedPaymentsScreenUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone

@RunWith(TestParameterInjector::class)
class PlannedPaymentsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Before
    fun setUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun snapshotPlannedPaymentsScreen() {
        snapshot(theme) {
            PlannedPaymentsScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotPlannedPaymentsEmpty() {
        snapshot(theme) {
            PlannedPaymentsEmptyUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotPlannedPaymentsInvalidRule() {
        snapshot(theme) {
            PlannedPaymentsInvalidRuleUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotEditPlannedScreen() {
        snapshot(theme) {
            EditPlannedScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotEditPlannedRepeat() {
        snapshot(theme) {
            EditPlannedRepeatUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
