package com.ivy.legacy.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

@Deprecated("Legacy code. Don't use it, please.")
@Immutable
sealed interface UiText {
    @Deprecated("Legacy code. Don't use it, please.")
    @Immutable
    data class DynamicString(val value: String) : UiText

    @Deprecated("Legacy code. Don't use it, please.")
    @Immutable
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText

    @Deprecated("Legacy code. Don't use it, please.")
    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }
}
