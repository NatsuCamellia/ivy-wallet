package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency

@OptIn(ExperimentalLayoutApi::class)
@Suppress("ParameterNaming")
@Composable
fun CurrencyPicker(
    initialSelectedCurrency: IvyCurrency?,
    includeKeyboardShownInsetSpacer: Boolean,
    modifier: Modifier = Modifier,
    preselectedCurrency: IvyCurrency = IvyCurrency.getDefault(),
    lastItemSpacer: Dp = 0.dp,
    onKeyboardShown: (keyboardVisible: Boolean) -> Unit = {},
    onSelectedCurrencyChanged: (IvyCurrency) -> Unit
) {
    val keyboardShown = WindowInsets.isImeVisible
    val currentOnKeyboardShown by rememberUpdatedState(onKeyboardShown)
    LaunchedEffect(keyboardShown) { currentOnKeyboardShown(keyboardShown) }

    var preselected by remember {
        mutableStateOf(initialSelectedCurrency == null)
    }
    var selectedCurrency by remember {
        mutableStateOf(initialSelectedCurrency ?: preselectedCurrency)
    }
    var searchTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!keyboardShown) {
                SelectedCurrencyCard(
                    currency = selectedCurrency,
                    preselected = preselected
                )
            }

            SearchInput(searchTextFieldValue = searchTextFieldValue) {
                searchTextFieldValue = it
            }
        }

        CurrencyList(
            searchQueryLowercase = searchTextFieldValue.text.lowercase(LocalConfiguration.current.locales[0]),
            selectedCurrency = selectedCurrency,
            lastItemSpacer = if (includeKeyboardShownInsetSpacer) {
                WindowInsets.ime.asPaddingValues().calculateBottomPadding() + lastItemSpacer
            } else {
                lastItemSpacer
            },
        ) {
            preselected = false
            selectedCurrency = it
            onSelectedCurrencyChanged(it)
        }
    }
}

@Composable
private fun SearchInput(
    searchTextFieldValue: TextFieldValue,
    onSetSearchTextFieldValue: (TextFieldValue) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = searchTextFieldValue,
        onValueChange = { onSetSearchTextFieldValue(it.copy(it.text.trim())) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_input"),
        placeholder = { Text(stringResource(R.string.search_currency)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Text
        ),
    )
}

@Composable
private fun SelectedCurrencyCard(
    currency: IvyCurrency,
    preselected: Boolean,
) {
    val container = if (preselected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val content = if (preselected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        ListItem(
            leadingContent = { Icon(Icons.Default.Check, contentDescription = null) },
            supportingContent = { Text(currency.name) },
            trailingContent = {
                Text(stringResource(if (preselected) R.string.pre_selected else R.string.selected_text))
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = content,
                leadingContentColor = content,
                supportingContentColor = content,
                trailingContentColor = content,
            ),
        ) {
            Text(currency.code, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
@Suppress("ParameterNaming")
private fun CurrencyList(
    searchQueryLowercase: String,
    selectedCurrency: IvyCurrency,
    lastItemSpacer: Dp,
    onCurrencySelected: (IvyCurrency) -> Unit
) {
    val currencies = IvyCurrency.getAvailable()
        .filter {
            searchQueryLowercase.isBlank() ||
                it.code.toLowerCaseLocal().startsWith(searchQueryLowercase) ||
                it.name.toLowerCaseLocal().startsWith(searchQueryLowercase)
        }
        .sortedBy { it.code }
        .sortedBy { it.isCrypto }

    val currenciesWithLetters = mutableListOf<Any>()

    var lastFirstLetter: String? = null
    for (currency in currencies) {
        val firstLetter = if (currency.isCrypto) stringResource(R.string.crypto) else currency.code.first().toString()
        if (firstLetter != lastFirstLetter) {
            currenciesWithLetters.add(
                LetterDivider(
                    letter = firstLetter
                )
            )
            lastFirstLetter = firstLetter
        }

        currenciesWithLetters.add(currency)
    }

    val listState = remember(searchQueryLowercase) { LazyListState() }

    LazyColumn(state = listState) {
        items(currenciesWithLetters) { item ->
            when (item) {
                is IvyCurrency -> {
                    ListItem(
                        selected = item == selectedCurrency,
                        onClick = { onCurrencySelected(item) },
                        supportingContent = { Text(item.name) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) {
                        Text(item.code)
                    }
                }

                is LetterDivider -> {
                    Text(
                        text = item.letter,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(ListItemDefaults.ContentPadding),
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(lastItemSpacer))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        CurrencyPicker(
            initialSelectedCurrency = null,
            includeKeyboardShownInsetSpacer = true
        ) {
        }
    }
}

private data class LetterDivider(
    val letter: String
)
