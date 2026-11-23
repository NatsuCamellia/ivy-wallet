package com.ivy.disclaimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.disclaimer.composables.AcceptTermsText
import com.ivy.disclaimer.composables.AgreeButton
import com.ivy.disclaimer.composables.AgreementCheckBox
import com.ivy.disclaimer.composables.DisclaimerTopAppBar
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.component.OpenSourceCard

@Composable
fun DisclaimerScreenImpl() {
    val viewModel: DisclaimerViewModel = screenScopedViewModel()
    val viewState = viewModel.uiState()
    DisclaimerScreenUi(viewState = viewState, onEvent = viewModel::onEvent)
}

@Composable
fun DisclaimerScreenUi(
    viewState: DisclaimerViewState,
    onEvent: (DisclaimerViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DisclaimerTopAppBar()
        },
        content = { innerPadding ->
            Content(
                modifier = Modifier.padding(innerPadding),
                viewState = viewState,
                onEvent = onEvent,
            )
        }
    )
}

@Composable
private fun Content(
    viewState: DisclaimerViewState,
    onEvent: (DisclaimerViewEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OpenSourceCard()
        }
        item {
            AcceptTermsText()
        }
        itemsIndexed(items = viewState.checkboxes) { index, item ->
            AgreementCheckBox(
                viewState = item,
                onClick = {
                    onEvent(DisclaimerViewEvent.OnCheckboxClick(index))
                }
            )
        }
        item {
            AgreeButton(
                enabled = viewState.agreeButtonEnabled,
            ) { onEvent(DisclaimerViewEvent.OnAgreeClick) }
        }
    }
}