package com.ivy.piechart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.PeriodSelector
import com.ivy.legacy.ui.component.color
import com.ivy.legacy.ui.component.signedAmount
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.sheet.ChoosePeriodModal
import com.ivy.legacy.utils.format
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.color.Gray
import com.ivy.ui.color.Green
import com.ivy.ui.color.IvyDark
import com.ivy.ui.color.IvyLight
import com.ivy.ui.color.Orange
import com.ivy.ui.color.Red
import com.ivy.ui.color.RedLight
import com.ivy.ui.component.BackButton
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

@Composable
fun PieChartStatisticScreen(
    screen: PieChartStatisticScreen
) {
    val viewModel: PieChartStatisticViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(PieChartStatisticEvent.OnStart(screen))
    }

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Suppress("LongMethod")
@Composable
private fun UI(
    state: PieChartStatisticState,
    onEvent: (PieChartStatisticEvent) -> Unit = {}
) {
    val nav = navigation()
    val listState = rememberScrollPositionListState(key = "item_pie_chart_lazy_column")
    val type = state.transactionType
    val categoryAmounts = state.categoryAmounts.filter { it.amount != 0.0 }
    val gutter = Modifier.padding(horizontal = 16.dp)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (type == TransactionType.EXPENSE) R.string.expenses else R.string.income
                        ),
                        modifier = Modifier.testTag("piechart_title"),
                    )
                },
                navigationIcon = { BackButton(onClick = { nav.back() }, modifier = Modifier.testTag("toolbar_close")) },
                actions = {
                    if (!state.showCloseButtonOnly) {
                        IconButton(
                            onClick = {
                                nav.navigateTo(EditTransactionScreen(initialTransactionId = null, type = type))
                            },
                            modifier = Modifier.testTag("piechart_add"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(
                                    if (type == TransactionType.EXPENSE) R.string.add_expense else R.string.add_income
                                ),
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AmountText(
                        amount = signedAmount(type, state.totalAmount),
                        currency = state.baseCurrency,
                        modifier = gutter.testTag("piechart_total_amount"),
                        style = MaterialTheme.typography.displaySmallEmphasized.copy(color = type.color()),
                        showPlusSign = type == TransactionType.INCOME,
                    )

                    if (!state.showCloseButtonOnly) {
                        PeriodSelector(
                            period = state.period,
                            onPreviousMonth = { onEvent(PieChartStatisticEvent.OnSelectPreviousMonth) },
                            onNextMonth = { onEvent(PieChartStatisticEvent.OnSelectNextMonth) },
                            onShowChoosePeriodModal = {
                                onEvent(PieChartStatisticEvent.OnShowMonthModal(state.period))
                            },
                        )
                    }
                }
            }

            if (categoryAmounts.isEmpty()) {
                item {
                    EmptyState(
                        emptyStateTitle = stringResource(R.string.no_transactions),
                        emptyStateText = emptyText(state),
                    )
                }
            } else {
                item {
                    PieChart(
                        type = type,
                        categoryAmounts = state.categoryAmounts,
                        selectedCategory = state.selectedCategory,
                        modifier = Modifier.padding(vertical = 16.dp),
                        onCategoryClick = { onEvent(PieChartStatisticEvent.OnCategoryClicked(it)) },
                    )
                }

                itemsIndexed(categoryAmounts) { index, item ->
                    CategoryAmountRow(
                        categoryAmount = item,
                        type = type,
                        currency = state.baseCurrency,
                        totalAmount = state.totalAmount,
                        selected = state.selectedCategory != null && item.category == state.selectedCategory.category,
                        index = index,
                        count = categoryAmounts.size,
                        modifier = gutter,
                        onClick = {
                            nav.navigateTo(
                                TransactionsScreen(
                                    categoryId = item.category?.id?.value,
                                    unspecifiedCategory = item.isCategoryUnspecified,
                                    accountIdFilterList = state.accountIdFilterList,
                                    transactions = item.associatedTransactions
                                )
                            )
                        },
                    )
                }
            }
        }
    }

    ChoosePeriodModal(
        modal = state.choosePeriodModal,
        dismiss = {
            onEvent(PieChartStatisticEvent.OnShowMonthModal(null))
        }
    ) {
        onEvent(PieChartStatisticEvent.OnSetPeriod(it))
    }
}

@Composable
private fun emptyText(state: PieChartStatisticState): String {
    if (state.showCloseButtonOnly) return stringResource(R.string.no_transactions_for_your_filter)
    return stringResource(
        R.string.no_transactions_description,
        state.period.toDisplayLong(
            startDateOfMonth = ivyWalletCtx().startDayOfMonth,
            timeProvider = LocalTimeProvider.current,
            timeConverter = LocalTimeConverter.current,
            timeFormatter = LocalTimeFormatter.current,
        )
    )
}

@Suppress("LongParameterList")
@Composable
private fun CategoryAmountRow(
    categoryAmount: CategoryAmount,
    type: TransactionType,
    currency: String,
    totalAmount: Double,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = categoryAmount.category

    SegmentedListItem(
        onClick = onClick,
        selected = selected,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.testTag("piechart_category_row"),
        leadingContent = {
            IconBadge(
                color = category?.color?.value?.let(::Color) ?: Gray,
                iconName = category?.icon?.id,
                defaultIcon = R.drawable.ic_custom_category_m,
            )
        },
        supportingContent = { PercentText(amount = categoryAmount.amount, totalAmount = totalAmount) },
        trailingContent = {
            AmountText(
                amount = signedAmount(type, categoryAmount.amount),
                currency = currency,
                style = MaterialTheme.typography.titleMedium.copy(color = type.color()),
                showPlusSign = type == TransactionType.INCOME,
            )
        },
    ) {
        Text(
            text = category?.name?.value ?: stringResource(R.string.unspecified),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun percentLabel(amount: Double, totalAmount: Double): String =
    stringResource(R.string.percent, if (totalAmount != 0.0) (amount / totalAmount * PERCENT).format(2) else "0")

@Composable
private fun PercentText(
    amount: Double,
    totalAmount: Double,
    modifier: Modifier = Modifier,
) {
    Text(
        text = percentLabel(amount, totalAmount),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
    )
}

private const val PERCENT = 100

private fun previewCategory(name: String, color: Int, icon: String? = null): Category = Category(
    name = NotBlankTrimmedString.unsafe(name),
    color = ColorInt(color),
    icon = icon?.let { IconAsset.unsafe(it) },
    id = CategoryId(UUID.randomUUID()),
    orderNum = 0.0,
)

private val previewCategoryAmounts = listOf(
    CategoryAmount(previewCategory("Bills", Green.toArgb(), "bills"), 791.0),
    CategoryAmount(category = null, amount = 497.0, isCategoryUnspecified = true),
    CategoryAmount(previewCategory("Shisha", Orange.toArgb(), "trees"), 411.93),
    CategoryAmount(previewCategory("Food & Drink", IvyDark.toArgb()), 260.03),
    CategoryAmount(previewCategory("Gifts", RedLight.toArgb()), 160.0),
    CategoryAmount(previewCategory("Clothes & Jewelery Fancy", Red.toArgb()), 2.0),
    CategoryAmount(previewCategory("Finances, Burocracy & Governance", IvyLight.toArgb(), "work"), 2.0),
)

private fun previewState(
    type: TransactionType,
    selected: Int? = null,
    empty: Boolean = false,
): PieChartStatisticState {
    val amounts = if (empty) emptyList() else previewCategoryAmounts
    val selectedAmount = selected?.let { amounts[it] }
    return PieChartStatisticState(
        transactionType = type,
        period = TimePeriod(month = Month.monthsList().first(), year = 2023),
        baseCurrency = "BGN",
        totalAmount = amounts.sumOf { it.amount },
        categoryAmounts = (listOfNotNull(selectedAmount) + amounts.filter { it != selectedAmount }).toImmutableList(),
        selectedCategory = selectedAmount?.category?.let { SelectedCategory(it) },
        accountIdFilterList = persistentListOf(),
        choosePeriodModal = null,
        filterExcluded = false,
        showCloseButtonOnly = false,
        transactions = persistentListOf()
    )
}

@Composable
private fun PieChartPreview(
    theme: Theme,
    type: TransactionType = TransactionType.EXPENSE,
    selected: Int? = null,
    empty: Boolean = false,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(state = previewState(type, selected, empty))
        }
    }
}

@Preview
@Composable
private fun Preview_Expense(theme: Theme = Theme.LIGHT) {
    PieChartPreview(theme)
}

@Preview
@Composable
private fun Preview_Income(theme: Theme = Theme.LIGHT) {
    PieChartPreview(theme, TransactionType.INCOME)
}

@Preview
@Composable
private fun Preview_Selected(theme: Theme = Theme.LIGHT) {
    PieChartPreview(theme, selected = 2)
}

@Preview
@Composable
private fun Preview_Empty(theme: Theme = Theme.LIGHT) {
    PieChartPreview(theme, empty = true)
}

private fun Boolean.theme(): Theme = if (this) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun PieChartStatisticUiTest(isDark: Boolean) {
    Preview_Expense(isDark.theme())
}

/** For screenshot testing */
@Composable
fun PieChartStatisticIncomeUiTest(isDark: Boolean) {
    Preview_Income(isDark.theme())
}

/** For screenshot testing */
@Composable
fun PieChartStatisticSelectedUiTest(isDark: Boolean) {
    Preview_Selected(isDark.theme())
}

/** For screenshot testing */
@Composable
fun PieChartStatisticEmptyUiTest(isDark: Boolean) {
    Preview_Empty(isDark.theme())
}
