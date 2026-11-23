package com.ivy.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import kotlinx.coroutines.flow.drop

/** Searchable fiat and crypto currencies; a tap on a row selects it, the caller dismisses. */
@Composable
internal fun CurrencyPickerDialog(
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val allCurrencies = remember { IvyCurrency.getAvailable() }
    val matching = remember(query) {
        val trimmed = query.trim()
        allCurrencies.filter {
            it.code.contains(trimmed, ignoreCase = true) || it.name.contains(trimmed, ignoreCase = true)
        }.sortedBy { it.code }.partition { !it.isCrypto }
    }
    val (fiat, crypto) = matching
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = remember {
            val fiatRow = fiat.indexOfFirst { it.code == selectedCode }
            val cryptoRow = crypto.indexOfFirst { it.code == selectedCode }
            when {
                fiatRow >= 0 -> fiatRow + 1
                cryptoRow >= 0 -> fiat.size + 1 + cryptoRow + 1
                else -> 0
            }
        },
    )

    LaunchedEffect(Unit) {
        snapshotFlow { query }.drop(1).collect { listState.scrollToItem(0) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = { Text(text = stringResource(R.string.set_currency)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = stringResource(R.string.search)) },
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.selectableGroup(), state = listState) {
                    currencyGroup(R.string.fiat_currencies, fiat, selectedCode, onSelect)
                    currencyGroup(R.string.crypto_currencies, crypto, selectedCode, onSelect)
                }
            }
        },
    )
}

private fun LazyListScope.currencyGroup(
    titleResId: Int,
    currencies: List<IvyCurrency>,
    selectedCode: String,
    onSelect: (String) -> Unit,
) {
    if (currencies.isEmpty()) return
    item(key = titleResId) {
        Text(
            text = stringResource(titleResId),
            modifier = Modifier.padding(ListItemDefaults.ContentPadding),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    items(currencies, key = { it.code }) { currency ->
        ListItem(
            selected = currency.code == selectedCode,
            onClick = { onSelect(currency.code) },
            leadingContent = { RadioButton(selected = currency.code == selectedCode, onClick = null) },
            supportingContent = { Text(text = currency.name) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(text = currency.code)
        }
    }
}
