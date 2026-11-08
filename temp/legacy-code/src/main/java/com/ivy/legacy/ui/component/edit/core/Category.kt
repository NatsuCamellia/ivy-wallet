package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ivy.data.model.Category
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.ui.R

@Composable
fun Category(
    category: Category?,
    onChooseCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onChooseCategory, modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                if (category != null) {
                    IconBadge(
                        color = Color(category.color.value),
                        iconName = category.icon?.id,
                        defaultIcon = R.drawable.ic_custom_category_m,
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(category?.name?.value ?: stringResource(R.string.add_category))
        }
    }
}
