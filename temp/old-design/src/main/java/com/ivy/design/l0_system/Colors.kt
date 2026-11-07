package com.ivy.design.l0_system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ivy.ui.color.Green

// --------------------------------------------------------------------------------------------------

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val TrueBlack = Color(0xFF000000)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val MediumBlack = Color(0xFF2B2C2D)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val Gray = Color(0xFF939199)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val MediumWhite = Color(0xFFEFEEF0)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val Transparent = Color(0x00000000)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val GradientGreen = Gradient(Green, Color(0xFF49F2C8))

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Immutable
data class Gradient(
    val startColor: Color,
    val endColor: Color
) {
    companion object {
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
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun Color.asBrush(): Brush {
    return Brush.horizontalGradient(listOf(this, this))
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun Int.toComposeColor() = Color(this)
