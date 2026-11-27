package com.ivy.accounts

sealed interface AccountsEvent {
    data class OnReorder(val reorderedList: List<com.ivy.legacy.data.model.AccountData>) :
        AccountsEvent
    data class OnReorderModalVisible(val reorderVisible: Boolean) : AccountsEvent

    /** A tap on the hidden Balance banner: reveal the amounts for a few seconds. */
    data object HiddenBalanceClick : AccountsEvent
}
