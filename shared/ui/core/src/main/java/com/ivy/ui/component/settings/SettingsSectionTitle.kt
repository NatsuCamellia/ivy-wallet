package com.ivy.ui.component.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMaterial3Theme

@Composable
fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        modifier = modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Preview
@Composable
private fun SettingsSectionTitlePreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        SettingsSectionTitle(text = "Appearance")
    }
}
