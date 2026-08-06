package com.ivy.transaction.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionTopBar(
    title: String,
    onClose: () -> Unit,
    showDuplicateButton: Boolean,
    onDuplicate: () -> Unit,
    showDeleteButton: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        actions = {
            if (showDuplicateButton) {
                IconButton(onClick = onDuplicate) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.duplicate),
                    )
                }
            }
            if (showDeleteButton) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
        },
    )
}
