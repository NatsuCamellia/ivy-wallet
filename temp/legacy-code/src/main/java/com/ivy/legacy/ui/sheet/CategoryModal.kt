package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import arrow.core.raise.either
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.ColorPicker
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.ui.color.Ivy
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import java.util.UUID

data class CategoryModalData(
    val category: Category?,
    val id: UUID = UUID.randomUUID(),
    val autoFocusKeyboard: Boolean = true,
)

@SuppressLint("ComposeModifierMissing")
@Composable
fun CategoryModal(
    modal: CategoryModalData?,
    onCreateCategory: (CreateCategoryData) -> Unit,
    onEditCategory: (Category) -> Unit,
    dismiss: () -> Unit,
) {
    val initialCategory = modal?.category
    var nameTextFieldValue by remember(modal) {
        mutableStateOf(selectEndTextFieldValue(initialCategory?.name?.value))
    }
    var color by remember(modal) {
        mutableStateOf(initialCategory?.color?.let { Color(it.value) } ?: Ivy)
    }
    var icon by remember(modal) {
        mutableStateOf(initialCategory?.icon)
    }

    var chooseIconModalVisible by remember(modal) {
        mutableStateOf(false)
    }

    if (modal != null) {
        key(modal.id) {
            ActionSheet(
                onDismiss = dismiss,
                title = if (initialCategory != null) {
                    stringResource(R.string.edit_category)
                } else {
                    stringResource(R.string.create_category)
                },
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (initialCategory != null) {
                                val updatedCategory = either {
                                    initialCategory.copy(
                                        name = NotBlankTrimmedString.from(nameTextFieldValue.text.trim()).bind(),
                                        color = ColorInt(color.toArgb()),
                                        icon = icon
                                    )
                                }.getOrNull()

                                if (updatedCategory != null) {
                                    onEditCategory(updatedCategory)
                                }
                            } else {
                                onCreateCategory(
                                    CreateCategoryData(
                                        name = nameTextFieldValue.text.trim(),
                                        color = color,
                                        icon = icon?.id
                                    )
                                )
                            }

                            dismissSheet()
                        },
                        enabled = nameTextFieldValue.text.isNotNullOrBlank(),
                        modifier = if (initialCategory == null) Modifier.testTag("modal_add") else Modifier,
                    ) {
                        Text(stringResource(if (initialCategory != null) R.string.save else R.string.add))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    IconNameRow(
                        hint = stringResource(R.string.category_name),
                        defaultIcon = R.drawable.ic_custom_category_m,
                        color = color,
                        icon = icon?.id,

                        autoFocusKeyboard = modal.autoFocusKeyboard,

                        nameTextFieldValue = nameTextFieldValue,
                        setNameTextFieldValue = { nameTextFieldValue = it },
                        showChooseIconModal = {
                            chooseIconModalVisible = true
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    ColorPicker(
                        selectedColor = color,
                        onColorSelect = { color = it }
                    )
                }
            }
        }
    }

    ChooseIconModal(
        visible = chooseIconModalVisible,
        initialIcon = icon?.id ?: "category",
        color = color,
        dismiss = { chooseIconModalVisible = false }
    ) {
        icon = it?.let { iconId -> IconAsset.unsafe(iconId) }
    }
}

@Composable
fun IconNameRow(
    hint: String,
    @DrawableRes defaultIcon: Int,
    color: Color,
    icon: String?,

    autoFocusKeyboard: Boolean,

    nameTextFieldValue: TextFieldValue,
    setNameTextFieldValue: (TextFieldValue) -> Unit,

    showChooseIconModal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val chooseIconLabel = stringResource(R.string.choose_icon)
    LaunchedEffect(Unit) {
        if (autoFocusKeyboard) {
            nameFocus.requestFocus()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            color = color,
            iconName = icon,
            defaultIcon = defaultIcon,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button) { showChooseIconModal() }
                .semantics { contentDescription = chooseIconLabel }
                .testTag("modal_item_icon"),
        )

        OutlinedTextField(
            value = nameTextFieldValue,
            onValueChange = setNameTextFieldValue,
            modifier = Modifier
                .weight(1f)
                .focusRequester(nameFocus)
                .testTag("base_input"),
            label = { Text(hint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
                autoCorrect = true
            ),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        )
    }
}

@Preview
@Composable
private fun PreviewCategoryModal(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        CategoryModal(
            modal = CategoryModalData(null),
            onCreateCategory = { },
            onEditCategory = { }
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun CategoryModalUiTest(isDark: Boolean) {
    PreviewCategoryModal(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun EditCategoryModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        CategoryModal(
            modal = CategoryModalData(
                Category(
                    name = NotBlankTrimmedString.unsafe("Groceries"),
                    color = ColorInt(Ivy.toArgb()),
                    icon = null,
                    id = CategoryId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                    orderNum = 0.0,
                )
            ),
            onCreateCategory = { },
            onEditCategory = { }
        ) {
        }
    }
}
