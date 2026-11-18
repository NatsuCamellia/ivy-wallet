package com.ivy.navigation

/**
 * Marks a screen in the Ivy Wallet's navigation graph.
 * Extend it when creating a new screen.
 */
sealed interface Screen {
    /**
     * Marks whether a given screen is a legacy screen. `NavigationRoot` does not act on it:
     * it never clears the activity's ViewModel store, which also holds `RootViewModel`.
     */
    val isLegacy: Boolean
        get() = false
}
