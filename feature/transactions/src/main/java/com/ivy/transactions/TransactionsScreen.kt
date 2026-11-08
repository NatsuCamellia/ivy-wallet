package com.ivy.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
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
import com.ivy.legacy.Constants
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.SummaryCard
import com.ivy.legacy.ui.component.transaction.transactions
import com.ivy.legacy.ui.sheet.AccountModal
import com.ivy.legacy.ui.sheet.AccountModalData
import com.ivy.legacy.ui.sheet.CategoryModal
import com.ivy.legacy.ui.sheet.CategoryModalData
import com.ivy.legacy.ui.sheet.ChoosePeriodModal
import com.ivy.legacy.ui.sheet.ChoosePeriodModalData
import com.ivy.legacy.ui.sheet.DeleteConfirmationModal
import com.ivy.legacy.ui.sheet.DeleteModal
import com.ivy.legacy.utils.balancePrefix
import com.ivy.legacy.utils.format
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.Purple
import com.ivy.ui.component.BackButton
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.ui.util.thenIf
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.ui.theme.toComposeColor
import com.ivy.wallet.ui.theme.wallet.PeriodSelector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.absoluteValue

@Composable
fun TransactionsScreen(screen: TransactionsScreen) {
    val viewModel: TransactionsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.start(screen)
    }

    TransactionsUi(
        screen = screen,
        period = uiState.period,
        baseCurrency = uiState.baseCurrency,
        currency = uiState.currency,

        categories = uiState.categories,
        accounts = uiState.accounts,

        account = uiState.account,
        category = uiState.category,

        balance = uiState.balance,
        balanceBaseCurrency = uiState.balanceBaseCurrency,
        income = uiState.income,
        expenses = uiState.expenses,

        initWithTransactions = uiState.initWithTransactions,
        treatTransfersAsIncomeExpense = uiState.treatTransfersAsIncomeExpense,

        history = uiState.history,
        shouldShowAccountSpecificColorInTransactions = uiState.showAccountColorsInTransactions,

        upcoming = uiState.upcoming,
        upcomingExpanded = uiState.upcomingExpanded,
        setUpcomingExpanded = {
            viewModel.onEvent(TransactionsEvent.SetUpcomingExpanded(it))
        },
        upcomingIncome = uiState.upcomingIncome,
        upcomingExpenses = uiState.upcomingExpenses,

        overdue = uiState.overdue,
        overdueExpanded = uiState.overdueExpanded,
        setOverdueExpanded = {
            viewModel.onEvent(TransactionsEvent.SetOverdueExpanded(it))
        },
        overdueIncome = uiState.overdueIncome,
        overdueExpenses = uiState.overdueExpenses,

        onSetPeriod = {
            viewModel.onEvent(
                TransactionsEvent.SetPeriod(
                    screen = screen,
                    period = it
                )
            )
        },
        onNextMonth = {
            viewModel.onEvent(TransactionsEvent.NextMonth(screen))
        },
        onPreviousMonth = {
            viewModel.onEvent(TransactionsEvent.PreviousMonth(screen))
        },
        onDelete = {
            viewModel.onEvent(TransactionsEvent.Delete(screen))
        },
        onEditCategory = {
            viewModel.onEvent(TransactionsEvent.EditCategory(it))
        },
        onEditAccount = { acc, newBalance ->
            viewModel.onEvent(TransactionsEvent.EditAccount(screen, acc, newBalance))
        },
        onPayOrGet = { transaction ->
            viewModel.onEvent(TransactionsEvent.PayOrGet(screen, transaction))
        },
        onSkipTransaction = { transaction ->
            viewModel.onEvent(TransactionsEvent.SkipTransaction(screen, transaction))
        },
        onSkipAllTransactions = { transactions ->
            viewModel.onEvent(TransactionsEvent.SkipTransactions(screen, transactions))
        },
        updateAccountNameConfirmation = {
            viewModel.onEvent(TransactionsEvent.UpdateAccountDeletionState(it))
        },
        enableDeletionButton = uiState.enableDeletionButton,
        skipAllModalVisible = uiState.skipAllModalVisible,
        onSkipAllModalVisible = {
            viewModel.onEvent(TransactionsEvent.SetSkipAllModalVisible(it))
        },
        deleteModal1Visible = uiState.deleteModal1Visible,
        onDeleteModal1Visible = {
            viewModel.onEvent(TransactionsEvent.OnDeleteModal1Visible(it))
        },
        onChoosePeriodModal = {
            viewModel.onEvent(TransactionsEvent.OnChoosePeriodModalData(it))
        },
        choosePeriodModal = uiState.choosePeriodModal
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun TransactionsUi(
    screen: TransactionsScreen,
    period: TimePeriod,
    baseCurrency: String,
    currency: String,
    skipAllModalVisible: Boolean,
    onSkipAllModalVisible: (Boolean) -> Unit,

    account: Account?,
    category: Category?,

    updateAccountNameConfirmation: (String) -> Unit,
    enableDeletionButton: Boolean,

    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,

    balance: Double,
    balanceBaseCurrency: Double?,
    income: Double,
    expenses: Double,
    choosePeriodModal: ChoosePeriodModalData?,

    history: ImmutableList<TransactionHistoryItem>,
    shouldShowAccountSpecificColorInTransactions: Boolean,

    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSetPeriod: (TimePeriod) -> Unit,
    onEditAccount: (Account, Double) -> Unit,
    onEditCategory: (Category) -> Unit,
    onDelete: () -> Unit,
    deleteModal1Visible: Boolean,
    onDeleteModal1Visible: (Boolean) -> Unit,
    modifier: Modifier = Modifier,

    initWithTransactions: Boolean = false,
    treatTransfersAsIncomeExpense: Boolean = false,
    upcomingExpanded: Boolean = true,
    setUpcomingExpanded: (Boolean) -> Unit = {},
    upcomingIncome: Double = 0.0,
    upcomingExpenses: Double = 0.0,
    upcoming: ImmutableList<Transaction> = persistentListOf(),

    overdueExpanded: Boolean = true,
    setOverdueExpanded: (Boolean) -> Unit = {},
    overdueIncome: Double = 0.0,
    overdueExpenses: Double = 0.0,
    overdue: ImmutableList<Transaction> = persistentListOf(),

    onPayOrGet: (Transaction) -> Unit = {},
    onSkipTransaction: (Transaction) -> Unit = {},
    onSkipAllTransactions: (List<Transaction>) -> Unit = {},
    onChoosePeriodModal: (ChoosePeriodModalData?) -> Unit,
) {
    val ivyContext = ivyWalletCtx()
    val nav = navigation()

    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

    val onEdit = {
        when {
            account != null -> {
                accountModalData = AccountModalData(
                    account = account,
                    baseCurrency = currency,
                    balance = balance,
                    autoFocusKeyboard = false
                )
            }

            category != null -> {
                categoryModalData = CategoryModalData(
                    category = category,
                    autoFocusKeyboard = false
                )
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberScrollPositionListState(key = "item_stats_lazy_column")
    val timeProvider = LocalTimeProvider.current
    val timeConverter = LocalTimeConverter.current
    val timeFormatter = LocalTimeFormatter.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TransactionsTopBar(
                account = account,
                category = category,
                showActions = screen.transactions.none { it.type == TransactionType.TRANSFER },
                onEdit = onEdit,
                onDelete = { onDeleteModal1Visible(true) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("item_stats_lazy_column"),
            state = listState,
            contentPadding = innerPadding,
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Balance(
                            currency = currency,
                            baseCurrency = baseCurrency,
                            balance = balance,
                            balanceBaseCurrency = balanceBaseCurrency,
                            prefix = category?.let { balancePrefix(income = income, expenses = expenses) },
                            onClick = account?.let {
                                {
                                    accountModalData = AccountModalData(
                                        account = it,
                                        baseCurrency = currency,
                                        balance = balance,
                                        adjustBalanceMode = true,
                                        autoFocusKeyboard = false
                                    )
                                }
                            },
                        )

                        IncomeExpenses(
                            history = history,
                            currency = currency,
                            income = income,
                            expenses = expenses,
                            onSummaryClick = { type ->
                                if (account != null) {
                                    nav.navigateTo(
                                        PieChartStatisticScreen(
                                            type = type,
                                            accountList = persistentListOf(account.id),
                                            filterExcluded = false,
                                            treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense
                                        )
                                    )
                                }
                            },
                            onAdd = { type ->
                                nav.navigateTo(
                                    EditTransactionScreen(
                                        initialTransactionId = null,
                                        type = type,
                                        accountId = account?.id,
                                        categoryId = category?.id?.value
                                    )
                                )
                            },
                            hasSummaryAction = account != null,
                        )
                    }

                    if (!initWithTransactions) {
                        PeriodSelector(
                            period = period,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onShowChoosePeriodModal = {
                                onChoosePeriodModal(ChoosePeriodModalData(period = period))
                            }
                        )
                    }
                }
            }

            transactions(
                baseData = AppBaseData(
                    baseCurrency,
                    accounts,
                    categories
                ),
                upcoming = LegacyDueSection(
                    trns = upcoming,
                    stats = IncomeExpensePair(
                        income = upcomingIncome.toBigDecimal(),
                        expense = upcomingExpenses.toBigDecimal()
                    ),
                    expanded = upcomingExpanded
                ),
                setUpcomingExpanded = setUpcomingExpanded,

                overdue = LegacyDueSection(
                    trns = overdue,
                    stats = IncomeExpensePair(
                        income = overdueIncome.toBigDecimal(),
                        expense = overdueExpenses.toBigDecimal()
                    ),
                    expanded = overdueExpanded
                ),
                setOverdueExpanded = setOverdueExpanded,

                history = history,
                lastItemSpacer = 0.dp,

                onPayOrGet = onPayOrGet,
                onSkipTransaction = onSkipTransaction,
                onSkipAllTransactions = {
                    onSkipAllModalVisible(true)
                },
                emptyStateTitle = stringRes(R.string.no_transactions),
                emptyStateText = stringRes(
                    R.string.no_transactions_for_period,
                    period.toDisplayLong(
                        startDateOfMonth = ivyContext.startDayOfMonth,
                        timeProvider = timeProvider,
                        timeConverter = timeConverter,
                        timeFormatter = timeFormatter,
                    )
                ),
                shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions
            )
        }
    }

    DeleteModals(
        account = account,
        category = category,
        updateAccountNameConfirmation = updateAccountNameConfirmation,
        enableDeletionButton = enableDeletionButton,
        onDelete = onDelete,
        skipAllModalVisible = skipAllModalVisible,
        onSkipAllModalVisible = {
            onSkipAllModalVisible(it)
        },
        onSkipAllTransactions = onSkipAllTransactions,
        deleteModal1Visible = deleteModal1Visible,
        setDeleteModal1Visible = onDeleteModal1Visible,
        overdue = overdue,
    )

    CategoryModal(
        modal = categoryModalData,
        onCreateCategory = { },
        onEditCategory = onEditCategory,
        dismiss = {
            categoryModalData = null
        }
    )

    AccountModal(
        modal = accountModalData,
        onCreateAccount = { },
        onEditAccount = onEditAccount,
        dismiss = {
            accountModalData = null
        }
    )

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = {
            onChoosePeriodModal(null)
        }
    ) {
        onSetPeriod(it)
    }
}

@Composable
private fun TransactionsTopBar(
    account: Account?,
    category: Category?,
    showActions: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val badgeColor = when {
        account != null -> account.color.toComposeColor()
        category != null -> category.color.value.toComposeColor()
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val name = when {
        account != null -> account.name
        category != null -> category.name.value
        else -> Constants.CATEGORY_UNSPECIFIED_NAME
    }

    LargeFlexibleTopAppBar(
        title = {
            Row(
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(R.string.edit),
                    role = Role.Button,
                    onClick = onEdit
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconBadge(
                    color = badgeColor,
                    iconName = account?.icon ?: category?.icon?.id,
                    defaultIcon = if (account != null) {
                        R.drawable.ic_custom_account_m
                    } else {
                        R.drawable.ic_custom_category_m
                    },
                )
                Text(text = name, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        modifier = modifier,
        subtitle = if (account != null && !account.includeInBalance) {
            { Text(text = stringResource(R.string.excluded)) }
        } else {
            null
        },
        navigationIcon = {
            BackButton(onClick = { nav.back() }, modifier = Modifier.testTag("toolbar_close"))
        },
        actions = {
            if (showActions) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun Balance(
    currency: String,
    baseCurrency: String,
    balance: Double,
    balanceBaseCurrency: Double?,
    prefix: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .thenIf(onClick != null) { clickable(onClick = onClick!!) }
            .testTag("balance")
    ) {
        Text(
            text = "${prefix.orEmpty()}${balance.format(currency)} $currency",
            style = MaterialTheme.typography.displaySmallEmphasized,
        )

        if (currency != baseCurrency && balanceBaseCurrency != null) {
            Text(
                text = "${prefix.orEmpty()}${balanceBaseCurrency.format(baseCurrency)} $baseCurrency",
                modifier = Modifier.testTag("balance_base_currency"),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IncomeExpenses(
    history: ImmutableList<TransactionHistoryItem>,
    currency: String,
    income: Double,
    expenses: Double,
    hasSummaryAction: Boolean,
    onSummaryClick: (TransactionType) -> Unit,
    onAdd: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trns = history.filterIsInstance<Transaction>()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(
            type = TransactionType.INCOME,
            label = stringResource(R.string.income),
            currency = currency,
            amount = income,
            count = trns.count { it.type == TransactionType.INCOME },
            modifier = Modifier.weight(1f),
            onClick = if (hasSummaryAction) {
                { onSummaryClick(TransactionType.INCOME) }
            } else {
                null
            },
            addLabel = stringResource(R.string.add_income),
            onAdd = { onAdd(TransactionType.INCOME) },
        )

        SummaryCard(
            type = TransactionType.EXPENSE,
            label = stringResource(R.string.expenses),
            currency = currency,
            amount = 0.0 - expenses.absoluteValue,
            count = trns.count { it.type == TransactionType.EXPENSE },
            modifier = Modifier.weight(1f),
            onClick = if (hasSummaryAction) {
                { onSummaryClick(TransactionType.EXPENSE) }
            } else {
                null
            },
            addLabel = stringResource(R.string.add_expense),
            onAdd = { onAdd(TransactionType.EXPENSE) },
        )
    }
}

@Composable
private fun DeleteModals(
    deleteModal1Visible: Boolean,
    setDeleteModal1Visible: (Boolean) -> Unit,
    account: Account?,
    category: Category?,
    updateAccountNameConfirmation: (String) -> Unit,
    enableDeletionButton: Boolean,
    onDelete: () -> Unit,
    skipAllModalVisible: Boolean,
    onSkipAllModalVisible: (Boolean) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    overdue: ImmutableList<Transaction> = persistentListOf(),
) {
    var deleteModal3Visible by remember { mutableStateOf(false) }

    DeleteModal(
        visible = deleteModal1Visible,
        title = stringResource(R.string.confirm_deletion),
        description = if (account != null) {
            stringResource(R.string.account_confirm_deletion_description)
        } else {
            stringResource(R.string.category_confirm_deletion_description)
        },
        dismiss = {
            setDeleteModal1Visible(false)
        }
    ) {
        deleteModal3Visible = true
    }

    DeleteConfirmationModal(
        visible = deleteModal3Visible,
        title = stringResource(id = R.string.confirm_deletion),
        description = if (account != null) {
            stringResource(
                id = R.string.account_confirm_deletion_type_account_name,
                account.name
            )
        } else {
            stringResource(R.string.please_type_category_name, category?.name?.value ?: "")
        },
        hint = if (account != null) stringResource(id = R.string.account_name) else "Category name",
        onAccountNameChange = updateAccountNameConfirmation,
        enableDeletionButton = enableDeletionButton,
        dismiss = {
            updateAccountNameConfirmation("")
            deleteModal3Visible = false
            setDeleteModal1Visible(false)
        }
    ) {
        onDelete()
        updateAccountNameConfirmation("")
        setDeleteModal1Visible(false)
    }

    DeleteModal(
        visible = skipAllModalVisible,
        title = stringResource(R.string.confirm_skip_all),
        description = stringResource(R.string.confirm_skip_all_description),
        dismiss = {
            onSkipAllModalVisible(false)
        }
    ) {
        onSkipAllTransactions(overdue)
        onSkipAllModalVisible(false)
    }
}

private class PreviewData(
    val account: Account?,
    val category: Category?,
    val history: ImmutableList<TransactionHistoryItem>,
    val upcoming: ImmutableList<Transaction>,
    val currency: String,
    val balance: Double,
    val balanceBaseCurrency: Double?,
    val income: Double,
    val expenses: Double,
)

private val previewAccount = Account(name = "Cash", color = Green.toArgb())

private val previewCategory = Category(
    name = NotBlankTrimmedString.unsafe("Food"),
    color = ColorInt(Purple.toArgb()),
    icon = null,
    id = CategoryId(UUID.randomUUID()),
    orderNum = 0.0,
)

@Suppress("MagicNumber")
private fun previewHistory(): ImmutableList<TransactionHistoryItem> {
    val at = LocalDateTime.of(2023, 1, 12, 12, 30).toInstant(ZoneOffset.UTC)
    return persistentListOf(
        TransactionHistoryDateDivider(LocalDate.of(2023, 1, 12), income = 2500.0, expenses = 84.3),
        Transaction(
            accountId = previewAccount.id,
            type = TransactionType.INCOME,
            amount = BigDecimal.valueOf(2500),
            title = "Salary",
            dateTime = at,
        ),
        Transaction(
            accountId = previewAccount.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal.valueOf(84.3),
            title = "Groceries",
            categoryId = previewCategory.id.value,
            dateTime = at,
        ),
    )
}

@Suppress("MagicNumber")
private fun previewUpcoming(): ImmutableList<Transaction> = persistentListOf(
    Transaction(
        accountId = previewAccount.id,
        type = TransactionType.EXPENSE,
        amount = BigDecimal.valueOf(40),
        title = "Gym",
        dueDate = LocalDateTime.of(2030, 1, 20, 9, 0).toInstant(ZoneOffset.UTC),
    )
)

@Suppress("MagicNumber")
private fun accountPreview(): PreviewData = PreviewData(
    account = previewAccount,
    category = null,
    history = previewHistory(),
    upcoming = previewUpcoming(),
    currency = "BGN",
    balance = 2415.7,
    balanceBaseCurrency = null,
    income = 2500.0,
    expenses = 84.3,
)

@Suppress("MagicNumber")
private fun categoryPreview(): PreviewData = PreviewData(
    account = null,
    category = previewCategory,
    history = previewHistory().let {
        persistentListOf(
            TransactionHistoryDateDivider(LocalDate.of(2023, 1, 12), income = 0.0, expenses = 84.3),
            it[2]
        )
    },
    upcoming = persistentListOf(),
    currency = "BGN",
    balance = 84.3,
    balanceBaseCurrency = null,
    income = 0.0,
    expenses = -84.3,
)

@Suppress("MagicNumber")
private fun emptyPeriodPreview(): PreviewData = PreviewData(
    account = previewAccount,
    category = null,
    history = persistentListOf(),
    upcoming = persistentListOf(),
    currency = "BGN",
    balance = 2415.7,
    balanceBaseCurrency = null,
    income = 0.0,
    expenses = 0.0,
)

@Suppress("MagicNumber")
private fun hugeBalancePreview(): PreviewData = PreviewData(
    account = previewAccount.copy(
        name = "Long term savings and cryptocurrency holdings account",
        includeInBalance = false,
    ),
    category = null,
    history = previewHistory(),
    upcoming = persistentListOf(),
    currency = "ADA",
    balance = 123456789012.57,
    balanceBaseCurrency = 987654321098.76,
    income = 123456789012.0,
    expenses = 98765432109.0,
)

@Composable
private fun TransactionsPreview(theme: Theme, data: PreviewData) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            TransactionsUi(
                screen = TransactionsScreen(),
                period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                baseCurrency = "BGN",
                currency = data.currency,
                categories = persistentListOf(previewCategory),
                accounts = persistentListOf(previewAccount),
                balance = data.balance,
                balanceBaseCurrency = data.balanceBaseCurrency,
                income = data.income,
                expenses = data.expenses,
                history = data.history,
                category = data.category,
                account = data.account,
                upcoming = data.upcoming,
                onSetPeriod = { },
                onPreviousMonth = {},
                onNextMonth = {},
                onDelete = {},
                onEditAccount = { _, _ -> },
                onEditCategory = {},
                updateAccountNameConfirmation = {},
                enableDeletionButton = true,
                deleteModal1Visible = false,
                onDeleteModal1Visible = {},
                skipAllModalVisible = false,
                onSkipAllModalVisible = {},
                onChoosePeriodModal = {},
                choosePeriodModal = null,
                shouldShowAccountSpecificColorInTransactions = false
            )
        }
    }
}

@Preview
@Composable
private fun PreviewAccount() {
    TransactionsPreview(Theme.LIGHT, accountPreview())
}

private fun themeOf(isDark: Boolean): Theme = if (isDark) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun TransactionsUiTest(isDark: Boolean) {
    TransactionsPreview(themeOf(isDark), accountPreview())
}

/** For screenshot testing: a category has no account, so no excluded label and a signed balance */
@Composable
fun TransactionsCategoryUiTest(isDark: Boolean) {
    TransactionsPreview(themeOf(isDark), categoryPreview())
}

/** For screenshot testing: no transactions in the period */
@Composable
fun TransactionsEmptyPeriodUiTest(isDark: Boolean) {
    TransactionsPreview(themeOf(isDark), emptyPeriodPreview())
}

/** For screenshot testing: a 12-digit balance, a long account name, a crypto account excluded from the balance */
@Composable
fun TransactionsHugeBalanceUiTest(isDark: Boolean) {
    TransactionsPreview(themeOf(isDark), hugeBalancePreview())
}
