package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountItem
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import java.util.UUID
import kotlin.math.abs

data class BufferModalData(
    val balance: Double,
    val buffer: Double,
    val currency: String,
    val id: UUID = UUID.randomUUID()
)

@Suppress("ParameterNaming")
@SuppressLint("ComposeModifierMissing")
@Composable
fun BufferModal(
    modal: BufferModalData?,
    dismiss: () -> Unit,
    onBufferChanged: (Double) -> Unit
) {
    var newBufferAmount by remember(modal) {
        mutableStateOf(modal?.buffer ?: 0.0)
    }

    var amountModalVisible by remember { mutableStateOf(false) }

    if (modal != null) {
        key(modal.id) {
            ActionSheet(
                onDismiss = dismiss,
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            onBufferChanged(newBufferAmount)
                            dismissSheet()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BufferStatus(
                        buffer = newBufferAmount,
                        balance = modal.balance,
                        currency = modal.currency,
                    )

                    AmountItem(
                        label = stringResource(R.string.edit_savings_goal),
                        currency = modal.currency,
                        amount = newBufferAmount,
                        index = 0,
                        count = 1,
                        onClick = { amountModalVisible = true },
                    )
                }
            }
        }
    }

    val amountModalId = remember(modal, newBufferAmount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = modal?.currency ?: "",
        initialAmount = newBufferAmount,
        dismiss = { amountModalVisible = false }
    ) {
        newBufferAmount = it
    }
}

@Composable
fun BufferStatus(
    buffer: Double,
    balance: Double,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val exceeded = balance < buffer

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(if (exceeded) R.string.buffer_exceeded_by else R.string.left_to_spend),
            style = MaterialTheme.typography.labelLarge,
        )

        Text(
            text = "${abs(balance - buffer).format(currency)} $currency",
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )

        LinearWavyProgressIndicator(
            progress = { if (balance > 0.0) (buffer / balance).toFloat().coerceIn(0f, 1f) else 1f },
            modifier = Modifier.fillMaxWidth(),
            color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

/** For screenshot testing */
@Composable
fun BufferModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        BufferModal(
            modal = BufferModalData(balance = 1250.0, buffer = 500.0, currency = "BGN"),
            dismiss = {},
            onBufferChanged = {}
        )
    }
}
