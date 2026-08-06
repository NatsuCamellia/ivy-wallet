package com.ivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.l0_system.Blue
import com.ivy.design.l0_system.Green
import com.ivy.design.l0_system.Orange
import com.ivy.design.l0_system.Purple
import com.ivy.design.l0_system.Red
import com.ivy.design.utils.hideKeyboard
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.EditTransactionDisplayLoan
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.edit.TransactionDateTime
import com.ivy.legacy.ui.component.edit.core.Description
import com.ivy.legacy.utils.format
import com.ivy.legacy.ui.component.tags.AddTagButton
import com.ivy.legacy.ui.component.tags.ShowTagModal
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.transaction.components.AmountCard
import com.ivy.transaction.components.EditTransactionTopBar
import com.ivy.transaction.components.SaveBar
import com.ivy.transaction.components.SelectableChipRow
import com.ivy.transaction.components.TitleField
import com.ivy.transaction.components.TransactionTypeSwitch
import com.ivy.ui.R
import com.ivy.wallet.domain.data.CustomExchangeRateState
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.edit.core.DueDate
import com.ivy.wallet.ui.theme.components.AddPrimaryAttributeButton
import com.ivy.wallet.ui.theme.components.CustomExchangeRateCard
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.ProgressModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.ivy.wallet.ui.theme.modal.edit.DescriptionModal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.EditTransactionScreen(screen: EditTransactionScreen) {
    val viewModel: EditTransactionViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.start(screen)
    }

    val view = LocalView.current

    UI(
        screen = screen,
        transactionType = uiState.transactionType,
        baseCurrency = uiState.currency,
        initialTitle = uiState.initialTitle,
        titleSuggestions = uiState.titleSuggestions,
        description = uiState.description,
        dateTime = uiState.dateTime,
        category = uiState.category,
        account = uiState.account,
        toAccount = uiState.toAccount,
        dueDate = uiState.dueDate,
        amount = uiState.amount,
        loanData = uiState.displayLoanHelper,
        backgroundProcessing = uiState.backgroundProcessingStarted,
        customExchangeRateState = uiState.customExchangeRateState,

        categories = uiState.categories,
        accounts = uiState.accounts,
        tags = uiState.tags,
        transactionAssociatedTags = uiState.transactionAssociatedTags,
        hasChanges = uiState.hasChanges,
        onSetDate = {
            viewModel.onEvent(EditTransactionViewEvent.OnChangeDate)
        },
        onSetTime = {
            viewModel.onEvent(EditTransactionViewEvent.OnChangeTime)
        },
        onTitleChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnTitleChanged(it))
        },
        onDescriptionChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnDescriptionChanged(it))
        },
        onAmountChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnAmountChanged(it))
        },
        onCategoryChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnCategoryChanged(it))
        },
        onAccountChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnAccountChanged(it))
        },
        onToAccountChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnToAccountChanged(it))
        },
        onDueDateChange = {
            viewModel.onEvent(EditTransactionViewEvent.OnDueDateChanged(it))
        },
        onSetTransactionType = {
            viewModel.onEvent(EditTransactionViewEvent.OnSetTransactionType(it))
        },
        onCreateCategory = {
            viewModel.onEvent(EditTransactionViewEvent.CreateCategory(it))
        },
        onEditCategory = {
            viewModel.onEvent(EditTransactionViewEvent.EditCategory(it))
        },
        onPayPlannedPayment = {
            viewModel.onEvent(EditTransactionViewEvent.OnPayPlannedPayment)
        },
        onSave = {
            view.hideKeyboard()
            viewModel.onEvent(EditTransactionViewEvent.Save(it))
        },
        onSetHasChanges = {
            viewModel.onEvent(EditTransactionViewEvent.SetHasChanges(it))
        },
        onDelete = {
            viewModel.onEvent(EditTransactionViewEvent.Delete)
        },
        onDuplicate = {
            viewModel.onEvent(EditTransactionViewEvent.Duplicate)
        },
        onCreateAccount = {
            viewModel.onEvent(EditTransactionViewEvent.CreateAccount(it))
        },
        onExchangeRateChange = {
            viewModel.onEvent(EditTransactionViewEvent.UpdateExchangeRate(it))
        },
        onTagOperation = {
            viewModel.onEvent(it)
        }
    )
}

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    screen: EditTransactionScreen,
    transactionType: TransactionType,
    baseCurrency: String,
    initialTitle: String?,
    titleSuggestions: ImmutableSet<String>,
    description: String?,
    category: Category?,
    dateTime: Instant?,
    account: Account?,
    toAccount: Account?,
    dueDate: Instant?,
    amount: Double,

    customExchangeRateState: CustomExchangeRateState,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    tags: ImmutableList<Tag>,
    transactionAssociatedTags: ImmutableList<TagId>,
    onTitleChange: (String?) -> Unit,
    onDescriptionChange: (String?) -> Unit,
    onAmountChange: (Double) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onAccountChange: (Account) -> Unit,
    onToAccountChange: (Account) -> Unit,
    onDueDateChange: (LocalDateTime?) -> Unit,
    onSetDate: () -> Unit,
    onSetTime: () -> Unit,
    onSetTransactionType: (TransactionType) -> Unit,

    onCreateCategory: (CreateCategoryData) -> Unit,
    onEditCategory: (Category) -> Unit,
    onPayPlannedPayment: () -> Unit,
    onSave: (closeScreen: Boolean) -> Unit,
    onSetHasChanges: (hasChanges: Boolean) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onCreateAccount: (CreateAccountData) -> Unit,
    onExchangeRateChange: (Double?) -> Unit = { },
    onTagOperation: (EditTransactionViewEvent.TagEvent) -> Unit = {},
    loanData: EditTransactionDisplayLoan = EditTransactionDisplayLoan(),
    backgroundProcessing: Boolean = false,
    hasChanges: Boolean = false,

    ) {
    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    var descriptionModalVisible by remember { mutableStateOf(false) }
    var deleteTrnModalVisible by remember { mutableStateOf(false) }
    var tagModalVisible by remember { mutableStateOf(false) }
    var amountModalShown by remember { mutableStateOf(false) }
    var exchangeRateAmountModalShown by remember { mutableStateOf(false) }
    var accountChangeModal by remember { mutableStateOf(false) }
    val waitModalVisible by remember(backgroundProcessing) {
        mutableStateOf(backgroundProcessing)
    }
    var selectedAcc by remember(account) {
        mutableStateOf(account)
    }

    val amountModalId =
        remember(screen.initialTransactionId, customExchangeRateState.exchangeRate) {
            UUID.randomUUID()
        }

    var titleTextFieldValue by remember(initialTitle) {
        mutableStateOf(
            TextFieldValue(
                initialTitle ?: ""
            )
        )
    }
    val titleFocus = FocusRequester()
    val scrollState = rememberScrollState()

    val nav = navigation()
    val ivyContext = ivyWalletCtx()
    val timeConverter = LocalTimeConverter.current

    // Loan records always display (and can only be) a transfer; the type switch is hidden for them.
    val displayType = if (loanData.isLoanRecord) TransactionType.TRANSFER else transactionType

    val (saveLabel, onSaveClick) = saveBarAction(
        hasExistingTransaction = screen.initialTransactionId != null,
        dueDate = dueDate,
        hasChanges = hasChanges,
        transactionType = transactionType,
        onSave = onSave,
        onSetHasChanges = onSetHasChanges,
        onPayPlannedPayment = onPayPlannedPayment,
    )

    Scaffold(
        topBar = {
            EditTransactionTopBar(
                title = topBarTitle(
                    isNewTransaction = screen.initialTransactionId == null,
                    type = displayType,
                ),
                onClose = { nav.back() },
                showDuplicateButton = screen.initialTransactionId != null,
                onDuplicate = onDuplicate,
                showDeleteButton = screen.initialTransactionId != null,
                onDelete = { deleteTrnModalVisible = true },
            )
        },
        bottomBar = {
            SaveBar(label = saveLabel, onClick = onSaveClick)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (!loanData.isLoanRecord) {
                TransactionTypeSwitch(
                    selected = transactionType,
                    onSelected = onSetTransactionType,
                )
                Spacer(Modifier.height(16.dp))
            }

            AmountCard(
                amount = amount,
                currency = baseCurrency,
                onClick = { amountModalShown = true },
                convertedAmountText = convertedAmountText(customExchangeRateState, transactionType),
            )

            Spacer(Modifier.height(16.dp))

            TitleField(
                type = transactionType,
                value = titleTextFieldValue,
                onValueChange = {
                    titleTextFieldValue = it
                    onTitleChange(it.text)
                },
                suggestions = titleSuggestions,
                focusRequester = titleFocus,
                onNext = {
                    when {
                        shouldFocusAmount(amount = amount) -> amountModalShown = true
                        else -> onSave(true)
                    }
                },
            )

            if (loanData.loanCaption != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = loanData.loanCaption!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))

            SelectableChipRow(
                label = stringResource(R.string.category),
                items = categories,
                selectedItem = category,
                itemLabel = { it.name.value },
                onItemSelected = { newCategory ->
                    onCategoryChange(newCategory)
                    if (shouldFocusTitle(titleTextFieldValue, transactionType)) {
                        titleFocus.requestFocus()
                    } else if (shouldFocusAmount(amount = amount)) {
                        amountModalShown = true
                    }
                },
                onAddNew = { categoryModalData = CategoryModalData(null) },
                addNewContentDescription = stringResource(R.string.add_category),
            )

            Spacer(Modifier.height(12.dp))

            SelectableChipRow(
                label = stringResource(R.string.account),
                items = accounts,
                selectedItem = account,
                itemLabel = { it.name },
                onItemSelected = { newAccount ->
                    if (loanData.isLoan && account?.currency != newAccount.currency) {
                        selectedAcc = newAccount
                        accountChangeModal = true
                    } else {
                        onAccountChange(newAccount)
                    }
                },
                onAddNew = {
                    accountModalData = AccountModalData(account = null, baseCurrency = baseCurrency, balance = 0.0)
                },
                addNewContentDescription = stringResource(R.string.add_account),
            )

            if (transactionType == TransactionType.TRANSFER) {
                Spacer(Modifier.height(12.dp))

                SelectableChipRow(
                    label = stringResource(R.string.to_account),
                    items = accounts,
                    selectedItem = toAccount,
                    itemLabel = { it.name },
                    onItemSelected = onToAccountChange,
                    onAddNew = {
                        accountModalData = AccountModalData(account = null, baseCurrency = baseCurrency, balance = 0.0)
                    },
                    addNewContentDescription = stringResource(R.string.add_account),
                )

                if (customExchangeRateState.showCard) {
                    Spacer(Modifier.height(12.dp))
                    CustomExchangeRateCard(
                        fromCurrencyCode = baseCurrency,
                        toCurrencyCode = customExchangeRateState.toCurrencyCode ?: baseCurrency,
                        exchangeRate = customExchangeRateState.exchangeRate,
                        onRefresh = { onExchangeRateChange(null) },
                    ) {
                        exchangeRateAmountModalShown = true
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            AddTagButton(transactionAssociatedTags = transactionAssociatedTags, onClick = { tagModalVisible = true })

            Spacer(Modifier.height(16.dp))

            if (dueDate != null) {
                DueDate(dueDate = dueDate) {
                    ivyContext.datePicker(
                        initialDate = with(timeConverter) { dueDate.toLocalDate() }
                    ) {
                        onDueDateChange(it.atTime(12, 0))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Description(
                description = description,
                onAddDescription = { descriptionModalVisible = true },
                onEditDescription = { descriptionModalVisible = true }
            )

            TransactionDateTime(
                dateTime = dateTime,
                dueDateTime = dueDate,
                onEditDate = onSetDate,
                onEditTime = onSetTime,
            )

            if (dueDate == null && transactionType != TransactionType.TRANSFER && dateTime == null) {
                Spacer(Modifier.height(12.dp))
                AddPrimaryAttributeButton(
                    icon = R.drawable.ic_planned_payments,
                    text = stringResource(R.string.add_planned_date_payment),
                    onClick = {
                        nav.back()
                        nav.navigateTo(
                            EditPlannedScreen(
                                plannedPaymentRuleId = null,
                                type = transactionType,
                                amount = amount,
                                accountId = account?.id,
                                categoryId = category?.id?.value,
                                title = titleTextFieldValue.text,
                                description = description,
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    onScreenStart {
        if (screen.initialTransactionId == null) {
            amountModalShown = true
        }
    }

    AmountModal(
        id = amountModalId,
        visible = amountModalShown,
        currency = baseCurrency,
        initialAmount = amount.takeIf { it > 0 },
        dismiss = { amountModalShown = false },
        onAmountChanged = {
            onAmountChange(it)
            if (shouldFocusTitle(titleTextFieldValue, transactionType)) {
                titleFocus.requestFocus()
            }
        }
    )

    // Modals
    CategoryModal(modal = categoryModalData, onCreateCategory = { createData ->
        onCreateCategory(createData)
    }, onEditCategory = onEditCategory, dismiss = {
        categoryModalData = null
    })

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = { _, _ -> },
        dismiss = {
            accountModalData = null
        }
    )

    DescriptionModal(
        visible = descriptionModalVisible,
        description = description,
        onDescriptionChanged = onDescriptionChange,
        dismiss = {
            descriptionModalVisible = false
        }
    )

    DeleteModal(
        visible = deleteTrnModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.transaction_confirm_deletion_description),
        dismiss = { deleteTrnModalVisible = false }
    ) {
        onDelete()
    }

    DeleteModal(
        visible = accountChangeModal,
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_change_description),
        buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed,
        dismiss = {
            accountChangeModal = false
        }
    ) {
        selectedAcc?.let { onAccountChange(it) }
        accountChangeModal = false
    }

    ProgressModal(
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_loan_change),
        visible = waitModalVisible
    )

    AmountModal(
        id = amountModalId,
        visible = exchangeRateAmountModalShown,
        currency = "",
        initialAmount = customExchangeRateState.exchangeRate,
        dismiss = { exchangeRateAmountModalShown = false },
        decimalCountMax = IvyCurrency.getDecimalPlaces(
            customExchangeRateState.toCurrencyCode ?: baseCurrency
        ),
        onAmountChanged = {
            onExchangeRateChange(it)
        }
    )

    ShowTagModal(
        visible = tagModalVisible,
        onDismiss = {
            tagModalVisible = false
            // Reset TagList, avoids showing incorrect tag list when user has searched for a tag
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagSearch(""))
        },
        allTagList = tags,
        selectedTagList = transactionAssociatedTags,
        onTagAdd = {
            onTagOperation(EditTransactionViewEvent.TagEvent.SaveTag(name = it))
        },
        onTagEdit = { oldTag, newTag ->
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagEdit(oldTag, newTag))
        },
        onTagDelete = {
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagDelete(it))
        },
        onTagSelected = {
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagSelect(it))
        },
        onTagDeSelected = {
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagDeSelect(it))
        },
        onTagSearch = {
            onTagOperation(EditTransactionViewEvent.TagEvent.OnTagSearch(it))
        }
    )
}

@Composable
private fun topBarTitle(isNewTransaction: Boolean, type: TransactionType): String = stringResource(
    if (isNewTransaction) {
        when (type) {
            TransactionType.EXPENSE -> R.string.new_expense
            TransactionType.INCOME -> R.string.new_income
            TransactionType.TRANSFER -> R.string.new_transfer
        }
    } else {
        when (type) {
            TransactionType.EXPENSE -> R.string.edit_expense
            TransactionType.INCOME -> R.string.edit_income
            TransactionType.TRANSFER -> R.string.edit_transfer
        }
    }
)

@Composable
private fun saveBarAction(
    hasExistingTransaction: Boolean,
    dueDate: Instant?,
    hasChanges: Boolean,
    transactionType: TransactionType,
    onSave: (closeScreen: Boolean) -> Unit,
    onSetHasChanges: (hasChanges: Boolean) -> Unit,
    onPayPlannedPayment: () -> Unit,
): Pair<String, () -> Unit> = when {
    hasExistingTransaction && dueDate != null && hasChanges ->
        stringResource(R.string.save) to {
            onSave(false)
            onSetHasChanges(false)
        }

    hasExistingTransaction && dueDate != null && !hasChanges ->
        stringResource(if (transactionType == TransactionType.EXPENSE) R.string.pay else R.string.get) to {
            onPayPlannedPayment()
        }

    hasExistingTransaction ->
        stringResource(R.string.save) to { onSave(true) }

    else ->
        stringResource(R.string.save_transaction) to { onSave(true) }
}

private fun convertedAmountText(
    customExchangeRateState: CustomExchangeRateState,
    transactionType: TransactionType,
): String? {
    val convertedAmount = customExchangeRateState.convertedAmount
    val convertedCurrencyCode = customExchangeRateState.toCurrencyCode
    return if (transactionType == TransactionType.TRANSFER && convertedAmount != null && convertedCurrencyCode != null) {
        "${convertedAmount.format(IvyCurrency.getDecimalPlaces(convertedCurrencyCode))} $convertedCurrencyCode"
    } else {
        null
    }
}

private fun shouldFocusTitle(
    titleTextFieldValue: TextFieldValue,
    type: TransactionType
): Boolean = titleTextFieldValue.text.isBlank() && type != TransactionType.TRANSFER

private fun shouldFocusAmount(amount: Double) = amount == 0.0

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 27, 0, 35)
    .toInstant(ZoneOffset.UTC)

private fun previewCategory(name: String, color: Int) = Category(
    name = NotBlankTrimmedString.unsafe(name),
    color = ColorInt(color),
    icon = null,
    id = CategoryId(UUID.randomUUID()),
    orderNum = 0.0,
)

@ExperimentalFoundationApi
@Preview
@Composable
private fun BoxWithConstraintsScope.Preview(isDark: Boolean = false) {
    val groceries = previewCategory("Groceries", Green.toArgb())
    val cash = Account(name = "Cash", Orange.toArgb())

    IvyPreview(isDark) {
        UI(
            // A non-null id keeps the amount keypad closed by default, matching edit mode.
            screen = EditTransactionScreen(UUID.randomUUID(), TransactionType.EXPENSE),
            initialTitle = "Weekly grocery run",
            titleSuggestions = persistentSetOf(),
            tags = persistentListOf(),
            transactionAssociatedTags = persistentListOf(),
            baseCurrency = "USD",
            dateTime = testDateTime,
            description = null,
            category = groceries,
            account = cash,
            toAccount = null,
            amount = 128.4,
            dueDate = null,
            transactionType = TransactionType.EXPENSE,
            customExchangeRateState = CustomExchangeRateState(),

            categories = persistentListOf(
                groceries,
                previewCategory("Transport", Blue.toArgb()),
                previewCategory("Entertainment", Purple.toArgb()),
                previewCategory("Shopping", Red.toArgb()),
            ),
            accounts = persistentListOf(
                cash,
                Account(name = "Revolut", Blue.toArgb()),
                Account(name = "DSK", Purple.toArgb()),
            ),

            onDueDateChange = {},
            onCategoryChange = {},
            onAccountChange = {},
            onToAccountChange = {},
            onDescriptionChange = {},
            onTitleChange = {},
            onAmountChange = {},

            onCreateCategory = { },
            onEditCategory = {},
            onPayPlannedPayment = {},
            onSave = {},
            onSetHasChanges = {},
            onDelete = {},
            onDuplicate = {},
            onCreateAccount = { },
            onSetDate = {},
            onSetTime = {},
            onSetTransactionType = {}
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditTransactionScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    IvyWalletPreview(theme) {
        Preview(isDark)
    }
}
