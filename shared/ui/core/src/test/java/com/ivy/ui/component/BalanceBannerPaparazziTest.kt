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
class BalanceBannerPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun `three columns with a negative cashflow and two columns with a small line`() {
        snapshot(theme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BalanceBanner(
                        label = "Total Balance",
                        value = "2,415.70 USD",
                        columns = persistentListOf(
                            BalanceBannerColumn("Income", "+2,500.00 USD", onClick = {}),
                            BalanceBannerColumn("Expenses", "-84.30 USD", onClick = {}),
                            BalanceBannerColumn("Cashflow", "-1,284.30 USD"),
                        ),
                        onClick = {},
                    )
                    BalanceBanner(
                        modifier = Modifier.padding(top = 16.dp),
                        label = "Total Balance",
                        value = "12,415.70 USD",
                        supportingText = "Including excluded accounts: 15,000.00 USD",
                        columns = persistentListOf(
                            BalanceBannerColumn("Income", "+2,500.00 USD"),
                            BalanceBannerColumn("Expenses", "-84.30 USD"),
                        ),
                    )
                }
            }
        }
    }
}
