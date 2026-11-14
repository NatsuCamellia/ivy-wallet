package com.ivy.loans.loan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.data.model.LoanType
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.humanReadableType
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.AmountText
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.sheet.LoanModal
import com.ivy.legacy.ui.sheet.ReorderModalSingleType
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.getDefaultFIATCurrency
import com.ivy.loans.loan.data.DisplayLoan
import com.ivy.navigation.LoanDetailsScreen
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.color.Blue
import com.ivy.ui.color.Green
import com.ivy.ui.color.Orange
import com.ivy.ui.component.BackButton
import com.ivy.ui.rememberScrollPositionListState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDateTime

@Composable
fun LoansScreen(screen: LoansScreen) {
    val viewModel: LoanViewModel = viewModel()
    val state = viewModel.uiState()
    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@Composable
private fun UI(
    state: LoanScreenState,
    modifier: Modifier = Modifier,
    onEventHandler: (LoanScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    val loans = if (state.selectedTab == LoanTab.PENDING) state.pendingLoans else state.completedLoans

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loans)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) },
                actions = {
                    IconButton(
                        onClick = { onEventHandler(LoanScreenEvent.OnReOrderModalShow(show = true)) },
                        modifier = Modifier.testTag("reorder_button")
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.reorder))
                    }
                }
            )
        },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_loan)
            ExtendedFloatingActionButton(
                onClick = { onEventHandler(LoanScreenEvent.OnAddLoan) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(addLabel) },
                modifier = Modifier
                    .testTag("loans_add")
                    .semantics { contentDescription = addLabel }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabRow(
                tab = state.selectedTab,
                onTabChange = { onEventHandler(LoanScreenEvent.OnTabChanged(it)) },
            )

            val scrollState = rememberScrollPositionListState(
                key = "loans_lazy_column",
                initialFirstVisibleItemIndex = ivyWalletCtx()
                    .loanListState?.firstVisibleItemIndex ?: 0,
                initialFirstVisibleItemScrollOffset = ivyWalletCtx()
                    .loanListState?.firstVisibleItemScrollOffset ?: 0
            )

            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    // clears the add button that the Scaffold draws over the list
                    bottom = innerPadding.calculateBottomPadding() + LocalMinimumInteractiveComponentSize.current * 2
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.totalOweAmount.isNotEmpty() || state.totalOwedAmount.isNotEmpty()) {
                    item(key = "totals") {
                        Totals(
                            totalOweAmount = state.totalOweAmount,
                            totalOwedAmount = state.totalOwedAmount
                        )
                    }
                }

                if (loans.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            emptyStateTitle = stringResource(R.string.no_loans),
                            emptyStateText = stringResource(R.string.no_loans_description),
                            icon = Icons.Outlined.Handshake,
                        )
                    }
                }

                items(loans, key = { it.loan.id.toString() }) { item ->
                    LoanItem(
                        displayLoan = item,
                        onClick = {
                            nav.navigateTo(screen = LoanDetailsScreen(loanId = item.loan.id))
                        }
                    )
                }
            }
        }
    }

    ReorderModalSingleType(
        visible = state.reorderModalVisible,
        initialItems = loans,
        dismiss = {
            onEventHandler(LoanScreenEvent.OnReOrderModalShow(show = false))
        },
        onReordered = {
            onEventHandler(LoanScreenEvent.OnReordered(reorderedList = it))
        }
    ) { _, item ->
        Text(text = item.loan.name, style = MaterialTheme.typography.bodyLarge)
    }

    if (state.loanModalData != null) {
        LoanModal(
            accounts = state.accounts,
            onCreateAccount = {
                onEventHandler(LoanScreenEvent.OnCreateAccount(accountData = it))
            },
            modal = state.loanModalData,
            onCreateLoan = {
                onEventHandler(LoanScreenEvent.OnLoanCreate(createLoanData = it))
            },
            onEditLoan = { _, _ -> },
            dismiss = {
                onEventHandler(LoanScreenEvent.OnLoanModalDismiss)
            },
            dateTime = state.dateTime,
            onSetDate = {
                onEventHandler(LoanScreenEvent.OnChangeDate)
            },
            onSetTime = {
                onEventHandler(LoanScreenEvent.OnChangeTime)
            }
        )
    }
}

@Composable
private fun TabRow(
    tab: LoanTab,
    onTabChange: (LoanTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        LoanTab.entries.forEachIndexed { index, entry ->
            val pending = entry == LoanTab.PENDING
            SegmentedButton(
                selected = tab == entry,
                onClick = { onTabChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = LoanTab.entries.size),
                modifier = Modifier.testTag(if (pending) "pending" else "completed"),
            ) {
                Text(stringResource(if (pending) R.string.loans_pending else R.string.loans_completed))
            }
        }
    }
}

@Composable
private fun Totals(
    totalOweAmount: String,
    totalOwedAmount: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (totalOweAmount.isNotEmpty()) {
            Text(
                text = stringResource(R.string.you_owe, totalOweAmount),
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (totalOwedAmount.isNotEmpty()) {
            Text(
                text = stringResource(R.string.you_are_owed, totalOwedAmount),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Suppress("MagicNumber")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoanItem(
    displayLoan: DisplayLoan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loan = displayLoan.loan
    val currency = displayLoan.currencyCode ?: getDefaultFIATCurrency().currencyCode
    val total = displayLoan.loanTotalAmount
    val paidOff = displayLoan.percentPaid >= 1.0
    val statusColor = if (paidOff) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface

    Card(onClick = onClick, modifier = modifier.fillMaxWidth().testTag("loan_item")) {
        ListItem(
            leadingContent = {
                IconBadge(
                    color = Color(loan.color),
                    iconName = loan.icon,
                    defaultIcon = R.drawable.ic_custom_loan_m,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            supportingContent = { Text(loan.humanReadableType()) },
        ) {
            Text(loan.name)
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(if (paidOff) R.string.loan_paid_off else R.string.loan_left_to_pay),
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
            AmountText(
                amount = if (paidOff) total else total - displayLoan.amountPaid,
                currency = currency,
                style = MaterialTheme.typography.headlineSmallEmphasized.copy(color = statusColor),
            )
            LinearWavyProgressIndicator(
                progress = { displayLoan.percentPaid.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (paidOff) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AmountText(
                    amount = displayLoan.amountPaid,
                    currency = currency,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(text = "/", style = MaterialTheme.typography.bodyMedium)
                AmountText(
                    amount = total,
                    currency = currency,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.loan_percent_paid, (displayLoan.percentPaid * 100).format(2)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 20, 0, 35)

@Suppress("LongParameterList")
private fun displayLoan(
    name: String,
    type: LoanType,
    total: Double,
    paid: Double,
    color: Color,
    icon: String? = null,
    order: Double = 0.0,
): DisplayLoan = DisplayLoan(
    loan = Loan(
        name = name,
        icon = icon,
        color = color.toArgb(),
        amount = total,
        type = type,
        dateTime = testDateTime,
        orderNum = order,
    ),
    loanTotalAmount = total,
    amountPaid = paid,
    currencyCode = "BGN",
    percentPaid = if (total != 0.0) paid / total else 0.0
)

@Suppress("MagicNumber")
private fun previewLoans(): ImmutableList<DisplayLoan> = persistentListOf(
    displayLoan("Car repair", LoanType.BORROW, total = 3000.0, paid = 1200.0, color = Orange, icon = "bank"),
    displayLoan("Rent for Alex", LoanType.LEND, total = 7000.0, paid = 5600.0, color = Blue, order = 1.0),
)

@Suppress("MagicNumber")
private fun paidOffLoans(): ImmutableList<DisplayLoan> = persistentListOf(
    displayLoan("Old phone", LoanType.LEND, total = 700.0, paid = 700.0, color = Green),
    displayLoan("Holiday tickets", LoanType.BORROW, total = 450.0, paid = 450.0, color = Blue, order = 1.0),
)

@Suppress("MagicNumber")
private fun hugeAmountLoans(): ImmutableList<DisplayLoan> = persistentListOf(
    displayLoan(
        name = "Long-term family investment fund for the whole generation",
        type = LoanType.BORROW,
        total = 123456789012.34,
        paid = 98765432109.87,
        color = Orange,
    ),
)

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    tab: LoanTab = LoanTab.PENDING,
    pending: ImmutableList<DisplayLoan> = previewLoans(),
    completed: ImmutableList<DisplayLoan> = paidOffLoans(),
    totals: Boolean = true,
) {
    val state = LoanScreenState(
        baseCurrency = "BGN",
        selectedTab = tab,
        completedLoans = completed,
        pendingLoans = pending,
        accounts = persistentListOf(),
        totalOweAmount = if (totals) "1800.00 BGN" else "",
        totalOwedAmount = if (totals) "1400.00 BGN" else "",
        loanModalData = null,
        reorderModalVisible = false,
        selectedAccount = null,
        paidOffLoanVisibility = true,
        dateTime = Instant.now()
    )
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(state = state)
        }
    }
}

private fun Boolean.theme(): Theme = if (this) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun LoanScreenUiTest(isDark: Boolean) {
    Preview(theme = isDark.theme())
}

/** For screenshot testing */
@Composable
fun LoansCompletedUiTest(isDark: Boolean) {
    Preview(theme = isDark.theme(), tab = LoanTab.COMPLETED, totals = false)
}

/** For screenshot testing */
@Composable
fun LoansEmptyUiTest(isDark: Boolean) {
    Preview(theme = isDark.theme(), pending = persistentListOf(), totals = false)
}

/** For screenshot testing */
@Composable
fun LoansHugeAmountUiTest(isDark: Boolean) {
    Preview(theme = isDark.theme(), pending = hugeAmountLoans(), totals = false)
}
