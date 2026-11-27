package com.ivy.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.stringRes
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.SummaryCard
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.component.transaction.transactions
import com.ivy.legacy.utils.format
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.GreenDark
import com.ivy.ui.color.GreenLight
import com.ivy.ui.color.IvyDark
import com.ivy.ui.color.Purple1Dark
import com.ivy.ui.color.Red3Light
import com.ivy.ui.component.BackButton
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.absoluteValue

@Composable
fun ReportScreen(screen: ReportScreen) {
    val viewModel: ReportViewModel = viewModel()
    val state = viewModel.uiState()

    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@Suppress("LongMethod")
@Composable
private fun UI(
    state: ReportScreenState = ReportScreenState(),
    onEventHandler: (ReportScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    val context = LocalContext.current
    val listState = rememberScrollPositionListState(key = "reports")

    val showFilter = {
        onEventHandler(ReportScreenEvent.OnFilterOverlayVisible(filterOverlayVisible = true))
    }
    val openPieChart = { type: TransactionType ->
        if (state.transactions.isNotEmpty()) {
            nav.navigateTo(
                PieChartStatisticScreen(
                    type = type,
                    transactions = state.transactions.toImmutableList(),
                    accountList = state.accountIdFilters,
                    treatTransfersAsIncomeExpense = state.treatTransfersAsIncExp
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reports)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
                actions = {
                    IconButton(
                        onClick = { onEventHandler(ReportScreenEvent.OnExport(context = context)) },
                        modifier = Modifier.testTag("report_export"),
                        enabled = !state.loading,
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.export))
                    }
                    IconButton(
                        onClick = showFilter,
                        modifier = Modifier.testTag("report_filter"),
                        enabled = !state.loading,
                    ) {
                        BadgedBox(badge = { if (state.filter != null) Badge() }) {
                            Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = innerPadding,
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Balance(currency = state.baseCurrency, balance = state.balance)

                        IncomeExpenses(state = state, onSummaryClick = openPieChart)

                        if (state.showTransfersAsIncExpCheckbox) {
                            TransfersAsIncomeExpense(
                                checked = state.treatTransfersAsIncExp,
                                onCheckedChange = {
                                    onEventHandler(
                                        ReportScreenEvent.OnTreatTransfersAsIncomeExpense(
                                            transfersAsIncomeExpense = it
                                        )
                                    )
                                },
                            )
                        }
                    }
                }

                if (state.filter != null) {
                    transactions(
                        baseData = AppBaseData(
                            baseCurrency = state.baseCurrency,
                            categories = state.categories,
                            accounts = state.accounts,
                        ),
                        upcoming = LegacyDueSection(
                            trns = state.upcomingTransactions,
                            stats = IncomeExpensePair(
                                income = state.upcomingIncome.toBigDecimal(),
                                expense = state.upcomingExpenses.toBigDecimal()
                            ),
                            expanded = state.upcomingExpanded
                        ),
                        setUpcomingExpanded = {
                            onEventHandler(ReportScreenEvent.OnUpcomingExpanded(upcomingExpanded = it))
                        },
                        overdue = LegacyDueSection(
                            trns = state.overdueTransactions,
                            stats = IncomeExpensePair(
                                income = state.overdueIncome.toBigDecimal(),
                                expense = state.overdueExpenses.toBigDecimal()
                            ),
                            expanded = state.overdueExpanded
                        ),
                        setOverdueExpanded = {
                            onEventHandler(ReportScreenEvent.OnOverdueExpanded(overdueExpanded = it))
                        },
                        history = state.history,
                        onPayOrGet = {
                            onEventHandler(ReportScreenEvent.OnPayOrGetLegacy(transaction = it))
                        },
                        emptyStateTitle = stringRes(R.string.no_transactions),
                        emptyStateText = stringRes(R.string.no_transactions_for_your_filter),
                        onSkipTransaction = {
                            onEventHandler(ReportScreenEvent.SkipTransactionLegacy(transaction = it))
                        },
                        onSkipAllTransactions = {
                            onEventHandler(ReportScreenEvent.SkipTransactionsLegacy(transactions = it))
                        }
                    )
                } else {
                    item {
                        NoFilterEmptyState(onSetFilter = showFilter)
                    }
                }
            }

            if (state.loading) {
                LoadingOverlay()
            }
        }
    }

    FilterOverlay(
        visible = state.filterOverlayVisible,
        baseCurrency = state.baseCurrency,
        accounts = state.accounts,
        categories = state.categories,
        filter = state.filter,
        allTags = state.allTags,
        onClose = {
            onEventHandler(ReportScreenEvent.OnFilterOverlayVisible(filterOverlayVisible = false))
        },
        onSetFilter = {
            onEventHandler(ReportScreenEvent.OnFilter(filter = it))
        },
        onTagSearch = {
            onEventHandler(ReportScreenEvent.OnTagSearch(data = it))
        }
    )
}

@Composable
private fun Balance(
    currency: String,
    balance: Double,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${if (balance > 0) "+" else ""}${balance.format(currency)} $currency",
        modifier = modifier.testTag("balance"),
        style = MaterialTheme.typography.displaySmallEmphasized,
    )
}

@Composable
private fun IncomeExpenses(
    state: ReportScreenState,
    onSummaryClick: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trns = state.history.filterIsInstance<Transaction>()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(
            type = TransactionType.INCOME,
            label = stringResource(R.string.income),
            currency = state.baseCurrency,
            amount = state.income,
            count = trns.count { it.type == TransactionType.INCOME },
            modifier = Modifier.weight(1f),
            onClick = { onSummaryClick(TransactionType.INCOME) },
        )

        SummaryCard(
            type = TransactionType.EXPENSE,
            label = stringResource(R.string.expenses),
            currency = state.baseCurrency,
            amount = 0.0 - state.expenses.absoluteValue,
            count = trns.count { it.type == TransactionType.EXPENSE },
            modifier = Modifier.weight(1f),
            onClick = { onSummaryClick(TransactionType.EXPENSE) },
        )
    }
}

@Composable
private fun TransfersAsIncomeExpense(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = stringResource(R.string.transfers_as_income_expense),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NoFilterEmptyState(
    onSetFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            emptyStateTitle = stringResource(R.string.no_filter),
            emptyStateText = stringResource(R.string.invalid_filter_warning),
            icon = Icons.Outlined.FilterList,
        )

        Button(onClick = onSetFilter, modifier = Modifier.testTag("report_set_filter")) {
            Text(stringResource(R.string.set_filter))
        }
    }
}

@Composable
private fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ContainedLoadingIndicator()
                Text(
                    text = stringResource(R.string.generating_report),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val previewAccounts = persistentListOf(
    Account("Cash", color = Green.toArgb()),
    Account("DSK", color = GreenDark.toArgb()),
    Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
    Account("Revolut", color = IvyDark.toArgb()),
)

private val previewCategories = persistentListOf(
    Category(
        name = NotBlankTrimmedString.unsafe("Science"),
        color = ColorInt(Purple1Dark.toArgb()),
        icon = IconAsset.unsafe("atom"),
        id = CategoryId(UUID.randomUUID()),
        orderNum = 0.0,
    ),
    Category(
        name = NotBlankTrimmedString.unsafe("Pet"),
        color = ColorInt(Red3Light.toArgb()),
        icon = IconAsset.unsafe("pet"),
        id = CategoryId(UUID.randomUUID()),
        orderNum = 1.0,
    ),
    Category(
        name = NotBlankTrimmedString.unsafe("Home"),
        color = ColorInt(Green.toArgb()),
        icon = null,
        id = CategoryId(UUID.randomUUID()),
        orderNum = 2.0,
    ),
)

@Suppress("MagicNumber")
private fun previewState(filter: ReportFilter?): ReportScreenState {
    val at = LocalDateTime.of(2023, 1, 12, 12, 30).toInstant(ZoneOffset.UTC)
    val withRows = filter != null
    return ReportScreenState(
        baseCurrency = "BGN",
        balance = if (withRows) 1915.7 else 0.0,
        income = if (withRows) 2000.0 else 0.0,
        expenses = if (withRows) 84.3 else 0.0,
        history = if (withRows) {
            persistentListOf(
                TransactionHistoryDateDivider(LocalDate.of(2023, 1, 12), income = 2000.0, expenses = 84.3),
                Transaction(
                    accountId = previewAccounts[0].id,
                    type = TransactionType.INCOME,
                    amount = BigDecimal.valueOf(2000),
                    title = "Salary",
                    dateTime = at,
                ),
                Transaction(
                    accountId = previewAccounts[0].id,
                    type = TransactionType.EXPENSE,
                    amount = BigDecimal.valueOf(84.3),
                    title = "Vet",
                    categoryId = previewCategories[1].id.value,
                    dateTime = at,
                ),
            )
        } else {
            persistentListOf()
        },
        upcomingExpanded = true,
        overdueExpanded = true,
        filter = filter,
        accounts = previewAccounts,
        categories = previewCategories,
    )
}

@Composable
private fun ReportPreview(theme: Theme, state: ReportScreenState) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(state = state)
        }
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    ReportPreview(
        theme,
        previewState(
            ReportFilter.emptyFilter("BGN").copy(
                trnTypes = listOf(TransactionType.INCOME, TransactionType.EXPENSE),
                period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                accounts = previewAccounts,
                categories = previewCategories,
            )
        )
    )
}

@Preview
@Composable
private fun Preview_NO_FILTER(theme: Theme = Theme.LIGHT) {
    ReportPreview(theme, previewState(filter = null))
}

/** For screenshot testing */
@Composable
fun ReportUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun ReportNoFilterUiTest(isDark: Boolean) {
    Preview_NO_FILTER(if (isDark) Theme.DARK else Theme.LIGHT)
}
