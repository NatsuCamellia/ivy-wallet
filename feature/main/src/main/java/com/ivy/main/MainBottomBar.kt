package com.ivy.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.legacy.data.model.MainTab
import com.ivy.ui.R

private const val FabIconSwitchProgressThreshold = 0.5f

// Raises the toolbar+FAB corner control above the NavigationBar so the two floating elements
// don't visually overlap (NavigationBar's own height + a small gap).
private val BottomBarClearance = 88.dp

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

    // Anchors this composable's bounds to the full available size so that the toolbar corner
    // control below (which isn't full-width) aligns against the real screen edges rather than
    // shrink-wrapping around just itself.
    Spacer(modifier = Modifier.fillMaxSize())

    NavigationBar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
    ) {
        NavigationBarItem(
            selected = tab == MainTab.HOME,
            onClick = { selectTab(MainTab.HOME) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = stringResource(R.string.home),
                )
            },
            label = { Text(text = stringResource(R.string.home)) },
            modifier = Modifier.testTag("home"),
        )
        NavigationBarItem(
            selected = tab == MainTab.ACCOUNTS,
            onClick = { selectTab(MainTab.ACCOUNTS) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_accounts),
                    contentDescription = stringResource(R.string.accounts),
                )
            },
            label = { Text(text = stringResource(R.string.accounts)) },
            modifier = Modifier.testTag("accounts"),
        )
    }

    HorizontalFloatingToolbar(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(bottom = BottomBarClearance, end = 16.dp),
        expanded = fabMenuExpanded && tab == MainTab.HOME,
        floatingActionButton = {
            ToggleFloatingActionButton(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("fab_add"),
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
        IconButton(
            onClick = {
                fabMenuExpanded = false
                onAddIncome()
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_income),
                contentDescription = stringResource(R.string.income),
            )
        }
        IconButton(
            onClick = {
                fabMenuExpanded = false
                onAddExpense()
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_expense),
                contentDescription = stringResource(R.string.expenses),
            )
        }
        IconButton(
            onClick = {
                fabMenuExpanded = false
                onAddTransfer()
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_transfer),
                contentDescription = stringResource(R.string.transfer),
            )
        }
        IconButton(
            onClick = {
                fabMenuExpanded = false
                onAddPlannedPayment()
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_planned_payments),
                contentDescription = stringResource(R.string.add_planned_payment),
            )
        }
    }
}
