package com.ivy.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.budgets.model.DisplayBudget
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.datamodel.Budget
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.sheet.ReorderModalSingleType
import com.ivy.legacy.utils.format
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.withFabClearance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

@Composable
fun BudgetScreen(screen: BudgetScreen) {
    val viewModel: BudgetViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun UI(
    state: BudgetScreenState,
    modifier: Modifier = Modifier,
    onEvent: (BudgetScreenEvent) -> Unit = {}
) {
    val nav = navigation()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budgets)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
                actions = {
                    IconButton(
                        onClick = { onEvent(BudgetScreenEvent.OnReorderModalVisible(true)) },
                        modifier = Modifier.testTag("reorder_button")
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.reorder))
                    }
                }
            )
        },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_budget)
            ExtendedFloatingActionButton(
                onClick = {
                    onEvent(
                        BudgetScreenEvent.OnBudgetModalData(
                            BudgetModalData(
                                budget = null,
                                baseCurrency = state.baseCurrency,
                                categories = state.categories,
                                accounts = state.accounts
                            )
                        )
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(addLabel) },
                modifier = Modifier
                    .testTag("budgets_add")
                    .semantics { contentDescription = addLabel }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding.withFabClearance(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "info") {
                BudgetInfo(
                    timeRange = state.timeRange,
                    totalRemainingBudgetText = state.totalRemainingBudgetText,
                    baseCurrency = state.baseCurrency,
                    appBudgetMax = state.appBudgetMax,
                    categoryBudgetsTotal = state.categoryBudgetsTotal,
                )
            }

            if (state.budgets.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emptyStateTitle = stringResource(R.string.no_budgets),
                        emptyStateText = stringResource(R.string.no_budgets_text),
                        icon = Icons.Outlined.Savings,
                    )
                }
            }

            items(state.budgets, key = { it.budget.id.toString() }) { item ->
                BudgetItem(
                    displayBudget = item,
                    baseCurrency = state.baseCurrency,
                    onClick = {
                        onEvent(
                            BudgetScreenEvent.OnBudgetModalData(
                                BudgetModalData(
                                    budget = item.budget,
                                    baseCurrency = state.baseCurrency,
                                    categories = state.categories,
                                    accounts = state.accounts,
                                    autoFocusKeyboard = false
                                )
                            )
                        )
                    }
                )
            }
        }
    }

    ReorderModalSingleType(
        visible = state.reorderModalVisible,
        initialItems = state.budgets,
        dismiss = {
            onEvent(BudgetScreenEvent.OnReorderModalVisible(false))
        },
        onReordered = { onEvent(BudgetScreenEvent.OnReorder(it)) }
    ) { _, item ->
        Text(text = item.budget.name, style = MaterialTheme.typography.bodyLarge)
    }

    BudgetModal(
        modal = state.budgetModalData,
        onCreate = { onEvent(BudgetScreenEvent.OnCreateBudget(it)) },
        onEdit = { onEvent(BudgetScreenEvent.OnEditBudget(it)) },
        onDelete = { onEvent(BudgetScreenEvent.OnDeleteBudget(it)) },
        dismiss = {
            onEvent(BudgetScreenEvent.OnBudgetModalData(null))
        }
    )
}

@Composable
private fun BudgetInfo(
    timeRange: FromToTimeRange?,
    totalRemainingBudgetText: String?,
    baseCurrency: String,
    appBudgetMax: Double,
    categoryBudgetsTotal: Double,
    modifier: Modifier = Modifier,
) {
    val categoryBudgetText = if (categoryBudgetsTotal > 0) {
        stringResource(R.string.for_categories, categoryBudgetsTotal.format(baseCurrency), baseCurrency)
    } else {
        ""
    }
    val appBudgetMaxText = if (appBudgetMax > 0) {
        stringResource(R.string.app_budget, appBudgetMax.format(baseCurrency), baseCurrency)
    } else {
        ""
    }
    val hasBothBudgetTypes = categoryBudgetText.isNotBlank() && appBudgetMaxText.isNotBlank()

    Column(modifier = modifier) {
        if (timeRange != null) {
            Text(
                text = timeRange.toDisplay(LocalTimeFormatter.current),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (categoryBudgetsTotal > 0 || appBudgetMax > 0) {
            Text(
                text = if (hasBothBudgetTypes) {
                    stringResource(R.string.budget_info_both, categoryBudgetText, appBudgetMaxText)
                } else {
                    stringResource(R.string.budget_info, categoryBudgetText, appBudgetMaxText)
                },
                modifier = Modifier.testTag("budgets_info_text"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalRemainingBudgetText != null) {
                Text(
                    text = totalRemainingBudgetText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BudgetItem(
    displayBudget: DisplayBudget,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val limit = displayBudget.budget.amount
    val spent = displayBudget.spentAmount
    val exceeded = spent > limit
    val statusColor = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            supportingContent = {
                Text(determineBudgetType(displayBudget.budget.parseCategoryIds().size))
            },
        ) {
            Text(displayBudget.budget.name)
        }

        if (limit > 0) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(if (exceeded) R.string.budget_exceeded_by else R.string.left_to_spend),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor
                )
                AmountText(
                    amount = abs(limit - spent),
                    currency = baseCurrency,
                    style = MaterialTheme.typography.headlineSmallEmphasized.copy(color = statusColor),
                )
                LinearWavyProgressIndicator(
                    progress = { (spent / limit).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AmountText(amount = spent, currency = baseCurrency, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "/", style = MaterialTheme.typography.bodyMedium)
                    AmountText(amount = limit, currency = baseCurrency, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun PreviewBudgets(
    theme: Theme = Theme.LIGHT,
    budgets: ImmutableList<DisplayBudget> = previewBudgets(),
    showInfo: Boolean = true,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(
                state = BudgetScreenState(
                    baseCurrency = "BGN",
                    budgets = budgets,
                    categories = persistentListOf(),
                    accounts = persistentListOf(),
                    categoryBudgetsTotal = if (showInfo) 750.0 else 0.0,
                    appBudgetMax = if (showInfo) 1000.0 else 0.0,
                    totalRemainingBudgetText = if (showInfo) "Remaining: 419.50 BGN" else null,
                    timeRange = FromToTimeRange(
                        from = LocalDate.of(2023, 4, 1).atTime(12, 0).toInstant(ZoneOffset.UTC),
                        to = LocalDate.of(2023, 4, 30).atTime(12, 0).toInstant(ZoneOffset.UTC),
                    ),
                    reorderModalVisible = false,
                    budgetModalData = null
                )
            )
        }
    }
}

private fun displayBudget(name: String, limit: Double, spent: Double, order: Double): DisplayBudget = DisplayBudget(
    budget = Budget(
        name = name,
        amount = limit,
        categoryIdsSerialized = null,
        accountIdsSerialized = null,
        orderId = order,
    ),
    spentAmount = spent
)

@Suppress("MagicNumber")
private fun previewBudgets(): ImmutableList<DisplayBudget> = persistentListOf(
    displayBudget("Groceries", limit = 600.0, spent = 420.5, order = 0.0),
    displayBudget("Entertainment", limit = 150.0, spent = 160.0, order = 1.0),
)

@Suppress("MagicNumber")
private fun progressBudgets(): ImmutableList<DisplayBudget> = persistentListOf(
    displayBudget("Untouched", limit = 400.0, spent = 0.0, order = 0.0),
    displayBudget("Halfway", limit = 400.0, spent = 200.0, order = 1.0),
    displayBudget("Exactly used", limit = 400.0, spent = 400.0, order = 2.0),
)

@Suppress("MagicNumber")
private fun hugeAmountBudgets(): ImmutableList<DisplayBudget> = persistentListOf(
    displayBudget(
        name = "Long-term savings and investments for the whole family",
        limit = 123456789012.34,
        spent = 98765432109.87,
        order = 0.0
    ),
)

private fun Boolean.theme(): Theme = if (this) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun BudgetScreenUiTest(isDark: Boolean) {
    PreviewBudgets(theme = isDark.theme())
}

/** For screenshot testing */
@Composable
fun BudgetScreenProgressUiTest(isDark: Boolean) {
    PreviewBudgets(theme = isDark.theme(), budgets = progressBudgets(), showInfo = false)
}

/** For screenshot testing */
@Composable
fun BudgetScreenEmptyUiTest(isDark: Boolean) {
    PreviewBudgets(theme = isDark.theme(), budgets = persistentListOf(), showInfo = false)
}

/** For screenshot testing */
@Composable
fun BudgetScreenHugeAmountUiTest(isDark: Boolean) {
    PreviewBudgets(theme = isDark.theme(), budgets = hugeAmountBudgets(), showInfo = false)
}
