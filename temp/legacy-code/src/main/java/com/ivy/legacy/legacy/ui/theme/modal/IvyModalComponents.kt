package com.ivy.wallet.ui.theme.modal

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.GradientGreen
import com.ivy.wallet.ui.theme.components.IvyButton

@Deprecated("Old design system. Use `:ivy-design` and Material3")
const val DURATION_MODAL_ANIM = 200

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun ModalSet(
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.set),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ModalCheck(
        modifier = modifier,
        label = label,
        enabled = enabled,
        onClick = onClick
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun ModalCheck(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ModalPositiveButton(
        modifier = modifier,
        text = label,
        iconStart = R.drawable.ic_check,
        enabled = enabled,
        onClick = onClick
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun ModalSave(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ModalPositiveButton(
        modifier = modifier,
        text = stringResource(R.string.save),
        iconStart = R.drawable.ic_save,
        enabled = enabled,
        onClick = onClick
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun ModalAdd(
    enabled: Boolean = true,
    testTag: String = "modal_add",
    onClick: () -> Unit
) {
    ModalPositiveButton(
        modifier = Modifier.testTag(testTag),
        text = stringResource(R.string.add),
        iconStart = R.drawable.ic_plus,
        enabled = enabled,
        onClick = onClick
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun ModalPositiveButton(
    modifier: Modifier = Modifier,
    text: String,
    @DrawableRes iconStart: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IvyButton(
        modifier = modifier,
        text = text,
        backgroundGradient = GradientGreen,
        iconStart = iconStart,
        onClick = onClick,
        enabled = enabled
    )
}
