package com.ivy.legacy.datamodel.temp

import com.ivy.base.legacy.LegacyTag
import com.ivy.base.legacy.LegacyTransaction
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.model.Tag
import com.ivy.data.model.Transaction
import com.ivy.data.model.primitive.AssociationId
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.legacy.datamodel.toEntity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

fun Transaction.toLegacy(mapper: TransactionMapper): LegacyTransaction {
    return with(mapper) { toEntity().toLegacyDomain() }
}

suspend fun LegacyTransaction.toDomain(mapper: TransactionMapper): Transaction? {
   return with(mapper) {
       toEntity().toDomain().getOrNull()
    }
}

fun TransactionEntity.toLegacyDomain(
    tags: ImmutableList<LegacyTag> = persistentListOf()
): LegacyTransaction = LegacyTransaction(
    accountId = accountId,
    type = type,
    amount = amount.toBigDecimal(),
    toAccountId = toAccountId,
    toAmount = toAmount?.toBigDecimal() ?: amount.toBigDecimal(),
    title = title,
    description = description,
    dateTime = dateTime,
    categoryId = categoryId,
    dueDate = dueDate,
    recurringRuleId = recurringRuleId,
    paidFor = paidForDateTime,
    attachmentUrl = attachmentUrl,
    loanId = loanId,
    loanRecordId = loanRecordId,
    id = id,
    tags = tags
)

fun Tag.toLegacyTag(): LegacyTag = LegacyTag(this.id.value, this.name.value)
fun List<Tag>.toImmutableLegacyTags(): ImmutableList<LegacyTag> =
    this.map { it.toLegacyTag() }.toImmutableList()

/**
 * The tags of the transactions with these [ids], one batched query; a transaction without tags is not in
 * the map.
 */
suspend fun TagRepository.findLegacyTagsByTransactionIds(
    ids: Collection<UUID>
): Map<UUID, ImmutableList<LegacyTag>> = findByAssociatedId(ids.map(::AssociationId))
    .entries
    .associate { (id, tags) -> id.value to tags.toImmutableLegacyTags() }

/** This transaction with its tags from [tagsByTransactionId], or as it is when it has none there. */
fun LegacyTransaction.withTags(
    tagsByTransactionId: Map<UUID, ImmutableList<LegacyTag>>
): LegacyTransaction = tagsByTransactionId[id]?.let { copy(tags = it) } ?: this

/** These transactions with their tags, loaded with one query for all of them. */
suspend fun List<LegacyTransaction>.withTags(tagRepository: TagRepository): List<LegacyTransaction> {
    val tags = tagRepository.findLegacyTagsByTransactionIds(map { it.id })
    return map { it.withTags(tags) }
}

fun TransactionEntity.isIdenticalWith(transaction: TransactionEntity?): Boolean {
    if (transaction == null) return false

    // Set isSynced && isDeleted to false so they aren't accounted in the equals check
    return this.copy(
        isSynced = false,
        isDeleted = false
    ) == transaction.copy(
        isSynced = false,
        isDeleted = false
    )
}
