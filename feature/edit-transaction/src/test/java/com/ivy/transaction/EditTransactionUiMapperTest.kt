package com.ivy.transaction

import com.ivy.base.model.TransactionType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class EditTransactionUiMapperTest {

    @Test
    fun `new transaction commits with Add`() {
        commitAction(
            isNewTransaction = true,
            hasDueDate = false,
            hasChanges = false,
            type = TransactionType.EXPENSE,
        ) shouldBe CommitAction.Add
    }

    @Test
    fun `editing a normal transaction commits with Save`() {
        commitAction(
            isNewTransaction = false,
            hasDueDate = false,
            hasChanges = false,
            type = TransactionType.EXPENSE,
        ) shouldBe CommitAction.Save
    }

    @Test
    fun `editing a planned payment with changes commits with Save`() {
        commitAction(
            isNewTransaction = false,
            hasDueDate = true,
            hasChanges = true,
            type = TransactionType.EXPENSE,
        ) shouldBe CommitAction.Save
    }

    @Test
    fun `planned expense with no changes commits with Pay`() {
        commitAction(
            isNewTransaction = false,
            hasDueDate = true,
            hasChanges = false,
            type = TransactionType.EXPENSE,
        ) shouldBe CommitAction.Pay
    }

    @Test
    fun `planned income with no changes commits with Get`() {
        commitAction(
            isNewTransaction = false,
            hasDueDate = true,
            hasChanges = false,
            type = TransactionType.INCOME,
        ) shouldBe CommitAction.Get
    }

    @Test
    fun `a new transaction offers only the planned shortcut`() {
        overflowItems(
            isNewTransaction = true,
            isLoanRecord = false,
            type = TransactionType.EXPENSE,
            hasDateTime = false,
            hasDueDate = false,
        ) shouldContainExactly listOf(OverflowItem.MakePlanned)
    }

    @Test
    fun `an existing transaction offers duplicate and delete`() {
        overflowItems(
            isNewTransaction = false,
            isLoanRecord = false,
            type = TransactionType.EXPENSE,
            hasDateTime = true,
            hasDueDate = false,
        ) shouldContainExactly listOf(OverflowItem.Duplicate, OverflowItem.Delete)
    }

    @Test
    fun `transfers never offer the planned shortcut`() {
        overflowItems(
            isNewTransaction = true,
            isLoanRecord = false,
            type = TransactionType.TRANSFER,
            hasDateTime = false,
            hasDueDate = false,
        ) shouldContainExactly emptyList()
    }

    @Test
    fun `a dated transaction never offers the planned shortcut`() {
        overflowItems(
            isNewTransaction = true,
            isLoanRecord = false,
            type = TransactionType.EXPENSE,
            hasDateTime = true,
            hasDueDate = false,
        ) shouldContainExactly emptyList()
    }

    @Test
    fun `loan records cannot be deleted from here`() {
        overflowItems(
            isNewTransaction = false,
            isLoanRecord = true,
            type = TransactionType.EXPENSE,
            hasDateTime = true,
            hasDueDate = false,
        ) shouldContainExactly listOf(OverflowItem.Duplicate)
    }
}
