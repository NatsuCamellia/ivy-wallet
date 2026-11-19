package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** A total on a tonal container: a labelled icon above the amount. Clickable only when [onClick] is given. */
@Composable
fun TonalSummaryCard(
    label: String,
    amount: Double,
    currency: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier,
    showPlusSign: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }

            AmountText(
                amount = amount,
                currency = currency,
                style = MaterialTheme.typography.titleMediumEmphasized,
                showPlusSign = showPlusSign,
                textAlign = TextAlign.Start,
                shortenBigNumbers = true,
            )
        }
    }

    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, content = content)
    } else {
        Card(modifier = modifier, colors = colors, content = content)
    }
}
