package com.ivy.legacy.ui.component.tags

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import arrow.core.raise.either
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.sheet.ActionSheet
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.ui.color.Ivy
import java.time.Instant
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Composable
fun AddOrEditTagModal(
    id: TagId,
    modifier: Modifier = Modifier,
    visible: Boolean = false,
    initialTag: Tag? = null,
    onTagAdd: (String) -> Unit = {},
    onTagEdit: (oldTag: Tag, newTag: Tag) -> Unit = { _, _ -> },
    onTagDelete: (Tag) -> Unit = {},
    onDismiss: () -> Unit
) {
    if (visible) {
        key(id) {
            var titleTextFieldValue by remember {
                mutableStateOf(selectEndTextFieldValue(initialTag?.name?.value ?: ""))
            }

            ActionSheet(
                onDismiss = onDismiss,
                modifier = modifier,
                title = if (initialTag == null) "Tag Name" else "Edit Tag Name",
                actions = { dismissSheet ->
                    if (initialTag != null) {
                        IconButton(onClick = { onTagDelete(initialTag) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (initialTag != null) {
                                val updatedTag = either {
                                    initialTag.copy(
                                        name = NotBlankTrimmedString.from(titleTextFieldValue.text).bind()
                                    )
                                }.getOrNull()

                                if (updatedTag != null) {
                                    onTagEdit(initialTag, updatedTag)
                                }
                            } else {
                                onTagAdd(titleTextFieldValue.text)
                            }
                            dismissSheet()
                        },
                        enabled = titleTextFieldValue.text.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            ) {
                val titleFocus = remember { FocusRequester() }
                LaunchedEffect(Unit) { titleFocus.requestFocus() }

                OutlinedTextField(
                    value = titleTextFieldValue,
                    onValueChange = { titleTextFieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .focusRequester(titleFocus),
                    label = { Text("Enter TagName") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onNext = {}),
                )
            }
        }
    }
}

/** For screenshot testing */
@Composable
fun AddOrEditTagModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        AddOrEditTagModal(
            id = TagId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            visible = true,
            onDismiss = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun EditTagModalUiTest(isDark: Boolean) {
    val tag = Tag(
        id = TagId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        name = NotBlankTrimmedString.unsafe("Groceries"),
        description = null,
        color = ColorInt(Ivy.toArgb()),
        icon = null,
        orderNum = 0.0,
        creationTimestamp = Instant.parse("2026-01-15T12:00:00Z"),
    )
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        AddOrEditTagModal(
            id = tag.id,
            visible = true,
            initialTag = tag,
            onDismiss = {}
        )
    }
}
