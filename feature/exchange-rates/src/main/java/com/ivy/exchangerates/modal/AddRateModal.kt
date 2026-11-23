package com.ivy.exchangerates.modal

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.exchangerates.RatesEvent
import com.ivy.exchangerates.data.RateUi
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.sheet.ActionSheet
import com.ivy.legacy.ui.sheet.AmountModal
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Composable
fun AddRateModal(
    visible: Boolean,
    baseCurrency: String,
    dismiss: () -> Unit,
    onAdd: (RatesEvent.AddRate) -> Unit,
) {
    if (!visible) return

    var toCurrency by remember { mutableStateOf(selectEndTextFieldValue("")) }
    var amountModalVisible by remember { mutableStateOf(false) }
    var rate by remember { mutableStateOf<Double?>(null) }

    ActionSheet(
        onDismiss = dismiss,
        title = stringResource(R.string.add_manual_exchange_rate),
        actions = { dismissSheet ->
            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    onAdd(
                        RatesEvent.AddRate(
                            RateUi(
                                from = baseCurrency,
                                to = toCurrency.text,
                                rate = rate ?: 0.0,
                            )
                        )
                    )
                    dismissSheet()
                },
            ) {
                Text(stringResource(R.string.add))
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = toCurrency,
                onValueChange = { toCurrency = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.currency_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
            )

            SegmentedListItem(
                onClick = { amountModalVisible = true },
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                overlineContent = { Text(stringResource(R.string.exchange_rate)) },
            ) {
                Text(
                    text = "$baseCurrency-${toCurrency.text} = ${rate ?: "???"}",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
            }
        }
    }

    AmountModal(
        id = remember { UUID.randomUUID() },
        visible = amountModalVisible,
        currency = "",
        initialAmount = rate,
        decimalCountMax = 12,
        dismiss = { amountModalVisible = false },
        onAmountChanged = {
            rate = it
        }
    )
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        AddRateModal(
            visible = true,
            baseCurrency = "USD",
            dismiss = {},
            onAdd = {},
        )
    }
}

/** For screenshot testing */
@Composable
fun AddRateModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
