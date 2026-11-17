package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.WalletPreview
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RunWith(TestParameterInjector::class)
class TransactionListPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun snapshotListSections() {
        snapshot(theme) {
            List(
                theme = theme,
                upcoming = due(
                    expanded = false,
                    income = 4200.0,
                    expenses = 1250.0,
                    expense("Rent", 1250.0, FAR_FUTURE),
                ),
                overdue = due(
                    expanded = true,
                    income = 0.0,
                    expenses = 35.5,
                    expense("Gym", 35.5, FAR_PAST),
                ),
                history = history(),
            )
        }
    }

    @Test
    fun snapshotListUpcomingExpanded() {
        snapshot(theme) {
            List(
                theme = theme,
                upcoming = due(
                    expanded = true,
                    income = 4200.0,
                    expenses = 1250.0,
                    expense("Rent", 1250.0, FAR_FUTURE),
                    income("Salary", 4200.0, FAR_FUTURE),
                ),
                overdue = due(
                    expanded = false,
                    income = 0.0,
                    expenses = 35.5,
                    expense("Gym", 35.5, FAR_PAST),
                ),
            )
        }
    }

    @Test
    fun snapshotListEmpty() {
        snapshot(theme) { List(theme = theme) }
    }

    @Test
    fun snapshotListLargeFontScale() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_6_PRO.copy(fontScale = 1.5f))
        snapshot(theme) {
            List(
                theme = theme,
                upcoming = due(
                    expanded = true,
                    income = HugeAmount,
                    expenses = 1250.0,
                    expense("Rent", 1250.0, FAR_FUTURE),
                ),
                overdue = due(
                    expanded = true,
                    income = 0.0,
                    expenses = 35.5,
                    expense("Gym", 35.5, FAR_PAST),
                ),
                history = history().take(3).toImmutableList(),
            )
        }
    }
}

@Composable
private fun List(
    theme: PaparazziTheme,
    upcoming: LegacyDueSection? = null,
    overdue: LegacyDueSection? = null,
    history: ImmutableList<TransactionHistoryItem> = persistentListOf(),
) {
    WalletPreview(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
        NavigationRoot(navigation = Navigation()) {
            LazyColumn {
                transactions(
                    baseData = BaseData,
                    upcoming = upcoming,
                    overdue = overdue,
                    history = history,
                    emptyStateText = "You don't have any transactions for March.",
                    shouldShowAccountSpecificColorInTransactions = false,
                    onPayOrGet = {},
                    setUpcomingExpanded = {},
                    setOverdueExpanded = {},
                )
            }
        }
    }
}

private fun due(
    expanded: Boolean,
    income: Double,
    expenses: Double,
    vararg transactions: Transaction,
): LegacyDueSection = LegacyDueSection(
    trns = transactions.toList().toImmutableList(),
    expanded = expanded,
    stats = IncomeExpensePair(income.toBigDecimal(), -expenses.toBigDecimal()),
)

private fun history(): ImmutableList<TransactionHistoryItem> = persistentListOf(
    TransactionHistoryDateDivider(LocalDate.of(2024, 3, 15), income = 4200.0, expenses = 250.75),
    income("Salary March", 4200.0),
    expense("Lidl", 250.75),
    TransactionHistoryDateDivider(LocalDate.of(2024, 3, 14), income = 0.0, expenses = 62.4),
    expense("Pharmacy", 62.4),
    TransactionHistoryDateDivider(LocalDate.of(2023, 12, 31), income = 15.0, expenses = 0.0),
    income("Refund", 15.0),
)

private const val HugeAmount = 9999999999.99
private val FAR_FUTURE = Instant.parse("2100-01-01T12:00:00Z")
private val FAR_PAST = Instant.parse("2020-01-01T12:00:00Z")
private val NOW = Instant.parse("2024-03-15T10:30:00Z")

private val Cash = Account(
    name = "Cash",
    color = 0xFF4CAF50.toInt(),
    currency = "USD",
    id = UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
)
private val Food = Category(
    name = NotBlankTrimmedString.unsafe("Food"),
    color = ColorInt(0xFFFF9800.toInt()),
    icon = IconAsset.unsafe("groceries"),
    id = CategoryId(UUID.fromString("00000000-0000-0000-0000-0000000000c1")),
    orderNum = 0.0,
)
private val BaseData = AppBaseData(
    baseCurrency = "USD",
    categories = persistentListOf(Food),
    accounts = persistentListOf(Cash),
)

private var nextId = 0L

private fun transaction(
    type: TransactionType,
    title: String,
    amount: Double,
    dueDate: Instant?,
): Transaction = Transaction(
    accountId = Cash.id,
    type = type,
    amount = amount.toBigDecimal(),
    title = title,
    dateTime = if (dueDate == null) NOW else null,
    dueDate = dueDate,
    categoryId = Food.id.value,
    id = UUID(0, nextId++),
)

private fun expense(title: String, amount: Double, dueDate: Instant? = null): Transaction =
    transaction(TransactionType.EXPENSE, title, amount, dueDate)

private fun income(title: String, amount: Double, dueDate: Instant? = null): Transaction =
    transaction(TransactionType.INCOME, title, amount, dueDate)
