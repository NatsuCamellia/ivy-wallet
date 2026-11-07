package com.ivy.legacy.ui.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetGesturesEnabled: Boolean = true,
    actions: @Composable RowScope.(dismiss: () -> Unit) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberActionSheetState()
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var finished by remember { mutableStateOf(false) }
    var hiding by remember { mutableStateOf(false) }
    val finish: () -> Unit = {
        if (!finished) {
            finished = true
            currentOnDismiss()
        }
    }
    val dismiss: () -> Unit = {
        if (!hiding) {
            hiding = true
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) finish()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = finish,
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        contentWindowInsets = {
            BottomSheetDefaults.modalWindowInsets
                .exclude(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        },
    ) {
        Column(Modifier.imePadding()) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                content()
            }
            FlexibleBottomAppBar {
                IconButton(
                    onClick = dismiss,
                    modifier = Modifier.testTag("modal_close_button"),
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
                actions(dismiss)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberActionSheetState(): SheetState {
    // ModalBottomSheet starts Hidden, so Paparazzi would record only the scrim on the first frame
    return if (LocalInspectionMode.current) {
        remember {
            SheetState(
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                positionalThreshold = { 0f },
                velocityThreshold = { 0f },
                initialValue = SheetValue.Expanded,
            )
        }
    } else {
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    }
}

@Preview
@Composable
private fun PreviewActionSheet() {
    WalletPreview {
        ActionSheet(onDismiss = {}, title = "Action sheet") {
            Text("Content")
        }
    }
}
