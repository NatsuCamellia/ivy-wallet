package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import java.util.UUID

@Suppress("ParameterNaming")
@SuppressLint("ComposeModifierMissing")
@Composable
fun NameModal(
    visible: Boolean,
    name: String,
    dismiss: () -> Unit,
    id: UUID = remember { UUID.randomUUID() },
    onNameChanged: (String) -> Unit
) {
    if (visible) {
        key(id) {
            var modalName by remember { mutableStateOf(selectEndTextFieldValue(name)) }

            ActionSheet(
                onDismiss = dismiss,
                title = stringResource(R.string.edit_name),
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            onNameChanged(modalName.text)
                            dismissSheet()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            ) {
                OutlinedTextField(
                    value = modalName,
                    onValueChange = { modalName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    label = { Text(stringResource(R.string.what_is_your_name)) },
                    singleLine = true,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        NameModal(
            visible = true,
            name = "Iliyan",
            dismiss = {}
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun NameModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
