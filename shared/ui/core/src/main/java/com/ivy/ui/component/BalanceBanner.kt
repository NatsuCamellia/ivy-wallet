package com.ivy.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ivy.ui.theme.rounded
import kotlinx.collections.immutable.ImmutableList

/** What a hidden amount reads as (Settings: Hide balance and Hide income). */
const val HiddenBalance = "****"

/** One "label over number" column at the bottom of a [BalanceBanner]. Clickable only when [onClick] is set. */
@Immutable
data class BalanceBannerColumn(
    val label: String,
    val value: String,
    val onClick: (() -> Unit)?,
    val testTag: String?,
)

/**
 * The summary block of a tab: a large rounded container on primaryFixed. The top has a [label] over a large
 * rounded [value] (clickable when [onClick] is set, described by [onClickLabel]) and an optional small
 * [supportingText]. The bottom has two or three [columns] side by side, each with an optional click.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BalanceBanner(
    label: String,
    value: String,
    columns: ImmutableList<BalanceBannerColumn>,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    valueTestTag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = colors.primaryFixed,
        contentColor = colors.onPrimaryFixed,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickableIf(onClick, onClickLabel)
                    .then(if (valueTestTag != null) Modifier.testTag(valueTestTag) else Modifier)
                    .padding(8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryFixedVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmallEmphasized.rounded(),
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryFixedVariant,
                    )
                }
            }
            Row {
                columns.forEach { column ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickableIf(column.onClick, null)
                            .then(if (column.testTag != null) Modifier.testTag(column.testTag) else Modifier)
                            .padding(8.dp),
                    ) {
                        Text(
                            text = column.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onPrimaryFixedVariant,
                        )
                        Text(
                            text = column.value,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableIf(onClick: (() -> Unit)?, onClickLabel: String?): Modifier =
    if (onClick == null) this else clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
