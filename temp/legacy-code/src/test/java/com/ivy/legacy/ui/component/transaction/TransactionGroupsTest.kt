package com.ivy.legacy.ui.component.transaction

import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class TransactionGroupsTest {

    private fun trn(): Transaction =
        Transaction(accountId = UUID.randomUUID(), type = TransactionType.EXPENSE, amount = 1.toBigDecimal())

    private fun divider(day: Int): TransactionHistoryDateDivider =
        TransactionHistoryDateDivider(LocalDate.of(2024, 3, day), 0.0, 0.0)

    @Test
    fun runsBetweenDividersAreGroupedSeparately() {
        val a = trn()
        val b = trn()
        val c = trn()
        val d = trn()
        val history: List<TransactionHistoryItem> = listOf(divider(2), a, b, c, divider(1), d)

        val groups = history.transactionGroups()

        assertEquals(0 to 3, groups[a.id])
        assertEquals(1 to 3, groups[b.id])
        assertEquals(2 to 3, groups[c.id])
        assertEquals(0 to 1, groups[d.id])
        assertEquals(4, groups.size)
    }

    @Test
    fun emptyHistoryHasNoGroups() {
        assertEquals(emptyMap<UUID, Pair<Int, Int>>(), emptyList<TransactionHistoryItem>().transactionGroups())
    }
}
