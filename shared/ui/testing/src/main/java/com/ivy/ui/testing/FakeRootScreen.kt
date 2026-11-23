package com.ivy.ui.testing

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.ivy.domain.RootScreen

/** For screens that cast `LocalContext` to [RootScreen], which Paparazzi's context isn't. */
@Composable
fun WithFakeRootScreen(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContext provides FakeRootScreenContext(LocalContext.current),
        content = content
    )
}

private class FakeRootScreenContext(base: Context) : ContextWrapper(base), RootScreen {
    override val isDebug = false
    override val buildVersionName = "1.0.0"
    override val buildVersionCode = 1

    override fun reviewIvyWallet(dismissReviewCard: Boolean): Unit = Unit
    override fun shareIvyWallet(): Unit = Unit
    override fun openUrlInBrowser(url: String): Unit = Unit
    override fun shareCSVFile(fileUri: Uri): Unit = Unit
    override fun shareZipFile(fileUri: Uri): Unit = Unit
    override fun openGooglePlayAppPage(appId: String): Unit = Unit
    override fun <T> pinWidget(widget: Class<T>): Unit = Unit
}
