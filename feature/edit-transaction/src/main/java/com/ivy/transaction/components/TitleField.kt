package com.ivy.transaction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.SUGGESTIONS_LIMIT

@Composable
fun TitleField(
    type: TransactionType,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    suggestions: Set<String>,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(text = stringResource(hintFor(type))) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = true,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
        )
        if (value.text.isBlank() && suggestions.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(suggestions.take(SUGGESTIONS_LIMIT).toList()) { suggestion ->
                    SuggestionChip(
                        onClick = { onValueChange(selectEndTextFieldValue(suggestion)) },
                        label = { Text(text = suggestion) },
                    )
                }
            }
        }
    }
}

private fun hintFor(type: TransactionType): Int = when (type) {
    TransactionType.INCOME -> R.string.income_title
    TransactionType.EXPENSE -> R.string.expense_title
    TransactionType.TRANSFER -> R.string.transfer_title
}
