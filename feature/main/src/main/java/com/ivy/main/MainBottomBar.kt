package com.ivy.main

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ivy.legacy.data.model.MainTab
import com.ivy.ui.R

private const val FabIconSwitchProgressThreshold = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.BottomBar(
    tab: MainTab,
    selectTab: (MainTab) -> Unit,

    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,

    showAddAccountModal: () -> Unit,
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(tab) {
        fabMenuExpanded = false
    }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    // Anchors this composable's bounds to the full available size so that the toolbar and FAB
    // below (neither of which is full-width, unlike the old NavigationBar) align against the
    // real screen edges rather than shrink-wrapping around just themselves.
    Spacer(modifier = Modifier.fillMaxSize())

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        ToolbarTab(
            icon = R.drawable.ic_home,
            label = stringResource(R.string.home),
            testTag = "home",
            selected = tab == MainTab.HOME,
            onClick = { selectTab(MainTab.HOME) },
        )
        ToolbarTab(
            icon = R.drawable.ic_accounts,
            label = stringResource(R.string.accounts),
            testTag = "accounts",
            selected = tab == MainTab.ACCOUNTS,
            onClick = { selectTab(MainTab.ACCOUNTS) },
        )
    }

    FloatingActionButtonMenu(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(16.dp),
        expanded = fabMenuExpanded && tab == MainTab.HOME,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier.testTag("fab_add"),
                checked = fabMenuExpanded,
                onCheckedChange = {
                    if (tab == MainTab.HOME) {
                        fabMenuExpanded = !fabMenuExpanded
                    } else {
                        showAddAccountModal()
                    }
                },
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (tab == MainTab.HOME && checkedProgress > FabIconSwitchProgressThreshold) {
                            Icons.Filled.Close
                        } else {
                            Icons.Filled.Add
                        }
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = stringResource(R.string.add_transaction),
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onAddIncome()
            },
            icon = {
                Icon(painter = painterResource(R.drawable.ic_income), contentDescription = null)
            },
            text = { Text(text = stringResource(R.string.add_income_uppercase)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onAddExpense()
            },
            icon = {
                Icon(painter = painterResource(R.drawable.ic_expense), contentDescription = null)
            },
            text = { Text(text = stringResource(R.string.add_expense_uppercase)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onAddTransfer()
            },
            icon = {
                Icon(painter = painterResource(R.drawable.ic_transfer), contentDescription = null)
            },
            text = { Text(text = stringResource(R.string.account_transfer)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onAddPlannedPayment()
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_planned_payments),
                    contentDescription = null,
                )
            },
            text = { Text(text = stringResource(R.string.add_planned_payment)) },
        )
    }
}

@Composable
private fun RowScope.ToolbarTab(
    @DrawableRes icon: Int,
    label: String,
    testTag: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(text = label, color = MaterialTheme.colorScheme.primary)
        }
    }
}
