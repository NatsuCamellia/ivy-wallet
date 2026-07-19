package com.ivy.design.system

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors Material 3 schemes don't guarantee: dynamic color may
 * produce a scheme with no green or orange role at all. Provided by
 * [IvyMaterial3Theme] via [LocalIvyExtendedColors].
 */
@Immutable
data class IvyExtendedColors(
    val income: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val LightExtendedColors: IvyExtendedColors = IvyExtendedColors(
    income = Color(0xFF1E7C46),
    warning = Color(0xFF8A5100),
    warningContainer = Color(0xFFFFDDB8),
    onWarningContainer = Color(0xFF2C1600),
)

internal val DarkExtendedColors: IvyExtendedColors = IvyExtendedColors(
    income = Color(0xFF7ADC9E),
    warning = Color(0xFFFFB95C),
    warningContainer = Color(0xFF693C00),
    onWarningContainer = Color(0xFFFFDDB8),
)

val LocalIvyExtendedColors: ProvidableCompositionLocal<IvyExtendedColors> =
    staticCompositionLocalOf { LightExtendedColors }
