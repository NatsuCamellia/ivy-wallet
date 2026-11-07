package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview

@SuppressLint("ComposeModifierMissing")
@Composable
fun ProgressModal(
    title: String,
    description: String,
    visible: Boolean,
    dismiss: () -> Unit = {},
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {},
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text(description)

                    ContainedLoadingIndicator()
                }
            },
        )
    }
}

/** For screenshot testing */
@Composable
fun ProgressModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        ProgressModal(
            title = "Exporting data",
            description = "This may take a few minutes. Please do not close the app.",
            visible = true
        )
    }
}
