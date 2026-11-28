package com.ivy.data.backup

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.data.DataObserver
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeBudgetDao
import com.ivy.data.db.dao.fake.FakeCategoryDao
import com.ivy.data.db.dao.fake.FakeLoanDao
import com.ivy.data.db.dao.fake.FakeLoanRecordDao
import com.ivy.data.db.dao.fake.FakePlannedPaymentDao
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.db.dao.fake.FakeTagAssociationDao
import com.ivy.data.db.dao.fake.FakeTagDao
import com.ivy.data.db.dao.fake.FakeTransactionDao
import com.ivy.data.db.entity.TagEntity
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TagRepository
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import com.ivy.data.repository.mapper.AccountMapper
import com.ivy.data.repository.mapper.CategoryMapper
import com.ivy.data.repository.mapper.TagMapper
import com.ivy.data.testResource
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Test
import java.time.Instant
import java.util.UUID

class BackupDataUseCaseTest {
    private fun newBackupDataUseCase(
        accountDao: FakeAccountDao = FakeAccountDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        transactionDao: FakeTransactionDao = FakeTransactionDao(),
        plannedPaymentDao: FakePlannedPaymentDao = FakePlannedPaymentDao(),
        budgetDao: FakeBudgetDao = FakeBudgetDao(),
        settingsDao: FakeSettingsDao = FakeSettingsDao(),
        loanDao: FakeLoanDao = FakeLoanDao(),
        loanRecordDao: FakeLoanRecordDao = FakeLoanRecordDao(),
        tagDao: FakeTagDao = FakeTagDao(),
        tagAssociationDao: FakeTagAssociationDao = FakeTagAssociationDao(),
        categoryRepository: CategoryRepository = newCategoryRepository(categoryDao),
        tagRepository: TagRepository = newTagRepository(tagDao, tagAssociationDao),
    ): BackupDataUseCase {
        val accountMapper = AccountMapper(
            CurrencyRepository(
                settingsDao = settingsDao,
                writeSettingsDao = settingsDao,
                dispatchersProvider = TestDispatchersProvider,
            )
        )
        return BackupDataUseCase(
            accountDao = accountDao,
            accountMapper = accountMapper,
            accountRepository = AccountRepository(
                accountDao = accountDao,
                writeAccountDao = accountDao,
                mapper = accountMapper,
                dispatchersProvider = TestDispatchersProvider,
                memoFactory = fakeRepositoryMemoFactory(),
            ),
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            loanRecordDao = loanRecordDao,
            loanDao = loanDao,
            plannedPaymentRuleDao = plannedPaymentDao,
            transactionDao = transactionDao,
            transactionWriter = transactionDao,
            settingsDao = settingsDao,
            categoryWriter = categoryDao,
            settingsWriter = settingsDao,
            budgetWriter = budgetDao,
            loanWriter = loanDao,
            loanRecordWriter = loanRecordDao,
            plannedPaymentRuleWriter = plannedPaymentDao,

            context = mockk(relaxed = true),
            sharedPrefs = mockk(relaxed = true),
            json = KotlinxSerializationModule.provideJson(),
            dispatchersProvider = TestDispatchersProvider,
            fileSystem = mockk(relaxed = true),
            dataObserver = DataObserver(),
            tagsReader = tagDao,
            tagsWriter = tagDao,
            tagAssociationReader = tagAssociationDao,
            tagAssociationWriter = tagAssociationDao,
            categoryRepository = categoryRepository,
            tagRepository = tagRepository,
        )
    }

    private fun newCategoryRepository(categoryDao: FakeCategoryDao) = CategoryRepository(
        mapper = CategoryMapper(),
        writeCategoryDao = categoryDao,
        categoryDao = categoryDao,
        dispatchersProvider = TestDispatchersProvider,
        memoFactory = fakeRepositoryMemoFactory(),
    )

    private fun newTagRepository(
        tagDao: FakeTagDao,
        tagAssociationDao: FakeTagAssociationDao
    ) = TagRepository(
        mapper = TagMapper(),
        tagDao = tagDao,
        tagAssociationDao = tagAssociationDao,
        writeTagDao = tagDao,
        writeTagAssociationDao = tagAssociationDao,
        dispatchersProvider = TestDispatchersProvider,
        memoFactory = fakeRepositoryMemoFactory(),
    )

    private suspend fun backupTestCase(backupVersion: String) {
        // given
        val originalBackupUseCase = newBackupDataUseCase()
        val backupJsonData = testResource("backups/$backupVersion.json")
            .readText(Charsets.UTF_16)

        // when
        val importedDataRes = originalBackupUseCase.importJson(backupJsonData, onProgress = {})

        // then
        importedDataRes.accountsImported shouldBeGreaterThan 0
        importedDataRes.transactionsImported shouldBeGreaterThan 0
        importedDataRes.categoriesImported shouldBeGreaterThan 0
        importedDataRes.failedRows.size shouldBe 0

        // Also - exporting and re-importing the data should work
        // given
        val exportedJson = originalBackupUseCase.generateJsonBackup()

        // when
        val freshBackupUseCase = newBackupDataUseCase()
        val reImportedDataRes = freshBackupUseCase.importJson(exportedJson, onProgress = {})
        // then
        reImportedDataRes shouldBe importedDataRes

        // Finally, exporting again should yield the same result
        freshBackupUseCase.generateJsonBackup() shouldBe exportedJson
    }

    @Test
    fun `backup compatibility with 450 (150)`() = runTest {
        backupTestCase("450-150")
    }

    @Test
    fun `categories read before an import are refreshed by it`() = runTest {
        // given - something (Home, the Categories tab) read the categories before the import
        val categoryDao = FakeCategoryDao()
        val categoryRepository = newCategoryRepository(categoryDao)
        val backupDataUseCase = newBackupDataUseCase(
            categoryDao = categoryDao,
            categoryRepository = categoryRepository,
        )
        categoryRepository.findAll() shouldBe emptyList()
        // "Delete all data" clears the memo but leaves it memoized
        categoryRepository.deleteAll()
        categoryRepository.findAll() shouldBe emptyList()
        val backupJsonData = testResource("backups/450-150.json")
            .readText(Charsets.UTF_16)

        // when
        val result = backupDataUseCase.importJson(backupJsonData, onProgress = {})

        // then
        result.categoriesImported shouldBeGreaterThan 0
        categoryRepository.findAll().size shouldBe result.categoriesImported
    }

    @Test
    fun `tags read before an import are refreshed by it`() = runTest {
        // given
        val tagDao = FakeTagDao()
        val tagAssociationDao = FakeTagAssociationDao()
        val tagRepository = newTagRepository(tagDao, tagAssociationDao)
        val backupDataUseCase = newBackupDataUseCase(
            tagDao = tagDao,
            tagAssociationDao = tagAssociationDao,
            tagRepository = tagRepository,
        )
        tagRepository.findAll() shouldBe emptyList()
        tagRepository.deleteAll()
        tagRepository.findAll() shouldBe emptyList()
        val json = KotlinxSerializationModule.provideJson()
        val backupJsonData = json.encodeToString(
            json.decodeFromString<IvyWalletCompleteData>(
                testResource("backups/450-150.json").readText(Charsets.UTF_16)
            ).copy(
                tags = listOf(
                    TagEntity(
                        id = UUID.randomUUID(),
                        name = "trip",
                        description = null,
                        color = 0,
                        icon = null,
                        orderNum = 0.0,
                        dateTime = Instant.EPOCH,
                        isDeleted = false,
                        lastSyncedTime = Instant.EPOCH,
                    )
                )
            )
        )

        // when
        backupDataUseCase.importJson(backupJsonData, onProgress = {})

        // then
        tagRepository.findAll().map { it.name.value } shouldBe listOf("trip")
    }
}
