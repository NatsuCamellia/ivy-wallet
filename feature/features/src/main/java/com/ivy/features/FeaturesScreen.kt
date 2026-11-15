package com.ivy.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
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
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Title()
                },
                navigationIcon = {
                    BackButton()
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
private fun BackButton(
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    IconButton(
        modifier = modifier,
        onClick = {
            nav.back()
        }
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back"
        )
    }
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
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W700
                )
                if (feature.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = feature.enabled, onCheckedChange = { onToggleClick() })
        }
    }
}

@Composable
private fun FeatureSectionDivider(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        )
    }
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
                            key = "compact_mode",
                            name = "Compact mode",
                            enabled = false,
                            description = "Show more items on the screen.",
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
