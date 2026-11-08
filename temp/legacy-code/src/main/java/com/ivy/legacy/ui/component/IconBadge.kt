package com.ivy.legacy.ui.component

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
import com.ivy.ui.color.findContrastTextColor
import com.ivy.wallet.ui.theme.components.getCustomIconId

private const val IconInsetDivisor = 12

@DrawableRes
@Composable
fun customIconResource(iconName: String?, @DrawableRes defaultIcon: Int, size: String): Int {
    val context = LocalContext.current
    return remember(context, iconName, defaultIcon, size) {
        getCustomIconId(context, iconName, size)?.iconId ?: defaultIcon
    }
}

/** The only place a user-picked colour and `ic_custom_*` icon are shown: a coloured circle with the icon on it. */
@Composable
fun IconBadge(
    color: Color,
    iconName: String?,
    @DrawableRes defaultIcon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val iconRes = customIconResource(iconName, defaultIcon, "m")
    BadgeSurface(color, size, modifier) {
        Icon(painter = painterResource(iconRes), contentDescription = null, modifier = it)
    }
}

/** A badge for an interface icon, such as the transfer arrows. */
@Composable
fun IconBadge(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    BadgeSurface(color, size, modifier) {
        Icon(imageVector = icon, contentDescription = null, modifier = it)
    }
}

@Composable
private fun BadgeSurface(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    icon: @Composable (Modifier) -> Unit,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = color,
        contentColor = findContrastTextColor(color),
    ) {
        icon(Modifier.padding(size / IconInsetDivisor))
    }
}
