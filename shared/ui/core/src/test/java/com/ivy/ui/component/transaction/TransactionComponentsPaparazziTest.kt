package com.ivy.ui.component.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    fun `snapshot transaction items`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    TransactionItem(
                        ui = expense.copy(title = "Lidl groceries"),
                        position = TransactionItemPosition.First,
                        onClick = {},
                        icon = { CategoryIcon() },
                    )
                    TransactionItem(
                        ui = expense.copy(
                            id = "2",
                            title = "Salary — July",
                            supportingText = "Salary · DSK Bank",
                            time = "09:30",
                            categoryColor = Color(0xFF14CC9E),
                            amountText = "+8,049.70 USD",
                            amountKind = TransactionAmountKind.Income,
                        ),
                        position = TransactionItemPosition.Middle,
                        onClick = {},
                        icon = { CategoryIcon() },
                    )
                    TransactionItem(
                        ui = expense.copy(
                            id = "3",
                            title = "Top-up Revolut",
                            supportingText = "Cash → Revolut",
                            time = "18:20",
                            categoryColor = null,
                            amountText = "40.00 USD",
                            amountKind = TransactionAmountKind.Transfer,
                            secondaryText = "36.50 EUR",
                        ),
                        position = TransactionItemPosition.Last,
                        onClick = {},
                        icon = { TransferIcon() },
                    )
                    TransactionItem(
                        ui = expense.copy(
                            id = "4",
                            title = "Rent",
                            supportingText = null,
                            time = null,
                            amountText = "-500.00 USD",
                            amountKind = TransactionAmountKind.Upcoming,
                            secondaryText = null,
                            dueText = "Due on Fri, Jul 24",
                        ),
                        position = TransactionItemPosition.Single,
                        onClick = {},
                        onSkip = {},
                        onPayOrGet = {},
                        payOrGetText = "Pay",
                        icon = { CategoryIcon() },
                    )
                    TransactionItem(
                        ui = expense.copy(
                            id = "5",
                            title = "Spotify",
                            supportingText = null,
                            time = null,
                            amountText = "-5.99 USD",
                            amountKind = TransactionAmountKind.Overdue,
                            secondaryText = null,
                            dueText = "Due on Jul 15",
                        ),
                        position = TransactionItemPosition.Single,
                        onClick = {},
                        onSkip = {},
                        onPayOrGet = {},
                        payOrGetText = "Pay",
                        icon = { CategoryIcon() },
                    )
                }
            }
        }
    }

    @Test
    fun `snapshot transaction items with long text`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    TransactionItem(
                        ui = expense.copy(
                            supportingText = "Groceries and household supplies · Joint savings account at the bank",
                        ),
                        position = TransactionItemPosition.First,
                        onClick = {},
                        icon = { CategoryIcon() },
                    )
                    TransactionItem(
                        ui = expense.copy(
                            id = "2",
                            supportingText = "Checking account of the long named company · Savings account at the bank",
                            categoryColor = null,
                            amountText = "40,000.00 USD",
                            amountKind = TransactionAmountKind.Transfer,
                            secondaryText = "36,500.00 EUR",
                        ),
                        position = TransactionItemPosition.Last,
                        onClick = {},
                        icon = { TransferIcon() },
                    )
                }
            }
        }
    }

    @Test
    fun `snapshot headers`() {
        snapshot(theme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TransactionSectionHeader(
                        title = "Upcoming",
                        titleColor = MaterialTheme.colorScheme.secondary,
                        subtitle = "+120.00 USD · -500.00 USD",
                        expanded = false,
                        onExpandedChange = {},
                    )
                    TransactionSectionHeader(
                        title = "Overdue",
                        titleColor = MaterialTheme.colorScheme.error,
                        subtitle = "-5.99 USD",
                        expanded = true,
                        onExpandedChange = {},
                        trailing = {
                            TextButton(onClick = {}) { Text("Skip all") }
                        },
                    )
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

    @Composable
    private fun CategoryIcon() {
        Icon(
            painter = painterResource(R.drawable.ic_custom_category_m),
            contentDescription = null,
        )
    }

    @Composable
    private fun TransferIcon() {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = null,
        )
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
        )
    }
}
