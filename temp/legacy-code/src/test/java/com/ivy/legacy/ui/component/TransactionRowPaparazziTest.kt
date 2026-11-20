package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.LegacyTag
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.WalletPreview
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.ui.R
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(TestParameterInjector::class)
class TransactionRowPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun snapshotBadgeLightUserColor() {
        snapshot(theme) { Badges(theme, Color(0xFFFFFFFF)) }
    }

    @Test
    fun snapshotBadgeDarkUserColor() {
        snapshot(theme) { Badges(theme, Color(0xFF000000)) }
    }

    @Test
    fun snapshotBadgeIconFamilies() {
        snapshot(theme) { IconFamilies(theme) }
    }

    @Test
    fun snapshotRowChipsWrap() {
        snapshot(theme) {
            Rows(
                theme,
                expense(categoryId = LongCategory.id.value, tags = true),
                income(title = "Salary March"),
                baseData = LongBaseData,
            )
        }
    }

    @Test
    fun snapshotRowTypes() {
        snapshot(theme) {
            Rows(
                theme,
                income(),
                expense(),
                transfer(),
                transfer(toAmount = 920.0, toCurrency = "EUR"),
                transfer(toAccountId = UUID(0, 99)),
            )
        }
    }

    @Test
    fun snapshotRowHugeAmount() {
        snapshot(theme) {
            Rows(
                theme,
                expense(title = "Yacht", amount = HugeAmount),
                income(title = "Lottery", amount = HugeAmount),
                transfer(amount = HugeAmount, toAmount = HugeAmount, toCurrency = "EUR"),
            )
        }
    }

    @Test
    fun snapshotRowDue() {
        snapshot(theme) {
            Rows(
                theme,
                expense(title = "Rent", dueDate = FAR_FUTURE, dateTime = null),
                income(title = "Salary", dueDate = FAR_PAST, dateTime = null),
                expense(title = "Gym", dueDate = FAR_PAST, dateTime = null),
            )
        }
    }

    @Test
    fun snapshotRowAccountColors() {
        snapshot(theme) {
            Rows(
                theme,
                expense(),
                transfer(toAmount = 920.0, toCurrency = "EUR"),
                showAccountColors = true,
            )
        }
    }

    @Test
    fun snapshotRowFallbacks() {
        snapshot(theme) {
            Rows(
                theme,
                expense(title = null, description = "Only a description, long enough to need a second line"),
                expense(title = null, description = null),
                expense(title = "No category", categoryId = null, tags = true),
                expense(title = "Free sample", amount = 0.0),
            )
        }
    }

    @Test
    fun snapshotRowLargeFontScale() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_6_PRO.copy(fontScale = 1.5f))
        snapshot(theme) {
            Rows(
                theme,
                expense(dueDate = FAR_FUTURE, dateTime = null),
                transfer(toAmount = 920.0, toCurrency = "EUR"),
                expense(title = "Yacht", amount = HugeAmount),
            )
        }
    }
}

@Composable
private fun Badges(theme: PaparazziTheme, color: Color) {
    WalletPreview(theme.toTheme()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(color = color, iconName = "groceries", defaultIcon = R.drawable.ic_custom_category_m)
            IconBadge(color = color, iconName = null, defaultIcon = R.drawable.ic_custom_account_m)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconFamilies(theme: PaparazziTheme) {
    WalletPreview(theme.toTheme()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf(IvyIcons, VueBuildingIcons, VueOtherIcons).forEach { icons ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    icons.forEach {
                        IconBadge(
                            color = Color(0xFF6B4DFF),
                            iconName = it,
                            defaultIcon = R.drawable.ic_custom_category_m,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Rows(
    theme: PaparazziTheme,
    vararg transactions: Transaction,
    showAccountColors: Boolean = false,
    baseData: AppBaseData = BaseData,
) {
    WalletPreview(theme.toTheme()) {
        NavigationRoot(navigation = Navigation()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                transactions.forEachIndexed { index, transaction ->
                    TransactionRow(
                        baseData = baseData,
                        transaction = transaction,
                        shouldShowAccountSpecificColorInTransactions = showAccountColors,
                        onPayOrGet = {},
                        onClick = {},
                        modifier = Modifier.padding(top = 8.dp),
                        index = index,
                        count = transactions.size,
                    )
                }
            }
        }
    }
}

private fun PaparazziTheme.toTheme(): Theme = if (this == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT

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
private val Bank = Account(
    name = "Revolut",
    color = 0xFF6B4DFF.toInt(),
    currency = "USD",
    icon = "revolut",
    id = UUID.fromString("00000000-0000-0000-0000-0000000000a2"),
)
private val Euro = Account(
    name = "Euro savings",
    color = 0xFFFF9800.toInt(),
    currency = "EUR",
    id = UUID.fromString("00000000-0000-0000-0000-0000000000a3"),
)
private val Food = Category(
    name = NotBlankTrimmedString.unsafe("Food"),
    color = ColorInt(0xFFFF9800.toInt()),
    icon = IconAsset.unsafe("groceries"),
    id = CategoryId(UUID.fromString("00000000-0000-0000-0000-0000000000c1")),
    orderNum = 0.0,
)
private val Work = Category(
    name = NotBlankTrimmedString.unsafe("Salary"),
    color = ColorInt(0xFF2196F3.toInt()),
    icon = null,
    id = CategoryId(UUID.fromString("00000000-0000-0000-0000-0000000000c2")),
    orderNum = 1.0,
)
private val BaseData = AppBaseData(
    baseCurrency = "USD",
    categories = persistentListOf(Food, Work),
    accounts = persistentListOf(Cash, Bank, Euro),
)

private val LongCategory = Category(
    name = NotBlankTrimmedString.unsafe("Home improvement and gardening"),
    color = ColorInt(0xFF4CAF50.toInt()),
    icon = IconAsset.unsafe("house"),
    id = CategoryId(UUID.fromString("00000000-0000-0000-0000-0000000000c3")),
    orderNum = 2.0,
)
private val LongBaseData = AppBaseData(
    baseCurrency = "USD",
    categories = persistentListOf(Food, Work, LongCategory),
    accounts = persistentListOf(Cash.copy(name = "Savings at the credit union"), Bank, Euro),
)

private val IvyIcons = listOf("bank", "house", "education", "doctor", "farmacy", "cash")
private val VueBuildingIcons = listOf(
    "ic_vue_building_bank",
    "ic_vue_building_house",
    "ic_vue_building_building",
    "ic_vue_building_buildings",
    "ic_vue_building_courthouse",
    "ic_vue_building_hospital",
)
private val VueOtherIcons = listOf(
    "ic_vue_main_cake",
    "ic_vue_main_coffee",
    "ic_vue_main_clock",
    "ic_vue_main_crown",
    "ic_vue_main_cup",
    "ic_vue_main_calendar",
)

private var nextId = 0L

private fun transaction(
    type: TransactionType,
    title: String?,
    amount: Double,
    description: String? = null,
    categoryId: UUID? = null,
    dueDate: Instant? = null,
    dateTime: Instant? = NOW,
    toAccountId: UUID? = null,
    toAmount: Double = amount,
    tags: Boolean = false,
): Transaction = Transaction(
    accountId = Cash.id,
    type = type,
    amount = amount.toBigDecimal(),
    toAccountId = toAccountId,
    toAmount = toAmount.toBigDecimal(),
    title = title,
    description = description,
    dateTime = dateTime,
    dueDate = dueDate,
    categoryId = categoryId,
    tags = if (tags) {
        persistentListOf(
            LegacyTag(UUID(0, 1), "holiday"),
            LegacyTag(UUID(0, 2), "family"),
        )
    } else {
        persistentListOf()
    },
    id = UUID(0, nextId++),
)

private fun expense(
    title: String? = "Lidl",
    amount: Double = 250.75,
    description: String? = null,
    categoryId: UUID? = Food.id.value,
    dueDate: Instant? = null,
    dateTime: Instant? = NOW,
    tags: Boolean = false,
): Transaction = transaction(
    type = TransactionType.EXPENSE,
    title = title,
    amount = amount,
    description = description,
    categoryId = categoryId,
    dueDate = dueDate,
    dateTime = dateTime,
    tags = tags,
)

private fun income(
    title: String? = "Salary March",
    amount: Double = 4200.0,
    dueDate: Instant? = null,
    dateTime: Instant? = NOW,
): Transaction = transaction(
    type = TransactionType.INCOME,
    title = title,
    amount = amount,
    categoryId = Work.id.value,
    dueDate = dueDate,
    dateTime = dateTime,
)

private fun transfer(
    amount: Double = 1000.0,
    toAmount: Double = amount,
    toCurrency: String = "USD",
    toAccountId: UUID? = null,
): Transaction = transaction(
    type = TransactionType.TRANSFER,
    title = "Top-up",
    amount = amount,
    toAccountId = toAccountId ?: if (toCurrency == "EUR") Euro.id else Bank.id,
    toAmount = toAmount,
)
