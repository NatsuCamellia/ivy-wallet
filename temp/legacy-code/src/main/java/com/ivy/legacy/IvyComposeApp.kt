package com.ivy.legacy

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.ivy.domain.RootScreen

@Deprecated("Legacy code. Don't use it, please.")
@Composable
fun ivyWalletCtx(): IvyWalletCtx = LocalWalletCtx.current

@Deprecated("Legacy code. Don't use it, please.")
@Composable
fun rootView(): View = LocalView.current

@Composable
fun rootScreen(): RootScreen = LocalContext.current as RootScreen
