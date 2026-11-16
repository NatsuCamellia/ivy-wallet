package com.ivy.navigation

/**
 * Marks a screen in the Ivy Wallet's navigation graph.
 * Extend it when creating a new screen.
 */
sealed interface Screen {
    /**
     * Marks whether a given screen keeps its ViewModel store when the user leaves it.
     * `NavigationRoot` clears the ViewModel store when the user leaves a screen whose
     * `isLegacy` is false.
     */
    val isLegacy: Boolean
        get() = false
}
