package com.ivy.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMaterial3Theme

@Composable
fun ScreenDisplayTitle(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onDescriptionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
            Text(
                modifier = if (onDescriptionClick != null) {
                    Modifier.clickable(onClick = onDescriptionClick)
                } else {
                    Modifier
                },
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun ScreenDisplayTitlePreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        ScreenDisplayTitle(text = "Settings", description = "1.0.0 (100)")
    }
}
