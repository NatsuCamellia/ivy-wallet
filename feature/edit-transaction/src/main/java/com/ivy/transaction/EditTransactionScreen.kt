package com.ivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.utils.hideKeyboard
import com.ivy.legacy.data.EditTransactionDisplayLoan
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.tags.ShowTagModal
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.onScreenStart
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.ui.component.amount.AmountKeypadSheet
import com.ivy.ui.component.amount.KeypadAccountUi
import com.ivy.ui.component.dialog.ProgressDialog
import com.ivy.ui.component.dialog.TextInputDialog
import com.ivy.ui.component.picker.PickerItemUi
import com.ivy.ui.component.picker.PickerSheet
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.CustomExchangeRateState
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

/** Sentinel id for the picker's "No category" entry; no real category can carry it. */
private const val NoCategoryItemId = "no-category"

/** The custom exchange rate is entered and displayed with more precision than a currency. */
private const val ExchangeRateDecimals = 4

/** Planned payments are due at noon, the value the legacy due-date picker used. */
private const val DueDateHour = 12

@Composable
fun EditTransactionScreen(screen: EditTransactionScreen) {
    val viewModel: EditTransactionViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.start(screen)
    }

    val view = LocalView.current
    val timeFormatter = LocalTimeFormatter.current
    val timeConverter = LocalTimeConverter.current
    val ivyContext = ivyWalletCtx()

    EditTransactionUi(
        isNewTransaction = screen.initialTransactionId == null,
        transactionType = uiState.transactionType,
        baseCurrency = uiState.currency,
        initialTitle = uiState.initialTitle,
        titleSuggestions = uiState.titleSuggestions,
        description = uiState.description,
        // Formatted here rather than in the UI so the screen composable stays free of the legacy
        // time composition locals and can be rendered by previews and Paparazzi.
        dateTimeText = uiState.dateTime?.let {
            with(timeFormatter) {
                it.formatLocal(TimeFormatter.Style.DateAndTime(includeWeekDay = true))
            }
        },
        dueDateText = uiState.dueDate?.let {
            with(timeFormatter) {
                it.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = true))
            }
        },
        category = uiState.category,
        account = uiState.account,
        toAccount = uiState.toAccount,
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
        onDueDateClick = {
            uiState.dueDate?.let { dueDate ->
                ivyContext.datePicker(
                    initialDate = with(timeConverter) { dueDate.toLocalDate() },
                ) {
                    viewModel.onEvent(
                        EditTransactionViewEvent.OnDueDateChanged(it.atTime(DueDateHour, 0))
                    )
                }
            }
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
        },
    )
}

/** Which of the (up to two) account rows the account picker was opened from. */
private enum class AccountPickerTarget { From, To }

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionUi(
    isNewTransaction: Boolean,
    transactionType: TransactionType,
    baseCurrency: String,
    initialTitle: String?,
    titleSuggestions: ImmutableSet<String>,
    description: String?,
    dateTimeText: String?,
    dueDateText: String?,
    category: Category?,
    account: Account?,
    toAccount: Account?,
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
    onDueDateClick: () -> Unit,
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
    onExchangeRateChange: (Double?) -> Unit,
    onTagOperation: (EditTransactionViewEvent.TagEvent) -> Unit,
    loanData: EditTransactionDisplayLoan = EditTransactionDisplayLoan(),
    backgroundProcessing: Boolean = false,
    hasChanges: Boolean = false,
    // Previews and screenshots pass false: a bottom sheet lives in its own window, so an
    // auto-opened keypad would cover the very screen the snapshot is meant to record.
    keypadAutoOpenEnabled: Boolean = true,
) {
    val nav = navigation()

    var overflowExpanded by remember { mutableStateOf(false) }
    var keypadVisible by remember { mutableStateOf(false) }
    var exchangeRateKeypadVisible by remember { mutableStateOf(false) }
    var categoryPickerVisible by remember { mutableStateOf(false) }
    var accountPickerTarget by remember { mutableStateOf<AccountPickerTarget?>(null) }
    var descriptionDialogVisible by remember { mutableStateOf(false) }
    var deleteDialogVisible by remember { mutableStateOf(false) }
    var accountChangeDialogVisible by remember { mutableStateOf(false) }
    var tagModalVisible by remember { mutableStateOf(false) }
    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    var pendingAccount by remember(account) { mutableStateOf(account) }

    var titleTextFieldValue by remember(initialTitle) {
        mutableStateOf(TextFieldValue(initialTitle.orEmpty()))
    }
    val titleFocus = remember { FocusRequester() }

    fun selectAccount(selected: Account) {
        if (loanData.isLoan && account?.currency != selected.currency) {
            pendingAccount = selected
            accountChangeDialogVisible = true
        } else {
            onAccountChange(selected)
        }
    }

    onScreenStart {
        if (isNewTransaction && keypadAutoOpenEnabled) {
            keypadVisible = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onClick = { nav.back() }) },
                actions = {
                    val items = overflowItems(
                        isNewTransaction = isNewTransaction,
                        isLoanRecord = loanData.isLoanRecord,
                        type = transactionType,
                        hasDateTime = dateTimeText != null,
                        hasDueDate = dueDateText != null,
                    )
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                        ) {
                            items.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(item.labelRes)) },
                                    onClick = {
                                        overflowExpanded = false
                                        when (item) {
                                            OverflowItem.Duplicate -> onDuplicate()
                                            OverflowItem.Delete -> deleteDialogVisible = true
                                            OverflowItem.MakePlanned -> {
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
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            val action = commitAction(
                isNewTransaction = isNewTransaction,
                hasDueDate = dueDateText != null,
                hasChanges = hasChanges,
                type = transactionType,
            )
            Button(
                onClick = {
                    when (action) {
                        CommitAction.Add -> onSave(true)
                        CommitAction.Save -> if (dueDateText != null) {
                            // Planned payment with unsaved edits: keep the screen open, exactly
                            // as the legacy ModalSave did.
                            onSave(false)
                            onSetHasChanges(false)
                        } else {
                            onSave(true)
                        }

                        CommitAction.Pay, CommitAction.Get -> onPayPlannedPayment()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(text = stringResource(action.labelRes))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (!loanData.isLoanRecord) {
                TransactionTypeSelector(
                    type = transactionType,
                    onTypeChange = onSetTransactionType,
                )
            }
            AmountHeadline(
                amountText = amount.format(baseCurrency),
                currency = baseCurrency,
                type = transactionType,
                supportingText = amountSupportingText(
                    transactionType = transactionType,
                    account = account,
                    customExchangeRateState = customExchangeRateState,
                ),
                onClick = { keypadVisible = true },
            )
            loanData.loanCaption?.let { caption ->
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            TitleField(
                value = titleTextFieldValue,
                onValueChange = {
                    titleTextFieldValue = it
                    onTitleChange(it.text)
                },
                type = transactionType,
                suggestions = titleSuggestions,
                onSuggestionClick = { suggestion ->
                    titleTextFieldValue = selectEndTextFieldValue(suggestion)
                    onTitleChange(suggestion)
                },
                onNext = {
                    if (shouldFocusAmount(amount = amount)) {
                        keypadVisible = true
                    } else {
                        onSave(true)
                    }
                },
                focusRequester = titleFocus,
            )
            Spacer(Modifier.height(8.dp))
            CategoryRow(
                categoryName = category?.name?.value,
                categoryColor = category?.color?.value?.let(::Color),
                categoryIcon = category?.icon?.id,
                onClick = { categoryPickerVisible = true },
            )
            if (transactionType == TransactionType.TRANSFER) {
                AccountRow(
                    accountName = account?.name,
                    currency = account?.currency,
                    label = stringResource(R.string.from),
                    onClick = { accountPickerTarget = AccountPickerTarget.From },
                )
                AccountRow(
                    accountName = toAccount?.name,
                    currency = toAccount?.currency,
                    label = stringResource(R.string.to),
                    onClick = { accountPickerTarget = AccountPickerTarget.To },
                )
                if (customExchangeRateState.showCard) {
                    // Directly under the pair of accounts it explains: this rate is what converts
                    // From's currency into To's. It also keeps the control above the fold, which
                    // is what the deleted `animateScrollTo` hack used to achieve by force.
                    ExchangeRateRow(
                        rateText = stringResource(
                            R.string.exchange_rate_value,
                            baseCurrency,
                            customExchangeRateState.exchangeRate.format(ExchangeRateDecimals),
                            customExchangeRateState.toCurrencyCode ?: baseCurrency,
                        ),
                        onClick = { exchangeRateKeypadVisible = true },
                        onReset = { onExchangeRateChange(null) },
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                AccountRow(
                    accountName = account?.name,
                    currency = account?.currency,
                    label = stringResource(R.string.account),
                    onClick = { accountPickerTarget = AccountPickerTarget.From },
                )
            }
            // An unpaid planned payment has a due date but no date-time yet; offering to set one
            // would turn it into a normal transaction, so the legacy screen hid the row there too.
            if (dueDateText == null || dateTimeText != null) {
                DateTimeRow(
                    dateTimeText = dateTimeText,
                    onClick = onSetDate,
                    onTimeClick = onSetTime,
                )
            }
            if (dueDateText != null) {
                DueDateRow(dueDateText = dueDateText, onClick = onDueDateClick)
            }
            DescriptionRow(
                description = description,
                onClick = { descriptionDialogVisible = true },
            )
            TagsRow(
                tagCount = transactionAssociatedTags.size,
                onClick = { tagModalVisible = true },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (keypadVisible) {
        AmountKeypadSheet(
            currency = baseCurrency,
            initialAmount = amount.takeIf { it != 0.0 },
            accounts = accounts.map {
                KeypadAccountUi(
                    id = it.id.toString(),
                    name = it.name,
                    selected = it.id == account?.id,
                )
            }.toImmutableList(),
            onAccountClick = { id ->
                accounts.firstOrNull { it.id.toString() == id }?.let(::selectAccount)
            },
            onAddAccountClick = {
                accountModalData = AccountModalData(
                    account = null,
                    baseCurrency = baseCurrency,
                    balance = 0.0,
                )
            },
            onDone = {
                keypadVisible = false
                onAmountChange(it)
                if (shouldFocusCategory(category)) {
                    categoryPickerVisible = true
                } else if (shouldFocusTitle(titleTextFieldValue, transactionType)) {
                    titleFocus.requestFocus()
                }
            },
            onDismiss = { keypadVisible = false },
            decimalCountMax = IvyCurrency.getDecimalPlaces(baseCurrency),
        )
    }

    if (exchangeRateKeypadVisible) {
        AmountKeypadSheet(
            currency = "",
            initialAmount = customExchangeRateState.exchangeRate,
            accounts = persistentListOf(),
            onAccountClick = {},
            onAddAccountClick = {},
            onDone = {
                exchangeRateKeypadVisible = false
                onExchangeRateChange(it)
            },
            onDismiss = { exchangeRateKeypadVisible = false },
            decimalCountMax = ExchangeRateDecimals,
        )
    }

    if (categoryPickerVisible) {
        val noCategory = PickerItemUi(
            id = NoCategoryItemId,
            title = stringResource(R.string.no_category),
            selected = category == null,
        )
        PickerSheet(
            title = stringResource(R.string.categories),
            items = (listOf(noCategory) + categories.map { it.toPickerItem(category?.id?.value) })
                .toImmutableList(),
            onItemClick = { id ->
                categoryPickerVisible = false
                onCategoryChange(categories.firstOrNull { it.id.value.toString() == id })
                if (shouldFocusTitle(titleTextFieldValue, transactionType)) {
                    titleFocus.requestFocus()
                } else if (shouldFocusAmount(amount = amount)) {
                    keypadVisible = true
                }
            },
            addLabel = stringResource(R.string.new_category),
            onAddClick = { categoryModalData = CategoryModalData(category = null) },
            onDismiss = { categoryPickerVisible = false },
            icon = { item ->
                ItemIconSDefaultIcon(
                    iconName = categories.firstOrNull { it.id.value.toString() == item.id }
                        ?.icon?.id,
                    defaultIcon = R.drawable.ic_custom_category_s,
                    tint = LocalContentColor.current,
                )
            },
        )
    }

    accountPickerTarget?.let { target ->
        val selectedId = when (target) {
            AccountPickerTarget.From -> account?.id
            AccountPickerTarget.To -> toAccount?.id
        }
        PickerSheet(
            title = stringResource(R.string.accounts),
            items = accounts.map { it.toPickerItem(selectedId) }.toImmutableList(),
            onItemClick = { id ->
                accountPickerTarget = null
                accounts.firstOrNull { it.id.toString() == id }?.let { selected ->
                    when (target) {
                        AccountPickerTarget.From -> selectAccount(selected)
                        AccountPickerTarget.To -> onToAccountChange(selected)
                    }
                }
            },
            addLabel = stringResource(R.string.new_account),
            onAddClick = {
                accountModalData = AccountModalData(
                    account = null,
                    baseCurrency = baseCurrency,
                    balance = 0.0,
                )
            },
            onDismiss = { accountPickerTarget = null },
            icon = { item ->
                ItemIconSDefaultIcon(
                    iconName = accounts.firstOrNull { it.id.toString() == item.id }?.icon,
                    defaultIcon = R.drawable.ic_custom_account_s,
                    tint = LocalContentColor.current,
                )
            },
        )
    }

    if (descriptionDialogVisible) {
        TextInputDialog(
            title = stringResource(R.string.description),
            initialValue = description.orEmpty(),
            onConfirm = {
                descriptionDialogVisible = false
                onDescriptionChange(it.trim().takeIf(String::isNotBlank))
            },
            onDismiss = { descriptionDialogVisible = false },
        )
    }

    if (deleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { deleteDialogVisible = false },
            title = { Text(text = stringResource(R.string.confirm_deletion)) },
            text = {
                Text(text = stringResource(R.string.transaction_confirm_deletion_description))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDialogVisible = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogVisible = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (accountChangeDialogVisible) {
        AlertDialog(
            onDismissRequest = { accountChangeDialogVisible = false },
            title = { Text(text = stringResource(R.string.confirm_account_change)) },
            text = { Text(text = stringResource(R.string.confirm_account_change_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountChangeDialogVisible = false
                        pendingAccount?.let(onAccountChange)
                    },
                ) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountChangeDialogVisible = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (backgroundProcessing) {
        ProgressDialog(
            title = stringResource(R.string.confirm_account_change),
            description = stringResource(R.string.confirm_account_loan_change),
        )
    }

    LegacyModals(
        categoryModalData = categoryModalData,
        accountModalData = accountModalData,
        tagModalVisible = tagModalVisible,
        onCreateCategory = {
            onCreateCategory(it)
            categoryPickerVisible = false
        },
        onEditCategory = onEditCategory,
        onCategoryModalDismiss = { categoryModalData = null },
        onCreateAccount = onCreateAccount,
        onAccountModalDismiss = { accountModalData = null },
        tags = tags,
        transactionAssociatedTags = transactionAssociatedTags,
        onTagOperation = onTagOperation,
        onTagModalDismiss = { tagModalVisible = false },
    )
}

/**
 * The three kept legacy modals. They are `BoxWithConstraintsScope` extensions and read the old
 * design system's composition locals, which previews and screenshot tests do not provide, so they
 * are mounted lazily on first use. Once mounted they stay mounted, so their dismiss animations
 * still play.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LegacyModals(
    categoryModalData: CategoryModalData?,
    accountModalData: AccountModalData?,
    tagModalVisible: Boolean,
    onCreateCategory: (CreateCategoryData) -> Unit,
    onEditCategory: (Category) -> Unit,
    onCategoryModalDismiss: () -> Unit,
    onCreateAccount: (CreateAccountData) -> Unit,
    onAccountModalDismiss: () -> Unit,
    tags: ImmutableList<Tag>,
    transactionAssociatedTags: ImmutableList<TagId>,
    onTagOperation: (EditTransactionViewEvent.TagEvent) -> Unit,
    onTagModalDismiss: () -> Unit,
) {
    val anyVisible =
        categoryModalData != null || accountModalData != null || tagModalVisible
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(anyVisible) {
        if (anyVisible) {
            mounted = true
        }
    }
    if (!mounted) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        CategoryModal(
            modal = categoryModalData,
            onCreateCategory = onCreateCategory,
            onEditCategory = onEditCategory,
            dismiss = onCategoryModalDismiss,
        )

        AccountModal(
            modal = accountModalData,
            onCreateAccount = onCreateAccount,
            onEditAccount = { _, _ -> },
            dismiss = onAccountModalDismiss,
        )

        ShowTagModal(
            visible = tagModalVisible,
            onDismiss = {
                onTagModalDismiss()
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
            },
        )
    }
}

@Composable
private fun amountSupportingText(
    transactionType: TransactionType,
    account: Account?,
    customExchangeRateState: CustomExchangeRateState,
): String? = when {
    transactionType == TransactionType.TRANSFER -> {
        val converted = customExchangeRateState.convertedAmount
        val toCurrency = customExchangeRateState.toCurrencyCode
        if (converted != null && toCurrency != null) {
            "≈ ${converted.format(toCurrency)} $toCurrency"
        } else {
            null
        }
    }

    account != null -> account.name
    else -> stringResource(R.string.select_account)
}

private val CommitAction.labelRes: Int
    get() = when (this) {
        CommitAction.Add -> R.string.add
        CommitAction.Save -> R.string.save
        CommitAction.Pay -> R.string.pay
        CommitAction.Get -> R.string.get
    }

private val OverflowItem.labelRes: Int
    get() = when (this) {
        OverflowItem.Duplicate -> R.string.duplicate
        OverflowItem.Delete -> R.string.delete
        OverflowItem.MakePlanned -> R.string.make_it_planned
    }

private fun shouldFocusCategory(
    category: Category?,
): Boolean = category == null

private fun shouldFocusTitle(
    titleTextFieldValue: TextFieldValue,
    type: TransactionType
): Boolean = titleTextFieldValue.text.isBlank() && type != TransactionType.TRANSFER

private fun shouldFocusAmount(amount: Double) = amount == 0.0

enum class EditTransactionPreviewState { NewExpense, EditFilled, TransferWithRate }

@Preview
@Composable
private fun EditTransactionPreview(
    dark: Boolean = false,
    state: EditTransactionPreviewState = EditTransactionPreviewState.EditFilled,
) {
    IvyPreview(dark = dark) {
        when (state) {
            EditTransactionPreviewState.NewExpense -> PreviewEditTransactionUi(
                isNewTransaction = true,
                transactionType = TransactionType.EXPENSE,
                amount = 0.0,
                account = PreviewRevolut,
            )

            EditTransactionPreviewState.EditFilled -> PreviewEditTransactionUi(
                isNewTransaction = false,
                transactionType = TransactionType.EXPENSE,
                amount = 42.50,
                account = PreviewRevolut,
                initialTitle = "Groceries",
                category = PreviewCategory,
                dateTimeText = "Fri, Aug 07 14:20",
                description = "Weekly shop",
                transactionAssociatedTags = persistentListOf(
                    TagId(UUID.randomUUID()),
                    TagId(UUID.randomUUID()),
                ),
            )

            EditTransactionPreviewState.TransferWithRate -> PreviewEditTransactionUi(
                isNewTransaction = true,
                transactionType = TransactionType.TRANSFER,
                amount = 250.0,
                account = PreviewRevolut,
                toAccount = PreviewN26,
                customExchangeRateState = CustomExchangeRateState(
                    showCard = true,
                    toCurrencyCode = "EUR",
                    fromCurrencyCode = "USD",
                    exchangeRate = 0.9176,
                    convertedAmount = 229.4,
                ),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun PreviewEditTransactionUi(
    isNewTransaction: Boolean,
    transactionType: TransactionType,
    amount: Double,
    account: Account?,
    initialTitle: String? = null,
    category: Category? = null,
    dateTimeText: String? = null,
    description: String? = null,
    toAccount: Account? = null,
    transactionAssociatedTags: ImmutableList<TagId> = persistentListOf(),
    customExchangeRateState: CustomExchangeRateState = CustomExchangeRateState(),
) {
    EditTransactionUi(
        isNewTransaction = isNewTransaction,
        transactionType = transactionType,
        baseCurrency = "USD",
        initialTitle = initialTitle,
        titleSuggestions = persistentSetOf(),
        description = description,
        dateTimeText = dateTimeText,
        dueDateText = null,
        category = category,
        account = account,
        toAccount = toAccount,
        amount = amount,
        customExchangeRateState = customExchangeRateState,
        categories = persistentListOf(),
        accounts = persistentListOf(),
        tags = persistentListOf(),
        transactionAssociatedTags = transactionAssociatedTags,
        onTitleChange = {},
        onDescriptionChange = {},
        onAmountChange = {},
        onCategoryChange = {},
        onAccountChange = {},
        onToAccountChange = {},
        onDueDateClick = {},
        onSetDate = {},
        onSetTime = {},
        onSetTransactionType = {},
        onCreateCategory = {},
        onEditCategory = {},
        onPayPlannedPayment = {},
        onSave = {},
        onSetHasChanges = {},
        onDelete = {},
        onDuplicate = {},
        onCreateAccount = {},
        onExchangeRateChange = {},
        onTagOperation = {},
        keypadAutoOpenEnabled = false,
    )
}

private val PreviewRevolut = Account(
    name = "Revolut",
    color = 0xFF2196F3.toInt(),
    currency = "USD",
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
)

private val PreviewN26 = Account(
    name = "N26",
    color = 0xFF9C27B0.toInt(),
    currency = "EUR",
    id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
)

private val PreviewCategory = Category(
    id = CategoryId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
    name = NotBlankTrimmedString.unsafe("Food & Drink"),
    color = ColorInt(0xFF25B26A.toInt()),
    icon = null,
    orderNum = 0.0,
)

/** For screenshot testing */
@Composable
fun EditTransactionUiTest(isDark: Boolean, state: EditTransactionPreviewState) {
    EditTransactionPreview(dark = isDark, state = state)
}
