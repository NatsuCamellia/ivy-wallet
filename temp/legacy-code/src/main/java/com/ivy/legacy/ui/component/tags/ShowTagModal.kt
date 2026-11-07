package com.ivy.legacy.ui.component.tags

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.mapper.TagMapper.Companion.createNewTagId
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.sheet.ActionSheet
import com.ivy.legacy.ui.sheet.DeleteModal
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.ui.color.Ivy
import java.time.Instant
import java.util.UUID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Suppress("ParameterNaming")
@SuppressLint("ComposeModifierMissing")
@Composable
fun ShowTagModal(
    onDismiss: () -> Unit,
    allTagList: ImmutableList<Tag>,
    selectedTagList: ImmutableList<TagId>,
    onTagAdd: (String) -> Unit,
    onTagEdit: (oldTag: Tag, newTag: Tag) -> Unit,
    onTagDelete: (Tag) -> Unit,
    onTagSelected: (Tag) -> Unit,
    onTagDeSelected: (Tag) -> Unit,
    modifier: Modifier = Modifier,
    id: UUID = remember { UUID.randomUUID() },
    visible: Boolean = false,
    selectOnlyMode: Boolean = false,
    onTagSearch: (String) -> Unit
) {
    var showTagAddModal by remember {
        mutableStateOf(false)
    }

    var deleteTagModalVisible by remember {
        mutableStateOf(false)
    }

    var selectedTag by remember {
        mutableStateOf<Tag?>(null)
    }

    var selectedTagId by remember(selectedTag) {
        mutableStateOf(selectedTag?.id ?: createNewTagId())
    }

    if (visible) {
        key(id) {
            var searchQueryTextFieldValue by remember {
                mutableStateOf(selectEndTextFieldValue(""))
            }

            ActionSheet(
                onDismiss = onDismiss,
                modifier = modifier,
                title = stringResource(R.string.tags),
                contentScrollable = false,
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(onClick = dismissSheet) {
                        Text(stringResource(R.string.done))
                    }
                }
            ) {
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(Unit) { keyboard?.hide() }

                OutlinedTextField(
                    value = searchQueryTextFieldValue,
                    onValueChange = {
                        searchQueryTextFieldValue = it
                        onTagSearch(it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text(stringResource(R.string.search_tags)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                )

                TagList(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(top = 8.dp),
                    transactionTags = allTagList,
                    selectedTagList = selectedTagList,
                    selectOnlyMode = selectOnlyMode,
                    onAddNewTag = {
                        showTagAddModal = true
                    },
                    onTagSelect = onTagSelected,
                    onTagDeselect = onTagDeSelected,
                    onTagLongClick = {
                        if (!selectOnlyMode) {
                            selectedTag = it
                            showTagAddModal = true
                        }
                    }
                )
            }
        }
    }

    AddOrEditTagModal(
        id = selectedTagId,
        initialTag = selectedTag,
        visible = showTagAddModal,
        onDismiss = {
            showTagAddModal = false
            selectedTag = null
            selectedTagId = createNewTagId()
        },
        onTagAdd = onTagAdd,
        onTagDelete = {
            deleteTagModalVisible = true
        },
        onTagEdit = { oldTag, newTag ->
            onTagEdit(oldTag, newTag)
        }
    )

    DeleteModal(
        visible = deleteTagModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = "Are you sure you want to delete the following tag:\t'${selectedTag?.name?.value}' ?",
        dismiss = { deleteTagModalVisible = false }
    ) {
        if (selectedTag != null) {
            deleteTagModalVisible = false
            onTagDelete(selectedTag!!)
            showTagAddModal = false
            selectedTag = null
        }
    }
}

@Composable
private fun TagList(
    transactionTags: ImmutableList<Tag>,
    onAddNewTag: () -> Unit,
    selectedTagList: ImmutableList<TagId>,
    selectOnlyMode: Boolean,
    modifier: Modifier = Modifier,
    onTagSelect: (Tag) -> Unit = {},
    onTagDeselect: (Tag) -> Unit = {},
    onTagLongClick: (Tag) -> Unit = {}
) {
    val offset = if (selectOnlyMode) 0 else 1
    val count = transactionTags.size + offset

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        if (!selectOnlyMode) {
            item(key = "add_new") {
                SegmentedListItem(
                    onClick = onAddNewTag,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = count),
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                ) {
                    Text(stringResource(R.string.add_new))
                }
            }
        }

        itemsIndexed(transactionTags, key = { _, tag -> tag.id.value }) { index, tag ->
            val selected = selectedTagList.contains(tag.id)
            SegmentedListItem(
                selected = selected,
                onClick = { onTagSelect(tag) },
                shapes = ListItemDefaults.segmentedShapes(index = index + offset, count = count),
                modifier = Modifier.testTag("choose_category_button"),
                trailingContent = if (selected) {
                    {
                        IconButton(onClick = { onTagDeselect(tag) }) {
                            Icon(
                                Icons.Default.RemoveCircleOutline,
                                contentDescription = stringResource(R.string.deselect),
                            )
                        }
                    }
                } else {
                    null
                },
                onLongClick = { onTagLongClick(tag) },
            ) {
                Text(
                    text = "#${tag.name.value}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** For screenshot testing */
@Composable
fun ShowTagModalUiTest(isDark: Boolean) {
    val tags = listOf("Groceries", "Travel", "Work", "Gifts").mapIndexed { index, name ->
        Tag(
            id = TagId(UUID.fromString("00000000-0000-0000-0000-00000000000${index + 1}")),
            name = NotBlankTrimmedString.unsafe(name),
            description = null,
            color = ColorInt(Ivy.toArgb()),
            icon = null,
            orderNum = index.toDouble(),
            creationTimestamp = Instant.parse("2026-01-15T12:00:00Z"),
        )
    }
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        ShowTagModal(
            visible = true,
            onDismiss = {},
            allTagList = tags.toImmutableList(),
            selectedTagList = persistentListOf(tags[1].id),
            onTagAdd = {},
            onTagEdit = { _, _ -> },
            onTagDelete = {},
            onTagSelected = {},
            onTagDeSelected = {},
            onTagSearch = {}
        )
    }
}
