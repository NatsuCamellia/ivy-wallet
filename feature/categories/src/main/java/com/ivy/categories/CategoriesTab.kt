package com.ivy.categories

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ui.SearchInput
import com.ivy.legacy.utils.balancePrefix
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.domain.data.SortOrder
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientGreen
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.GreenDark
import com.ivy.wallet.ui.theme.GreenLight
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.CircleButtonFilled
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.ReorderButton
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSet
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.ivy.wallet.ui.theme.toComposeColor
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Composable
fun BoxWithConstraintsScope.CategoriesTab() {
    val viewModel: CategoriesViewModel = screenScopedViewModel()
    val state = viewModel.uiState()

    UI(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: CategoriesScreenState = CategoriesScreenState(
        compactCategoriesModeEnabled = false,
        showCategorySearchBar = false,
        totalMonthlyExpenses = 0.0,
        totalMonthlyIncome = 0.0
    ),
    onEvent: (CategoriesScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    val ivyContext = com.ivy.legacy.ivyWalletCtx()
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        state = listState
    ) {
        item {
            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(24.dp))

                Text(
                    text = stringResource(R.string.categories),
                    style = UI.typo.b1.style(
                        color = UI.colors.pureInverse,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.weight(1f))

                CircleButtonFilled(
                    icon = R.drawable.ic_sort_by_alpha_24,
                    onClick = {
                        onEvent(CategoriesScreenEvent.OnSortOrderModalVisible(visible = true))
                    },
                    clickAreaPadding = 12.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                ReorderButton {
                    onEvent(CategoriesScreenEvent.OnReorderModalVisible(true))
                }

                Spacer(Modifier.width(24.dp))
            }

            Spacer(Modifier.height(16.dp))
            CategoryTotalsCards(
                currency = state.baseCurrency,
                totalMonthlyExpenses = state.totalMonthlyExpenses,
                totalMonthlyIncome = state.totalMonthlyIncome
            )
            Spacer(Modifier.height(16.dp))

            if (state.showCategorySearchBar) {
                Spacer(Modifier.height(16.dp))
                SearchField(onSearch = { onEvent(CategoriesScreenEvent.OnSearchQueryUpdate(it)) })
                Spacer(Modifier.height(16.dp))
            }
        }

        items(state.categories, key = { it.category.id.value }) { categoryData ->
            Spacer(Modifier.height(16.dp))
            DefaultCategoryCard(
                onClick = {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = null,
                            categoryId = categoryData.category.id.value
                        )
                    )
                },
                categoryData = categoryData,
                currency = state.baseCurrency,
                compactModeEnabled = state.compactCategoriesModeEnabled
            )
        }

        item {
            Spacer(Modifier.height(150.dp)) // scroll hack
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
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.category.name.value,
            style = UI.typo.b1.style(
                color = item.category.color.value.toComposeColor(),
                fontWeight = FontWeight.Bold
            )
        )
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
private fun CategoryTotalsCards(
    currency: String,
    totalMonthlyExpenses: Double,
    totalMonthlyIncome: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        CategoryTotalCard(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            label = stringResource(R.string.month_expenses),
            currency = currency,
            amount = totalMonthlyExpenses
        )

        Spacer(Modifier.width(12.dp))

        CategoryTotalCard(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            label = stringResource(R.string.month_income),
            currency = currency,
            amount = totalMonthlyIncome
        )

        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun RowScope.CategoryTotalCard(
    containerColor: Color,
    label: String,
    currency: String,
    amount: Double,
) {
    val contentColor = contentColorFor(containerColor)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(16.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            AmountCurrencyB1(
                amount = amount,
                currency = currency,
                textColor = contentColor,
                shortenBigNumbers = true
            )

            Spacer(Modifier.width(4.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun DefaultCategoryCard(
    onClick: () -> Unit,
    categoryData: CategoryData,
    currency: String,
    compactModeEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(MaterialTheme.colorScheme.surfaceContainer, UI.shapes.r4)
            .clickable(
                onClick = onClick
            )
    ) {
        CategoryHeader(
            categoryData = categoryData,
            currency = currency
        )

        if (!compactModeEnabled) {
            Spacer(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(UI.colors.pureInverse.copy(alpha = 0.1f))
            )

            Spacer(Modifier.height(12.dp))

            AddedSpent(
                currency = currency,
                monthlyIncome = categoryData.monthlyIncome,
                monthlyExpenses = categoryData.monthlyExpenses,
                dividerColor = UI.colors.pureInverse.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AddedSpent(
    monthlyIncome: Double,
    monthlyExpenses: Double,
    currency: String,
    modifier: Modifier = Modifier,
    textColor: Color = UI.colors.pureInverse,
    dividerColor: Color = UI.colors.medium,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))

        LabelAmount(
            textColor = textColor,
            label = stringResource(R.string.month_expenses),
            amount = monthlyExpenses,
            currency = currency
        )

        Spacer(Modifier.weight(1f))

        // Divider
        Spacer(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(dividerColor, UI.shapes.rFull)
        )

        Spacer(Modifier.weight(1f))

        LabelAmount(
            textColor = textColor,
            label = stringResource(R.string.month_income),
            amount = monthlyIncome,
            currency = currency
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LabelAmount(
    label: String,
    amount: Double,
    currency: String,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = UI.typo.c.style(
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AmountCurrencyB1(
                textColor = textColor,
                amount = amount,
                currency = currency
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    categoryData: CategoryData,
    currency: String,
) {
    val category = categoryData.category
    val categoryColor = category.color.value.toComposeColor()
    val iconTint = findContrastTextColor(categoryColor)
    val balancePrefixValue = balancePrefix(
        income = categoryData.monthlyIncome,
        expenses = categoryData.monthlyExpenses
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor),
                contentAlignment = Alignment.Center
            ) {
                ItemIconSDefaultIcon(
                    iconName = category.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_s,
                    tint = iconTint
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                modifier = Modifier.weight(1f),
                text = category.name.value,
                style = UI.typo.b1.style(
                    color = UI.colors.pureInverse,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(16.dp))
        }

        Spacer(Modifier.height(12.dp))

        BalanceRow(
            modifier = Modifier.align(Alignment.CenterHorizontally),

            currency = currency,
            balance = categoryData.monthlyBalance,

            balanceFontSize = 30.sp,
            currencyFontSize = 30.sp,

            currencyUpfront = false,
            balanceAmountPrefix = balancePrefixValue
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Suppress("UnusedParameter")
@Composable
fun BoxWithConstraintsScope.SortModal(
    items: ImmutableList<SortOrder>,
    visible: Boolean,
    initialType: SortOrder,
    dismiss: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.sort_by),
    id: UUID = UUID.randomUUID()
) {
    var sortOrder by remember(initialType) {
        mutableStateOf(initialType)
    }

    val applyChange = {
        onSortOrderChange(sortOrder)
        dismiss()
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSet {
                applyChange()
            }
        },
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = title)

        Spacer(Modifier.height(32.dp))

        items.forEach {
            SelectTypeButton(
                text = it.displayName,
                icon = when (it) {
                    SortOrder.DEFAULT -> R.drawable.ic_custom_star_s
                    SortOrder.BALANCE_AMOUNT -> R.drawable.ic_vue_money_coins
                    SortOrder.EXPENSES -> R.drawable.ic_expense
                    SortOrder.ALPHABETICAL -> R.drawable.ic_sort_by_alpha_24
                },
                selected = it == sortOrder
            ) {
                sortOrder = it
                applyChange()
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SelectTypeButton(
    text: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    selectedGradient: Gradient = GradientGreen,
    textSelectedColor: Color = White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(UI.shapes.r4)
            .background(
                brush = if (selected) selectedGradient.asHorizontalBrush() else SolidColor(UI.colors.medium),
                shape = UI.shapes.r4
            )
            .clickable {
                onClick()
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        val textColor = if (selected) textSelectedColor else UI.colors.pureInverse

        IvyIcon(
            icon = icon,
            tint = textColor,
            modifier = Modifier.fillMaxHeight()
        )

        Spacer(Modifier.width(12.dp))

        Text(
            modifier = Modifier.wrapContentHeight(),
            text = text,
            style = UI.typo.b1.style(
                color = textColor
            ),
            textAlign = TextAlign.Center,
        )

        if (selected) {
            Spacer(Modifier.weight(1f))

            IvyIcon(
                icon = R.drawable.ic_check,
                tint = textSelectedColor
            )

            Text(
                text = stringResource(R.string.selected_text),
                style = UI.typo.b2.style(
                    fontWeight = FontWeight.SemiBold,
                    color = textSelectedColor
                )
            )

            Spacer(Modifier.width(24.dp))
        }
    }
}

@Preview
@Composable
private fun PreviewCategoriesCompactModeEnabled(theme: Theme = Theme.LIGHT) {
    Preview(theme = theme, compactModeEnabled = true)
}

@Preview
@Composable
private fun PreviewCategoriesCompactModeEnabledAndSearchBarEnabled(theme: Theme = Theme.LIGHT) {
    Preview(theme = theme, compactModeEnabled = true, displaySearchBarEnabled = true)
}

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    compactModeEnabled: Boolean = false,
    displaySearchBarEnabled: Boolean = false
) {
    IvyWalletPreview(theme) {
        val state = CategoriesScreenState(
            baseCurrency = "BGN",
            compactCategoriesModeEnabled = compactModeEnabled,
            showCategorySearchBar = displaySearchBarEnabled,
            totalMonthlyExpenses = 3360.50,
            totalMonthlyIncome = 16445.48,
            categories = persistentListOf(
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Groceries"),
                        color = ColorInt(Green.toArgb()),
                        icon = IconAsset.unsafe("groceries"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 2125.0,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Fun"),
                        color = ColorInt(Orange.toArgb()),
                        icon = IconAsset.unsafe("game"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 0.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Ivy"),
                        color = ColorInt(IvyDark.toArgb()),
                        icon = IconAsset.unsafe("star"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 0.0,
                    monthlyIncome = 5000.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Food"),
                        color = ColorInt(GreenLight.toArgb()),
                        icon = IconAsset.unsafe("atom"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 12125.21,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(GreenDark.toArgb()),
                        icon = IconAsset.unsafe("drink"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 820.0,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),

                )
        )
        UI(state = state)
    }
}

@Preview
@Composable
private fun PreviewWithSearchBarEnabled(
    theme: Theme = Theme.LIGHT,
    compactModeEnabled: Boolean = false,
    displaySearchBarEnabled: Boolean = true
) {
    IvyWalletPreview(theme) {
        val state = CategoriesScreenState(
            baseCurrency = "BGN",
            compactCategoriesModeEnabled = compactModeEnabled,
            showCategorySearchBar = displaySearchBarEnabled,
            totalMonthlyExpenses = 3360.50,
            totalMonthlyIncome = 16445.48,
            categories = persistentListOf(
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Groceries"),
                        color = ColorInt(Green.toArgb()),
                        icon = IconAsset.unsafe("groceries"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 2125.0,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Fun"),
                        color = ColorInt(Orange.toArgb()),
                        icon = IconAsset.unsafe("game"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 0.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Ivy"),
                        color = ColorInt(IvyDark.toArgb()),
                        icon = IconAsset.unsafe("star"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 0.0,
                    monthlyIncome = 5000.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Food"),
                        color = ColorInt(GreenLight.toArgb()),
                        icon = IconAsset.unsafe("atom"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 12125.21,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(GreenDark.toArgb()),
                        icon = IconAsset.unsafe("drink"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 820.0,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),

                )
        )
        UI(state = state)
    }
}

@Composable
private fun SearchField(
    onSearch: (String) -> Unit,
) {
    var searchQueryTextFieldValue by remember {
        mutableStateOf(selectEndTextFieldValue(""))
    }

    SearchInput(
        searchQueryTextFieldValue = searchQueryTextFieldValue,
        hint = "Search categories",
        focus = false,
        showClearIcon = searchQueryTextFieldValue.text.isNotEmpty(),
        onSetSearchQueryTextField = {
            searchQueryTextFieldValue = it
            onSearch(it.text)
        }
    )
}

/** For screenshot testing */
@Composable
fun CategoriesTabUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}

/** For screenshot testing */
@Composable
fun CategoriesTabWithSearchBarUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme = theme, displaySearchBarEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesTabCompactUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme, compactModeEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesTabWithSearchBarCompactUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme, compactModeEnabled = true, displaySearchBarEnabled = true)
}