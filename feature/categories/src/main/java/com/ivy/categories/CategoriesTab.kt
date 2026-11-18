package com.ivy.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
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
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.OverviewAmount
import com.ivy.legacy.ui.component.OverviewCard
import com.ivy.legacy.ui.component.TonalSummaryCard
import com.ivy.legacy.ui.component.color
import com.ivy.legacy.ui.component.signedAmount
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.sheet.ActionSheet
import com.ivy.legacy.ui.sheet.CategoryModal
import com.ivy.legacy.ui.sheet.ReorderModalSingleType
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
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.withFabClearance
import com.ivy.wallet.domain.data.SortOrder
import java.util.UUID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun CategoriesTab() {
    val viewModel: CategoriesViewModel = screenScopedViewModel()
    val state = viewModel.uiState()

    CategoriesUi(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun CategoriesUi(
    state: CategoriesScreenState,
    modifier: Modifier = Modifier,
    onEvent: (CategoriesScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    val ivyContext = ivyWalletCtx()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.categories)) },
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
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            state = listState,
            contentPadding = innerPadding.withFabClearance(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.categories.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emptyStateTitle = stringResource(R.string.no_categories),
                        emptyStateText = stringResource(R.string.no_categories_description),
                        icon = Icons.Outlined.Category,
                    )
                }
            } else {
                item(key = "totals") {
                    CategoryTotalCards(state)
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
private fun CategoryTotalCards(state: CategoriesScreenState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TonalSummaryCard(
            label = stringResource(R.string.month_expenses),
            amount = signedAmount(TransactionType.EXPENSE, state.totalMonthlyExpenses),
            currency = state.baseCurrency,
            icon = Icons.Default.ArrowUpward,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        TonalSummaryCard(
            label = stringResource(R.string.month_income),
            amount = state.totalMonthlyIncome,
            currency = state.baseCurrency,
            icon = Icons.Default.ArrowDownward,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            showPlusSign = true,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun CategoryCard(
    currency: String,
    categoryData: CategoryData,
    compactModeEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverviewCard(
        name = categoryData.category.name.value,
        badge = { CategoryBadge(categoryData.category) },
        balance = categoryData.monthlyBalance,
        currency = currency,
        onClick = onClick,
        modifier = modifier,
        showPlusSign = true,
        footer = if (compactModeEnabled) {
            persistentListOf()
        } else {
            persistentListOf(
                OverviewAmount(
                    label = stringResource(R.string.month_expenses),
                    amount = signedAmount(TransactionType.EXPENSE, categoryData.monthlyExpenses),
                    color = TransactionType.EXPENSE.color(),
                ),
                OverviewAmount(
                    label = stringResource(R.string.month_income),
                    amount = categoryData.monthlyIncome,
                    color = TransactionType.INCOME.color(),
                    showPlusSign = true,
                ),
            )
        },
    )
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
    empty: Boolean = false,
    hugeAmountOnly: Boolean = false,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            CategoriesUi(
                state = CategoriesScreenState(
                    baseCurrency = "BGN",
                    compactCategoriesModeEnabled = compactModeEnabled,
                    totalMonthlyExpenses = if (empty) 0.0 else 3360.5,
                    totalMonthlyIncome = if (empty) 0.0 else 16445.48,
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
fun CategoriesTabUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme())
}

/** For screenshot testing */
@Composable
fun CategoriesTabCompactUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme(), compactModeEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesTabEmptyUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme(), empty = true)
}

/** For screenshot testing */
@Composable
fun CategoriesTabHugeAmountUiTest(isDark: Boolean) {
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
