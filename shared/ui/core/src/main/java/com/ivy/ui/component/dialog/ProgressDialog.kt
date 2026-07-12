package com.ivy.ui.component.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.ivy.design.system.IvyMaterial3Theme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressDialog(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        icon = { LoadingIndicator() },
        title = { Text(text = title) },
        text = { Text(text = description) },
        confirmButton = {},
    )
}

@Preview
@Composable
private fun ProgressDialogPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        ProgressDialog(title = "Exporting data", description = "Please wait…")
    }
}
