package com.ivy.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.transaction.transactions
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.Purple
import com.ivy.ui.component.BackButton
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Composable
fun SearchScreen(screen: SearchScreen) {
    val viewModel: SearchViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    SearchUi(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun SearchUi(
    uiState: SearchState,
    onEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val listState = rememberLazyListState()
    val queryState = rememberTextFieldState(uiState.searchQuery)
    val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(queryState) {
        snapshotFlow { queryState.text.toString() }
            .drop(1)
            .collect { currentOnEvent(SearchEvent.Search(it)) }
    }

    LaunchedEffect(searchBarState) {
        snapshotFlow { searchBarState.currentValue }
            .first { it == SearchBarValue.Collapsed }
        nav.back()
    }

    LaunchedEffect(uiState.transactions) {
        // scroll to top when transactions are changed
        listState.animateScrollToItem(index = 0, scrollOffset = 0)
    }

    val inputField: @Composable () -> Unit = {
        // read inside the field: the expanded bar is its own window with its own keyboard controller
        val keyboardController = LocalSoftwareKeyboardController.current
        SearchBarDefaults.InputField(
            textFieldState = queryState,
            searchBarState = searchBarState,
            onSearch = { keyboardController?.hide() },
            modifier = Modifier.testTag("search_input"),
            placeholder = { Text(stringResource(R.string.search_transactions)) },
            leadingIcon = { BackButton(onClick = { nav.back() }) },
            trailingIcon = if (queryState.text.isNotEmpty()) {
                {
                    IconButton(onClick = { queryState.clearText() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_search)
                        )
                    }
                }
            } else {
                null
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppBarWithSearch(state = searchBarState, inputField = inputField) }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding))
    }

    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
        val emptyStateTitle = stringResource(R.string.no_transactions)
        val emptyStateText = stringResource(R.string.no_transactions_for_query, queryState.text.toString())
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = listState,
        ) {
            transactions(
                baseData = AppBaseData(
                    baseCurrency = uiState.baseCurrency,
                    accounts = uiState.accounts,
                    categories = uiState.categories
                ),
                upcoming = null,
                setUpcomingExpanded = { },
                overdue = null,
                setOverdueExpanded = { },
                history = uiState.transactions,
                onPayOrGet = { },
                emptyStateTitle = emptyStateTitle,
                emptyStateText = emptyStateText,
                lastItemSpacer = 0.dp,
                shouldShowAccountSpecificColorInTransactions = uiState.shouldShowAccountSpecificColorInTransactions
            )
        }
    }
}

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
        TransactionHistoryDateDivider(LocalDate.of(2023, 1, 12), income = 0.0, expenses = 98.8),
        Transaction(
            accountId = previewAccount.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal.valueOf(84.3),
            title = "Groceries at the corner shop",
            categoryId = previewCategory.id.value,
            dateTime = at,
        ),
        Transaction(
            accountId = previewAccount.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal.valueOf(14.5),
            title = "Groceries delivery",
            description = "Weekly groceries",
            dateTime = at,
        ),
    )
}

@Composable
private fun SearchPreview(
    theme: Theme,
    query: String,
    transactions: ImmutableList<TransactionHistoryItem>,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            SearchUi(
                uiState = SearchState(
                    searchQuery = query,
                    transactions = transactions,
                    baseCurrency = "BGN",
                    accounts = persistentListOf(previewAccount),
                    categories = persistentListOf(previewCategory),
                    shouldShowAccountSpecificColorInTransactions = false
                ),
                onEvent = {}
            )
        }
    }
}

@Preview
@Composable
private fun PreviewEmptyQuery() {
    SearchPreview(Theme.LIGHT, query = "", transactions = persistentListOf())
}

private fun themeOf(isDark: Boolean): Theme = if (isDark) Theme.DARK else Theme.LIGHT

/** For screenshot testing: a query with matching transactions */
@Composable
fun SearchUiTest(isDark: Boolean) {
    SearchPreview(themeOf(isDark), query = "groceries", transactions = previewHistory())
}

/** For screenshot testing: a query nothing matches */
@Composable
fun SearchNoResultsUiTest(isDark: Boolean) {
    SearchPreview(themeOf(isDark), query = "no such payment", transactions = persistentListOf())
}
