package com.ivy.onboarding.steps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.CurrencyPicker
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency

@Composable
fun OnboardingSetCurrency(
    preselectedCurrency: IvyCurrency,
    onSetCurrency: (IvyCurrency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currency by remember { mutableStateOf(preselectedCurrency) }
    val nav = navigation()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            OnboardingToolbar(
                hasSkip = false,
                onBack = { nav.onBackPressed() },
                onSkip = {},
                title = stringResource(R.string.set_currency)
            )
        },
        bottomBar = {
            Button(
                onClick = { onSetCurrency(currency) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .testTag("set_currency_save")
            ) {
                Text(stringResource(R.string.set))
            }
        }
    ) { innerPadding ->
        CurrencyPicker(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            initialSelectedCurrency = null,
            preselectedCurrency = preselectedCurrency,
            includeKeyboardShownInsetSpacer = false,
        ) {
            currency = it
        }
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            OnboardingSetCurrency(
                preselectedCurrency = IvyCurrency.getDefault(),
                onSetCurrency = {}
            )
        }
    }
}

/** For screenshot testing */
@Composable
fun OnboardingSetCurrencyUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}
