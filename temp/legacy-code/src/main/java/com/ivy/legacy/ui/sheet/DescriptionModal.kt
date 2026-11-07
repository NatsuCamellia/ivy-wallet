package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Suppress("ParameterNaming")
@Composable
fun DescriptionModal(
    id: UUID = remember { UUID.randomUUID() },
    visible: Boolean,
    description: String?,

    onDescriptionChanged: (String?) -> Unit,
    dismiss: () -> Unit,
) {
    var descTextFieldValue by remember(description) {
        mutableStateOf(selectEndTextFieldValue(description))
    }

    if (visible) {
        key(id) {
            ActionSheet(
                onDismiss = dismiss,
                title = stringResource(R.string.description),
                actions = { dismissSheet ->
                    val keyboard = LocalSoftwareKeyboardController.current

                    Spacer(Modifier.weight(1f))

                    when {
                        description.isNullOrBlank() -> Button(
                            onClick = {
                                onDescriptionChanged(descTextFieldValue.text)
                                keyboard?.hide()
                                dismissSheet()
                            },
                            modifier = Modifier.testTag("modal_desc_save"),
                        ) {
                            Text(stringResource(R.string.add))
                        }

                        description == descTextFieldValue.text -> Button(
                            onClick = {
                                onDescriptionChanged(null)
                                keyboard?.hide()
                                dismissSheet()
                            },
                            modifier = Modifier.testTag("modal_desc_delete"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(stringResource(R.string.delete))
                        }

                        else -> Button(
                            onClick = {
                                onDescriptionChanged(descTextFieldValue.text)
                                keyboard?.hide()
                                dismissSheet()
                            },
                            modifier = Modifier.testTag("modal_desc_save"),
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            ) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = descTextFieldValue,
                        onValueChange = { descTextFieldValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .testTag("modal_desc_input"),
                        placeholder = { Text(stringResource(R.string.description_text_field_hint)) },
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = true,
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        keyboardActions = KeyboardActions(
                            onAny = {
                                descTextFieldValue = descTextFieldValue.copy(
                                    text = StringBuilder(descTextFieldValue.text)
                                        .insert(descTextFieldValue.selection.end, "\n")
                                        .toString(),
                                    selection = TextRange(descTextFieldValue.selection.end + 1)
                                )
                            }
                        ),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewDescriptionModal_emptyText(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        DescriptionModal(
            visible = true,
            description = "",
            onDescriptionChanged = {}
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun DescriptionModalUiTest(isDark: Boolean) {
    PreviewDescriptionModal_emptyText(if (isDark) Theme.DARK else Theme.LIGHT)
}
