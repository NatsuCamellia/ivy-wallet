package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.SUGGESTIONS_LIMIT
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Title(
    type: TransactionType,
    titleFocus: FocusRequester,
    titleTextFieldValue: TextFieldValue,
    setTitleTextFieldValue: (TextFieldValue) -> Unit,
    suggestions: Set<String>,
    onTitleChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null,
    onNext: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = titleTextFieldValue,
            onValueChange = {
                setTitleTextFieldValue(it)
                onTitleChange(it.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(titleFocus)
                .testTag("input_field"),
            label = {
                Text(
                    when (type) {
                        TransactionType.INCOME -> stringResource(R.string.income_title)
                        TransactionType.EXPENSE -> stringResource(R.string.expense_title)
                        TransactionType.TRANSFER -> stringResource(R.string.transfer_title)
                    }
                )
            },
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
        )

        if (WindowInsets.isImeVisible && suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (suggestion in suggestions.take(SUGGESTIONS_LIMIT)) {
                    SuggestionChip(
                        onClick = {
                            setTitleTextFieldValue(selectEndTextFieldValue(suggestion))
                            onTitleChange(suggestion)
                            coroutineScope.launch {
                                // scroll to top for better UX
                                scrollState?.animateScrollTo(0)
                            }
                        },
                        label = { Text(suggestion) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTitleWithSuggestions() {
    WalletPreview(Theme.LIGHT) {
        Title(
            type = TransactionType.EXPENSE,
            titleFocus = FocusRequester(),
            titleTextFieldValue = selectEndTextFieldValue(""),
            setTitleTextFieldValue = {},
            suggestions = setOf("Tabu", "Harem", "Club 35"),
            onTitleChange = {}
        ) {
        }
    }
}
