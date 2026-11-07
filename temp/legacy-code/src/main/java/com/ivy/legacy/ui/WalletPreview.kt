package com.ivy.legacy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.appContext
import com.ivy.base.resource.AndroidResourceProvider
import com.ivy.base.time.impl.DeviceTimeProvider
import com.ivy.base.time.impl.StandardTimeConverter
import com.ivy.legacy.IvyWalletCtx
import com.ivy.ui.time.impl.AndroidDevicePreferences
import com.ivy.ui.time.impl.IvyTimeFormatter

@Composable
fun WalletPreview(
    theme: Theme = Theme.LIGHT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    appContext = context
    val ctx = remember(theme) { IvyWalletCtx().apply { switchTheme(theme) } }
    val timeProvider = remember { DeviceTimeProvider() }
    val timeConverter = remember { StandardTimeConverter(timeProvider) }
    val timeFormatter = remember {
        IvyTimeFormatter(
            resourceProvider = AndroidResourceProvider(context),
            timeProvider = timeProvider,
            converter = timeConverter,
            devicePreferences = AndroidDevicePreferences(context)
        )
    }
    WalletRoot(ctx, timeConverter, timeProvider, timeFormatter, content)
}
