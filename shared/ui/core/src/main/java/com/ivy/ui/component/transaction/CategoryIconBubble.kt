package com.ivy.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val ContainerAlpha = 0.24f
private const val ContentBlend = 0.45f

/**
 * Circular tonal container carrying a category's color, with the icon tinted
 * for legibility on it. Shared by the transaction list, the edit-transaction
 * rows and the category picker.
 */
@Composable
fun CategoryIconBubble(
    categoryColor: Color?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    icon: @Composable () -> Unit,
) {
    val container = if (categoryColor != null) {
        categoryColor.copy(alpha = ContainerAlpha)
            .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (categoryColor != null) {
        lerp(categoryColor, MaterialTheme.colorScheme.onSurface, ContentBlend)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            icon()
        }
    }
}
