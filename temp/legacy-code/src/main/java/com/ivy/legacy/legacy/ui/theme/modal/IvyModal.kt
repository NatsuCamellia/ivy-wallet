package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ui.sheet.ActionSheet
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
const val DURATION_MODAL_ANIM = 200

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming", "UnusedParameter")
@Composable
fun BoxScope.IvyModal(
    id: UUID?,
    visible: Boolean,
    dismiss: () -> Unit,
    SecondaryActions: (@Composable () -> Unit)? = null,
    PrimaryAction: @Composable () -> Unit,
    scrollState: ScrollState? = rememberScrollState(),
    shiftIfKeyboardShown: Boolean = true,
    includeActionsRowPadding: Boolean = true,
    Content: @Composable ColumnScope.() -> Unit
) {
    if (visible) {
        key(id) {
            ActionSheet(
                onDismiss = dismiss,
                contentScrollable = scrollState != null,
                actions = {
                    SecondaryActions?.invoke()
                    Spacer(Modifier.weight(1f))
                    PrimaryAction()
                },
                content = Content
            )
        }
    }
}

@Preview
@Composable
private fun PreviewIvyModal_minimal(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        IvyModal(
            id = UUID.randomUUID(),
            visible = true,
            PrimaryAction = {
                ModalSave {
                }
            },
            dismiss = {}
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "My first Ivy Modal"
            )
        }
    }
}

@Composable
fun modalPreviewActionRowHeight() = 80.dp

/** For screenshot testing */
@Composable
fun IvyModalUiTest(isDark: Boolean) {
    PreviewIvyModal_minimal(if (isDark) Theme.DARK else Theme.LIGHT)
}
