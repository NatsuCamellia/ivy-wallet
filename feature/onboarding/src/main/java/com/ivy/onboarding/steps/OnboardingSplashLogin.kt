package com.ivy.onboarding.steps

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.Constants
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.openUrl
import com.ivy.onboarding.OnboardingState
import com.ivy.ui.R

@Composable
fun OnboardingSplashLogin(
    onboardingState: OnboardingState,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Crossfade(
            targetState = onboardingState == OnboardingState.SPLASH,
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            label = "Splash"
        ) { splash ->
            if (splash) {
                Splash()
            } else {
                Login(onSkip = onSkip)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Splash(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Brand()
        LoadingIndicator()
    }
}

@Composable
private fun Login(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Brand()

                TextButton(
                    onClick = {
                        openUrl(
                            uriHandler = uriHandler,
                            url = Constants.URL_IVY_WALLET_REPO
                        )
                    }
                ) {
                    Text(stringResource(R.string.opensource))
                }
            }

            Text(
                text = stringResource(R.string.enter_with_offline_account),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.your_data_will_be_saved_only_locally),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.offline_account))
            }

            PrivacyPolicyAndTC()
        }
    }
}

@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ivy_wallet_logo),
            contentDescription = null
        )

        Text(
            text = "Ivy Wallet",
            style = MaterialTheme.typography.headlineLarge,
        )

        Text(
            text = stringResource(R.string.your_personal_money_manager),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PrivacyPolicyAndTC(modifier: Modifier = Modifier) {
    val terms = stringResource(R.string.terms_conditions)
    val privacy = stringResource(R.string.privacy_policy)
    val text = stringResource(R.string.by_signing_in, terms, privacy)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )

    Text(
        text = buildAnnotatedString {
            append(text)
            val tcStart = text.indexOf(terms)
            addLink(LinkAnnotation.Url(Constants.URL_TC, linkStyles), tcStart, tcStart + terms.length)
            val privacyStart = text.indexOf(privacy)
            addLink(
                LinkAnnotation.Url(Constants.URL_PRIVACY_POLICY, linkStyles),
                privacyStart,
                privacyStart + privacy.length
            )
        },
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    onboardingState: OnboardingState = OnboardingState.SPLASH
) {
    WalletPreview(theme) {
        OnboardingSplashLogin(
            onboardingState = onboardingState,
            onSkip = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun OnboardingSplashUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme, OnboardingState.SPLASH)
}

/** For screenshot testing */
@Composable
fun OnboardingLoginUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme, OnboardingState.LOGIN)
}
