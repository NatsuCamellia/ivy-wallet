package com.ivy.ui.component.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import com.ivy.ui.R
import com.ivy.ui.component.FlatListItem
import com.ivy.ui.component.StickyListHeader
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class TransactionComponentsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot planned payment rows`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    FlatTransactionItem(
                        ui = expense.copy(
                            id = "4",
                            title = "Spotify",
                            supportingText = "Cash",
                            time = null,
                            amountText = "-5.99 USD",
                            dueText = "Due on Jul 15",
                            dueOverdue = true,
                        ),
                        onClick = {},
                        onSkip = {},
                        onPayOrGet = {},
                        payOrGetText = "Pay",
                    ) { Badge(Color(0xFFFF9235)) }
                    FlatTransactionItem(
                        ui = expense.copy(
                            id = "5",
                            title = "Freelance",
                            supportingText = "Cash",
                            time = null,
                            amountText = "+120.00 USD",
                            amountKind = TransactionAmountKind.Income,
                            dueText = "Due on Fri, Jul 24",
                        ),
                        onClick = {},
                        onSkip = {},
                        onPayOrGet = {},
                        payOrGetText = "Get",
                    ) { Badge(Color(0xFFFF9235)) }
                }
            }
        }
    }

    @Test
    fun `snapshot flat transaction rows`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    StickyListHeader(title = "Today", subtotal = "-83.26 USD")
                    FlatTransactionItem(ui = expense, onClick = {}) { Badge(Color(0xFFFF9235)) }
                    FlatTransactionItem(
                        ui = expense.copy(
                            id = "2",
                            title = "Salary — July",
                            supportingText = "Salary · DSK Bank",
                            amountText = "+8,049.70 USD",
                            amountKind = TransactionAmountKind.Income,
                        ),
                        onClick = {},
                    ) { Badge(Color(0xFFFF9235)) }
                    FlatTransactionItem(
                        ui = expense.copy(
                            id = "3",
                            title = "Top-up Revolut",
                            supportingText = "Cash → Revolut",
                            categoryColor = null,
                            amountText = "40.00 USD",
                            amountKind = TransactionAmountKind.Transfer,
                        ),
                        onClick = {},
                    ) { Badge(MaterialTheme.colorScheme.primaryContainer) }
                    FlatTransactionItem(
                        ui = expense.copy(
                            id = "4",
                            title = "A very long transaction title that has to be cut off at the end of the line",
                            supportingText = "Groceries and household supplies · Joint savings account at the bank",
                            amountText = "-40,000.00 USD",
                        ),
                        onClick = {},
                    ) { Badge(Color(0xFFFF9235)) }
                    StickyListHeader(title = "Yesterday, Jul 22", subtotal = "+8,049.70 USD")
                    FlatListItem(title = "Without numbers", onClick = {}, supportingText = "Just text")
                }
            }
        }
    }

    @Composable
    private fun Badge(color: Color) {
        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = color) {
            Icon(
                painter = painterResource(R.drawable.ic_custom_category_m),
                contentDescription = null,
                modifier = Modifier.padding(4.dp),
            )
        }
    }

    companion object {
        private val expense = TransactionItemUi(
            id = "1",
            title = "Expense",
            supportingText = "Groceries · Cash",
            time = "14:05",
            categoryColor = Color(0xFFFF9235),
            amountText = "-32.51 USD",
            amountKind = TransactionAmountKind.Expense,
            secondaryText = null,
            dueText = null,
            dueOverdue = false,
        )
    }
}
