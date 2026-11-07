package com.ivy.ui.color

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

fun findContrastTextColor(backgroundColor: Color): Color {
    return if (isDarkColor(backgroundColor.toArgb())) White else Black
}

fun isDarkColor(color: Color): Boolean {
    return isDarkColor(color.toArgb())
}

fun isDarkColor(@ColorInt color: Int): Boolean {
    return ColorUtils.calculateLuminance(color) <= 0.5
}

fun Color.dynamicContrast(): Color {
    val pickedColor = this.toHSVSpec()

    return when {
        pickedColor.s >= 0.5f && pickedColor.v >= 0.4f -> {
            // Primary
            if (isDarkColor(this)) {
                lighten()
            } else {
                darken()
            }
        }
        pickedColor.s <= 0.5f && pickedColor.v >= 0.8f -> {
            // Light
            darken()
        }
        pickedColor.s >= 0.1f && pickedColor.v <= 0.6f -> {
            // Dark
            lighten()
        }
        else -> {
            if (isDarkColor(this)) {
                lighten()
            } else {
                darken()
            }
        }
    }
}

fun Color.lighten(): Color {
    return this.hsv(
        s = 0.3f,
        v = 1f
    )
}

fun Color.darken(): Color {
    return this.hsv(
        s = 0.6f,
        v = 0.5f
    )
}

fun Color.toHSVSpec(): HSVSpec {
    val hsv = FloatArray(3)
    val color: Int = this.toArgb()
    android.graphics.Color.colorToHSV(color, hsv)
    return HSVSpec(hsv[0], hsv[1], hsv[2])
}

data class HSVSpec(
    val h: Float,
    val s: Float,
    val v: Float
)

fun Color.hsv(
    h: Float? = null,
    s: Float,
    v: Float
): Color {
    val hsv = FloatArray(3)
    val color: Int = this.toArgb()
    android.graphics.Color.colorToHSV(color, hsv)

    if (h != null) {
        hsv[0] = h
    }

    hsv[1] = s
    hsv[2] = v

    return Color(android.graphics.Color.HSVToColor(hsv))
}
