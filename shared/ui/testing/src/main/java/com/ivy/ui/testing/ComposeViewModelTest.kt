package com.ivy.ui.testing

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.paparazzi.Paparazzi
import app.cash.turbine.test
import com.ivy.ui.ComposeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule

open class ComposeViewModelTest {
    @get:Rule
    val paparazzi = Paparazzi()
}

/**
 * Runs a [ComposeViewModel] test simulation.
 * Compose runtime effects are executed [RecompositionMode.Immediate].
 * @param events pass the events that have occurred in your simulation
 * @param verify assert what's the expected state after all the events
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <UiState, UiEvent> ComposeViewModel<UiState, UiEvent>.runTest(
    events: List<UiEvent> = emptyList(),
    verify: UiState.() -> Unit
) {
    try {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val viewModel = this
        kotlinx.coroutines.test.runTest(UnconfinedTestDispatcher()) {
            moleculeFlow(mode = RecompositionMode.Immediate) {
                viewModel.uiState()
            }.test {
                events.onEach(viewModel::onEvent)
                verify(expectMostRecentItem())
                // Use cancelAndIgnoreRemainingEvents() instead of cancel(): ViewModels can keep
                // recomposing after we've sampled the state we care about (e.g. onStart() work
                // that hops onto the real Dispatchers.IO and lands after expectMostRecentItem()
                // returns). Plain cancel() still runs Turbine's ensureAllEventsConsumed() check,
                // so any such late, uninteresting recomposition fails the test with a
                // TurbineAssertionError even though verify() already got the state it wanted.
                cancelAndIgnoreRemainingEvents()
            }
        }
    } finally {
        Dispatchers.resetMain()
    }
}
