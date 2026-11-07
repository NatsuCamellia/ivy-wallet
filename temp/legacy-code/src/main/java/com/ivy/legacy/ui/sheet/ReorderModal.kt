package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.numberBetween
import com.ivy.legacy.utils.swap
import com.ivy.ui.R
import com.ivy.wallet.domain.data.Reorderable
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Suppress("UNCHECKED_CAST", "ParameterNaming")
@Composable
fun <T : Reorderable> ReorderModalSingleType(
    visible: Boolean,
    id: UUID = remember { UUID.randomUUID() },
    initialItems: List<T>,
    dismiss: () -> Unit,
    onUpdateItemOrderNum: (item: T, newOrderNum: Double) -> Unit = { _, _ -> },
    onReordered: ((List<T>) -> Unit)? = null,
    itemContent: @Composable (Int, T) -> Unit
) {
    ReorderModal<T>(
        visible = visible,
        id = id,
        initialItems = initialItems,
        dismiss = dismiss,
        onUpdateItemOrderNum = { _, item, newOrderNum ->
            onUpdateItemOrderNum(item, newOrderNum)
        },
        onReordered = { listAny ->
            onReordered?.invoke(
                listAny as? List<T> ?: error("List<T> cast exception.")
            )
        },
        itemContent = { index, itemAny ->
            itemContent(index, itemAny as T)
        }
    )
}

@SuppressLint("ComposeModifierMissing")
@Suppress("ParameterNaming")
@Composable
fun <T : Reorderable> ReorderModal(
    visible: Boolean,
    id: UUID = remember { UUID.randomUUID() },
    initialItems: List<Any>,
    dismiss: () -> Unit,
    onUpdateItemOrderNum: (
        itemsInNewOrder: List<Any>,
        item: T,
        newOrderNum: Double
    ) -> Unit = { _, _, _ -> },
    onReordered: ((List<Any>) -> Unit)? = null,
    itemContent: @Composable (Int, Any) -> Unit
) {
    if (visible) {
        key(id) {
            var items by remember(initialItems) { mutableStateOf(initialItems) }
            var reOrderedList: List<Any>? by remember { mutableStateOf(null) }
            var orderNumUpdates by remember { mutableStateOf(mapOf<T, Double>()) }

            ActionSheet(
                onDismiss = dismiss,
                title = stringResource(R.string.reorder),
                sheetGesturesEnabled = false,
                contentScrollable = false,
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            orderNumUpdates.forEach { (item, newOrderNum) ->
                                onUpdateItemOrderNum(items, item, newOrderNum)
                            }

                            onReordered?.invoke(reOrderedList ?: items)
                            dismissSheet()
                        },
                        modifier = Modifier.testTag("reorder_done"),
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            ) {
                val dragColor = MaterialTheme.colorScheme.surfaceContainerHighest
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = {
                        RecyclerView(it).apply {
                            val itemTouchHelper = itemTouchHelper<T>(
                                dragColor = dragColor
                            )
                            adapter = Adapter<T>(
                                itemTouchHelper = itemTouchHelper,
                                itemContent = itemContent,
                                addItemOrderNumUpdate = { item, newOrderNum ->
                                    orderNumUpdates = orderNumUpdates
                                        .toMutableMap()
                                        .apply {
                                            this[item] = newOrderNum
                                        }
                                },
                                onReorderInternalList = { reorderedItems ->
                                    items = reorderedItems
                                    reOrderedList = reorderedItems
                                }
                            )
                            layoutManager = LinearLayoutManager(it)
                            itemTouchHelper.attachToRecyclerView(this)

                            adapter<T>().display(items)
                        }
                    },
                )
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private class Adapter<T : Reorderable>(
    private val itemTouchHelper: ItemTouchHelper,
    private val itemContent: @Composable (Int, Any) -> Unit,
    private val addItemOrderNumUpdate: (item: T, orderNum: Double) -> Unit,
    private val onReorderInternalList: (List<Any>) -> Unit
) : RecyclerView.Adapter<Adapter<T>.ItemViewHolder>() {
    val data = mutableListOf<Any>()

    @SuppressLint("NotifyDataSetChanged")
    fun display(items: List<Any>) {
        data.clear()
        data.addAll(items)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        data.swap(from, to)
        notifyItemMoved(from, to)
    }

    fun onItemMoved(item: T, to: Int) {
        val newOrderNum = calculateOrderNum<T>(
            itemsInNewOrder = data,
            to = to
        )

        data[to] = item.withNewOrderNum(newOrderNum) as? T
            ?: error("Incorrect Reorderable implementation for $item")
        addItemOrderNumUpdate(item, newOrderNum)
    }

    fun onReorderInternalList() {
        onReorderInternalList(data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.display(
            item = data[position],
            itemContent = itemContent,
        )
    }

    override fun getItemCount() = data.size

    inner class ItemViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {

        fun display(
            item: Any,
            itemContent: @Composable (Int, Any) -> Unit,
        ) {
            (itemView as ComposeView).setContent {
                ListItem(
                    leadingContent = if (item as? T != null) {
                        {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = stringResource(R.string.reorder),
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                itemTouchHelper.startDrag(this@ItemViewHolder)
                                            }
                                        )
                                    }
                                    .testTag("reorder_drag_handle"),
                            )
                        }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) {
                    itemContent(adapterPosition, item)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> itemTouchHelper(
    dragColor: Color,
): ItemTouchHelper {
    // 1. Note that I am specifying all 4 directions.
    //    Specifying START and END also allows
    //    more organic dragging than just specifying UP and DOWN.
    val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
        var movedItem: T? = null
        var finalTo: Int? = null

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val adapter = recyclerView.adapter<T>()

            val from = viewHolder.adapterPosition
            val to = target.adapterPosition

            val targetItem = adapter.data[from] as? T ?: return false

            if (movedItem == null) {
                movedItem = targetItem
            }
            finalTo = to

            adapter.moveItem(from, to)

            return true
        }

        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            // 4. Code block for horizontal swipe.
            //    ItemTouchHelper handles horizontal swipe as well, but
            //    it is not relevant with reordering. Ignoring here.
        }

        // 1. This callback is called when a ViewHolder is selected.
        //    We highlight the ViewHolder here.
        override fun onSelectedChanged(
            viewHolder: RecyclerView.ViewHolder?,
            actionState: Int
        ) {
            super.onSelectedChanged(viewHolder, actionState)

            if (actionState == ACTION_STATE_DRAG) {
                viewHolder?.itemView?.setBackgroundColor(dragColor.toArgb())
            }
        }

        override fun clearView(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.background = null
            val adapter = recyclerView.adapter<T>()
            if (movedItem != null && finalTo != null) {
                adapter.onItemMoved(movedItem!!, finalTo!!)
            }
            adapter.onReorderInternalList()

            movedItem = null
            finalTo = null
        }
    }
    return ItemTouchHelper(simpleItemTouchCallback)
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> RecyclerView.adapter() = adapter as? Adapter<T>
    ?: error("Adapter not set or wrong adapter set to recyclerview.")

@Composable
fun ReorderButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.testTag("reorder_button"),
    ) {
        Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.reorder))
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> calculateOrderNum(
    itemsInNewOrder: List<*>,
    to: Int
): Double {
    val itemBefore = itemsInNewOrder.getOrNull(to - 1) as? T
    val itemAfter = itemsInNewOrder.getOrNull(to + 1) as? T

    return when {
        itemBefore != null && itemAfter != null -> {
            numberBetween(
                itemBefore.getItemOrderNum(),
                itemAfter.getItemOrderNum()
            )
        }

        itemBefore != null && itemAfter == null -> {
            // It's last in it's priority
            itemBefore.getItemOrderNum() + 1
        }

        itemBefore == null && itemAfter != null -> {
            // It's first in it's priority
            itemAfter.getItemOrderNum() - 1
        }

        else -> 0.0
    }
}

private data class ReorderItem(val name: String, val orderNum: Double) : Reorderable {
    override fun getItemOrderNum(): Double = orderNum

    override fun withNewOrderNum(newOrderNum: Double): Reorderable = copy(orderNum = newOrderNum)
}

/** For screenshot testing */
@Suppress("ModifierMissing")
@Composable
fun ReorderModalUiTest(isDark: Boolean) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        ReorderModalSingleType(
            visible = true,
            initialItems = listOf("Cash", "Bank", "Savings").mapIndexed { index, name ->
                ReorderItem(name, index.toDouble())
            },
            dismiss = {}
        ) { _, item ->
            Text(item.name)
        }
    }
}
