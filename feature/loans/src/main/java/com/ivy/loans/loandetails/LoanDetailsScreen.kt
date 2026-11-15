package com.ivy.loans.loandetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.base.model.LoanRecordType
import com.ivy.base.model.TransactionType
import com.ivy.base.model.processByType
import com.ivy.data.model.LoanType
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.legacy.humanReadableType
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.TypeAmount
import com.ivy.legacy.ui.component.amountLabel
import com.ivy.legacy.ui.component.label
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.sheet.DeleteModal
import com.ivy.legacy.ui.sheet.LoanModal
import com.ivy.legacy.ui.sheet.LoanRecordModal
import com.ivy.legacy.ui.sheet.ProgressModal
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.loans.loan.data.DisplayLoanRecord
import com.ivy.loans.loandetails.events.DeleteLoanModalEvent
import com.ivy.loans.loandetails.events.LoanDetailsScreenEvent
import com.ivy.loans.loandetails.events.LoanModalEvent
import com.ivy.loans.loandetails.events.LoanRecordModalEvent
import com.ivy.navigation.LoanDetailsScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.color.Blue
import com.ivy.ui.color.Green
import com.ivy.ui.component.BackButton
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.IvyCurrency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Composable
fun LoanDetailsScreen(screen: LoanDetailsScreen) {
    val viewModel: LoanDetailsViewModel = viewModel()
    viewModel.screen = screen
    val state = viewModel.uiState()

    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UI(
    state: LoanDetailsScreenState,
    modifier: Modifier = Modifier,
    onEventHandler: (LoanDetailsScreenEvent) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val loan = state.loan
    val records = state.displayLoanRecords

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LoanTopBar(
                loan = loan,
                onEdit = { onEventHandler(LoanDetailsScreenEvent.OnEditLoanClick) },
                onDelete = {
                    onEventHandler(DeleteLoanModalEvent.OnDismissDeleteLoan(isDeleteModalVisible = true))
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (loan != null) {
                val addLabel = stringResource(R.string.add_record)
                ExtendedFloatingActionButton(
                    onClick = { onEventHandler(LoanDetailsScreenEvent.OnAddRecord) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(addLabel) },
                    modifier = Modifier
                        .testTag("loan_add_record")
                        .semantics { contentDescription = addLabel }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("loan_details_lazy_column"),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                // clears the add button that the Scaffold draws over the list
                bottom = innerPadding.calculateBottomPadding() + LocalMinimumInteractiveComponentSize.current * 2
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            if (loan != null) {
                item(key = "summary") {
                    LoanSummary(
                        loan = loan,
                        baseCurrency = state.baseCurrency,
                        loanTotalAmount = state.loanTotalAmount,
                        amountPaid = state.amountPaid,
                        interestPaid = state.loanAmountPaid,
                        account = state.selectedLoanAccount,
                        onAmountClick = { onEventHandler(LoanDetailsScreenEvent.OnAmountClick) },
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                if (records.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            emptyStateTitle = stringResource(R.string.no_records),
                            emptyStateText = stringResource(R.string.no_records_for_the_loan),
                        )
                    }
                }

                loanRecords(
                    loan = loan,
                    records = records,
                    baseCurrency = state.baseCurrency,
                    onClick = { onEventHandler(LoanRecordModalEvent.OnClickLoanRecord(it)) },
                    onInitialClick = { onEventHandler(LoanDetailsScreenEvent.OnEditLoanClick) },
                )
            }
        }
    }

    LoanModal(
        modal = state.loanModalData, onCreateLoan = {
        // do nothing
    }, onEditLoan = { loan, createLoanTransaction ->
        onEventHandler.invoke(LoanModalEvent.OnEditLoanModal(loan, createLoanTransaction))
    }, dismiss = {
        onEventHandler.invoke(LoanModalEvent.OnDismissLoanModal)
    }, onCreateAccount = { createAccountData ->
        onEventHandler.invoke(LoanDetailsScreenEvent.OnCreateAccount(createAccountData))
    }, accounts = state.accounts, onPerformCalculations = {
        onEventHandler.invoke(LoanModalEvent.PerformCalculation)
    }, dateTime = state.dateTime,
        onSetDate = {
            onEventHandler.invoke(LoanModalEvent.OnChangeDate)
        },
        onSetTime = {
            onEventHandler.invoke(LoanModalEvent.OnChangeTime)
        },
    )

    LoanRecordModal(
        modal = state.loanRecordModalData, onCreate = {
        onEventHandler.invoke(LoanRecordModalEvent.OnCreateLoanRecord(it))
    }, onEdit = {
        onEventHandler.invoke(LoanRecordModalEvent.OnEditLoanRecord(it))
    }, onDelete = { loanRecord ->
        onEventHandler.invoke(LoanRecordModalEvent.OnDeleteLoanRecord(loanRecord))
    }, accounts = state.accounts, dismiss = {
        onEventHandler.invoke(LoanRecordModalEvent.OnDismissLoanRecord)
    }, onCreateAccount = { createAccountData ->
        onEventHandler.invoke(LoanDetailsScreenEvent.OnCreateAccount(createAccountData))
    },
        dateTime = state.dateTime,
        onSetDate = {
            onEventHandler.invoke(LoanRecordModalEvent.OnChangeDate)
        },
        onSetTime = {
            onEventHandler.invoke(LoanRecordModalEvent.OnChangeTime)
        },
    )

    DeleteModal(
        visible = state.isDeleteModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.loan_confirm_deletion_description),
        dismiss = {
            onEventHandler.invoke(DeleteLoanModalEvent.OnDismissDeleteLoan(isDeleteModalVisible = false))
        }
    ) {
        onEventHandler.invoke(DeleteLoanModalEvent.OnDeleteLoan)
    }

    ProgressModal(
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_loan_change),
        visible = state.waitModalVisible
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoanTopBar(
    loan: Loan?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    LargeFlexibleTopAppBar(
        title = {
            if (loan != null) {
                Row(
                    modifier = Modifier.clickable(
                        onClickLabel = stringResource(R.string.edit),
                        role = Role.Button,
                        onClick = onEdit
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconBadge(
                        color = Color(loan.color),
                        iconName = loan.icon,
                        defaultIcon = R.drawable.ic_custom_loan_m,
                    )
                    Text(
                        text = loan.name,
                        modifier = Modifier.testTag("loan_name"),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        modifier = modifier,
        subtitle = if (loan != null) {
            {
                val formatter = LocalTimeFormatter.current
                val date = loan.dateTime?.let {
                    with(formatter) { it.format(TimeFormatter.Style.DateOnly(includeWeekDay = false)) }
                }
                Text(text = listOfNotNull(loan.humanReadableType(), date).joinToString(" · "))
            }
        } else {
            null
        },
        navigationIcon = {
            BackButton(onClick = { nav.back() }, modifier = Modifier.testTag("toolbar_close"))
        },
        actions = {
            if (loan != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Suppress("LongParameterList")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoanSummary(
    loan: Loan,
    baseCurrency: String,
    loanTotalAmount: Double,
    amountPaid: Double,
    interestPaid: Double,
    account: Account?,
    onAmountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val percentPaid = if (loanTotalAmount != 0.0) amountPaid / loanTotalAmount else 0.0
    val interestPercent = if (loanTotalAmount != 0.0) interestPaid / loanTotalAmount else 0.0
    val paidOff = percentPaid >= 1.0
    val statusColor = if (paidOff) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(onClick = onAmountClick, modifier = Modifier.fillMaxWidth().testTag("loan_amount")) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        when {
                            paidOff -> R.string.loan_paid_off
                            loan.type == LoanType.LEND -> R.string.loan_left_to_receive
                            else -> R.string.loan_left_to_pay
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor
                )
                Text(
                    text = amountLabel(if (paidOff) loanTotalAmount else loanTotalAmount - amountPaid, baseCurrency),
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = statusColor,
                )
                LinearWavyProgressIndicator(
                    progress = { percentPaid.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (paidOff) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    modifier = Modifier.testTag("amount_paid"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AmountText(
                        amount = amountPaid,
                        currency = baseCurrency,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(text = "/", style = MaterialTheme.typography.bodyMedium)
                    AmountText(
                        amount = loanTotalAmount,
                        currency = baseCurrency,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.loan_percent_paid, (percentPaid * 100).format(2)),
                        modifier = Modifier.testTag("percent_paid"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IvyCurrency.fromCode(baseCurrency)?.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (interestPaid != 0.0) {
                    HorizontalDivider()
                    Text(text = stringResource(R.string.loan_interest), style = MaterialTheme.typography.labelLarge)
                    LinearWavyProgressIndicator(
                        progress = { interestPercent.toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.loan_percent_paid, (interestPercent * 100).format(2)),
                            modifier = Modifier.testTag("loan_interest_percent_paid"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.interest_paid,
                                interestPaid.format(baseCurrency),
                                baseCurrency
                            ),
                            modifier = Modifier.testTag("interest_paid"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (account != null) {
            AccountChip(
                account = account,
                onClick = {
                    nav.navigateTo(TransactionsScreen(accountId = account.id, categoryId = null))
                }
            )
        }
    }
}

@Composable
private fun AccountChip(account: Account, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(account.name) },
        modifier = modifier,
        leadingIcon = {
            IconBadge(
                color = Color(account.color),
                iconName = account.icon,
                defaultIcon = R.drawable.ic_custom_account_m,
                size = AssistChipDefaults.IconSize,
            )
        },
    )
}

/** The starting record closes the list: every record and the loan itself form one group of rows. */
private fun LazyListScope.loanRecords(
    loan: Loan,
    records: List<DisplayLoanRecord>,
    baseCurrency: String,
    onClick: (DisplayLoanRecord) -> Unit,
    onInitialClick: () -> Unit,
) {
    val count = records.size + 1
    records.forEachIndexed { index, displayRecord ->
        item(key = displayRecord.loanRecord.id) {
            LoanRecordRow(
                loan = loan,
                displayRecord = displayRecord,
                index = index,
                count = count,
                onClick = { onClick(displayRecord) },
            )
        }
    }
    item(key = "initial") {
        InitialRecordRow(
            loan = loan,
            baseCurrency = baseCurrency,
            index = records.size,
            count = count,
            onClick = onInitialClick,
        )
    }
}

@Composable
private fun LoanRecordRow(
    loan: Loan,
    displayRecord: DisplayLoanRecord,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val record = displayRecord.loanRecord
    val account = displayRecord.account
    val typeLabel = stringResource(
        if (record.loanRecordType == LoanRecordType.INCREASE) R.string.increase_loan else R.string.decrease_loan
    )
    val note = record.note?.takeIf { it.isNotNullOrBlank() }
    val converted = record.convertedAmount?.takeIf { displayRecord.loanCurrencyCode.isNotEmpty() }
    val timeFormatter = LocalTimeFormatter.current

    RecordRow(
        date = with(timeFormatter) {
            record.dateTime.formatLocal(TimeFormatter.Style.DateAndTime(includeWeekDay = true))
        },
        headline = note ?: typeLabel,
        supporting = if (note != null) typeLabel else null,
        type = loanTransactionType(loan.type, record.loanRecordType),
        amount = record.amount,
        currency = displayRecord.loanRecordCurrencyCode,
        index = index,
        count = count,
        onClick = onClick,
        modifier = modifier,
        typeDescription = typeLabel,
        converted = converted,
        convertedCurrency = displayRecord.loanCurrencyCode,
        chips = if (account != null || record.interest) {
            {
                if (account != null) {
                    AccountChip(
                        account = account,
                        onClick = {
                            nav.navigateTo(TransactionsScreen(accountId = account.id, categoryId = null))
                        }
                    )
                }
                if (record.interest) {
                    AssistChip(onClick = onClick, label = { Text(stringResource(R.string.interest)) })
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun InitialRecordRow(
    loan: Loan,
    baseCurrency: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = LocalTimeFormatter.current
    val startingLabel = stringResource(R.string.initial_loan_record)
    RecordRow(
        date = loan.dateTime?.let {
            with(timeFormatter) { it.format(TimeFormatter.Style.DateAndTime(includeWeekDay = true)) }
        },
        headline = startingLabel,
        supporting = loan.note?.takeIf { it.isNotNullOrBlank() },
        type = if (loan.type == LoanType.LEND) TransactionType.EXPENSE else TransactionType.INCOME,
        amount = loan.amount,
        currency = baseCurrency,
        index = index,
        count = count,
        onClick = onClick,
        modifier = modifier,
        typeDescription = startingLabel,
    )
}

/** A lent loan that grows is money out and a payment is money in; a borrowed loan is the other way round. */
private fun loanTransactionType(loanType: LoanType, recordType: LoanRecordType): TransactionType = when (loanType) {
    LoanType.LEND -> recordType.processByType(
        increaseAction = { TransactionType.EXPENSE },
        decreaseAction = { TransactionType.INCOME }
    )

    LoanType.BORROW -> recordType.processByType(
        increaseAction = { TransactionType.INCOME },
        decreaseAction = { TransactionType.EXPENSE }
    )
}

/** The name and the amount share a line; when the amount is too wide for that it moves to its own line. */
@Suppress("LongParameterList")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordRow(
    date: String?,
    headline: String,
    supporting: String?,
    type: TransactionType,
    amount: Double,
    currency: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    typeDescription: String = type.label(),
    converted: Double? = null,
    convertedCurrency: String = "",
    chips: (@Composable () -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.testTag("loan_record_item"),
        overlineContent = date?.let { { Text(it) } },
        supportingContent = if (supporting != null || chips != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (supporting != null) Text(text = supporting, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (chips != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) { chips() }
                    }
                }
            }
        } else {
            null
        },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = headline, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Column(horizontalAlignment = Alignment.End) {
                TypeAmount(type = type, amount = amount, currency = currency, iconDescription = typeDescription)
                if (converted != null) {
                    AmountText(
                        amount = converted,
                        currency = convertedCurrency,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                    )
                }
            }
        }
    }
}

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 27, 0, 35)

private val previewAccount = Account(name = "Cash", color = Green.toArgb())

@Suppress("LongParameterList")
private fun record(
    amount: Double,
    daysAgo: Long,
    type: LoanRecordType,
    note: String? = null,
    interest: Boolean = false,
    account: Account? = null,
    converted: Double? = null,
    currency: String = "BGN",
): DisplayLoanRecord = DisplayLoanRecord(
    loanRecord = LoanRecord(
        amount = amount,
        dateTime = testDateTime.minusDays(daysAgo).toInstant(ZoneOffset.UTC),
        note = note,
        interest = interest,
        convertedAmount = converted,
        loanId = UUID.randomUUID(),
        loanRecordType = type
    ),
    account = account,
    loanRecordCurrencyCode = currency,
    loanCurrencyCode = "BGN",
)

@Suppress("MagicNumber")
private fun previewRecords(): ImmutableList<DisplayLoanRecord> = persistentListOf(
    record(123.45, 1, LoanRecordType.INCREASE, note = "Cash"),
    record(0.50, 30, LoanRecordType.DECREASE, interest = true, account = previewAccount),
    record(1000.00, 60, LoanRecordType.INCREASE, note = "Revolut", currency = "EUR", converted = 1955.83),
    record(250.00, 400, LoanRecordType.DECREASE),
)

@Suppress("LongParameterList")
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    name: String = "Rent for Alex",
    type: LoanType = LoanType.LEND,
    total: Double = 4023.54,
    paid: Double = 3821.00,
    interest: Double = 100.0,
    records: ImmutableList<DisplayLoanRecord> = previewRecords(),
    account: Account? = previewAccount,
) {
    val state = LoanDetailsScreenState(
        baseCurrency = "BGN",
        loan = Loan(
            name = name,
            amount = total,
            color = Blue.toArgb(),
            type = type,
            note = "Paid back in instalments",
            dateTime = testDateTime,
        ),
        displayLoanRecords = records,
        loanTotalAmount = total,
        amountPaid = paid,
        loanAmountPaid = interest,
        accounts = persistentListOf(),
        selectedLoanAccount = account,
        createLoanTransaction = false,
        isDeleteModalVisible = false,
        loanModalData = null,
        loanRecordModalData = null,
        waitModalVisible = false,
        dateTime = Instant.now()
    )
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(state = state)
        }
    }
}

@Preview
@Composable
private fun Preview_Records() {
    Preview()
}

private fun Boolean.theme(): Theme = if (this) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun LoanDetailScreenUiTest(isDark: Boolean) {
    Preview(theme = isDark.theme())
}

/** For screenshot testing */
@Composable
fun LoanDetailsEmptyUiTest(isDark: Boolean) {
    Preview(
        theme = isDark.theme(),
        type = LoanType.BORROW,
        name = "Car repair",
        total = 3000.0,
        paid = 0.0,
        interest = 0.0,
        records = persistentListOf(),
        account = null,
    )
}

/** For screenshot testing */
@Suppress("MagicNumber")
@Composable
fun LoanDetailsPaidOffUiTest(isDark: Boolean) {
    Preview(
        theme = isDark.theme(),
        type = LoanType.BORROW,
        name = "Holiday tickets",
        total = 450.0,
        paid = 450.0,
        interest = 0.0,
        records = persistentListOf(
            record(300.0, 10, LoanRecordType.DECREASE, note = "First half"),
            record(150.0, 2, LoanRecordType.DECREASE, note = "Second half", account = previewAccount),
        ),
    )
}

/** For screenshot testing */
@Suppress("MagicNumber")
@Composable
fun LoanDetailsHugeAmountUiTest(isDark: Boolean) {
    Preview(
        theme = isDark.theme(),
        name = "Long-term family investment fund for the whole generation",
        total = 123456789012.34,
        paid = 98765432109.87,
        interest = 1234567890.12,
        records = persistentListOf(
            record(
                99999999999.99,
                1,
                LoanRecordType.DECREASE,
                note = "A single very large payment with a really long note that needs to wrap",
                currency = "EUR",
                converted = 195583000000.0
            ),
        ),
    )
}
