package com.ivy.legacy.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.ivy.legacy.utils.format

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
