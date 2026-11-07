package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AccountsRow(
    accounts: ImmutableList<Account>,
    selectedAccount: Account?,
    onSelectedAccountChange: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
    modifier: Modifier = Modifier,
    childrenTestTag: String = "account",
) {
    LazyRow(
        modifier = modifier,
        state = rememberLazyListState(
            initialFirstVisibleItemIndex = accounts.indexOf(selectedAccount).coerceAtLeast(0)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(accounts, key = { it.id }) { account ->
            FilterChip(
                selected = account == selectedAccount,
                onClick = { onSelectedAccountChange(account) },
                label = { Text(account.name) },
                modifier = Modifier.testTag(childrenTestTag),
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            customIconResource(account.icon, R.drawable.ic_custom_account_s, "s")
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }

        item(key = "add_account") {
            AssistChip(
                onClick = onAddNewAccount,
                label = { Text(stringResource(R.string.add_account)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}
