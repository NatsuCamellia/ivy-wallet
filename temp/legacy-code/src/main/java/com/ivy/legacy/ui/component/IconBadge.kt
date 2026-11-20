package com.ivy.legacy.ui.component

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.ui.color.findContrastTextColor

private const val IconInsetDivisor = 12

/**
 * The `ic_vue_*` drawables fill 90% of their viewport (line art, stroke included); the `ic_custom_*` and
 * Ivy drawables fill about 50%, with their padding built in. Insetting a vue icon by this share of the
 * badge on every side draws its art as large as the others.
 */
private const val VueIconInsetRatio = 0.27f
private const val VueIconPrefix = "ic_vue_"

@DrawableRes
@Composable
fun customIconResource(iconName: String?, @DrawableRes defaultIcon: Int, size: String): Int {
    val context = LocalContext.current
    return remember(context, iconName, defaultIcon, size) {
        customIconId(context, iconName, size) ?: defaultIcon
    }
}

private fun customIconId(context: Context, iconName: String?, size: String): Int? {
    val name = iconName?.replace(" ", "")?.trim()?.toLowerCaseLocal() ?: return null
    fun drawable(resName: String) =
        context.resources.getIdentifier(resName, "drawable", context.packageName).takeIf { it != 0 }
    return if (name.startsWith(VueIconPrefix)) drawable(name) else drawable("ic_custom_${name}_$size") ?: drawable(name)
}

/** The only place a user-picked colour and `ic_custom_*` icon are shown: a coloured circle with the icon on it. */
@Composable
fun IconBadge(
    color: Color,
    iconName: String?,
    @DrawableRes defaultIcon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val iconRes = customIconResource(iconName, defaultIcon, "m")
    val isVue = iconName?.startsWith(VueIconPrefix) == true
    BadgeSurface(color, size, if (isVue) size * VueIconInsetRatio else size / IconInsetDivisor, modifier) {
        Icon(painter = painterResource(iconRes), contentDescription = null, modifier = it)
    }
}

/** A badge for an interface icon, such as the transfer arrows. */
@Composable
fun IconBadge(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    BadgeSurface(color, size, size / IconInsetDivisor, modifier) {
        Icon(imageVector = icon, contentDescription = null, modifier = it)
    }
}

@Composable
private fun BadgeSurface(
    color: Color,
    size: Dp,
    inset: Dp,
    modifier: Modifier = Modifier,
    icon: @Composable (Modifier) -> Unit,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = color,
        contentColor = findContrastTextColor(color),
    ) {
        icon(Modifier.padding(inset))
    }
}
