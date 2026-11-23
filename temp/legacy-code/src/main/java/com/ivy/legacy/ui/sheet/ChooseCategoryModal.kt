package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.ui.R
import com.ivy.ui.color.Ivy
import com.ivy.ui.color.Orange
import com.ivy.ui.color.Red
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Suppress("ParameterNaming")
@Composable
fun ChooseCategoryModal(
    visible: Boolean,
    initialCategory: Category?,
    categories: List<Category>,
    showCategoryModal: (Category?) -> Unit,
    onCategoryChanged: (Category?) -> Unit,
    id: UUID = remember { UUID.randomUUID() },
    dismiss: () -> Unit
) {
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory)
    }

    if (visible) {
        key(id) {
            ActionSheet(
                onDismiss = dismiss,
                title = stringResource(R.string.choose_category),
                contentScrollable = false,
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    OutlinedButton(
                        onClick = {
                            onCategoryChanged(selectedCategory)
                            dismissSheet()
                        }
                    ) {
                        Text(stringResource(R.string.skip))
                    }
                }
            ) { dismissSheet ->
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(Unit) { keyboard?.hide() }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(categories, key = { _, category -> category.id.value }) { index, category ->
                        val selected = category == selectedCategory
                        SegmentedListItem(
                            selected = selected,
                            onClick = {
                                selectedCategory = category
                                dismissSheet()
                                onCategoryChanged(category)
                            },
                            shapes = ListItemDefaults.segmentedShapes(
                                index = index,
                                count = categories.size + 1,
                            ),
                            modifier = Modifier.testTag("choose_category_button"),
                            leadingContent = {
                                IconBadge(
                                    color = Color(category.color.value),
                                    iconName = category.icon?.id,
                                    defaultIcon = R.drawable.ic_custom_category_m,
                                )
                            },
                            trailingContent = if (selected) {
                                {
                                    IconButton(
                                        onClick = {
                                            selectedCategory = null
                                            onCategoryChanged(null)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.RemoveCircleOutline,
                                            contentDescription = stringResource(R.string.deselect),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                            onLongClick = { showCategoryModal(category) },
                        ) {
                            Text(category.name.value)
                        }
                    }

                    item(key = "add_new") {
                        SegmentedListItem(
                            onClick = { showCategoryModal(null) },
                            shapes = ListItemDefaults.segmentedShapes(
                                index = categories.size,
                                count = categories.size + 1,
                            ),
                            leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                        ) {
                            Text(stringResource(R.string.add_new))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewChooseCategoryModal(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        val categories = mutableListOf(
            Category(
                name = NotBlankTrimmedString.unsafe("Test"),
                color = ColorInt(Ivy.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
            Category(
                name = NotBlankTrimmedString.unsafe("Second"),
                color = ColorInt(Orange.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
            Category(
                name = NotBlankTrimmedString.unsafe("Third"),
                color = ColorInt(Red.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
        )

        ChooseCategoryModal(
            visible = true,
            initialCategory = categories.first(),
            categories = categories,
            showCategoryModal = { },
            onCategoryChanged = { }
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun ChooseCategoryModalUiTest(isDark: Boolean) {
    PreviewChooseCategoryModal(if (isDark) Theme.DARK else Theme.LIGHT)
}
