package com.ivy.categories

import androidx.compose.runtime.Immutable
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.wallet.domain.data.SortOrder
import com.ivy.legacy.ui.sheet.CategoryModalData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** [categories] holds every category in the chosen sort order, for the reorder sheet. */
@Immutable
data class CategoriesScreenState(
    /** The month the amounts are for. */
    val period: TimePeriod,
    val baseCurrency: String = "",
    val categories: ImmutableList<CategoryData> = persistentListOf(),
    val reorderModalVisible: Boolean = false,
    val categoryModalData: CategoryModalData? = null,
    val sortModalVisible: Boolean = false,
    val sortOrderItems: ImmutableList<SortOrder> = SortOrder.values().toList().toImmutableList(),
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val type: CategoryType,
    val content: CategoriesContent,
)
