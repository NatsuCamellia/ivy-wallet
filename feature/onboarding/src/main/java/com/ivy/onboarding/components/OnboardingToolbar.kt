package com.ivy.onboarding.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R
import com.ivy.ui.component.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingToolbar(
    hasSkip: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    TopAppBar(
        title = { title?.let { Text(it) } },
        modifier = modifier,
        navigationIcon = { BackButton(onClick = onBack) },
        actions = {
            if (hasSkip) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.skip))
                }
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        OnboardingToolbar(
            hasSkip = true,
            onBack = {},
            onSkip = {},
        )
    }
}
