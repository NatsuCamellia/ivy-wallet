package com.ivy.planned.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.IntervalType
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.ui.color.Purple
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.color.Green
import com.ivy.ui.color.Orange
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Composable
fun PlannedPaymentsScreen(screen: PlannedPaymentsScreen) {
    val viewModel: PlannedPaymentsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UI(
    state: PlannedPaymentsScreenState,
    modifier: Modifier = Modifier,
    onEvent: (PlannedPaymentsScreenEvent) -> Unit = {}
) {
    val nav = navigation()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.planned_payments_inline)) },
                navigationIcon = { BackButton(onClick = { nav.back() }) }
            )
        },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_planned_payment)
            ExtendedFloatingActionButton(
                onClick = {
                    nav.navigateTo(
                        EditPlannedScreen(
                            type = TransactionType.EXPENSE,
                            plannedPaymentRuleId = null
                        )
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(addLabel) },
                modifier = Modifier
                    .testTag("planned_payments_add")
                    .semantics { contentDescription = addLabel }
            )
        }
    ) { innerPadding ->
        PlannedPaymentsLazyColumn(
            currency = state.currency,
            categories = state.categories,
            accounts = state.accounts,
            oneTime = state.oneTimePlannedPayment,
            oneTimeIncome = state.oneTimeIncome,
            oneTimeExpenses = state.oneTimeExpenses,
            recurring = state.recurringPlannedPayment,
            recurringIncome = state.recurringIncome,
            recurringExpenses = state.recurringExpenses,
            oneTimeExpanded = state.isOneTimePaymentsExpanded,
            recurringExpanded = state.isRecurringPaymentsExpanded,
            setOneTimeExpanded = {
                onEvent(PlannedPaymentsScreenEvent.OnOneTimePaymentsExpanded(it))
            },
            setRecurringExpanded = {
                onEvent(PlannedPaymentsScreenEvent.OnRecurringPaymentsExpanded(it))
            },
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                // clears the add button that the Scaffold draws over the list
                bottom = innerPadding.calculateBottomPadding() + LocalMinimumInteractiveComponentSize.current * 2
            ),
            listState = rememberScrollPositionListState(key = "plannedPayments")
        )
    }
}

private val previewStart = LocalDateTime.of(2023, 4, 27, 12, 0).toInstant(ZoneOffset.UTC)
private val previewAccount = Account(name = "Cash", Green.toArgb(), id = UUID(0, 1))

@Suppress("MagicNumber")
private val previewFood = Category(
    name = NotBlankTrimmedString.unsafe("Food"),
    color = ColorInt(Purple.toArgb()),
    icon = null,
    id = CategoryId(UUID(0, 2)),
    orderNum = 0.0,
)

@Suppress("MagicNumber")
private val previewShisha = Category(
    name = NotBlankTrimmedString.unsafe("Shisha"),
    color = ColorInt(Orange.toArgb()),
    icon = null,
    id = CategoryId(UUID(0, 3)),
    orderNum = 0.0,
)

private fun previewRule(
    title: String,
    category: Category,
    amount: Double,
    oneTime: Boolean,
    intervalType: IntervalType? = null,
    type: TransactionType = TransactionType.EXPENSE,
    id: Long = 0,
): PlannedPaymentRule = PlannedPaymentRule(
    accountId = previewAccount.id,
    title = title,
    categoryId = category.id.value,
    amount = amount,
    startDate = previewStart,
    oneTime = oneTime,
    intervalType = intervalType,
    intervalN = intervalType?.let { 1 },
    type = type,
    id = UUID(1, id),
)

private val previewOneTime = previewRule(
    title = "Lidl pazar",
    category = previewFood,
    amount = 250.75,
    oneTime = true,
    id = 1
)
private val previewRecurring = previewRule(
    title = "Tabu",
    category = previewShisha,
    amount = 1025.5,
    oneTime = false,
    intervalType = IntervalType.MONTH,
    id = 2
)
private val previewIncome = previewRule(
    title = "Salary",
    category = previewFood,
    amount = 3200.0,
    oneTime = false,
    intervalType = IntervalType.MONTH,
    type = TransactionType.INCOME,
    id = 3
)
private val previewInvalid = previewRule(
    title = "Gym",
    category = previewShisha,
    amount = 40.0,
    oneTime = false,
    id = 4
)

private fun previewState(
    oneTime: List<PlannedPaymentRule> = emptyList(),
    recurring: List<PlannedPaymentRule> = emptyList(),
): PlannedPaymentsScreenState = PlannedPaymentsScreenState(
    currency = "BGN",
    accounts = persistentListOf(previewAccount),
    categories = persistentListOf(previewFood, previewShisha),
    oneTimePlannedPayment = oneTime.toImmutableList(),
    oneTimeExpenses = oneTime.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
    oneTimeIncome = oneTime.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
    recurringPlannedPayment = recurring.toImmutableList(),
    recurringExpenses = recurring.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
    recurringIncome = recurring.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
    isOneTimePaymentsExpanded = true,
    isRecurringPaymentsExpanded = true
)

@Composable
private fun PreviewUI(state: PlannedPaymentsScreenState, theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(state)
        }
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    PreviewUI(previewState(oneTime = listOf(previewOneTime), recurring = listOf(previewRecurring)), theme)
}

@Preview
@Composable
private fun PreviewEmpty(theme: Theme = Theme.LIGHT) {
    PreviewUI(previewState(), theme)
}

@Preview
@Composable
private fun PreviewInvalidRule(theme: Theme = Theme.LIGHT) {
    PreviewUI(
        state = previewState(
            oneTime = listOf(previewOneTime),
            recurring = listOf(previewRecurring, previewIncome, previewInvalid)
        ),
        theme = theme
    )
}

private fun Boolean.toTheme(): Theme = if (this) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun PlannedPaymentsScreenUiTest(isDark: Boolean) {
    Preview(isDark.toTheme())
}

/** For screenshot testing */
@Composable
fun PlannedPaymentsEmptyUiTest(isDark: Boolean) {
    PreviewEmpty(isDark.toTheme())
}

/** For screenshot testing */
@Composable
fun PlannedPaymentsInvalidRuleUiTest(isDark: Boolean) {
    PreviewInvalidRule(isDark.toTheme())
}
