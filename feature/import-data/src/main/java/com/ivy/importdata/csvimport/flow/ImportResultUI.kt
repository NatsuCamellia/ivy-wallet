package com.ivy.importdata.csvimport.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.backup.ImportResult
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.format
import com.ivy.navigation.CSVScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ImportResultUI(
    result: ImportResult,
    launchedFromOnboarding: Boolean,
    modifier: Modifier = Modifier,
    isManualCsvImport: Boolean = false,
    onTryAgain: (() -> Unit)? = null,
    onFinish: () -> Unit,
) {
    val nav = navigation()
    val importSuccess = result.transactionsImported > 0 &&
            result.transactionsImported > result.rowsFound / 2
    val successPercent = if (result.rowsFound > 0) {
        (result.transactionsImported / result.rowsFound.toDouble()) * 100
    } else {
        0.0
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingToolbar(
                hasSkip = false,
                onBack = { nav.onBackPressed() },
                onSkip = {},
                title = stringResource(R.string.import_data),
            )
        },
        bottomBar = {
            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.finish))
                    }

                    if (onTryAgain != null) {
                        OutlinedButton(
                            onClick = onTryAgain,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.try_again))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (importSuccess) stringResource(R.string.success) else stringResource(R.string.failure),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = if (importSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            )

            ResultCard(
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                color = MaterialTheme.colorScheme.tertiary,
                title = stringResource(R.string.imported),
                percent = successPercent,
                lines = persistentListOf(
                    stringResource(R.string.transactions_imported, result.transactionsImported),
                    stringResource(R.string.accounts_imported, result.accountsImported),
                    stringResource(R.string.categories_imported, result.categoriesImported),
                ),
            )

            ResultCard(
                icon = { Icon(Icons.Filled.Error, contentDescription = null) },
                color = MaterialTheme.colorScheme.error,
                title = stringResource(R.string.failed),
                percent = 100 - successPercent,
                lines = persistentListOf(
                    stringResource(
                        R.string.rows_from_csv_not_recognized,
                        result.rowsFound - result.transactionsImported
                    ),
                ),
            )

            if (!isManualCsvImport) {
                Text(
                    text = stringResource(R.string.csv_import_failed),
                    style = MaterialTheme.typography.bodyLarge,
                )

                FilledTonalButton(
                    onClick = {
                        nav.navigateTo(CSVScreen(launchedFromOnboarding = launchedFromOnboarding))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.manual_csv_import))
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    icon: @Composable () -> Unit,
    color: Color,
    title: String,
    percent: Double,
    lines: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        ListItem(
            overlineContent = { Text(text = title, color = color) },
            supportingContent = {
                Column {
                    lines.forEach { line ->
                        Text(text = line)
                    }
                }
            },
            leadingContent = icon,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                leadingContentColor = color,
            ),
        ) {
            Text(
                text = "${percent.format(2)}%",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        NavigationRoot(navigation = Navigation()) {
            ImportResultUI(
                result = ImportResult(
                    rowsFound = 356,
                    transactionsImported = 320,
                    accountsImported = 4,
                    categoriesImported = 13,
                    failedRows = persistentListOf()
                ),
                launchedFromOnboarding = false,
            ) {
            }
        }
    }
}
