package com.ivy.onboarding.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingProgressSlider(
    selectedStep: Int,
    stepsCount: Int,
    modifier: Modifier = Modifier,
) {
    LinearWavyProgressIndicator(
        progress = { (selectedStep + 1f) / stepsCount },
        modifier = modifier,
    )
}
