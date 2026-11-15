package com.ivy.wallet.ui.applocked

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.hasLockScreen
import com.ivy.ui.R

@Composable
fun AppLockedScreen(
    onShowOSBiometricsModal: () -> Unit,
    onContinueWithoutAuthentication: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnContinueWithoutAuthentication by rememberUpdatedState(
        newValue = onContinueWithoutAuthentication
    )
    val latestOnShowOSBiometricsModal by rememberUpdatedState(onShowOSBiometricsModal)
    val context = LocalContext.current

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_locked),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.authenticate_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = {
                    osAuthentication(
                        context = context,
                        onShowOSBiometricsModal = latestOnShowOSBiometricsModal,
                        onContinueWithoutAuthentication = latestOnContinueWithoutAuthentication
                    )
                },
            ) {
                Text(text = stringResource(R.string.unlock))
            }
        }
    }

    // To automatically launch the biometric screen on load of this composable
    LaunchedEffect(true) {
        osAuthentication(
            context = context,
            onShowOSBiometricsModal = latestOnShowOSBiometricsModal,
            onContinueWithoutAuthentication = latestOnContinueWithoutAuthentication
        )
    }
}

private fun osAuthentication(
    context: Context,
    onShowOSBiometricsModal: () -> Unit,
    onContinueWithoutAuthentication: () -> Unit
) {
    if (hasLockScreen(context)) {
        onShowOSBiometricsModal()
    } else {
        onContinueWithoutAuthentication()
    }
}

@Preview
@Composable
@Suppress("FunctionNaming", "UnusedPrivateMember")
private fun Preview_Locked() {
    WalletPreview {
        AppLockedScreen(
            onContinueWithoutAuthentication = {},
            onShowOSBiometricsModal = {}
        )
    }
}
