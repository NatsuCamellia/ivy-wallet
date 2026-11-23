package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** A labelled amount shown in the footer of an [OverviewCard]. */
@Immutable
class OverviewAmount(
    val label: String,
    val amount: Double,
    val color: Color,
    val showPlusSign: Boolean = false,
)

/** An account or category: badge and name, the centred balance, and the amounts of [footer] side by side. */
@Composable
fun OverviewCard(
    name: String,
    badge: @Composable () -> Unit,
    balance: Double,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPlusSign: Boolean = false,
    supportingText: String? = null,
    balanceSupporting: (@Composable () -> Unit)? = null,
    footer: ImmutableList<OverviewAmount> = persistentListOf(),
) {
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
            leadingContent = badge,
            supportingContent = supportingText?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors = transparent,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ListItem(supportingContent = balanceSupporting, colors = transparent) {
            AmountText(
                amount = balance,
                currency = currency,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                showPlusSign = showPlusSign,
                textAlign = TextAlign.Center,
            )
        }

        if (footer.isNotEmpty()) {
            HorizontalDivider()
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                footer.forEachIndexed { index, item ->
                    if (index > 0) VerticalDivider()
                    ListItem(
                        modifier = Modifier.weight(1f),
                        overlineContent = {
                            Text(text = item.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        },
                        colors = transparent,
                    ) {
                        AmountText(
                            amount = item.amount,
                            currency = currency,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleSmall.copy(color = item.color),
                            showPlusSign = item.showPlusSign,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
