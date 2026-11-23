package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
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
fun AddKeywordModal(
    keyword: String,
    visible: Boolean,
    dismiss: () -> Unit,
    id: UUID = remember { UUID.randomUUID() },
    onKeywordChanged: (String) -> Unit
) {
    if (visible) {
        key(id) {
            var modalKeyword by remember { mutableStateOf(selectEndTextFieldValue(keyword)) }

            ActionSheet(
                onDismiss = dismiss,
                title = stringResource(R.string.add_keyword),
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            onKeywordChanged(modalKeyword.text)
                            dismissSheet()
                        },
                        modifier = Modifier.testTag("modal_add"),
                    ) {
                        Text(stringResource(R.string.add))
                    }
                }
            ) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }

                OutlinedTextField(
                    value = modalKeyword,
                    onValueChange = { modalKeyword = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .focusRequester(focus),
                    label = { Text(stringResource(R.string.keyword)) },
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
        AddKeywordModal(
            visible = true,
            keyword = "",
            dismiss = {}
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun AddKeywordModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
