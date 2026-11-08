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

/** An amount followed by its currency code. Never truncates; it wraps when the space is short. */
@Composable
fun AmountText(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    showPlusSign: Boolean = false,
) {
    val sign = if (showPlusSign && amount > 0) "+" else ""
    Text(
        text = "$sign${amount.format(currency)} $currency",
        modifier = modifier.testTag("amount_text"),
        style = style,
        textAlign = TextAlign.End,
    )
}

/** The type icon and the signed amount in the type colour: expenses are negative, incomes get a plus. */
@Composable
fun TypeAmount(
    type: TransactionType,
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = type.label(),
            modifier = Modifier.size(AssistChipDefaults.IconSize),
            tint = type.color(),
        )
        AmountText(
            amount = if (type == TransactionType.EXPENSE) -abs(amount) else amount,
            currency = currency,
            style = MaterialTheme.typography.titleMediumEmphasized.copy(color = type.color()),
            showPlusSign = type == TransactionType.INCOME,
        )
    }
}
