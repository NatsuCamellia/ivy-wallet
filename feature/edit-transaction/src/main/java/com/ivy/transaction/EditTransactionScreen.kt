package com.ivy.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.EditTransactionDisplayLoan
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.edit.TransactionDateTime
import com.ivy.legacy.ui.component.edit.core.Description
import com.ivy.legacy.ui.sheet.AccountModal
import com.ivy.legacy.ui.sheet.AccountModalData
import com.ivy.legacy.ui.sheet.AmountModal
import com.ivy.legacy.ui.sheet.CategoryModal
import com.ivy.legacy.ui.sheet.CategoryModalData
import com.ivy.legacy.ui.sheet.ChangeTransactionTypeModal
import com.ivy.legacy.ui.sheet.ChooseCategoryModal
import com.ivy.legacy.ui.sheet.ConfirmModal
import com.ivy.legacy.ui.sheet.DeleteModal
import com.ivy.legacy.ui.sheet.DescriptionModal
import com.ivy.legacy.ui.sheet.ProgressModal
import com.ivy.legacy.ui.component.tags.ShowTagModal
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.color.Orange
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.wallet.domain.data.CustomExchangeRateState
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.edit.core.Category
import com.ivy.wallet.ui.edit.core.DueDate
import com.ivy.legacy.ui.component.edit.core.EditBottomBar
import com.ivy.wallet.ui.edit.core.Title
import com.ivy.wallet.ui.edit.core.Toolbar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Composable
fun EditTransactionScreen(screen: EditTransactionScreen) {
    val viewModel: EditTransactionViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.start(screen)
    }

    val keyboardController = LocalSoftwareKeyboardController.current

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
            keyboardController?.hide()
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
@Composable
private fun UI(
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
    modifier: Modifier = Modifier,
    onExchangeRateChange: (Double?) -> Unit = { },
    onTagOperation: (EditTransactionViewEvent.TagEvent) -> Unit = {},
    loanData: EditTransactionDisplayLoan = EditTransactionDisplayLoan(),
    backgroundProcessing: Boolean = false,
    hasChanges: Boolean = false,
) {
    var chooseCategoryModalVisible by remember { mutableStateOf(false) }
    var tagModelVisible by remember { mutableStateOf(false) }
    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    var descriptionModalVisible by remember { mutableStateOf(false) }
    var deleteTrnModalVisible by remember { mutableStateOf(false) }
    var changeTransactionTypeModalVisible by remember { mutableStateOf(false) }
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
    val titleFocus = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    val isEdit = screen.initialTransactionId != null
    val actionLabel = when {
        !isEdit -> stringResource(R.string.add)
        dueDate != null && !hasChanges -> if (transactionType == TransactionType.EXPENSE) {
            stringResource(R.string.pay)
        } else {
            stringResource(R.string.get)
        }
        else -> stringResource(R.string.save)
    }
    val onAction: () -> Unit = {
        when {
            !isEdit || dueDate == null -> onSave(true)
            hasChanges -> {
                onSave(false)
                onSetHasChanges(false)
            }
            else -> onPayPlannedPayment()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Toolbar(
                title = screenTitle(transactionType, isEdit),
                // Setting the transaction type to TransactionType.TRANSFER for transactions associated
                // with loan record to hide the ChangeTransactionType Button
                type = if (loanData.isLoanRecord) TransactionType.TRANSFER else transactionType,
                initialTransactionId = screen.initialTransactionId,
                onDeleteTrnModal = { deleteTrnModalVisible = true },
                onChangeTransactionTypeModal = { changeTransactionTypeModalVisible = true },
                showDuplicateButton = true,
                onDuplicate = onDuplicate
            )
        },
        bottomBar = {
            EditBottomBar(
                type = transactionType,
                accounts = accounts,
                selectedAccount = account,
                toAccount = toAccount,
                amount = amount,
                currency = baseCurrency,
                amountModalShown = amountModalShown,
                setAmountModalShown = { amountModalShown = it },
                onAmountChange = {
                    onAmountChange(it)
                    if (shouldFocusCategory(category)) {
                        chooseCategoryModalVisible = true
                    } else if (shouldFocusTitle(titleTextFieldValue, transactionType)) {
                        titleFocus.requestFocus()
                    }
                },
                onSelectedAccountChange = {
                    if (loanData.isLoan && account?.currency != it.currency) {
                        selectedAcc = it
                        accountChangeModal = true
                    } else {
                        onAccountChange(it)
                    }
                },
                onToAccountChange = onToAccountChange,
                onAddNewAccount = {
                    accountModalData = AccountModalData(
                        account = null, baseCurrency = baseCurrency, balance = 0.0
                    )
                },
                initialTransactionId = screen.initialTransactionId,
                convertedAmount = customExchangeRateState.convertedAmount,
                convertedAmountCurrencyCode = customExchangeRateState.toCurrencyCode,
            ) {
                Button(
                    onClick = onAction,
                    modifier = if (isEdit) Modifier else Modifier.testTag("modal_add"),
                ) {
                    Text(actionLabel)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Title(
                type = transactionType,
                titleFocus = titleFocus,
                titleTextFieldValue = titleTextFieldValue,
                setTitleTextFieldValue = { titleTextFieldValue = it },
                suggestions = titleSuggestions,
                scrollState = scrollState,
                onTitleChange = onTitleChange,
                onNext = {
                    when {
                        shouldFocusAmount(amount = amount) -> amountModalShown = true
                        else -> onSave(true)
                    }
                }
            )

            loanData.loanCaption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Category(category = category, onChooseCategory = { chooseCategoryModalVisible = true })

            TagsCard(
                transactionAssociatedTags = transactionAssociatedTags,
                onClick = { tagModelVisible = true },
            )

            val ivyContext = ivyWalletCtx()
            val timeConverter = LocalTimeConverter.current
            if (dueDate != null) {
                DueDate(dueDate = dueDate, onPickDueDate = {
                    ivyContext.datePicker(
                        initialDate = with(timeConverter) { dueDate.toLocalDate() }
                    ) {
                        onDueDateChange(it.atTime(12, 0))
                    }
                })
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

            if (transactionType == TransactionType.TRANSFER && customExchangeRateState.showCard) {
                ExchangeRateCard(
                    fromCurrencyCode = baseCurrency,
                    toCurrencyCode = customExchangeRateState.toCurrencyCode ?: baseCurrency,
                    exchangeRate = customExchangeRateState.exchangeRate,
                    onReset = { onExchangeRateChange(null) },
                    onClick = { exchangeRateAmountModalShown = true },
                )
            }

            if (dueDate == null && transactionType != TransactionType.TRANSFER && dateTime == null) {
                val nav = navigation()
                Card(
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
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) {
                        Text(stringResource(R.string.add_planned_date_payment))
                    }
                }
            }
        }
    }

    onScreenStart {
        if (screen.initialTransactionId == null) {
            amountModalShown = true
        }
    }

    // Modals
    ChooseCategoryModal(
        visible = chooseCategoryModalVisible,
        initialCategory = category,
        categories = categories,
        showCategoryModal = { categoryModalData = CategoryModalData(it) },
        onCategoryChanged = {
            onCategoryChange(it)
            if (shouldFocusTitle(titleTextFieldValue, transactionType)) {
                titleFocus.requestFocus()
            } else if (shouldFocusAmount(amount = amount)) {
                amountModalShown = true
            }
        },
        dismiss = {
            chooseCategoryModalVisible = false
        }
    )

    CategoryModal(modal = categoryModalData, onCreateCategory = { createData ->
        onCreateCategory(createData)
        chooseCategoryModalVisible = false
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

    ChangeTransactionTypeModal(
        visible = changeTransactionTypeModalVisible,
        includeTransferType = true,
        initialType = transactionType,
        dismiss = {
            changeTransactionTypeModalVisible = false
        }
    ) {
        onSetTransactionType(it)
    }

    ConfirmModal(
        visible = accountChangeModal,
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_change_description),
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
        visible = tagModelVisible,
        onDismiss = {
            tagModelVisible = false
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

private fun shouldFocusCategory(
    category: Category?,
): Boolean = category == null

private fun shouldFocusTitle(
    titleTextFieldValue: TextFieldValue,
    type: TransactionType
): Boolean = titleTextFieldValue.text.isBlank() && type != TransactionType.TRANSFER

private fun shouldFocusAmount(amount: Double) = amount == 0.0

@Composable
private fun screenTitle(type: TransactionType, isEdit: Boolean): String = stringResource(
    when (type) {
        TransactionType.INCOME -> if (isEdit) R.string.edit_income else R.string.add_income
        TransactionType.EXPENSE -> if (isEdit) R.string.edit_expense else R.string.add_expense
        TransactionType.TRANSFER -> if (isEdit) R.string.edit_transfer else R.string.add_transfer
    }
)

@Composable
private fun TagsCard(
    transactionAssociatedTags: ImmutableList<TagId>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTags = transactionAssociatedTags.isNotEmpty()
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = if (hasTags) Icons.AutoMirrored.Filled.Label else Icons.Default.Add,
                    contentDescription = null,
                )
            },
            supportingContent = if (hasTags) {
                { Text(transactionAssociatedTags.size.toString()) }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(stringResource(if (hasTags) R.string.tags else R.string.add_tags))
        }
    }
}

@Composable
private fun ExchangeRateCard(
    fromCurrencyCode: String,
    toCurrencyCode: String,
    exchangeRate: Double,
    onReset: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(Unit) { bringIntoView.bringIntoView() }

    Card(onClick = onClick, modifier = modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView)) {
        ListItem(
            leadingContent = { Icon(Icons.Default.CurrencyExchange, contentDescription = null) },
            overlineContent = { Text(stringResource(R.string.exchange_rate)) },
            trailingContent = {
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_exchange_rate))
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            val rate = exchangeRate.format(IvyCurrency.getDecimalPlaces(toCurrencyCode))
            Text("1 $fromCurrencyCode = $rate $toCurrencyCode")
        }
    }
}

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 27, 0, 35)
    .toInstant(ZoneOffset.UTC)

@Suppress("LongParameterList", "MagicNumber")
@Composable
private fun PreviewUI(
    isDark: Boolean,
    screen: EditTransactionScreen,
    transactionType: TransactionType,
    accounts: ImmutableList<Account>,
    account: Account?,
    modifier: Modifier = Modifier,
    initialTitle: String = "",
    toAccount: Account? = null,
    amount: Double = 0.0,
    category: Category? = null,
    description: String? = null,
    customExchangeRateState: CustomExchangeRateState = CustomExchangeRateState(),
) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        NavigationRoot(navigation = Navigation()) {
            UI(
                screen = screen,
                initialTitle = initialTitle,
                titleSuggestions = persistentSetOf(),
                tags = persistentListOf(),
                transactionAssociatedTags = persistentListOf(),
                baseCurrency = account?.currency ?: "BGN",
                dateTime = testDateTime,
                description = description,
                category = category,
                account = account,
                toAccount = toAccount,
                amount = amount,
                dueDate = null,
                transactionType = transactionType,
                customExchangeRateState = customExchangeRateState,

                categories = persistentListOf(),
                accounts = accounts,

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
                onSetTransactionType = {},
                modifier = modifier,
            )
        }
    }
}

private val previewPhyre = Account(name = "phyre", Orange.toArgb(), currency = "BGN")
private val previewCash = Account(name = "Cash", Color(0xFF2E7D32).toArgb(), currency = "BGN", icon = "cash")
private val previewRevolut = Account(name = "Revolut", Color(0xFF1565C0).toArgb(), currency = "EUR")

@Suppress("MagicNumber")
private val previewSalary = Category(
    id = CategoryId(UUID(0, 1)),
    name = NotBlankTrimmedString.unsafe("Salary"),
    color = ColorInt(Color(0xFF6A1B9A).toArgb()),
    icon = null,
    orderNum = 0.0,
)

/** For screenshot testing */
@Composable
fun EditTransactionScreenUiTest(isDark: Boolean) {
    PreviewUI(
        isDark = isDark,
        screen = EditTransactionScreen(UUID(0, 0), TransactionType.EXPENSE),
        transactionType = TransactionType.INCOME,
        accounts = persistentListOf(previewPhyre),
        account = previewPhyre,
    )
}

/** For screenshot testing: a new expense opens the amount sheet on top of the empty form */
@Composable
fun EditTransactionNewExpenseUiTest(isDark: Boolean) {
    PreviewUI(
        isDark = isDark,
        screen = EditTransactionScreen(null, TransactionType.EXPENSE),
        transactionType = TransactionType.EXPENSE,
        accounts = persistentListOf(previewPhyre, previewCash),
        account = previewPhyre,
    )
}

/** For screenshot testing */
@Suppress("MagicNumber")
@Composable
fun EditTransactionEditIncomeUiTest(isDark: Boolean) {
    PreviewUI(
        isDark = isDark,
        screen = EditTransactionScreen(UUID(0, 2), TransactionType.INCOME),
        transactionType = TransactionType.INCOME,
        accounts = persistentListOf(previewPhyre, previewCash),
        account = previewCash,
        initialTitle = "March salary",
        amount = 2500.0,
        category = previewSalary,
        description = "Paid by the employer on the last working day of the month",
    )
}

/** For screenshot testing */
@Suppress("MagicNumber")
@Composable
fun EditTransactionTransferUiTest(isDark: Boolean) {
    PreviewUI(
        isDark = isDark,
        screen = EditTransactionScreen(UUID(0, 3), TransactionType.TRANSFER),
        transactionType = TransactionType.TRANSFER,
        accounts = persistentListOf(previewCash, previewRevolut),
        account = previewCash,
        toAccount = previewRevolut,
        amount = 1000.0,
        customExchangeRateState = CustomExchangeRateState(
            showCard = true,
            toCurrencyCode = "EUR",
            fromCurrencyCode = "BGN",
            exchangeRate = 0.51,
            convertedAmount = 510.0,
        ),
    )
}

/** For screenshot testing */
@Suppress("MagicNumber")
@Composable
fun EditTransactionHugeAmountUiTest(isDark: Boolean) {
    val account = Account(
        name = "Long-term savings and investments for the whole family",
        color = Orange.toArgb(),
        currency = "BGN",
    )
    PreviewUI(
        isDark = isDark,
        screen = EditTransactionScreen(UUID(0, 4), TransactionType.EXPENSE),
        transactionType = TransactionType.EXPENSE,
        accounts = persistentListOf(account, previewCash),
        account = account,
        initialTitle = "A very long title that has to wrap onto several lines of the title field",
        amount = 123456789012.34,
        category = previewSalary,
    )
}
