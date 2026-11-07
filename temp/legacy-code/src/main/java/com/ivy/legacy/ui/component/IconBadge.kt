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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ivy.ui.color.findContrastTextColor
import com.ivy.wallet.ui.theme.components.getCustomIconId

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
) {
    val iconRes = customIconResource(iconName, defaultIcon, "m")
    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = color,
        contentColor = findContrastTextColor(color),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.padding(4.dp),
        )
    }
}
