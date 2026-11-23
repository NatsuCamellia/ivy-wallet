package com.ivy.onboarding.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.sheet.CategoryModal
import com.ivy.legacy.ui.sheet.CategoryModalData
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingProgressSlider
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.onboarding.components.Suggestions
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.Ivy
import com.ivy.ui.color.Orange
import com.ivy.ui.color.OrangeLight
import com.ivy.ui.color.Red
import com.ivy.ui.color.RedLight
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import java.util.UUID

@Composable
fun OnboardingCategories(
    suggestions: List<CreateCategoryData>,
    categories: List<Category>,
    modifier: Modifier = Modifier,
    onCreateCategory: (CreateCategoryData) -> Unit = { },
    onEditCategory: (Category) -> Unit = { _ -> },
    onSkip: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    val nav = navigation()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingToolbar(
                hasSkip = categories.isEmpty(),
                onBack = { nav.onBackPressed() },
                onSkip = onSkip
            )
        },
        bottomBar = {
            if (categories.isNotEmpty()) {
                Button(
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.finish))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.add_categories),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            if (categories.isEmpty()) {
                item {
                    Image(
                        painter = painterResource(id = R.drawable.onboarding_illustration_categories),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                OnboardingProgressSlider(
                    selectedStep = 3,
                    stepsCount = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(categories, key = { it.id.value }) { category ->
                CategoryCard(
                    category = category,
                    onClick = { categoryModalData = CategoryModalData(category = category) }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.suggestions),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                Suggestions(
                    suggestions = suggestions.filter { suggestion ->
                        categories.map { it.name.value.toLowerCaseLocal() }
                            .contains(suggestion.name.toLowerCaseLocal()).not()
                    },
                    onAddSuggestion = { onCreateCategory(it as CreateCategoryData) },
                    onAddNew = { categoryModalData = CategoryModalData(category = null) }
                )
            }
        }
    }

    CategoryModal(
        modal = categoryModalData,
        onCreateCategory = onCreateCategory,
        onEditCategory = onEditCategory,
        dismiss = { categoryModalData = null }
    )
}

@Composable
private fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                IconBadge(
                    color = Color(category.color.value),
                    iconName = category.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_m,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(category.name.value)
        }
    }
}

private val previewSuggestions = listOf(
    CreateCategoryData(name = "Food & Drinks", color = Green, icon = "fooddrink"),
    CreateCategoryData(name = "Bills & Fees", color = Red, icon = "bills"),
    CreateCategoryData(name = "Transport", color = OrangeLight, icon = "transport"),
    CreateCategoryData(name = "Groceries", color = Color(0xFF75ff4d), icon = "groceries"),
    CreateCategoryData(name = "Entertainment", color = Orange, icon = "game"),
    CreateCategoryData(name = "Shopping", color = Ivy, icon = "shopping"),
    CreateCategoryData(name = "Gifts", color = RedLight, icon = "gift"),
    CreateCategoryData(name = "Health", color = Color(0xFF4dfff3), icon = "health"),
    CreateCategoryData(name = "Investments", color = Color(0xFF1e5166), icon = "leaf"),
)

private val previewCategories = listOf(
    Category(
        name = NotBlankTrimmedString.unsafe("Food & Drinks"),
        color = ColorInt(Orange.toArgb()),
        icon = IconAsset.unsafe("fooddrink"),
        id = CategoryId(UUID.randomUUID()),
        orderNum = 0.0,
    ),
    Category(
        name = NotBlankTrimmedString.unsafe("Transport"),
        color = ColorInt(Ivy.toArgb()),
        icon = null,
        id = CategoryId(UUID.randomUUID()),
        orderNum = 1.0,
    ),
)

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT, categories: List<Category> = emptyList()) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            OnboardingCategories(
                suggestions = previewSuggestions,
                categories = categories,
            )
        }
    }
}

/** For screenshot testing */
@Composable
fun OnboardingCategoriesUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, previewCategories)
}

/** For screenshot testing */
@Composable
fun OnboardingCategoriesEmptyUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
