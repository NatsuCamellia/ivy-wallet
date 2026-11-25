package com.ivy.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.amountLabel
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
import com.ivy.ui.component.FlatListItem
import com.ivy.ui.component.StickyListHeader
import com.ivy.ui.component.TypeFilterChips
import com.ivy.ui.component.TypeFilterOption
import com.ivy.ui.component.screenMargin
import com.ivy.ui.component.transaction.TransactionAmountKind
import com.ivy.ui.component.transaction.amountColor
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.withFabClearance
import com.ivy.wallet.domain.data.SortOrder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.text.NumberFormat
import java.util.UUID

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
            CategoriesTopBar(
                period = state.period,
                onSort = { onEvent(CategoriesScreenEvent.OnSortOrderModalVisible(visible = true)) },
                onReorder = { onEvent(CategoriesScreenEvent.OnReorderModalVisible(true)) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        CategoriesList(
            state = state,
            listState = listState,
            contentPadding = innerPadding.withFabClearance(),
            onSelectType = { onEvent(CategoriesScreenEvent.OnTypeSelected(it)) },
        )
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

/** The current month as a small line above the large title, with Sort by and Reorder on the right. */
@Composable
private fun CategoriesTopBar(
    period: TimePeriod,
    onSort: () -> Unit,
    onReorder: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    LargeFlexibleTopAppBar(
        title = {
            Column {
                Text(
                    text = period.toDisplayShort(
                        startDateOfMonth = ivyWalletCtx().startDayOfMonth,
                        timeConverter = LocalTimeConverter.current,
                        timeProvider = LocalTimeProvider.current,
                        timeFormatter = LocalTimeFormatter.current,
                    ),
                    modifier = Modifier.testTag("categories_month_text"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(stringResource(R.string.categories))
            }
        },
        modifier = modifier,
        actions = {
            IconButton(onClick = onSort) {
                Icon(Icons.Default.SortByAlpha, contentDescription = stringResource(R.string.sort_by))
            }
            IconButton(onClick = onReorder, modifier = Modifier.testTag("reorder_button")) {
                Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.reorder))
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun CategoriesList(
    state: CategoriesScreenState,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onSelectType: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val content = state.content
    val onCategoryClick = { data: CategoryData ->
        nav.navigateTo(TransactionsScreen(accountId = null, categoryId = data.category.id.value))
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        if (state.categories.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    emptyStateTitle = stringResource(R.string.no_categories),
                    emptyStateText = stringResource(R.string.no_categories_description),
                    modifier = Modifier.screenMargin(),
                    icon = Icons.Outlined.Category,
                )
            }
            return@LazyColumn
        }
        item(key = "type_filter", contentType = "type_filter") {
            val options = typeOptions(
                content = content,
                currency = state.baseCurrency,
                expenseLabel = stringResource(R.string.expense),
                incomeLabel = stringResource(R.string.income),
            )
            TypeFilterChips(options = options, selected = state.type, onSelect = onSelectType)
        }
        items(content.active, key = { it.data.category.id.value }, contentType = { "category_row" }) { row ->
            CategoryItem(
                row = row,
                type = state.type,
                currency = state.baseCurrency,
                onClick = { onCategoryClick(row.data) },
            )
        }
        if (content.noRecords.isNotEmpty()) {
            stickyHeader(key = "header_no_records", contentType = "category_header") {
                StickyListHeader(title = stringResource(R.string.categories_no_records_this_month))
            }
            items(content.noRecords, key = { it.category.id.value }, contentType = { "no_records_row" }) { data ->
                NoRecordsItem(data = data, onClick = { onCategoryClick(data) })
            }
        }
    }
}

@Composable
private fun typeOptions(
    content: CategoriesContent,
    currency: String,
    expenseLabel: String,
    incomeLabel: String,
): ImmutableList<TypeFilterOption<CategoryType>> = remember(content, currency, expenseLabel, incomeLabel) {
    persistentListOf(
        TypeFilterOption(
            value = CategoryType.Expense,
            label = expenseLabel,
            number = amountLabel(content.expenseTotal, currency, shortenBigNumbers = true),
        ),
        TypeFilterOption(
            value = CategoryType.Income,
            label = incomeLabel,
            number = amountLabel(content.incomeTotal, currency, shortenBigNumbers = true),
        ),
    )
}

/** A category: icon, name and a share bar in its color, the amount and its share of the type's total. */
@Composable
private fun CategoryItem(
    row: CategoryRow,
    type: CategoryType,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = row.data.category
    val kind = when (type) {
        CategoryType.Expense -> TransactionAmountKind.Expense
        CategoryType.Income -> TransactionAmountKind.Income
    }
    FlatListItem(
        title = category.name.value,
        onClick = onClick,
        modifier = modifier.testTag("category_item"),
        primaryText = when (type) {
            CategoryType.Expense -> amountLabel(0.0 - row.amount, currency)
            CategoryType.Income -> amountLabel(row.amount, currency, showPlusSign = true)
        },
        primaryColor = kind.amountColor(),
        secondaryText = remember(row.percentage) { percentLabel(row.percentage) },
        leading = { CategoryBadge(category) },
        supportingContent = {
            LinearProgressIndicator(
                progress = { row.barFraction.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = Color(category.color.value),
                drawStopIndicator = {},
            )
        },
    )
}

private fun percentLabel(percentage: Double): String =
    NumberFormat.getPercentInstance().format(percentage / PercentScale)

/** A category with nothing recorded for the chosen type, dimmed. */
@Composable
private fun NoRecordsItem(data: CategoryData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FlatListItem(
        title = data.category.name.value,
        onClick = onClick,
        modifier = modifier.alpha(NoRecordsAlpha).testTag("category_no_records_item"),
        leading = { CategoryBadge(data.category) },
    )
}

private const val NoRecordsAlpha = 0.6f
private const val PercentScale = 100.0

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
    type: CategoryType = CategoryType.Expense,
    empty: Boolean = false,
    hugeAmountOnly: Boolean = false,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            val categories = previewCategories(empty = empty, hugeAmountOnly = hugeAmountOnly)
            CategoriesUi(
                state = CategoriesScreenState(
                    period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                    baseCurrency = "BGN",
                    categories = categories,
                    type = type,
                    content = mapCategories(categories, type, SortOrder.DEFAULT),
                )
            )
        }
    }
}

@Suppress("MagicNumber", "LongMethod")
private fun previewCategories(empty: Boolean, hugeAmountOnly: Boolean): ImmutableList<CategoryData> {
    fun category(name: String, color: Color, icon: String?, orderNum: Double) = Category(
        id = CategoryId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(color.toArgb()),
        icon = icon?.let { IconAsset.unsafe(it) },
        orderNum = orderNum,
    )

    val hugeAmount = CategoryData(
        category("Long-term savings and investments for the whole family", Color(0xFF6200EE), null, 5.0),
        monthlyBalance = 123456789012.34,
        monthlyExpenses = 98765432.1,
        monthlyIncome = 123456789.01
    )
    return when {
        empty -> persistentListOf()
        hugeAmountOnly -> persistentListOf(hugeAmount)
        else -> persistentListOf(
            CategoryData(category("Groceries", Green, "groceries", 0.0), 2125.0, 920.0, 3045.0),
            CategoryData(category("Fun", Orange, "game", 1.0), -750.0, 750.0, 0.0),
            CategoryData(category("Ivy", IvyDark, "star", 2.0), 5000.0, 0.0, 5000.0),
            CategoryData(category("Food", GreenLight, "atom", 3.0), 12125.21, 1350.50, 8000.48),
            CategoryData(category("Shisha", GreenDark, null, 4.0), 0.0, 0.0, 0.0),
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
fun CategoriesTabIncomeUiTest(isDark: Boolean) {
    PreviewCategories(theme = isDark.theme(), type = CategoryType.Income)
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
