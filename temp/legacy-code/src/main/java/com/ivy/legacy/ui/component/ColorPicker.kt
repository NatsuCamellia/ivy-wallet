package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.ui.color.IVY_COLOR_PICKER_COLORS_FREE
import com.ivy.ui.color.IVY_COLOR_PICKER_COLORS_PREMIUM
import com.ivy.ui.color.findContrastTextColor

private val pickerColors = IVY_COLOR_PICKER_COLORS_FREE + IVY_COLOR_PICKER_COLORS_PREMIUM

@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.choose_color),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        LazyRow(
            state = rememberLazyListState(
                initialFirstVisibleItemIndex = pickerColors.indexOf(selectedColor).coerceAtLeast(0)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(pickerColors) { color ->
                val selected = color == selectedColor
                val description = stringResource(R.string.color_value, "#%08X".format(color.toArgb()))
                FilledIconToggleButton(
                    checked = selected,
                    onCheckedChange = { onColorSelect(color) },
                    modifier = Modifier
                        .testTag("color_item_${color.value}")
                        .semantics { contentDescription = description },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = color,
                        contentColor = findContrastTextColor(color),
                        checkedContainerColor = color,
                        checkedContentColor = findContrastTextColor(color),
                    ),
                ) {
                    if (selected) Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        }
    }
}
