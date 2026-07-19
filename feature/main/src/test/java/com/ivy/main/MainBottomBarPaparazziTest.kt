package com.ivy.main

import androidx.compose.foundation.layout.BoxWithConstraints
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.legacy.data.model.MainTab
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class MainBottomBarPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot bottom bar - home tab`() {
        snapshot(theme) {
            BoxWithConstraints {
                BottomBar(
                    tab = MainTab.HOME,
                    selectTab = {},
                    onAddIncome = {},
                    onAddExpense = {},
                    onAddTransfer = {},
                    onAddPlannedPayment = {},
                    showAddAccountModal = {},
                )
            }
        }
    }

    @Test
    fun `snapshot bottom bar - accounts tab`() {
        snapshot(theme) {
            BoxWithConstraints {
                BottomBar(
                    tab = MainTab.ACCOUNTS,
                    selectTab = {},
                    onAddIncome = {},
                    onAddExpense = {},
                    onAddTransfer = {},
                    onAddPlannedPayment = {},
                    showAddAccountModal = {},
                )
            }
        }
    }
}
