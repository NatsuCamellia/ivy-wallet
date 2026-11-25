package com.ivy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

/** One chip of [TypeFilterChips]: its [label] and a [number] (or null) shown after it, e.g. a total. */
@Immutable
data class TypeFilterOption<T>(
    val value: T,
    val label: String,
    val number: String?,
)

/**
 * A single-select row of filter chips. The chip of [selected] is highlighted; tapping a chip calls
 * [onSelect] with its value. It pads its own 16dp screen margin and 8dp above and below.
 */
@Composable
fun <T> TypeFilterChips(
    options: ImmutableList<TypeFilterOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it.label }) { option ->
            val isSelected = option.value == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option.value) },
                label = {
                    Text(text = option.number?.let { "${option.label} $it" } ?: option.label)
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
