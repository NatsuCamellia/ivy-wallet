package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R

@SuppressLint("ComposeModifierMissing")
@Composable
fun DeleteModal(
    title: String,
    description: String,
    visible: Boolean,
    dismiss: () -> Unit,
    buttonText: String = stringResource(R.string.delete),
    onDelete: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(buttonText)
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            title = { Text(title) },
            text = { Text(description) },
        )
    }
}

@SuppressLint("ComposeModifierMissing")
@Composable
fun ConfirmModal(
    title: String,
    description: String,
    visible: Boolean,
    dismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            title = { Text(title) },
            text = { Text(description) },
        )
    }
}

@SuppressLint("ComposeModifierMissing")
@Composable
fun DeleteConfirmationModal(
    title: String,
    description: String,
    visible: Boolean,
    enableDeletionButton: Boolean,
    onAccountNameChange: (String) -> Unit,
    dismiss: () -> Unit,
    hint: String = stringResource(id = R.string.account_name),
    buttonText: String = stringResource(R.string.delete),
    onDelete: () -> Unit,
) {
    if (visible) {
        var deletionTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        val keyboard = LocalSoftwareKeyboardController.current

        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    enabled = enableDeletionButton,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(buttonText)
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(description)

                    OutlinedTextField(
                        value = deletionTextFieldValue,
                        onValueChange = {
                            deletionTextFieldValue = it
                            onAccountNameChange(it.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(hint) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text,
                            autoCorrect = true
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    )
                }
            },
        )
    }
}

/** For screenshot testing */
@Composable
fun DeleteModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        DeleteModal(
            title = "Delete account?",
            description = "This will delete the account and all of its transactions.",
            visible = true,
            dismiss = {},
            onDelete = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun DeleteConfirmationModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        DeleteConfirmationModal(
            title = "Delete account?",
            description = "Type the account name to confirm the deletion.",
            visible = true,
            enableDeletionButton = false,
            onAccountNameChange = {},
            dismiss = {},
            onDelete = {}
        )
    }
}
