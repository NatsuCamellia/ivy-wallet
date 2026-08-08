package com.ivy.ui.component.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.R

/** Tall enough that a multi-line field reads as a text area rather than a one-line box. */
private val MultiLineMinHeight = 120.dp

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = if (singleLine) {
                    Modifier
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MultiLineMinHeight)
                },
                // With singleLine off the field keeps ImeAction.Default, so Enter inserts a newline
                // instead of committing — the same affordance the legacy DescriptionModal had.
                singleLine = singleLine,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun TextInputDialogPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        TextInputDialog(
            title = "Name",
            initialValue = "Ivy",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun TextInputDialogMultiLinePreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        TextInputDialog(
            title = "Description",
            initialValue = "Weekly shop\nMilk, bread, coffee",
            onConfirm = {},
            onDismiss = {},
            singleLine = false,
        )
    }
}
