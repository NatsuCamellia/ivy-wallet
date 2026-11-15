package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.legacy.utils.format
import kotlin.math.abs

/** The amount and its currency code. A zero amount is never signed, even as -0.0 or a rounded-down negative. */
fun amountLabel(amount: Double, currency: String, showPlusSign: Boolean = false): String {
    val formatted = amount.format(currency)
    val isZero = formatted.none { (it.digitToIntOrNull() ?: 0) > 0 }
    val sign = if (showPlusSign && amount > 0 && !isZero) "+" else ""
    val digits = if (isZero) formatted.dropWhile { it.digitToIntOrNull() == null } else formatted
    return "$sign$digits $currency"
}

/** An amount followed by its currency code. Never truncates; it wraps when the space is short. */
@Composable
fun AmountText(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    showPlusSign: Boolean = false,
    textAlign: TextAlign = TextAlign.End,
) {
    Text(
        text = amountLabel(amount, currency, showPlusSign),
        modifier = modifier.testTag("amount_text"),
        style = style,
        textAlign = textAlign,
    )
}

/** The amount with its type's sign: expenses are negative, the other types keep theirs. */
fun signedAmount(type: TransactionType, amount: Double): Double =
    if (type == TransactionType.EXPENSE) 0.0 - abs(amount) else amount

/** The type icon and the signed amount in the type colour: expenses are negative, incomes get a plus. */
@Composable
fun TypeAmount(
    type: TransactionType,
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    iconDescription: String = type.label(),
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = iconDescription,
            modifier = Modifier.size(AssistChipDefaults.IconSize),
            tint = type.color(),
        )
        AmountText(
            amount = signedAmount(type, amount),
            currency = currency,
            style = MaterialTheme.typography.titleMediumEmphasized.copy(color = type.color()),
            showPlusSign = type == TransactionType.INCOME,
        )
    }
}
