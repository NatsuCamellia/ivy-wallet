package com.ivy.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Stack
import javax.inject.Inject
import javax.inject.Singleton

@Stable
@Singleton
class Navigation @Inject constructor() {
  var currentScreen: Screen? by mutableStateOf(null)
    private set

  @Deprecated("Legacy code. Use Compose `BackHandler {}` instead.")
  val onBackPressed: MutableMap<Screen, () -> Boolean> = mutableMapOf()

  private val backStack: Stack<Screen> = Stack()
  internal var onScreenDiscarded: ((Screen) -> Unit)? = null
  var lastScreen: Screen? = null
    private set

  fun navigateTo(screen: Screen) {
    if (lastScreen != null) {
      backStack.push(lastScreen)
    }
    switchScreen(screen)
  }

  fun backStackEmpty() = backStack.empty()

  private fun popBackStack() {
    discard(backStack.pop())
  }

  @Deprecated("Legacy code. Don't use it, please.")
  fun onBackPressed(): Boolean {
    val specialHandling = onBackPressed.getOrDefault(currentScreen) { false }.invoke()
    return specialHandling || back()
  }

  fun back(): Boolean {
    if (!backStack.empty()) {
      switchScreen(backStack.pop())
      return true
    }
    return false
  }

  private fun switchScreen(screen: Screen) {
    val previous = currentScreen
    this.currentScreen = screen
    lastScreen = screen
    if (previous != null) {
      discard(previous)
    }
  }

  private fun discard(screen: Screen) {
    if (screen != currentScreen && screen !in backStack) {
      onScreenDiscarded?.invoke(screen)
    }
  }

  fun resetBackStack() {
    while (!backStackEmpty()) {
      popBackStack()
    }
    lastScreen = null
  }
}
