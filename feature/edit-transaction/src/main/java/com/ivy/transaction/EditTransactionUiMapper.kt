package com.ivy.transaction

import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.component.picker.PickerItemUi
import com.ivy.wallet.ui.theme.toComposeColor
import java.util.UUID

enum class CommitAction { Add, Save, Pay, Get }

fun commitAction(
    isNewTransaction: Boolean,
    hasDueDate: Boolean,
    hasChanges: Boolean,
    type: TransactionType,
): CommitAction = when {
    isNewTransaction -> CommitAction.Add
    !hasDueDate || hasChanges -> CommitAction.Save
    type == TransactionType.EXPENSE -> CommitAction.Pay
    else -> CommitAction.Get
}

enum class OverflowItem { Duplicate, Delete, MakePlanned }

/**
 * Delete is offered for every saved transaction, loan records included. The legacy `Toolbar`
 * rendered `DeleteButton` under a bare `initialTransactionId != null`; `isLoanRecord` only ever
 * suppressed the *type* button, which it did by passing `type = TRANSFER` in its place. The screen
 * still hides the type selector for loan records — that part is unchanged.
 */
fun overflowItems(
    isNewTransaction: Boolean,
    type: TransactionType,
    hasDateTime: Boolean,
    hasDueDate: Boolean,
): List<OverflowItem> = buildList {
    if (!isNewTransaction) {
        add(OverflowItem.Duplicate)
        add(OverflowItem.Delete)
    }
    val plannedAvailable = type != TransactionType.TRANSFER && !hasDateTime && !hasDueDate
    if (isNewTransaction && plannedAvailable) add(OverflowItem.MakePlanned)
}

fun Account.toPickerItem(selectedId: UUID?): PickerItemUi = PickerItemUi(
    id = id.toString(),
    title = name,
    supportingText = currency,
    color = color.toComposeColor(),
    selected = id == selectedId,
)

fun Category.toPickerItem(selectedId: UUID?): PickerItemUi = PickerItemUi(
    id = id.value.toString(),
    title = name.value,
    supportingText = null,
    color = color.value.toComposeColor(),
    selected = id.value == selectedId,
)
