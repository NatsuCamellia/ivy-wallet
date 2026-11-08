package com.ivy.planned.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.IntervalType
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.ui.edit.core.Category
import com.ivy.legacy.ui.component.edit.core.Description
import com.ivy.legacy.ui.component.edit.core.EditBottomBar
import com.ivy.wallet.ui.edit.core.Title
import com.ivy.wallet.ui.edit.core.Toolbar
import com.ivy.ui.color.Orange
import com.ivy.ui.color.Purple
import com.ivy.legacy.ui.sheet.ChangeTransactionTypeModal
import com.ivy.legacy.ui.sheet.DeleteModal
import com.ivy.legacy.ui.sheet.RecurringRuleModal
import com.ivy.legacy.ui.sheet.RecurringRuleModalData
import com.ivy.legacy.ui.sheet.AccountModal
import com.ivy.legacy.ui.sheet.AccountModalData
import com.ivy.legacy.ui.sheet.CategoryModal
import com.ivy.legacy.ui.sheet.CategoryModalData
import com.ivy.legacy.ui.sheet.ChooseCategoryModal
import com.ivy.legacy.ui.sheet.DescriptionModal
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime
import java.util.UUID

@Composable
fun EditPlannedScreen(screen: EditPlannedScreen) {
    val viewModel: EditPlannedViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    LaunchedEffect(Unit) {
        viewModel.start(screen)
    }

    UI(
        screen = screen,
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

/**
 * Flow Empty: Type -> Amount -> Category -> Recurring Rule -> Title
 * Flow Amount + Category: Recurring Rule -> Title
 */
@Suppress("LongMethod")
@Composable
private fun UI(
    screen: EditPlannedScreen,
    state: EditPlannedScreenState,
    onEvent: (EditPlannedScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var titleTextFieldValue by remember(state.initialTitle) {
        mutableStateOf(
            TextFieldValue(
                state.initialTitle.orEmpty()
            )
        )
    }
    val titleFocus = FocusRequester()
    val isEdit = screen.plannedPaymentRuleId != null

    Scaffold(
        modifier = modifier,
        topBar = {
            Toolbar(
                title = stringResource(if (isEdit) R.string.edit_planned_payment else R.string.add_planned_payment),
                type = state.transactionType,
                initialTransactionId = screen.plannedPaymentRuleId,
                onDeleteTrnModal = {
                    onEvent(EditPlannedScreenEvent.OnDeleteTransactionModalVisible(true))
                },
                onChangeTransactionTypeModal = {
                    onEvent(EditPlannedScreenEvent.OnTransactionTypeModalVisible(true))
                },
                showDuplicateButton = false,
                onDuplicate = {}
            )
        },
        bottomBar = {
            EditBottomBar(
                initialTransactionId = screen.plannedPaymentRuleId,
                type = state.transactionType,
                accounts = state.accounts,
                selectedAccount = state.account,
                toAccount = null,
                amount = state.amount,
                currency = state.currency,

                amountModalShown = state.amountModalVisible,
                setAmountModalShown = {
                    onEvent(EditPlannedScreenEvent.OnAmountModalVisible(it))
                },

                onAmountChange = {
                    onEvent(EditPlannedScreenEvent.OnAmountChanged(it))
                    when {
                        shouldFocusCategory(state.category, state.transactionType) -> {
                            onEvent(EditPlannedScreenEvent.OnCategoryModalVisible(true))
                        }

                        shouldFocusRecurring(
                            state.startDate,
                            state.intervalN,
                            state.intervalType,
                            state.oneTime
                        ) -> {
                            onEvent(
                                EditPlannedScreenEvent.OnRecurringRuleModalDataChanged(
                                    RecurringRuleModalData(
                                        initialStartDate = state.startDate,
                                        initialIntervalN = state.intervalN,
                                        initialIntervalType = state.intervalType,
                                        initialOneTime = state.oneTime
                                    )
                                )
                            )
                        }

                        shouldFocusTitle(titleTextFieldValue, state.transactionType) -> {
                            titleFocus.requestFocus()
                        }
                    }
                },
                onSelectedAccountChange = { onEvent(EditPlannedScreenEvent.OnAccountChanged(it)) },
                onToAccountChange = { },
                onAddNewAccount = {
                    onEvent(
                        EditPlannedScreenEvent.OnAccountModalDataChanged(
                            AccountModalData(
                                account = null,
                                baseCurrency = state.currency,
                                balance = 0.0
                            )
                        )
                    )
                }
            ) {
                Button(
                    onClick = { onEvent(EditPlannedScreenEvent.OnSave()) },
                    modifier = Modifier.testTag("editPlannedScreen_set"),
                ) {
                    Text(stringResource(R.string.set))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Title(
                type = state.transactionType,
                titleFocus = titleFocus,
                titleTextFieldValue = titleTextFieldValue,
                setTitleTextFieldValue = {
                    titleTextFieldValue = it
                },
                suggestions = emptySet(), // DO NOT display title suggestions for "Planned Payments"
                onTitleChange = { onEvent(EditPlannedScreenEvent.OnTitleChanged(it)) },
                onNext = {
                    when {
                        shouldFocusRecurring(
                            state.startDate,
                            state.intervalN,
                            state.intervalType,
                            state.oneTime
                        ) -> {
                            onEvent(
                                EditPlannedScreenEvent.OnRecurringRuleModalDataChanged(
                                    RecurringRuleModalData(
                                        initialStartDate = state.startDate,
                                        initialIntervalN = state.intervalN,
                                        initialIntervalType = state.intervalType,
                                        initialOneTime = state.oneTime
                                    )
                                )
                            )
                        }

                        else -> {
                            onEvent(EditPlannedScreenEvent.OnSave())
                        }
                    }
                }
            )

            if (state.transactionType != TransactionType.TRANSFER) {
                Category(
                    category = state.category,
                    onChooseCategory = {
                        onEvent(EditPlannedScreenEvent.OnCategoryModalVisible(true))
                    }
                )
            }

            RecurringRule(
                startDate = state.startDate,
                intervalN = state.intervalN,
                intervalType = state.intervalType,
                oneTime = state.oneTime,
                onShowRecurringRuleModal = {
                    onEvent(
                        EditPlannedScreenEvent.OnRecurringRuleModalDataChanged(
                            RecurringRuleModalData(
                                initialStartDate = state.startDate,
                                initialIntervalN = state.intervalN,
                                initialIntervalType = state.intervalType,
                                initialOneTime = state.oneTime
                            )
                        )
                    )
                }
            )

            Description(
                description = state.description,
                onAddDescription = { onEvent(EditPlannedScreenEvent.OnDescriptionModalVisible(true)) },
                onEditDescription = { onEvent(EditPlannedScreenEvent.OnDescriptionModalVisible(true)) }
            )
        }
    }

    onScreenStart {
        if (screen.plannedPaymentRuleId == null) {
            // Create mode
            if (screen.mandatoryFilled()) {
                // Flow Convert (Amount, Account, Category)
                onEvent(
                    EditPlannedScreenEvent.OnRecurringRuleModalDataChanged(
                        RecurringRuleModalData(
                            initialStartDate = state.startDate,
                            initialIntervalN = state.intervalN,
                            initialIntervalType = state.intervalType,
                            initialOneTime = state.oneTime
                        )
                    )
                )
            } else {
                // Flow Empty
                onEvent(EditPlannedScreenEvent.OnTransactionTypeModalVisible(true))
            }
        }
    }

    // Modals
    ChooseCategoryModal(
        visible = state.categoryModalVisible,
        initialCategory = state.category,
        categories = state.categories,
        showCategoryModal = {
            onEvent(
                EditPlannedScreenEvent.OnCategoryModalDataChanged(
                    CategoryModalData(it)
                )
            )
        },
        onCategoryChanged = {
            onEvent(EditPlannedScreenEvent.OnCategoryChanged(it))
            onEvent(
                EditPlannedScreenEvent.OnRecurringRuleModalDataChanged(
                    RecurringRuleModalData(
                        initialStartDate = state.startDate,
                        initialIntervalN = state.intervalN,
                        initialIntervalType = state.intervalType,
                        initialOneTime = state.oneTime
                    )
                )
            )
        },
        dismiss = {
            onEvent(EditPlannedScreenEvent.OnCategoryModalVisible(false))
        }
    )

    CategoryModal(
        modal = state.categoryModalData,
        onCreateCategory = { onEvent(EditPlannedScreenEvent.OnCreateCategory(it)) },
        onEditCategory = {
            onEvent(EditPlannedScreenEvent.OnEditCategory(it))
        },
        dismiss = {
            onEvent(EditPlannedScreenEvent.OnCategoryModalDataChanged(null))
        }
    )

    AccountModal(
        modal = state.accountModalData,
        onCreateAccount = { onEvent(EditPlannedScreenEvent.OnCreateAccount(it)) },
        onEditAccount = { _, _ -> },
        dismiss = {
            onEvent(EditPlannedScreenEvent.OnAccountModalDataChanged(null))
        }
    )

    DescriptionModal(
        visible = state.descriptionModalVisible,
        description = state.description,
        onDescriptionChanged = { onEvent(EditPlannedScreenEvent.OnDescriptionChanged(it)) },
        dismiss = {
            onEvent(EditPlannedScreenEvent.OnDescriptionModalVisible(false))
        }
    )

    DeleteModal(
        visible = state.deleteTransactionModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.planned_payment_confirm_deletion_description),
        dismiss = { onEvent(EditPlannedScreenEvent.OnDeleteTransactionModalVisible(false)) }
    ) {
        onEvent(EditPlannedScreenEvent.OnDelete)
    }

    ChangeTransactionTypeModal(
        title = stringResource(R.string.set_payment_type),
        visible = state.transactionTypeModalVisible,
        includeTransferType = false,
        initialType = state.transactionType,
        dismiss = {
            onEvent(EditPlannedScreenEvent.OnTransactionTypeModalVisible(false))
        }
    ) {
        onEvent(EditPlannedScreenEvent.OnSetTransactionType(it))
        if (shouldFocusAmount(state.amount)) {
            onEvent(EditPlannedScreenEvent.OnAmountModalVisible(true))
        }
    }

    RecurringRuleModal(
        modal = state.recurringRuleModalData,
        onRuleChanged = { newStartDate, newOneTime, newIntervalN, newIntervalType ->
            onEvent(
                EditPlannedScreenEvent.OnRuleChanged(
                    newStartDate,
                    newOneTime,
                    newIntervalN,
                    newIntervalType
                )
            )

            when {
                shouldFocusCategory(state.category, state.transactionType) -> {
                    onEvent(EditPlannedScreenEvent.OnCategoryModalVisible(true))
                }

                shouldFocusTitle(titleTextFieldValue, state.transactionType) -> {
                    titleFocus.requestFocus()
                }
            }
        },
        dismiss = {
            onEvent(EditPlannedScreenEvent.OnRecurringRuleModalDataChanged(null))
        }
    )
}

private fun shouldFocusCategory(
    category: Category?,
    type: TransactionType,
): Boolean = category == null && type != TransactionType.TRANSFER

private fun shouldFocusTitle(
    titleTextFieldValue: TextFieldValue,
    type: TransactionType,
): Boolean = titleTextFieldValue.text.isBlank() && type != TransactionType.TRANSFER

private fun shouldFocusRecurring(
    startDate: LocalDateTime?,
    intervalN: Int?,
    intervalType: IntervalType?,
    oneTime: Boolean,
): Boolean {
    return !hasRecurringRule(
        startDate = startDate,
        intervalN = intervalN,
        intervalType = intervalType,
        oneTime = oneTime
    )
}

private fun shouldFocusAmount(amount: Double) = amount == 0.0

@Composable
private fun PreviewUI(
    isDark: Boolean,
    screen: EditPlannedScreen,
    state: EditPlannedScreenState,
) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        NavigationRoot(navigation = Navigation()) {
            UI(screen = screen, state = state, onEvent = {})
        }
    }
}

private val previewAccount = Account(name = "phyre", Orange.toArgb(), currency = "BGN")

@Suppress("MagicNumber")
private val previewRent = Category(
    name = NotBlankTrimmedString.unsafe("Rent"),
    color = ColorInt(Purple.toArgb()),
    icon = null,
    id = CategoryId(UUID(0, 1)),
    orderNum = 0.0,
)

@Suppress("MagicNumber")
private val previewStartDate = LocalDateTime.of(2023, 4, 27, 12, 0)

private val previewEmptyState = EditPlannedScreenState(
    oneTime = false,
    startDate = null,
    intervalN = null,
    intervalType = null,
    initialTitle = "",
    currency = "BGN",
    description = null,
    category = null,
    account = previewAccount,
    amount = 0.0,
    transactionType = TransactionType.INCOME,
    categories = persistentListOf(),
    accounts = persistentListOf(),
    categoryModalVisible = false,
    categoryModalData = null,
    accountModalData = null,
    descriptionModalVisible = false,
    deleteTransactionModalVisible = false,
    recurringRuleModalData = null,
    transactionTypeModalVisible = false,
    amountModalVisible = false
)

@Preview
@Composable
private fun Preview(isDark: Boolean = false) {
    PreviewUI(
        isDark = isDark,
        screen = EditPlannedScreen(plannedPaymentRuleId = null, type = TransactionType.INCOME),
        state = previewEmptyState
    )
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun PreviewEditRepeat(isDark: Boolean = false) {
    PreviewUI(
        isDark = isDark,
        screen = EditPlannedScreen(plannedPaymentRuleId = UUID(0, 2), type = TransactionType.EXPENSE),
        state = previewEmptyState.copy(
            startDate = previewStartDate,
            intervalN = 2,
            intervalType = IntervalType.MONTH,
            initialTitle = "Flat rent",
            description = "Paid on the first working day",
            category = previewRent,
            categories = persistentListOf(previewRent),
            accounts = persistentListOf(previewAccount),
            amount = 1250.0,
            transactionType = TransactionType.EXPENSE
        )
    )
}

/** For screenshot testing */
@Composable
fun EditPlannedScreenUiTest(isDark: Boolean) {
    Preview(isDark)
}

/** For screenshot testing */
@Composable
fun EditPlannedRepeatUiTest(isDark: Boolean) {
    PreviewEditRepeat(isDark)
}
