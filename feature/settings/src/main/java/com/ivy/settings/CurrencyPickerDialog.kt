package com.ivy.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency

private val CurrencyListMaxHeight = 400.dp

@Composable
internal fun CurrencyPickerDialog(
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val allCurrencies = remember { IvyCurrency.getAvailable() }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            allCurrencies
        } else {
            allCurrencies.filter {
                it.code.contains(q, ignoreCase = true) ||
                    it.name.contains(q, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.set_currency)) },
        text = {
            Column {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(text = stringResource(R.string.search)) },
                )
                LazyColumn(modifier = Modifier.heightIn(max = CurrencyListMaxHeight)) {
                    currencyGroup(
                        titleResId = R.string.fiat_currencies,
                        currencies = filtered.filterNot(IvyCurrency::isCrypto),
                        selectedCode = selectedCode,
                        onSelect = onSelect,
                    )
                    currencyGroup(
                        titleResId = R.string.crypto_currencies,
                        currencies = filtered.filter(IvyCurrency::isCrypto),
                        selectedCode = selectedCode,
                        onSelect = onSelect,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
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
    item(key = "group_$titleResId") {
        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            text = stringResource(titleResId),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    items(currencies, key = { it.code }) { currency ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(currency.code) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "${currency.code} — ${currency.name}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (currency.code == selectedCode) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
