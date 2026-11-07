package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.IntervalType
import com.ivy.legacy.forDisplay
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.selectEndTextFieldValue

private const val RepeatIntervalCharLimit = 5

@Composable
fun IntervalPickerRow(
    intervalN: Int,
    intervalType: IntervalType,

    onSetIntervalN: (Int) -> Unit,
    onSetIntervalType: (IntervalType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var interNTextFieldValue by remember(intervalN) {
            mutableStateOf(selectEndTextFieldValue(intervalN.toString()))
        }

        OutlinedTextField(
            value = interNTextFieldValue,
            onValueChange = {
                val filteredText = it.text.take(RepeatIntervalCharLimit)
                if (it.text != interNTextFieldValue.text) {
                    try {
                        onSetIntervalN(filteredText.toInt())
                    } catch (e: Exception) {
                    }
                }
                interNTextFieldValue = it.copy(text = filteredText)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("base_number_input"),
            placeholder = { Text("0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            IntervalType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = type == intervalType,
                    onClick = { onSetIntervalType(type) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = IntervalType.entries.size,
                    ),
                    icon = {},
                ) {
                    Text(type.forDisplay(intervalN).capitalizeLocal(), maxLines = 1)
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        IntervalPickerRow(
            intervalN = 1,
            intervalType = IntervalType.WEEK,
            onSetIntervalN = {},
            onSetIntervalType = {}
        )
    }
}
