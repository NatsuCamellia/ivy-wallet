package com.ivy.ui.component.dialog

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList

/** Picks one of [options]; a tap on a row selects it and the caller decides when to dismiss. */
@Composable
fun RadioSelectionDialog(
    title: String,
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = { Text(text = title) },
        text = {
            LazyColumn(modifier = Modifier.selectableGroup()) {
                itemsIndexed(options) { index, option ->
                    ListItem(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        leadingContent = { RadioButton(selected = index == selectedIndex, onClick = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) {
                        Text(text = option)
                    }
                }
            }
        },
    )
}
