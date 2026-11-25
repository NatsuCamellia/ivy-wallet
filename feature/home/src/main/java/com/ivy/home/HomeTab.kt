package com.ivy.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.legacy.stringRes
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.frp.forward
import com.ivy.frp.then2
import com.ivy.home.customerjourney.CustomerJourney
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.BufferInfo
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.home.transactionlist.HomeTypeFilter
import com.ivy.home.transactionlist.mapHomeDue
import com.ivy.home.transactionlist.homeTransactionsList
import com.ivy.home.transactionlist.rememberHomeTransactionMapper
import com.ivy.legacy.ui.sheet.BufferModal
import com.ivy.legacy.ui.sheet.BufferModalData
import com.ivy.legacy.ui.sheet.ChoosePeriodModal
import com.ivy.legacy.ui.sheet.ChoosePeriodModalData
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.Purple
import com.ivy.ui.component.screenMargin
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.withFabClearance
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Composable
fun HomeTab() {
    val viewModel: HomeViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    HomeUi(uiState, viewModel::onEvent)
}

@Suppress("LongMethod")
@Composable
fun HomeUi(
    uiState: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ivyContext = ivyWalletCtx()

    var bufferModalData: BufferModalData? by remember { mutableStateOf(null) }
    var choosePeriodModal: ChoosePeriodModalData? by remember {
        mutableStateOf(null)
    }
    var moreMenuExpanded by remember { mutableStateOf(ivyContext.moreMenuExpanded) }
    var skipAllModalVisible by remember { mutableStateOf(false) }
    val setMoreMenuExpanded = { expanded: Boolean ->
        moreMenuExpanded = expanded
        ivyContext.setMoreMenuExpanded(expanded)
    }

    val nav = navigation()
    val baseCurrency = uiState.baseData.baseCurrency
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberScrollPositionListState(
        key = "home_lazy_column",
        initialFirstVisibleItemIndex = ivyContext.transactionsListState
            ?.firstVisibleItemIndex ?: 0,
        initialFirstVisibleItemScrollOffset = ivyContext.transactionsListState
            ?.firstVisibleItemScrollOffset ?: 0
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                name = uiState.name,
                period = uiState.period,
                onShowPeriodModal = {
                    choosePeriodModal = ChoosePeriodModalData(period = uiState.period)
                },
                onSearch = { nav.navigateTo(SearchScreen) },
                onOpenMoreMenu = { setMoreMenuExpanded(true) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        HomeLazyColumn(
            hideBalance = uiState.hideBalance,
            hideIncome = uiState.hideIncome,
            balance = uiState.balance,
            onBalanceClick = {
                onEvent(HomeEvent.BalanceClick)
            },
            onHiddenBalanceClick = {
                onEvent(HomeEvent.HiddenBalanceClick)
            },
            onHiddenIncomeClick = {
                onEvent(HomeEvent.HiddenIncomeClick)
            },

            period = uiState.period,
            listState = listState,
            contentPadding = innerPadding.withFabClearance(),

            baseData = uiState.baseData,

            upcoming = uiState.upcoming,
            overdue = uiState.overdue,
            dueExpanded = uiState.dueExpanded,

            stats = uiState.stats,
            history = uiState.history,
            historyEmpty = uiState.historyEmpty,
            typeFilter = uiState.typeFilter,
            onTypeFilterChange = { onEvent(HomeEvent.SetTypeFilter(it)) },

            customerJourneyCards = uiState.customerJourneyCards,

            onPayOrGet = forward<Transaction>() then2 {
                HomeEvent.PayOrGetPlanned(it)
            } then2 onEvent,
            onDismiss = forward<CustomerJourneyCardModel>() then2 {
                HomeEvent.DismissCustomerJourneyCard(it)
            } then2 onEvent,
            onSkipTransaction = forward<Transaction>() then2 {
                HomeEvent.SkipPlanned(it)
            } then2 onEvent,
            setDueExpanded = forward<Boolean>() then2 {
                HomeEvent.SetDueExpanded(it)
            } then2 onEvent,
            onSkipAllTransactions = {
                skipAllModalVisible = true
            }
        )
    }

    if (moreMenuExpanded) {
        MoreMenu(
            theme = uiState.theme,
            balance = uiState.balance.toDouble(),
            currency = baseCurrency,
            buffer = uiState.buffer.amount.toDouble(),
            onSwitchTheme = {
                onEvent(HomeEvent.SwitchTheme)
            },
            onDismiss = { setMoreMenuExpanded(false) },
            onBufferClick = {
                bufferModalData = BufferModalData(
                    balance = uiState.balance.toDouble(),
                    currency = baseCurrency,
                    buffer = uiState.buffer.amount.toDouble()
                )
            },
            onCategoriesClick = {
                setMoreMenuExpanded(false)
                ivyContext.selectMainTab(MainTab.CATEGORIES)
            },
        )
    }

    BufferModal(
        modal = bufferModalData,
        dismiss = {
            bufferModalData = null
        },
        onBufferChanged = forward<Double>() then2 {
            HomeEvent.SetBuffer(it)
        } then2 onEvent
    )

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = {
            choosePeriodModal = null
        },
        onPeriodSelected = forward<TimePeriod>() then2 {
            HomeEvent.SetPeriod(it)
        } then2 onEvent
    )

    if (skipAllModalVisible) {
        AlertDialog(
            onDismissRequest = { skipAllModalVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(HomeEvent.SkipAllPlanned(uiState.overdue.trns))
                        skipAllModalVisible = false
                    }
                ) {
                    Text(text = stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { skipAllModalVisible = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            title = { Text(text = stringResource(R.string.confirm_skip_all)) },
            text = { Text(text = stringResource(R.string.confirm_skip_all_description)) },
        )
    }
}

@Suppress("LongParameterList")
@Composable
fun HomeLazyColumn(
    hideBalance: Boolean,
    hideIncome: Boolean,
    listState: LazyListState,
    period: TimePeriod,

    baseData: AppBaseData,

    upcoming: LegacyDueSection,
    overdue: LegacyDueSection,
    dueExpanded: Boolean,
    balance: BigDecimal,
    stats: IncomeExpensePair,
    history: ImmutableList<TransactionHistoryItem>,
    historyEmpty: Boolean,
    typeFilter: HomeTypeFilter,
    onTypeFilterChange: (HomeTypeFilter) -> Unit,

    customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,

    setDueExpanded: (Boolean) -> Unit,

    onBalanceClick: () -> Unit,

    onPayOrGet: (Transaction) -> Unit,
    onDismiss: (CustomerJourneyCardModel) -> Unit,
    onHiddenBalanceClick: () -> Unit,
    onHiddenIncomeClick: () -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val ivyContext = ivyWalletCtx()

    val timeProvider = LocalTimeProvider.current
    val timeConverter = LocalTimeConverter.current
    val timeFormatter = LocalTimeFormatter.current

    val mapper = rememberHomeTransactionMapper(baseData)
    val historyItems = remember(mapper, history) { mapper.mapHistory(history) }
    val due = remember(overdue, upcoming, dueExpanded) {
        mapHomeDue(overdue = overdue.trns, upcoming = upcoming.trns, expanded = dueExpanded)
    }
    val dueRows = remember(mapper, due) { mapper.mapDueRows(due.rows) }
    val nav = navigation()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_lazy_column"),
        state = listState,
        contentPadding = contentPadding,
    ) {
        item {
            Column(
                modifier = Modifier
                    .screenMargin()
                    .padding(bottom = ListItemDefaults.SegmentedGap),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HomeBalanceBanner(
                    currency = baseData.baseCurrency,
                    balance = balance.toDouble(),

                    hideBalance = hideBalance,

                    monthlyIncome = stats.income.toDouble(),
                    monthlyExpenses = stats.expense.toDouble(),

                    onBalanceClick = onBalanceClick,
                    onHiddenBalanceClick = onHiddenBalanceClick,
                    hideIncome = hideIncome,
                    onHiddenIncomeClick = onHiddenIncomeClick
                )

                if (customerJourneyCards.isNotEmpty()) {
                    CustomerJourney(
                        customerJourneyCards = customerJourneyCards,
                        onDismiss = onDismiss
                    )
                }
            }
        }

        homeTransactionsList(
            historyItems = historyItems,
            historyEmpty = historyEmpty,
            typeFilter = typeFilter,
            onTypeFilterChange = onTypeFilterChange,
            due = due,
            dueRows = dueRows,
            dueExpanded = dueExpanded,
            emptyStateTitle = stringRes(R.string.no_transactions),
            emptyStateText = stringRes(
                R.string.no_transactions_description,
                period.toDisplayLong(
                    startDateOfMonth = ivyContext.startDayOfMonth,
                    timeProvider = timeProvider,
                    timeConverter = timeConverter,
                    timeFormatter = timeFormatter,
                )
            ),
            onTransactionClick = { trn ->
                nav.navigateTo(EditTransactionScreen(initialTransactionId = trn.id, type = trn.type))
            },
            onPayOrGet = onPayOrGet,
            onSkipTransaction = onSkipTransaction,
            onSkipAllTransactions = onSkipAllTransactions,
            setDueExpanded = setDueExpanded,
        )
    }
}

@Preview
@Composable
private fun PreviewHomeTab(
    theme: Theme = Theme.LIGHT,
    moreMenuExpanded: Boolean = false,
    empty: Boolean = false,
    due: HomeDuePreview = HomeDuePreview.None,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            ivyWalletCtx().setMoreMenuExpanded(moreMenuExpanded)
            HomeUi(
                uiState = when {
                    empty -> emptyHomeState()
                    due != HomeDuePreview.None -> plannedHomeState(due)
                    else -> populatedHomeState()
                },
                onEvent = {}
            )
        }
    }
}

private fun emptyHomeState(): HomeState = HomeState(
    theme = Theme.AUTO,
    name = "",
    baseData = AppBaseData(
        baseCurrency = "",
        accounts = persistentListOf(),
        categories = persistentListOf()
    ),
    balance = BigDecimal.ZERO,
    buffer = BufferInfo(
        amount = BigDecimal.ZERO,
        bufferDiff = BigDecimal.ZERO,
    ),
    customerJourneyCards = persistentListOf(),
    history = persistentListOf(),
    historyEmpty = true,
    typeFilter = HomeTypeFilter.All,
    stats = IncomeExpensePair.zero(),
    upcoming = LegacyDueSection(
        trns = persistentListOf(),
        stats = IncomeExpensePair.zero(),
        expanded = false,
    ),
    overdue = LegacyDueSection(
        trns = persistentListOf(),
        stats = IncomeExpensePair.zero(),
        expanded = false,
    ),
    period = TimePeriod(month = Month.monthsList().first(), year = 2023),
    hideBalance = false,
    hideIncome = false,
    expanded = false,
    dueExpanded = false,
)

@Suppress("MagicNumber")
private fun populatedHomeState(): HomeState {
    val cash = Account(name = "Cash", Green.toArgb())
    val food = Category(
        name = NotBlankTrimmedString.unsafe("Food"),
        color = ColorInt(Purple.toArgb()),
        icon = null,
        id = CategoryId(UUID.randomUUID()),
        orderNum = 0.0,
    )
    val at = LocalDateTime.of(2023, 1, 12, 12, 30).toInstant(ZoneOffset.UTC)
    val history = persistentListOf<TransactionHistoryItem>(
        TransactionHistoryDateDivider(LocalDate.of(2023, 1, 12), income = 2500.0, expenses = 84.3),
        Transaction(
            accountId = cash.id,
            type = TransactionType.INCOME,
            amount = BigDecimal.valueOf(2500),
            title = "Salary",
            dateTime = at,
        ),
        Transaction(
            accountId = cash.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal.valueOf(84.3),
            title = "Groceries",
            categoryId = food.id.value,
            dateTime = at,
        ),
    )
    return emptyHomeState().copy(
        name = "Ivy",
        baseData = AppBaseData(
            baseCurrency = "BGN",
            accounts = persistentListOf(cash),
            categories = persistentListOf(food)
        ),
        balance = BigDecimal.valueOf(2415.7),
        buffer = BufferInfo(
            amount = BigDecimal.valueOf(1000),
            bufferDiff = BigDecimal.valueOf(1415.7),
        ),
        history = history,
        historyEmpty = false,
        stats = IncomeExpensePair(BigDecimal.valueOf(2500), BigDecimal.valueOf(84.3)),
    )
}

/** Which Due section a screenshot of Home shows. */
enum class HomeDuePreview { None, OverdueAndUpcoming, UpcomingOnly, CollapsedWithMore }

@Suppress("MagicNumber")
private fun plannedHomeState(scenario: HomeDuePreview): HomeState {
    val state = populatedHomeState()
    val account = state.baseData.accounts.first()
    val due = Instant.parse("2023-01-20T12:00:00Z")
    fun planned(type: TransactionType, title: String, amount: Long, dueDate: Instant) = Transaction(
        accountId = account.id,
        type = type,
        amount = BigDecimal.valueOf(amount),
        title = title,
        dueDate = dueDate,
    )

    val rent = planned(TransactionType.EXPENSE, "Rent", 500, due)
    val freelance = planned(TransactionType.INCOME, "Freelance", 120, due)
    val gym = planned(TransactionType.EXPENSE, "Gym", 30, due.plus(Duration.ofDays(3)))
    val phone = planned(TransactionType.EXPENSE, "Phone", 40, due.minus(Duration.ofDays(30)))
    val internet = planned(TransactionType.EXPENSE, "Internet", 25, due.minus(Duration.ofDays(10)))
    fun section(vararg trns: Transaction) = LegacyDueSection(
        trns = persistentListOf(*trns),
        stats = IncomeExpensePair.zero(),
        expanded = false,
    )
    return when (scenario) {
        HomeDuePreview.None -> state
        HomeDuePreview.OverdueAndUpcoming -> state.copy(
            upcoming = section(rent, freelance),
            overdue = section(phone),
        )
        HomeDuePreview.UpcomingOnly -> state.copy(upcoming = section(rent, freelance))
        HomeDuePreview.CollapsedWithMore -> state.copy(
            upcoming = section(rent, freelance, gym),
            overdue = section(phone, internet),
        )
    }
}

/** For screenshot testing */
@Composable
fun HomeUiTest(
    isDark: Boolean,
    moreMenuExpanded: Boolean = false,
    empty: Boolean = false,
    due: HomeDuePreview = HomeDuePreview.None,
) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewHomeTab(theme, moreMenuExpanded, empty, due)
}
