package com.ivy.legacy.ui.component.transaction

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.design.l0_system.UI
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.Ivy
import com.ivy.ui.color.Orange
import com.ivy.ui.color.Red
import com.ivy.ui.color.White
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientGreen
import com.ivy.wallet.ui.theme.GradientIvy
import com.ivy.wallet.ui.theme.GradientOrangeRevert
import com.ivy.wallet.ui.theme.GradientRed
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.time.LocalDateTime

@Composable
fun TypeAmountCurrency(
    transactionType: TransactionType,
    dueDate: LocalDateTime?,
    currency: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("type_amount_currency"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val style = when (transactionType) {
            TransactionType.INCOME -> {
                AmountTypeStyle(
                    icon = R.drawable.ic_income,
                    gradient = GradientGreen,
                    iconTint = White,
                    textColor = Green
                )
            }

            TransactionType.EXPENSE -> {
                when {
                    dueDate != null && dueDate.isAfter(timeNowUTC()) -> {
                        // Upcoming Expense
                        AmountTypeStyle(
                            icon = R.drawable.ic_expense,
                            gradient = GradientOrangeRevert,
                            iconTint = White,
                            textColor = Orange
                        )
                    }

                    dueDate != null && dueDate.isBefore(dateNowUTC().atStartOfDay()) -> {
                        // Overdue Expense
                        AmountTypeStyle(
                            icon = R.drawable.ic_overdue,
                            gradient = GradientRed,
                            iconTint = White,
                            textColor = Red
                        )
                    }

                    else -> {
                        // Normal Expense
                        AmountTypeStyle(
                            icon = R.drawable.ic_expense,
                            gradient = Gradient.black(),
                            iconTint = White,
                            textColor = UI.colors.pureInverse
                        )
                    }
                }
            }

            TransactionType.TRANSFER -> {
                // Transfer
                AmountTypeStyle(
                    icon = R.drawable.ic_transfer,
                    gradient = GradientIvy,
                    iconTint = White,
                    textColor = Ivy
                )
            }
        }

        IvyIcon(
            modifier = Modifier
                .background(style.gradient.asHorizontalBrush(), CircleShape),
            icon = style.icon,
            tint = style.iconTint
        )

        Spacer(Modifier.width(12.dp))

        AmountCurrencyB1(
            amount = amount,
            currency = currency,
            textColor = style.textColor
        )

        Spacer(Modifier.width(24.dp))
    }
}

private data class AmountTypeStyle(
    @DrawableRes val icon: Int,
    val gradient: Gradient,
    val iconTint: Color,
    val textColor: Color
)
