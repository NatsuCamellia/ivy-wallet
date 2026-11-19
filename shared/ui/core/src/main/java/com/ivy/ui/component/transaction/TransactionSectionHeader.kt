package com.ivy.ui.component.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.ivy.ui.R

/**
 * A collapsible section (upcoming, overdue) with the totals of its transactions under the title.
 * Only the title and chevron toggle it, so a [trailing] action stays its own control.
 */
@Composable
fun TransactionSectionHeader(
    title: String,
    titleColor: Color,
    subtitle: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val clickLabel = stringResource(if (expanded) R.string.collapse_section else R.string.expand_section, title)
    val stateDescription = stringResource(if (expanded) R.string.section_expanded else R.string.section_collapsed)
    ListItem(
        headlineContent = {
            Row(
                modifier = Modifier
                    .clickable(onClickLabel = clickLabel, role = Role.Button) { onExpandedChange(!expanded) }
                    .semantics { this.stateDescription = stateDescription }
                    .minimumInteractiveComponentSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
        },
        modifier = modifier,
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
