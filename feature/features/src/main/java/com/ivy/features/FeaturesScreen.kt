package com.ivy.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.component.BackButton
import com.ivy.ui.theme.WalletTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun FeaturesScreenImpl(
    viewModel: FeaturesViewModel = screenScopedViewModel()
) {
    FeaturesUi(
        uiState = viewModel.uiState(),
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturesUi(
    uiState: FeaturesUiState,
    onEvent: (FeaturesUiEvent) -> Unit,
) {
    val nav = navigation()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Title()
                },
                navigationIcon = {
                    BackButton(onClick = nav::back)
                },
            )
        },
        content = { innerPadding ->
            Content(
                modifier = Modifier.padding(innerPadding),
                items = uiState.featureItemViewStates,
                onToggleFeature = {
                    onEvent(FeaturesUiEvent.ToggleFeature(it))
                }
            )
        }
    )
}

@Composable
private fun Title(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = "Features",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun Content(
    items: ImmutableList<FeatureItemViewState>,
    onToggleFeature: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            when (item) {
                is FeatureItemViewState.FeatureHeaderViewState -> {
                    FeatureSectionDivider(text = item.name)
                }

                is FeatureItemViewState.FeatureToggleViewState -> {
                    FeatureRow(
                        feature = item,
                        onToggleClick = { onToggleFeature(item.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    feature: FeatureItemViewState.FeatureToggleViewState,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        onClick = onToggleClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                )
                if (feature.description != null) {
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Switch(checked = feature.enabled, onCheckedChange = { onToggleClick() })
        }
    }
}

@Composable
private fun FeatureSectionDivider(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(start = 16.dp, top = 16.dp),
        text = text,
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview
@Composable
private fun PreviewFeaturesUi(isDark: Boolean = false) {
    WalletTheme(if (isDark) Theme.DARK else Theme.LIGHT) {
        NavigationRoot(navigation = Navigation()) {
            FeaturesUi(
                uiState = FeaturesUiState(
                    featureItemViewStates = persistentListOf(
                        FeatureItemViewState.FeatureHeaderViewState(name = "Experimental"),
                        FeatureItemViewState.FeatureToggleViewState(
                            key = "standard_keypad",
                            name = "Standard keypad layout",
                            enabled = true,
                            description = "Use the standard 1-2-3 numeric keypad layout.",
                        ),
                        FeatureItemViewState.FeatureToggleViewState(
                            key = "account_colors",
                            name = "Colorful account labels",
                            enabled = false,
                            description = "Display account-specific colors in transactions.",
                        ),
                        FeatureItemViewState.FeatureHeaderViewState(name = "Debug"),
                        FeatureItemViewState.FeatureToggleViewState(
                            key = "extra_logging",
                            name = "Extra logging",
                            enabled = false,
                            description = null,
                        ),
                    )
                ),
                onEvent = {}
            )
        }
    }
}

/** For screenshot testing */
@Composable
fun FeaturesUiTest(isDark: Boolean) {
    PreviewFeaturesUi(isDark)
}
