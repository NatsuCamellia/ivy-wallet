package com.ivy.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class TypeFilterChipsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun `labels without and with numbers`() {
        snapshot(theme) {
            Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                TypeFilterChips(
                    options = listOf(
                        TypeFilterOption("all", "All"),
                        TypeFilterOption("expense", "Expense"),
                        TypeFilterOption("income", "Income"),
                    ),
                    selected = "all",
                    onSelect = {},
                )
                TypeFilterChips(
                    modifier = Modifier.padding(top = 16.dp),
                    options = listOf(
                        TypeFilterOption("expense", "Expense", "-1,234.50"),
                        TypeFilterOption("income", "Income", "+2,000.00"),
                    ),
                    selected = "income",
                    onSelect = {},
                )
            }
            }
        }
    }
}
