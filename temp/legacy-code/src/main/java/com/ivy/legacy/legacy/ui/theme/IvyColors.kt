package com.ivy.wallet.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.ui.color.Green
import com.ivy.ui.color.Ivy
import com.ivy.ui.color.Orange
import com.ivy.ui.color.Red

val Purple = Color(0xFFA020F0)

// --------------------------------------------------------------------------------------------------

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val MediumBlack = Color(0xFF2B2C2D)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val Gray = Color(0xFF939199)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val MediumWhite = Color(0xFFEFEEF0)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val Transparent = Color(0x00000000)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val GradientRed = Gradient(Red, Color(0xFFFF99AB))

val GradientPurple = Gradient(Purple, Color(0xFFED3EF7))

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val GradientGreen = Gradient(Green, Color(0xFF49F2C8))

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val GradientOrangeRevert = Gradient(Color(0xFFF2CD9E), Orange)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val GradientIvy = Gradient(Ivy, Color(0xFFAA99FF))

@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun Modifier.gradientCutBackgroundTop(
    pure: Color,
    density: Density,
    endY: Dp = 32.dp
): Modifier {
    return background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Transparent,
                pure,
            ),
            endY = with(density) {
                endY.toPx()
            }
        )
    ).padding(top = 16.dp)
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun Modifier.gradientCutBackgroundBottom(
    pure: Color,
    paddingBottom: Dp,
): Modifier {
    return background(
        brush = Brush.verticalGradient(
            colors = listOf(
                pure,
                Transparent
            ),
        )
    ).padding(bottom = paddingBottom)
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun pureBlur() = UI.colors.pure.copy(alpha = 0.95f)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun mediumBlur() = UI.colors.medium.copy(alpha = 0.95f)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun gradientExpenses() = Gradient(UI.colors.pureInverse, UI.colors.gray)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
data class Gradient(
    val startColor: Color,
    val endColor: Color
) {
    companion object {
        @Deprecated("Old design system. Use `:ivy-design` and Material3")
        fun from(gradient: com.ivy.design.l0_system.Gradient) =
            Gradient(gradient.startColor, gradient.endColor)

        @Deprecated("Old design system. Use `:ivy-design` and Material3")
        fun from(startColor: Int, endColor: Int?) = Gradient(
            startColor = startColor.toComposeColor(),
            endColor = (endColor ?: startColor).toComposeColor()
        )

        @Deprecated("Old design system. Use `:ivy-design` and Material3")
        fun solid(color: Color) = Gradient(color, color)

        @Deprecated("Old design system. Use `:ivy-design` and Material3")
        @Composable
        fun black() = Gradient(UI.colors.gray, UI.colors.pureInverse)
    }

    @Deprecated("Old design system. Use `:ivy-design` and Material3")
    fun asHorizontalBrush() = Brush.horizontalGradient(colors = listOf(startColor, endColor))

    @Deprecated("Old design system. Use `:ivy-design` and Material3")
    fun asVerticalBrush() = Brush.verticalGradient(colors = listOf(startColor, endColor))
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun Int.toComposeColor() = Color(this)
