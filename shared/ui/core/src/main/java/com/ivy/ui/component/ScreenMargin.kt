package com.ivy.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The horizontal screen margin for content that is not a flat list row. Flat rows ([FlatListItem],
 * [StickyListHeader]) inset themselves, so they must not use this.
 */
fun Modifier.screenMargin(): Modifier = padding(horizontal = 16.dp)
