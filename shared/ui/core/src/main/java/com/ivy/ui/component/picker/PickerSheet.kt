package com.ivy.ui.component.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.R
import com.ivy.ui.component.transaction.CategoryIconBubble
import com.ivy.ui.component.transaction.TransactionItemPosition
import com.ivy.ui.component.transaction.toShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
@Suppress("DataClassTypedIDs")
data class PickerItemUi(
    val id: String,
    val title: String,
    val supportingText: String?,
    val color: Color?,
    val selected: Boolean,
)

@Composable
fun PickerContent(
    title: String,
    items: ImmutableList<PickerItemUi>,
    onItemClick: (String) -> Unit,
    addLabel: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (PickerItemUi) -> Unit = {},
) {
    val itemCount = items.size + 1
    Column(modifier = modifier.navigationBarsPadding()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        )
        PickerItemList(
            items = items,
            itemCount = itemCount,
            onItemClick = onItemClick,
            addLabel = addLabel,
            onAddClick = onAddClick,
            icon = icon,
        )
    }
}

@Composable
private fun PickerItemList(
    items: ImmutableList<PickerItemUi>,
    itemCount: Int,
    onItemClick: (String) -> Unit,
    addLabel: String,
    onAddClick: () -> Unit,
    icon: @Composable (PickerItemUi) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(items, key = { _, pickerItem -> pickerItem.id }) { index, pickerItem ->
            PickerRow(
                position = positionOf(index, itemCount),
                onClick = { onItemClick(pickerItem.id) },
                leadingColor = pickerItem.color,
                title = pickerItem.title,
                supportingText = pickerItem.supportingText,
                trailing = if (pickerItem.selected) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
                icon = { icon(pickerItem) },
            )
        }
        item {
            PickerRow(
                position = positionOf(items.size, itemCount),
                onClick = onAddClick,
                leadingColor = null,
                title = addLabel,
                supportingText = null,
                trailing = null,
                icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
            )
        }
    }
}

/** [index] and [count] span the full rendered list, including the trailing add-entry. */
private fun positionOf(index: Int, count: Int): TransactionItemPosition = when {
    count == 1 -> TransactionItemPosition.Single
    index == 0 -> TransactionItemPosition.First
    index == count - 1 -> TransactionItemPosition.Last
    else -> TransactionItemPosition.Middle
}

@Composable
private fun PickerRow(
    position: TransactionItemPosition,
    onClick: () -> Unit,
    leadingColor: Color?,
    title: String,
    supportingText: String?,
    trailing: @Composable (() -> Unit)?,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(position.toShape())
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconBubble(categoryColor = leadingColor, size = 32.dp, icon = icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(
    title: String,
    items: ImmutableList<PickerItemUi>,
    onItemClick: (String) -> Unit,
    addLabel: String,
    onAddClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (PickerItemUi) -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        PickerContent(
            title = title,
            items = items,
            onItemClick = onItemClick,
            addLabel = addLabel,
            onAddClick = onAddClick,
            icon = icon,
        )
    }
}

/**
 * A real category icon asset, at the 32dp intrinsic size the app's icon set ships. The stand-in
 * `Icons.Outlined.Circle` used before is 24dp, so it left room around itself no matter how the
 * bubble was sized and the golden could not catch an icon that swallowed its colour ring.
 */
@Composable
private fun PreviewCategoryIcon() {
    Icon(
        painter = painterResource(id = R.drawable.ic_custom_groceries_s),
        contentDescription = null,
    )
}

/** For screenshot testing */
@Composable
fun PickerUiTest(accounts: Boolean, modifier: Modifier = Modifier) {
    if (accounts) {
        PickerContent(
            title = "Accounts",
            items = PreviewAccountItems,
            onItemClick = {},
            addLabel = "New account",
            onAddClick = {},
            modifier = modifier,
        )
    } else {
        PickerContent(
            title = "Categories",
            items = PreviewCategoryItems,
            onItemClick = {},
            addLabel = "New category",
            onAddClick = {},
            modifier = modifier,
            icon = { PreviewCategoryIcon() },
        )
    }
}

private val PreviewCategoryItems = persistentListOf(
    PickerItemUi(
        id = "1",
        title = "Groceries, household and pet supplies",
        supportingText = null,
        color = Color(0xFF4CAF50),
        selected = false,
    ),
    PickerItemUi(
        id = "2",
        title = "Rent",
        supportingText = null,
        color = Color(0xFF2196F3),
        selected = true,
    ),
    PickerItemUi(
        id = "3",
        title = "Entertainment",
        supportingText = null,
        color = Color(0xFFFF9800),
        selected = false,
    ),
    PickerItemUi(
        id = "4",
        title = "Transport",
        supportingText = null,
        color = Color(0xFF9C27B0),
        selected = false,
    ),
)

private val PreviewAccountItems = persistentListOf(
    PickerItemUi(id = "1", title = "Cash", supportingText = "USD", color = null, selected = false),
    PickerItemUi(
        id = "2",
        title = "Revolut",
        supportingText = "EUR",
        color = null,
        selected = false,
    ),
    PickerItemUi(
        id = "3",
        title = "DSK Bank",
        supportingText = "BGN",
        color = null,
        selected = false,
    ),
)

@Preview
@Composable
private fun PickerContentCategoryPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            PickerContent(
                title = "Categories",
                items = PreviewCategoryItems,
                onItemClick = {},
                addLabel = "New category",
                onAddClick = {},
                icon = { PreviewCategoryIcon() },
            )
        }
    }
}

@Preview
@Composable
private fun PickerContentAccountPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            PickerContent(
                title = "Accounts",
                items = PreviewAccountItems,
                onItemClick = {},
                addLabel = "New account",
                onAddClick = {},
            )
        }
    }
}
