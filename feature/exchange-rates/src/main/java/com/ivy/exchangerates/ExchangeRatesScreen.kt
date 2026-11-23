package com.ivy.exchangerates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.exchangerates.component.RateItem
import com.ivy.exchangerates.data.RateUi
import com.ivy.exchangerates.modal.AddRateModal
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.sheet.AmountModal
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.ui.withFabClearance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Composable
fun ExchangeRatesScreen() {
    val viewModel: ExchangeRatesViewModel = viewModel()
    val state = viewModel.uiState()

    UI(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun UI(
    state: RatesState,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    onEvent: (RatesEvent) -> Unit,
) {
    val nav = navigation()
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var amountModalVisible by remember { mutableStateOf(false) }
    var addRateModalVisible by remember { mutableStateOf(false) }
    var rateToUpdate by remember { mutableStateOf<RateUi?>(null) }
    var amountModalId by remember { mutableStateOf(UUID.randomUUID()) }
    val onRateClick = { rate: RateUi ->
        rateToUpdate = rate
        amountModalId = UUID.randomUUID()
        amountModalVisible = true
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exchange_rates)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
            )
        },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_manual_exchange_rate)
            ExtendedFloatingActionButton(
                onClick = { addRateModalVisible = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(addLabel) },
                modifier = Modifier
                    .testTag("rates_add")
                    .semantics { contentDescription = addLabel }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding.withFabClearance(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            item(key = "search") {
                SearchField(
                    query = query,
                    onQueryChange = {
                        query = it
                        onEvent(RatesEvent.Search(it))
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // the rates are filtered by the base currency, so they are empty until it has loaded
            if (state.baseCurrency.isNotEmpty() && state.manual.isEmpty() && state.automatic.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emptyStateTitle = stringResource(
                            if (query.isBlank()) R.string.no_exchange_rates else R.string.no_exchange_rates_match
                        ),
                        emptyStateText = stringResource(
                            if (query.isBlank()) {
                                R.string.no_exchange_rates_description
                            } else {
                                R.string.no_exchange_rates_for_search
                            }
                        ),
                        icon = Icons.Outlined.CurrencyExchange,
                    )
                }
            }
            ratesSection(
                title = R.string.rates_manual,
                keyPrefix = "manual",
                rates = state.manual,
                onRateClick = onRateClick,
                onDelete = { onEvent(RatesEvent.RemoveOverride(it)) },
            )
            ratesSection(
                title = R.string.rates_automatic,
                keyPrefix = "automatic",
                rates = state.automatic,
                onRateClick = onRateClick,
            )
        }
    }

    AddRateModal(
        visible = addRateModalVisible,
        baseCurrency = state.baseCurrency,
        dismiss = { addRateModalVisible = false },
        onAdd = onEvent
    )

    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = "",
        initialAmount = rateToUpdate?.rate,
        dismiss = { amountModalVisible = false },
        decimalCountMax = 12,
        Header = {
            rateToUpdate?.let {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = "${it.from}–${it.to}",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        },
        onAmountChanged = { newRate ->
            rateToUpdate?.let { onEvent(RatesEvent.UpdateRate(it, newRate)) }
        }
    )
}

private fun LazyListScope.ratesSection(
    title: Int,
    keyPrefix: String,
    rates: ImmutableList<RateUi>,
    onRateClick: (RateUi) -> Unit,
    onDelete: ((RateUi) -> Unit)? = null,
) {
    if (rates.isEmpty()) return
    item(key = "${keyPrefix}_header") {
        Text(
            text = stringResource(title),
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    itemsIndexed(rates, key = { _, rate -> "${keyPrefix}_${rate.from}_${rate.to}" }) { index, rate ->
        RateItem(
            rate = rate,
            index = index,
            count = rates.size,
            onClick = { onRateClick(rate) },
            onDelete = onDelete?.let { { it(rate) } },
        )
    }
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
        placeholder = { Text(stringResource(R.string.search_currency)) },
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

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(
                state = RatesState(
                    baseCurrency = "BGN",
                    manual = persistentListOf(
                        RateUi("BGN", "USD", 1.85),
                        RateUi("BGN", "EUR", 1.96),
                    ),
                    automatic = persistentListOf(
                        RateUi("BGN", "GBP", 0.43),
                        RateUi("BGN", "CHF", 0.49),
                        RateUi("BGN", "JPY", 80.12),
                        RateUi("BGN", "BTC", 0.00001234),
                        RateUi("BGN", "CAD", 0.76),
                        RateUi("BGN", "AUD", 0.85),
                        RateUi("BGN", "SEK", 5.71),
                        RateUi("BGN", "NOK", 5.88),
                    )
                ),
                onEvent = {}
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNoResult(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(
                state = RatesState(
                    baseCurrency = "BGN",
                    manual = persistentListOf(),
                    automatic = persistentListOf(),
                ),
                initialQuery = "XYZ",
                onEvent = {}
            )
        }
    }
}

/** For screenshot testing */
@Composable
fun ExchangeRatesScreenUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun ExchangeRatesNoResultUiTest(isDark: Boolean) {
    PreviewNoResult(if (isDark) Theme.DARK else Theme.LIGHT)
}
