package com.ivy.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalNavigation = compositionLocalOf<Navigation> { error("No LocalNavigation") }

@Composable
fun NavigationRoot(
    navigation: Navigation,
    navGraph: @Composable (screen: Screen?) -> Unit
) {
    val stateHolder = rememberSaveableStateHolder()
    DisposableEffect(navigation, stateHolder) {
        navigation.onScreenDiscarded = { stateHolder.removeState(it.stateKey()) }
        onDispose { navigation.onScreenDiscarded = null }
    }
    CompositionLocalProvider(
        LocalNavigation provides navigation,
    ) {
        // Do NOT clear LocalViewModelStoreOwner's store here: it is the Activity's store and
        // also holds RootViewModel — clearing it blanks the app on the next root recomposition.
        // Screen-scoped ViewModel destruction needs a per-screen ViewModelStoreOwner instead.
        val screen = navigation.currentScreen
        stateHolder.SaveableStateProvider(screen?.stateKey() ?: "") {
            navGraph(screen)
        }
    }
}

// Screens in the back stack keep their saveable state (app bar, scroll) while they are off screen.
private fun Screen.stateKey(): String = "${this::class.java.name}:${hashCode()}"

@Composable
fun navigation(): Navigation {
    return LocalNavigation.current
}

/**
 * Provides a [ViewModel] instance from the current [LocalViewModelStoreOwner]
 * (in practice the Activity), so it currently lives until the Activity is destroyed.
 * True per-screen scoping requires a per-screen ViewModelStoreOwner and is not
 * implemented yet.
 */
@Composable
inline fun <reified T : ViewModel> screenScopedViewModel(
    factory: ViewModelProvider.Factory? = null
): T {
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    requireNotNull(viewModelStoreOwner) { "No ViewModelStoreOwner provided" }
    val viewModelProvider = factory?.let {
        ViewModelProvider(viewModelStoreOwner, it)
    } ?: ViewModelProvider(viewModelStoreOwner)
    return viewModelProvider[T::class.java]
}
