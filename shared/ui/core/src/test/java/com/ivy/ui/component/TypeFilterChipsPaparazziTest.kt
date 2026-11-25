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
import kotlinx.collections.immutable.persistentListOf
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
                    options = persistentListOf(
                        TypeFilterOption("all", "All", null),
                        TypeFilterOption("expense", "Expense", null),
                        TypeFilterOption("income", "Income", null),
                    ),
                    selected = "all",
                    onSelect = {},
                )
                TypeFilterChips(
                    modifier = Modifier.padding(top = 16.dp),
                    options = persistentListOf(
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
