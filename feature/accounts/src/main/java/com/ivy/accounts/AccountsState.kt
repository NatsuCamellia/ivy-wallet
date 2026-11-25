package com.ivy.accounts

import androidx.compose.runtime.Immutable
import com.ivy.legacy.data.model.AccountData
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class AccountsState(
    val baseCurrency: String,
    /** The accounts in their saved order, for the reorder sheet. */
    val accountsData: ImmutableList<AccountData>,
    val content: AccountsContent,
    val reorderVisible: Boolean,
    val hideTotalBalance: Boolean,
)
