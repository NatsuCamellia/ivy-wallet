package com.ivy.budgets

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Budget
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountItem
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.sheet.ActionSheet
import com.ivy.legacy.ui.sheet.AmountModal
import com.ivy.legacy.ui.sheet.DeleteModal
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.Purple1Dark
import com.ivy.ui.color.Red3Light
import com.ivy.wallet.domain.deprecated.logic.model.CreateBudgetData
import java.util.UUID

data class BudgetModalData(
    val budget: Budget?,

    val baseCurrency: String,
    val categories: List<Category>,
    val accounts: List<Account>,

    val id: UUID = UUID.randomUUID(),
    val autoFocusKeyboard: Boolean = true,
)

@SuppressLint("ComposeModifierMissing")
@Composable
fun BudgetModal(
    modal: BudgetModalData?,

    onCreate: (CreateBudgetData) -> Unit,
    onEdit: (Budget) -> Unit,
    onDelete: (Budget) -> Unit,
    dismiss: () -> Unit
) {
    if (modal == null) return

    key(modal.id) {
        val initialBudget = modal.budget
        var nameTextFieldValue by remember {
            mutableStateOf(selectEndTextFieldValue(initialBudget?.name))
        }
        var amount by remember { mutableDoubleStateOf(initialBudget?.amount ?: 0.0) }
        var categoryIds by remember {
            mutableStateOf(initialBudget?.parseCategoryIds() ?: emptyList())
        }
        var accountIds by remember {
            mutableStateOf(initialBudget?.parseAccountIds() ?: emptyList())
        }

        var amountModalVisible by remember { mutableStateOf(false) }
        var deleteModalVisible by remember { mutableStateOf(false) }

        val nameFocus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            if (modal.autoFocusKeyboard) {
                nameFocus.requestFocus()
            }
        }

        val gutter = Modifier.padding(horizontal = 16.dp)

        ActionSheet(
            onDismiss = dismiss,
            title = stringResource(if (initialBudget != null) R.string.edit_budget else R.string.create_budget),
            actions = { dismissSheet ->
                if (initialBudget != null) {
                    IconButton(
                        onClick = { deleteModalVisible = true },
                        modifier = Modifier.testTag("modal_delete"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        if (initialBudget != null) {
                            onEdit(
                                initialBudget.copy(
                                    name = nameTextFieldValue.text.trim(),
                                    amount = amount,
                                    categoryIdsSerialized = Budget.serialize(categoryIds),
                                    accountIdsSerialized = Budget.serialize(accountIds)
                                )
                            )
                        } else {
                            onCreate(
                                CreateBudgetData(
                                    name = nameTextFieldValue.text.trim(),
                                    amount = amount,
                                    categoryIdsSerialized = Budget.serialize(categoryIds),
                                    accountIdsSerialized = Budget.serialize(accountIds)
                                )
                            )
                        }

                        dismissSheet()
                    },
                    enabled = nameTextFieldValue.text.isNotNullOrBlank() && amount > 0.0,
                    modifier = if (initialBudget == null) Modifier.testTag("modal_add") else Modifier,
                ) {
                    Text(stringResource(if (initialBudget != null) R.string.save else R.string.add))
                }
            }
        ) { dismissSheet ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = nameTextFieldValue,
                    onValueChange = { nameTextFieldValue = it },
                    modifier = gutter
                        .fillMaxWidth()
                        .focusRequester(nameFocus)
                        .testTag("base_input"),
                    label = { Text(stringResource(R.string.budget_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text,
                        autoCorrect = true
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                )

                CategoriesRow(
                    categories = modal.categories,
                    budgetCategoryIds = categoryIds,
                    onSetBudgetCategoryIds = { categoryIds = it },
                )

                AmountItem(
                    label = stringResource(R.string.budget_amount_uppercase).toLowerCaseLocal().capitalizeLocal(),
                    currency = modal.baseCurrency,
                    amount = amount,
                    index = 0,
                    count = 1,
                    onClick = { amountModalVisible = true },
                    modifier = gutter,
                )
            }

            DeleteModal(
                visible = deleteModalVisible,
                title = stringResource(R.string.confirm_deletion),
                description = stringResource(
                    R.string.confirm_budget_deletion_warning,
                    nameTextFieldValue.text
                ),
                dismiss = { deleteModalVisible = false }
            ) {
                if (initialBudget != null) {
                    onDelete(initialBudget)
                }
                deleteModalVisible = false
                dismissSheet()
            }
        }

        val amountModalId = remember(amount) { UUID.randomUUID() }
        AmountModal(
            id = amountModalId,
            visible = amountModalVisible,
            currency = modal.baseCurrency,
            initialAmount = amount,
            dismiss = { amountModalVisible = false }
        ) {
            amount = it
        }
    }
}

@Composable
private fun CategoriesRow(
    categories: List<Category>,
    budgetCategoryIds: List<UUID>,
    onSetBudgetCategoryIds: (List<UUID>) -> Unit,
) {
    Column {
        Text(
            text = determineBudgetType(budgetCategoryIds.size),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        LazyRow(
            modifier = Modifier.testTag("budget_categories_row"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = categories, key = { it.id.value }) { category ->
                val selected = budgetCategoryIds.contains(category.id.value)
                FilterChip(
                    selected = selected,
                    onClick = {
                        onSetBudgetCategoryIds(
                            if (selected) {
                                budgetCategoryIds.filter { it != category.id.value }
                            } else {
                                budgetCategoryIds.plus(category.id.value)
                            }
                        )
                    },
                    label = { Text(category.name.value) },
                    leadingIcon = {
                        IconBadge(
                            color = Color(category.color.value),
                            iconName = category.icon?.id,
                            defaultIcon = R.drawable.ic_custom_category_m,
                            size = FilterChipDefaults.IconSize,
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_create(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        val cat1 = Category(
            name = NotBlankTrimmedString.unsafe("Science"),
            color = ColorInt(Purple1Dark.toArgb()),
            icon = IconAsset.unsafe("atom"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

        BudgetModal(
            modal = BudgetModalData(
                budget = null,
                baseCurrency = "BGN",
                categories = listOf(
                    cat1,
                    Category(
                        name = NotBlankTrimmedString.unsafe("Pet"),
                        color = ColorInt(Red3Light.toArgb()),
                        icon = IconAsset.unsafe("pet"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    Category(
                        name = NotBlankTrimmedString.unsafe("Home"),
                        color = ColorInt(Green.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                ),
                accounts = emptyList()
            ),
            onCreate = {},
            onEdit = {},
            onDelete = {}
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_edit(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        val cat1 = Category(
            name = NotBlankTrimmedString.unsafe("Science"),
            color = ColorInt(Purple1Dark.toArgb()),
            icon = IconAsset.unsafe("atom"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

        BudgetModal(
            modal = BudgetModalData(
                budget = Budget(
                    name = "Shopping",
                    amount = 1250.0,
                    accountIdsSerialized = null,
                    categoryIdsSerialized = Budget.serialize(listOf(cat1.id.value)),
                    orderId = 0.0
                ),
                baseCurrency = "BGN",
                categories = listOf(
                    cat1,
                    Category(
                        name = NotBlankTrimmedString.unsafe("Pet"),
                        color = ColorInt(Red3Light.toArgb()),
                        icon = IconAsset.unsafe("pet"),
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                    Category(
                        name = NotBlankTrimmedString.unsafe("Home"),
                        color = ColorInt(Green.toArgb()),
                        icon = null,
                        id = CategoryId(UUID.randomUUID()),
                        orderNum = 0.0,
                    ),
                ),
                accounts = emptyList()
            ),
            onCreate = {},
            onEdit = {},
            onDelete = {}
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun BudgetModalCreateUiTest(isDark: Boolean) {
    Preview_create(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun BudgetModalEditUiTest(isDark: Boolean) {
    Preview_edit(if (isDark) Theme.DARK else Theme.LIGHT)
}
