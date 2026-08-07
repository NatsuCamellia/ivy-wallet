package com.ivy.ui.component.amount

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AmountKeypadPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun `snapshot amount keypad with accounts`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                AmountKeypadUiTest(withAccounts = true)
            }
        }
    }

    @Test
    fun `snapshot amount keypad without accounts`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                AmountKeypadUiTest(withAccounts = false)
            }
        }
    }
}
