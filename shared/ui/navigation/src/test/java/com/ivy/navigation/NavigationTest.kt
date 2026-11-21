package com.ivy.navigation

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class NavigationTest {
    private val discarded = mutableListOf<Screen>()
    private val navigation = Navigation().also { it.onScreenDiscarded = discarded::add }

    @Test
    fun navigateToKeepsThePreviousScreenInTheBackStack() {
        navigation.navigateTo(MainScreen)
        navigation.navigateTo(SettingsScreen)

        discarded.shouldContainExactly()
        navigation.currentScreen shouldBe SettingsScreen
    }

    @Test
    fun backDiscardsTheScreenItLeaves() {
        navigation.navigateTo(MainScreen)
        navigation.navigateTo(SettingsScreen)
        navigation.back()

        discarded.shouldContainExactly(SettingsScreen)
        navigation.currentScreen shouldBe MainScreen
    }

    @Test
    fun resetBackStackDiscardsTheStackAndTheReplacedScreen() {
        navigation.navigateTo(OnboardingScreen)
        navigation.navigateTo(SettingsScreen)
        navigation.resetBackStack()
        navigation.navigateTo(MainScreen)

        discarded.shouldContainExactly(OnboardingScreen, SettingsScreen)
    }

    @Test
    fun aScreenStillInTheBackStackIsNotDiscarded() {
        navigation.navigateTo(MainScreen)
        navigation.navigateTo(SettingsScreen)
        navigation.navigateTo(MainScreen)
        navigation.back()

        discarded.shouldContainExactly()
        navigation.currentScreen shouldBe SettingsScreen
    }

    @Test
    fun navigatingToTheCurrentScreenDiscardsNothing() {
        navigation.navigateTo(MainScreen)
        navigation.navigateTo(MainScreen)
        navigation.back()

        discarded.shouldContainExactly()
        navigation.currentScreen shouldBe MainScreen
        navigation.backStackEmpty() shouldBe true
    }

    @Test
    fun backOnAnEmptyStackDoesNothing() {
        navigation.navigateTo(MainScreen)

        navigation.back() shouldBe false

        discarded.shouldContainExactly()
        navigation.currentScreen shouldBe MainScreen
    }
}
