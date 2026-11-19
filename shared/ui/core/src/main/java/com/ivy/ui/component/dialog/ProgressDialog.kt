package com.ivy.ui.component.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/** A dialog that blocks the screen while work runs: it cannot be dismissed and has no buttons. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressDialog(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        modifier = modifier,
        icon = { LoadingIndicator() },
        title = { Text(text = title) },
        text = { Text(text = description) },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}
