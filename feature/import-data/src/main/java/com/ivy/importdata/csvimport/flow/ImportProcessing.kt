package com.ivy.importdata.csvimport.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportProcessing(
    progressPercent: Int,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContainedLoadingIndicator()

            Text(
                text = stringResource(R.string.please_wait),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.headlineMedium,
            )

            LinearWavyProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.importing_the_csv_file),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        ImportProcessing(progressPercent = 49)
    }
}
