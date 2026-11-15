package com.ivy.onboarding.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingProgressSlider
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.ui.R

@Composable
fun OnboardingType(
    onStartImport: () -> Unit,
    onStartFresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingToolbar(
                hasSkip = false,
                onBack = { nav.onBackPressed() },
                onSkip = {}
            )
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
            Image(
                painter = painterResource(id = R.drawable.onboarding_illustration_import),
                contentDescription = "import illustration",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )

            OnboardingProgressSlider(
                selectedStep = 0,
                stepsCount = 4,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedCard(
                onClick = onStartImport,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.import_backup_file)) },
                    supportingContent = { Text(stringResource(R.string.from_ivy_or_another_app)) },
                    leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            Text(
                text = stringResource(R.string.importing_another_time_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                onClick = onStartFresh,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.start_fresh)) },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        trailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            OnboardingType(
                onStartImport = {},
                onStartFresh = {}
            )
        }
    }
}

/** For screenshot testing */
@Composable
fun OnboardingTypeUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}
