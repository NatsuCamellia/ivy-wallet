package com.ivy.ui.component.picker

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PickerPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun `snapshot category picker`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                PickerUiTest(accounts = false)
            }
        }
    }

    @Test
    fun `snapshot account picker`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                PickerUiTest(accounts = true)
            }
        }
    }
}
