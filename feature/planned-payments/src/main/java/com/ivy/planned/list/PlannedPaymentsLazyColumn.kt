package com.ivy.planned.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ivy.data.model.Category
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.ui.component.transaction.EmptyState
import com.ivy.legacy.ui.component.transaction.SectionDivider
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.navigation
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.absoluteValue

@Suppress("LongParameterList")
@Composable
fun PlannedPaymentsLazyColumn(
    currency: String,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    oneTime: ImmutableList<PlannedPaymentRule>,
    oneTimeIncome: Double,
    oneTimeExpenses: Double,
    recurring: ImmutableList<PlannedPaymentRule>,
    recurringIncome: Double,
    recurringExpenses: Double,
    oneTimeExpanded: Boolean,
    recurringExpanded: Boolean,
    setOneTimeExpanded: (Boolean) -> Unit,
    setRecurringExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    listState: LazyListState = rememberLazyListState(),
) {
    val nav = navigation()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        plannedPaymentItems(
            nav = nav,
            currency = currency,
            categories = categories,
            accounts = accounts,

            oneTime = oneTime,
            oneTimeIncome = oneTimeIncome,
            oneTimeExpenses = oneTimeExpenses,
            oneTimeExpanded = oneTimeExpanded,
            setOneTimeExpanded = setOneTimeExpanded,

            recurring = recurring,
            recurringIncome = recurringIncome,
            recurringExpenses = recurringExpenses,
            recurringExpanded = recurringExpanded,
            setRecurringExpanded = setRecurringExpanded
        )
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.plannedPaymentItems(
    nav: Navigation,
    currency: String,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,

    oneTime: ImmutableList<PlannedPaymentRule>,
    oneTimeIncome: Double,
    oneTimeExpenses: Double,
    oneTimeExpanded: Boolean,
    setOneTimeExpanded: (Boolean) -> Unit,

    recurring: ImmutableList<PlannedPaymentRule>,
    recurringIncome: Double,
    recurringExpenses: Double,
    recurringExpanded: Boolean,
    setRecurringExpanded: (Boolean) -> Unit,
) {
    if (oneTime.isNotEmpty()) {
        item(key = "one_time_section") {
            SectionDivider(
                expanded = oneTimeExpanded,
                setExpanded = setOneTimeExpanded,
                title = stringResource(R.string.one_time_payments),
                titleColor = MaterialTheme.colorScheme.onSurface,
                baseCurrency = currency,
                income = oneTimeIncome,
                expenses = oneTimeExpenses.absoluteValue
            )
        }

        if (oneTimeExpanded) {
            plannedPaymentRows(nav, currency, categories, accounts, oneTime)
        }
    }

    if (recurring.isNotEmpty()) {
        item(key = "recurring_section") {
            SectionDivider(
                expanded = recurringExpanded,
                setExpanded = setRecurringExpanded,
                title = stringResource(R.string.recurring_payments),
                titleColor = MaterialTheme.colorScheme.onSurface,
                baseCurrency = currency,
                income = recurringIncome,
                expenses = recurringExpenses.absoluteValue
            )
        }

        if (recurringExpanded) {
            plannedPaymentRows(nav, currency, categories, accounts, recurring)
        }
    }

    if (oneTime.isEmpty() && recurring.isEmpty()) {
        item(key = "empty") {
            EmptyState(
                emptyStateTitle = stringResource(R.string.no_planned_payments),
                emptyStateText = stringResource(R.string.no_planned_payments_description),
                icon = Icons.Default.EventRepeat
            )
        }
    }
}

private fun LazyListScope.plannedPaymentRows(
    nav: Navigation,
    currency: String,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    rules: ImmutableList<PlannedPaymentRule>,
) {
    itemsIndexed(rules, key = { _, rule -> rule.id }) { index, rule ->
        PlannedPaymentCard(
            baseCurrency = currency,
            categories = categories,
            accounts = accounts,
            plannedPayment = rule,
            onClick = {
                nav.navigateTo(EditPlannedScreen(plannedPaymentRuleId = it.id, type = it.type))
            },
            index = index,
            count = rules.size,
        )
    }
}
