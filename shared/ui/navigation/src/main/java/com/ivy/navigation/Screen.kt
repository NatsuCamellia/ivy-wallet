package com.ivy.navigation

/**
 * Marks a screen in the Ivy Wallet's navigation graph.
 * Extend it when creating a new screen.
 */
sealed interface Screen {
    /**
     * Marks whether a given screen keeps the activity's ViewModel store when it is shown.
     * When the shown screen changes to a screen whose `isLegacy` is false, `NavigationRoot`
     * clears the store before that screen composes.
     */
    val isLegacy: Boolean
        get() = false
}
