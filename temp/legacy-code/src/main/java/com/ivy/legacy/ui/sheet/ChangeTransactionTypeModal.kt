package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R
import java.util.UUID

@Suppress("ParameterNaming")
@SuppressLint("ComposeModifierMissing")
@Composable
fun ChangeTransactionTypeModal(
    visible: Boolean,
    includeTransferType: Boolean,
    initialType: TransactionType,
    dismiss: () -> Unit,
    title: String = stringResource(R.string.set_transaction_type),
    id: UUID = remember { UUID.randomUUID() },
    onTransactionTypeChanged: (TransactionType) -> Unit
) {
    if (visible) {
        key(id) {
            val types = if (includeTransferType) {
                TransactionType.entries
            } else {
                TransactionType.entries - TransactionType.TRANSFER
            }

            ActionSheet(
                onDismiss = dismiss,
                title = title,
            ) { dismissSheet ->
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    types.forEachIndexed { index, type ->
                        val selected = type == initialType
                        SegmentedListItem(
                            selected = selected,
                            onClick = {
                                dismissSheet()
                                onTransactionTypeChanged(type)
                            },
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = types.size),
                            modifier = Modifier.testTag("modal_type_${type.name}"),
                            leadingContent = {
                                Icon(
                                    imageVector = when (type) {
                                        TransactionType.INCOME -> Icons.Default.ArrowDownward
                                        TransactionType.EXPENSE -> Icons.Default.ArrowUpward
                                        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                                    },
                                    contentDescription = null,
                                )
                            },
                            trailingContent = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.selected_text),
                                    )
                                }
                            } else {
                                null
                            },
                        ) {
                            Text(
                                when (type) {
                                    TransactionType.INCOME -> stringResource(R.string.income)
                                    TransactionType.EXPENSE -> stringResource(R.string.expense)
                                    TransactionType.TRANSFER -> stringResource(R.string.transfer)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        ChangeTransactionTypeModal(
            includeTransferType = true,
            visible = true,
            initialType = TransactionType.INCOME,
            dismiss = {}
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun ChangeTransactionTypeModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
