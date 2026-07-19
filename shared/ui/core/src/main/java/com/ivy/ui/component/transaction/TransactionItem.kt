package com.ivy.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.design.system.LocalIvyExtendedColors
import com.ivy.ui.R

private val LargeCorner = 24.dp
private val SmallCorner = 8.dp

fun TransactionItemPosition.toShape(): RoundedCornerShape = when (this) {
    TransactionItemPosition.Single -> RoundedCornerShape(LargeCorner)
    TransactionItemPosition.First ->
        RoundedCornerShape(LargeCorner, LargeCorner, SmallCorner, SmallCorner)
    TransactionItemPosition.Middle -> RoundedCornerShape(SmallCorner)
    TransactionItemPosition.Last ->
        RoundedCornerShape(SmallCorner, SmallCorner, LargeCorner, LargeCorner)
}

@Composable
fun TransactionItem(
    ui: TransactionItemUi,
    position: TransactionItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onPayOrGet: (() -> Unit)? = null,
    payOrGetText: String? = null,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(position.toShape())
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag("transaction_item"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBubble(categoryColor = ui.categoryColor, icon = icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ui.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    ui.dueText != null -> DueChip(
                        text = ui.dueText,
                        overdue = ui.amountKind == TransactionAmountKind.Overdue,
                    )
                    ui.supportingText != null -> Text(
                        text = ui.supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ui.amountText,
                    style = MaterialTheme.typography.titleMedium,
                    color = ui.amountKind.amountColor(),
                )
                if (ui.secondaryText != null) {
                    Text(
                        text = ui.secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (onSkip != null || onPayOrGet != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                if (onSkip != null) {
                    FilledTonalButton(onClick = onSkip) {
                        Text(text = stringResource(R.string.skip))
                    }
                }
                if (onPayOrGet != null) {
                    Button(onClick = onPayOrGet) {
                        Text(text = payOrGetText ?: stringResource(R.string.pay))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionAmountKind.amountColor(): Color = when (this) {
    TransactionAmountKind.Expense -> MaterialTheme.colorScheme.onSurface
    TransactionAmountKind.Income -> LocalIvyExtendedColors.current.income
    TransactionAmountKind.Transfer -> MaterialTheme.colorScheme.primary
    TransactionAmountKind.Upcoming -> LocalIvyExtendedColors.current.warning
    TransactionAmountKind.Overdue -> MaterialTheme.colorScheme.error
}

private const val ContainerAlpha = 0.24f
private const val ContentBlend = 0.45f

@Composable
private fun CategoryIconBubble(
    categoryColor: Color?,
    icon: @Composable () -> Unit,
) {
    val container = if (categoryColor != null) {
        categoryColor.copy(alpha = ContainerAlpha)
            .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (categoryColor != null) {
        lerp(categoryColor, MaterialTheme.colorScheme.onSurface, ContentBlend)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            icon()
        }
    }
}

@Composable
private fun DueChip(text: String, overdue: Boolean) {
    val background = if (overdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        LocalIvyExtendedColors.current.warningContainer
    }
    val contentColor = if (overdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        LocalIvyExtendedColors.current.onWarningContainer
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
