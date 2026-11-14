package com.ivy.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.color
import com.ivy.legacy.ui.component.signedAmount
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.sheet.ActionSheet
import com.ivy.legacy.ui.sheet.CategoryModal
import com.ivy.legacy.ui.sheet.CategoryModalData
import com.ivy.legacy.ui.sheet.ReorderModalSingleType
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.GreenDark
import com.ivy.ui.color.GreenLight
import com.ivy.ui.color.IvyDark
import com.ivy.ui.color.Orange
import com.ivy.ui.component.BackButton
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.domain.data.SortOrder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

@Composable
fun CategoriesScreen(screen: CategoriesScreen) {
    val viewModel: CategoriesViewModel = screenScopedViewModel()
    val state = viewModel.uiState()

    UI(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun UI(
    state: CategoriesScreenState,
    modifier: Modifier = Modifier,
    onEvent: (CategoriesScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    val ivyContext = ivyWalletCtx()
    var query by rememberSaveable { mutableStateOf("") }
    var listState = rememberLazyListState()
    if (!state.categories.isEmpty()) {
        listState = rememberScrollPositionListState(
            key = "categories_lazy_column",
            initialFirstVisibleItemIndex = ivyContext.categoriesListState?.firstVisibleItemIndex
                ?: 0,
            initialFirstVisibleItemScrollOffset = ivyContext.categoriesListState?.firstVisibleItemScrollOffset
                ?: 0
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
                actions = {
                    IconButton(
                        onClick = {
                            onEvent(CategoriesScreenEvent.OnSortOrderModalVisible(visible = true))
                        }
                    ) {
                        Icon(Icons.Default.SortByAlpha, contentDescription = stringResource(R.string.sort_by))
                    }
                    IconButton(
                        onClick = { onEvent(CategoriesScreenEvent.OnReorderModalVisible(true)) },
                        modifier = Modifier.testTag("reorder_button")
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.reorder))
                    }
                }
            )
        },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_category)
            ExtendedFloatingActionButton(
                onClick = {
                    onEvent(
                        CategoriesScreenEvent.OnCategoryModalVisible(
                            CategoryModalData(category = null)
                        )
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(addLabel) },
                modifier = Modifier
                    .testTag("categories_add")
                    .semantics { contentDescription = addLabel }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                // clears the add button that the Scaffold draws over the list
                bottom = innerPadding.calculateBottomPadding() + LocalMinimumInteractiveComponentSize.current * 2
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.showCategorySearchBar) {
                item(key = "search") {
                    SearchField(
                        query = query,
                        onQueryChange = {
                            query = it
                            onEvent(CategoriesScreenEvent.OnSearchQueryUpdate(it))
                        }
                    )
                }
            }
            if (state.categories.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emptyStateTitle = stringResource(R.string.no_categories),
                        emptyStateText = stringResource(
                            if (query.isEmpty()) {
                                R.string.no_categories_description
                            } else {
                                R.string.no_categories_for_search
                            }
                        ),
                        icon = Icons.Outlined.Category,
                    )
                }
            }
            items(state.categories, key = { it.category.id.value }) { categoryData ->
                CategoryCard(
                    currency = state.baseCurrency,
                    categoryData = categoryData,
                    compactModeEnabled = state.compactCategoriesModeEnabled,
                    onClick = {
                        nav.navigateTo(
                            TransactionsScreen(
                                accountId = null,
                                categoryId = categoryData.category.id.value
                            )
                        )
                    }
                )
            }
        }
    }

    ReorderModalSingleType(
        visible = state.reorderModalVisible,
        initialItems = state.categories,
        dismiss = {
            onEvent(CategoriesScreenEvent.OnReorderModalVisible(false))
        },
        onReordered = {
            onEvent(CategoriesScreenEvent.OnReorder(it))
        }
    ) { _, item ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryBadge(item.category)
            Text(text = item.category.name.value, style = MaterialTheme.typography.bodyLarge)
        }
    }

    CategoryModal(
        modal = state.categoryModalData,
        onCreateCategory = {
            onEvent(CategoriesScreenEvent.OnCreateCategory(it))
        },
        onEditCategory = { },
        dismiss = {
            onEvent(CategoriesScreenEvent.OnCategoryModalVisible(null))
        }
    )

    SortModal(
        initialType = state.sortOrder,
        items = state.sortOrderItems,
        visible = state.sortModalVisible,
        dismiss = {
            onEvent(CategoriesScreenEvent.OnSortOrderModalVisible(visible = false))
        },
        onSortOrderChange = {
            onEvent(CategoriesScreenEvent.OnReorder(state.categories, it))
        }
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_categories)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                }
            }
        } else {
            null
        },
        singleLine = true,
    )
}

@Composable
private fun CategoryCard(
    currency: String,
    categoryData: CategoryData,
    compactModeEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { CategoryBadge(categoryData.category) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            NameAndBalance(categoryData, currency)
        }

        if (!compactModeEnabled) {
            Row {
                LabelledAmount(
                    label = stringResource(R.string.month_income),
                    amount = categoryData.monthlyIncome,
                    currency = currency,
                    amountColor = TransactionType.INCOME.color(),
                    showPlusSign = true,
                    modifier = Modifier.weight(1f)
                )
                LabelledAmount(
                    label = stringResource(R.string.month_expenses),
                    amount = signedAmount(TransactionType.EXPENSE, categoryData.monthlyExpenses),
                    currency = currency,
                    amountColor = TransactionType.EXPENSE.color(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** The name and the balance share a line; when the balance is too wide for that it moves to its own line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameAndBalance(categoryData: CategoryData, currency: String) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = categoryData.category.name.value,
            modifier = Modifier.weight(1f)
        )
        AmountText(
            amount = categoryData.monthlyBalance,
            currency = currency,
            style = MaterialTheme.typography.titleMediumEmphasized,
            showPlusSign = true,
        )
    }
}

@Composable
private fun LabelledAmount(
    label: String,
    amount: Double,
    currency: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
    showPlusSign: Boolean = false,
) {
    ListItem(
        modifier = modifier,
        overlineContent = { Text(label) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        AmountText(
            amount = amount,
            currency = currency,
            style = MaterialTheme.typography.titleSmall.copy(color = amountColor),
            showPlusSign = showPlusSign,
        )
    }
}

@Composable
private fun CategoryBadge(category: Category) {
    IconBadge(
        color = Color(category.color.value),
        iconName = category.icon?.id,
        defaultIcon = R.drawable.ic_custom_category_m,
    )
}

@Composable
fun SortModal(
    items: ImmutableList<SortOrder>,
    visible: Boolean,
    initialType: SortOrder,
    dismiss: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.sort_by),
    id: UUID = remember { UUID.randomUUID() }
) {
    if (visible) {
        key(id) {
            ActionSheet(
                onDismiss = dismiss,
                modifier = modifier,
                title = title,
            ) { dismissSheet ->
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    items.forEachIndexed { index, sortOrder ->
                        val selected = sortOrder == initialType
                        SegmentedListItem(
                            selected = selected,
                            onClick = {
                                dismissSheet()
                                onSortOrderChange(sortOrder)
                            },
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = items.size),
                            leadingContent = {
                                Icon(
                                    imageVector = when (sortOrder) {
                                        SortOrder.DEFAULT -> Icons.Default.Star
                                        SortOrder.BALANCE_AMOUNT -> Icons.Default.Paid
                                        SortOrder.EXPENSES -> Icons.Default.ArrowUpward
                                        SortOrder.ALPHABETICAL -> Icons.Default.SortByAlpha
                                    },
                                    contentDescription = null,
                                )
                            },
                            trailingContent = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.selected_text),
                                    )
                                }
                            } else {
                                null
                            },
                        ) {
                            Text(sortOrder.displayName)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewCategories(
    theme: Theme = Theme.LIGHT,
    compactModeEnabled: Boolean = false,
    searchBarEnabled: Boolean = false,
    empty: Boolean = false,
    hugeAmountOnly: Boolean = false,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(
                state = CategoriesScreenState(
                    baseCurrency = "BGN",
                    compactCategoriesModeEnabled = compactModeEnabled,
                    showCategorySearchBar = searchBarEnabled,
                    categories = previewCategories(empty = empty, hugeAmountOnly = hugeAmountOnly),
                )
            )
        }
    }
}

@Suppress("MagicNumber", "LongMethod")
private fun previewCategories(empty: Boolean, hugeAmountOnly: Boolean): ImmutableList<CategoryData> {
    fun category(name: String, color: Color, icon: String?) = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(color.toArgb()),
        icon = icon?.let { IconAsset.unsafe(it) },
        orderNum = 0.0,
    )

    val hugeAmount = CategoryData(
        category("Long-term savings and investments for the whole family", Color(0xFF6200EE), null),
        monthlyBalance = 123456789012.34,
        monthlyExpenses = 98765432.1,
        monthlyIncome = 123456789.01
    )
    return when {
        empty -> persistentListOf()
        hugeAmountOnly -> persistentListOf(hugeAmount)
        else -> persistentListOf(
            CategoryData(category("Groceries", Green, "groceries"), 2125.0, 920.0, 3045.0),
            CategoryData(category("Fun", Orange, "game"), -750.0, 750.0, 0.0),
            CategoryData(category("Ivy", IvyDark, "star"), 5000.0, 0.0, 5000.0),
            CategoryData(category("Food", GreenLight, "atom"), 12125.21, 1350.50, 8000.48),
            CategoryData(category("Shisha", GreenDark, null), 0.0, 0.0, 0.0),
            hugeAmount,
        )
    }
}

/** For screenshot testing */
@Composable
fun CategoriesScreenUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme())
}

/** For screenshot testing */
@Composable
fun CategoriesScreenWithSearchBarUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme(), searchBarEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenCompactUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme(), compactModeEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenWithSearchBarCompactUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme(), compactModeEnabled = true, searchBarEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenEmptyUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme(), empty = true)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenHugeAmountUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme(), hugeAmountOnly = true)
}

private fun Boolean.theme(): Theme = if (this) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun SortModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        SortModal(
            items = SortOrder.entries.toImmutableList(),
            visible = true,
            initialType = SortOrder.DEFAULT,
            dismiss = {},
            onSortOrderChange = {}
        )
    }
}
