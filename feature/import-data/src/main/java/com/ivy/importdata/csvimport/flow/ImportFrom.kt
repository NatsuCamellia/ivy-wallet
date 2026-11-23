package com.ivy.importdata.csvimport.flow

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import com.ivy.legacy.ui.WalletPreview
import com.ivy.navigation.CSVScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.ui.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportFrom(
    hasSkip: Boolean,
    launchedFromOnboarding: Boolean,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    onImportFrom: (ImportType) -> Unit = {},
) {
    val nav = navigation()
    val importTypes = ImportType.values()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingToolbar(
                hasSkip = hasSkip,
                onBack = { nav.onBackPressed() },
                onSkip = onSkip,
                title = stringResource(R.string.import_data),
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Button(
                    onClick = { nav.navigateTo(CSVScreen(launchedFromOnboarding)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.manual_csv_import))
                }
            }

            item {
                Text(
                    text = stringResource(R.string.import_from),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    importTypes.forEachIndexed { index, importType ->
                        SegmentedListItem(
                            onClick = { onImportFrom(importType) },
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = importTypes.size),
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            leadingContent = {
                                Image(
                                    painter = painterResource(importType.logo()),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                )
                            },
                        ) {
                            Text(text = importType.listName())
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        NavigationRoot(navigation = Navigation()) {
            ImportFrom(
                hasSkip = true,
                launchedFromOnboarding = false,
            )
        }
    }
}
