package com.ivy.ui.component.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.design.system.LocalIvyExtendedColors
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import com.ivy.ui.R
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
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

    @Test
    fun `snapshot headers`() {
        snapshot(theme) {
            Column(modifier = Modifier.padding(16.dp)) {
                TransactionDayHeader(title = "Today", netText = "-83.26 USD")
                TransactionSectionHeader(
                    title = "Upcoming",
                    titleColor = LocalIvyExtendedColors.current.warning,
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

    @Composable
    private fun CategoryIcon() {
        Icon(
            painter = painterResource(R.drawable.ic_custom_category_s),
            contentDescription = null,
        )
    }

    @Composable
    private fun TransferIcon() {
        Icon(
            painter = painterResource(R.drawable.ic_transfer),
            contentDescription = null,
        )
    }

    companion object {
        private val expense = TransactionItemUi(
            id = "1",
            title = "Expense",
            supportingText = "Groceries · Cash",
            categoryColor = Color(0xFFFF9235),
            amountText = "-32.51 USD",
            amountKind = TransactionAmountKind.Expense,
            secondaryText = "14:05",
            dueText = null,
        )
    }
}
