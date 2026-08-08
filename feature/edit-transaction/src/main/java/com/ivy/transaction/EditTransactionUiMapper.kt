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

fun overflowItems(
    isNewTransaction: Boolean,
    isLoanRecord: Boolean,
    type: TransactionType,
    hasDateTime: Boolean,
    hasDueDate: Boolean,
): List<OverflowItem> = buildList {
    if (!isNewTransaction) {
        add(OverflowItem.Duplicate)
        if (!isLoanRecord) add(OverflowItem.Delete)
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
