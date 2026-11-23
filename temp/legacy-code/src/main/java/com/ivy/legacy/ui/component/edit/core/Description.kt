package com.ivy.legacy.ui.component.edit.core

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.ui.R

@Composable
fun Description(
    description: String?,
    onAddDescription: () -> Unit,
    onEditDescription: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasDescription = description.isNotNullOrBlank()
    Card(
        onClick = {
            if (hasDescription) onEditDescription(description.orEmpty()) else onAddDescription()
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        ListItem(
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
            overlineContent = if (hasDescription) {
                { Text(stringResource(R.string.description)) }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(
                text = if (hasDescription) description.orEmpty() else stringResource(R.string.add_description),
                modifier = Modifier.testTag("trn_description"),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDescriptionEmpty() {
    WalletPreview(Theme.LIGHT) {
        Description(description = null, onAddDescription = {}, onEditDescription = {})
    }
}

@Preview
@Composable
private fun PreviewDescriptionWithText() {
    WalletPreview(Theme.LIGHT) {
        Description(description = "This is my sample description.", onAddDescription = {}, onEditDescription = {})
    }
}
