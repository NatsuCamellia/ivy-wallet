package com.ivy.home

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.home.customerjourney.CustomerJourneyUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import com.ivy.ui.testing.WithFakeRootScreen
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class HomePaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot Home Screen`() {
        snapshot(theme) {
            HomeUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotHomeMoreMenuExpanded() {
        snapshot(theme) {
            WithFakeRootScreen {
                HomeUiTest(theme == PaparazziTheme.Dark, moreMenuExpanded = true)
            }
        }
    }

    @Test
    fun snapshotHomeEmpty() {
        snapshot(theme) {
            HomeUiTest(theme == PaparazziTheme.Dark, empty = true)
        }
    }

    @Test
    fun snapshotHomePlannedPayments() {
        snapshot(theme) {
            HomeUiTest(theme == PaparazziTheme.Dark, due = HomeDuePreview.OverdueAndUpcoming)
        }
    }

    @Test
    fun snapshotHomeDueUpcomingOnly() {
        snapshot(theme) {
            HomeUiTest(theme == PaparazziTheme.Dark, due = HomeDuePreview.UpcomingOnly)
        }
    }

    @Test
    fun snapshotHomeDueCollapsedWithMore() {
        snapshot(theme) {
            HomeUiTest(theme == PaparazziTheme.Dark, due = HomeDuePreview.CollapsedWithMore)
        }
    }

    @Test
    fun snapshotCustomerJourneyCards() {
        snapshot(theme) {
            CustomerJourneyUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
