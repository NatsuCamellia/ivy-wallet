package com.ivy.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Suggestions(
    suggestions: List<Any>,
    onAddSuggestion: (Any) -> Unit,
    modifier: Modifier = Modifier,
    onAddNew: () -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach {
            val name = when (it) {
                is CreateAccountData -> it.name
                is CreateCategoryData -> it.name
                else -> null
            }
            if (name != null) {
                NameChip(name = name, onClick = { onAddSuggestion(it) })
            }
        }

        NameChip(
            name = stringResource(R.string.add_new),
            onClick = onAddNew,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun NameChip(
    name: String,
    onClick: () -> Unit,
    containerColor: Color = Color.Unspecified,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(name) },
        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
        colors = AssistChipDefaults.assistChipColors(containerColor = containerColor),
    )
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        Suggestions(
            suggestions = listOf(
                CreateAccountData("Cash", "USD", Color.Green, null, 0.0),
                CreateAccountData("Bank", "USD", Color.Red, null, 0.0),
                CreateAccountData("Revolut", "USD", Color.Magenta, null, 0.0),
            ),
            onAddSuggestion = { }
        ) {
        }
    }
}
