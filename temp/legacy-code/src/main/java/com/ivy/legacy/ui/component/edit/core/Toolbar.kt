package com.ivy.wallet.ui.edit.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.ivy.base.model.TransactionType
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    title: String,
    type: TransactionType,
    initialTransactionId: UUID?,
    onDeleteTrnModal: () -> Unit,
    onChangeTransactionTypeModal: () -> Unit,
    showDuplicateButton: Boolean,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = { BackButton(onClick = { nav.back() }) },
        actions = {
            if (type != TransactionType.TRANSFER) {
                IconButton(
                    onClick = onChangeTransactionTypeModal,
                    modifier = Modifier.testTag("change_type_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.set_transaction_type),
                    )
                }
            }

            if (initialTransactionId != null) {
                if (showDuplicateButton) {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.testTag("duplicate_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.duplicate),
                        )
                    }
                }

                IconButton(onClick = onDeleteTrnModal) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
        },
    )
}
