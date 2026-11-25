package com.ivy.home.transactionlist

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import org.junit.Test

class HomeDueMapperTest {

    @Test
    fun `Overdue rows come before Upcoming rows and keep their order`() {
        val result = mapHomeDue(
            overdue = trns("o1", "o2"),
            upcoming = trns("u1"),
            expanded = false,
        )

        result.rows.map { it.transaction.title } shouldBe listOf("o1", "o2", "u1")
        result.rows.map { it.overdue } shouldBe listOf(true, true, false)
    }

    @Test
    fun `exactly 3 items show all of them without Show all`() {
        val result = mapHomeDue(overdue = trns("o1"), upcoming = trns("u1", "u2"), expanded = false)

        result.rows.size shouldBe 3
        result.showAllCount shouldBe null
    }

    @Test
    fun `collapsed with more than 3 items shows the first 3 and Show all with the total`() {
        val result = mapHomeDue(overdue = trns("o1", "o2"), upcoming = trns("u1", "u2", "u3"), expanded = false)

        result.rows.map { it.transaction.title } shouldBe listOf("o1", "o2", "u1")
        result.showAllCount shouldBe 5
    }

    @Test
    fun `expanded with more than 3 items shows every row and still offers the toggle`() {
        val result = mapHomeDue(overdue = trns("o1", "o2"), upcoming = trns("u1", "u2", "u3"), expanded = true)

        result.rows.map { it.transaction.title } shouldBe listOf("o1", "o2", "u1", "u2", "u3")
        result.showAllCount shouldBe 5
    }

    @Test
    fun `expanded with 3 items or fewer has no toggle`() {
        val result = mapHomeDue(overdue = emptyList(), upcoming = trns("u1", "u2"), expanded = true)

        result.rows.size shouldBe 2
        result.showAllCount shouldBe null
    }

    @Test
    fun `Upcoming without Overdue has no Skip all`() {
        val result = mapHomeDue(overdue = emptyList(), upcoming = trns("u1", "u2"), expanded = false)

        result.rows.map { it.overdue } shouldBe listOf(false, false)
        result.showSkipAll shouldBe false
    }

    @Test
    fun `Skip all shows whenever something is Overdue, even when collapsed past it`() {
        val result = mapHomeDue(overdue = trns("o1"), upcoming = trns("u1", "u2", "u3"), expanded = false)

        result.showSkipAll shouldBe true
    }

    @Test
    fun `nothing due yields nothing to show`() {
        val result = mapHomeDue(overdue = emptyList(), upcoming = emptyList(), expanded = true)

        result shouldBe HomeDue(rows = emptyList(), showAllCount = null, showSkipAll = false)
    }

    private fun trns(vararg titles: String) = titles.map {
        Transaction(
            accountId = java.util.UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            amount = BigDecimal("10.0"),
            title = it,
        )
    }
}
