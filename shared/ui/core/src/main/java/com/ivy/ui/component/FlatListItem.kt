package com.ivy.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A flat list row that sits directly on the background: a [leading] icon, a [title] with one line of
 * [supportingText] under it and on the right a [primaryText] stacked over a [secondaryText]. Only the
 * press shows a rounded ripple. Whoever places the row decides the horizontal screen margin: the row
 * is inset by 8dp on each side for the ripple and by 8dp more for its content, so it lines up with a
 * 16dp margin when used full width. An [overlineText] sits above the title, a [descriptionText]
 * (up to three lines) and a [tagsText] under the supporting text, and [actions] (buttons)
 * under all of them. A [supportingContent] slot, e.g. a share bar, sits between the two and
 * spans the full width under the text and the trailing amounts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlatListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    descriptionText: String? = null,
    tagsText: String? = null,
    primaryText: String? = null,
    primaryColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryText: String? = null,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    leading: (@Composable () -> Unit)? = null,
    overlineText: String? = null,
    overlineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actions: (@Composable RowScope.() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.largeIncreased)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        if (supportingContent == null) {
            Column(modifier = Modifier.weight(1f)) {
                TextLines(title, supportingText, descriptionText, tagsText, overlineText, overlineColor)
                ActionsRow(actions)
            }
            Trailing(primaryText, primaryColor, secondaryText, secondaryColor)
        } else {
            // the content spans the full width, under the text and the trailing amounts
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextLines(
                        title = title,
                        supportingText = supportingText,
                        descriptionText = descriptionText,
                        tagsText = tagsText,
                        overlineText = overlineText,
                        overlineColor = overlineColor,
                        modifier = Modifier.weight(1f),
                    )
                    Trailing(primaryText, primaryColor, secondaryText, secondaryColor)
                }
                supportingContent()
                ActionsRow(actions)
            }
        }
    }
}

@Composable
private fun TextLines(
    title: String,
    supportingText: String?,
    descriptionText: String?,
    tagsText: String?,
    overlineText: String?,
    overlineColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (overlineText != null) {
            Text(
                text = overlineText,
                style = MaterialTheme.typography.labelMedium,
                color = overlineColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (supportingText != null) {
            SupportingLine(supportingText, maxLines = 1)
        }
        if (descriptionText != null) {
            SupportingLine(descriptionText, maxLines = DescriptionMaxLines)
        }
        if (tagsText != null) {
            SupportingLine(tagsText, maxLines = TagsMaxLines)
        }
    }
}

@Composable
private fun SupportingLine(text: String, maxLines: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Like the transaction card of upstream Ivy Wallet, a long description is cut after three lines. */
private const val DescriptionMaxLines = 3
private const val TagsMaxLines = 2

@Composable
private fun ActionsRow(actions: (@Composable RowScope.() -> Unit)?) {
    if (actions != null) {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = actions,
        )
    }
}

@Composable
private fun Trailing(
    primaryText: String?,
    primaryColor: Color,
    secondaryText: String?,
    secondaryColor: Color,
) {
    if (primaryText != null || secondaryText != null) {
        Column(horizontalAlignment = Alignment.End) {
            if (primaryText != null) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryColor,
                    maxLines = 1,
                )
            }
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    maxLines = 1,
                )
            }
        }
    }
}
