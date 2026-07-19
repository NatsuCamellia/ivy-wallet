# Home Transaction List Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the legacy gradient `TransactionCard` list on the Home tab with M3 Expressive segmented rows (spec: `docs/superpowers/specs/2026-07-19-transaction-list-redesign-design.md`).

**Architecture:** Dumb, reusable composables in `shared/ui/core` (`com.ivy.ui.component.transaction`) driven by a pre-formatted `TransactionItemUi` model; a testable mapper + `LazyListScope` wiring in `feature/home` converts legacy `Transaction`/`AppBaseData` into that model. Only `HomeTab` switches renderers — the legacy `transactions()` in `temp/legacy-code` stays for Search/Transactions/Reports.

**Tech Stack:** Jetpack Compose Material 3 (Expressive), Paparazzi screenshot tests, JUnit4 + Kotest `shouldBe` + MockK unit tests.

## Global Constraints

- Every `./gradlew` call needs: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (run from repo root).
- Commits: single-line conventional commits (`feat:`/`docs:`/`test:`…), no bodies. **Never push.**
- Detekt runs `allRules = true`; new code must be violation-free (baseline covers only pre-existing code). Compose rules apply: `modifier: Modifier = Modifier` as first optional param, slot lambdas last.
- The in-house "explicit" ruleset requires explicit types on public declarations.
- Paparazzi snapshots pin `IvyColorSource.BrandSeed` (handled by the `PaparazziScreenshotTest` base class); record with `recordPaparazziDebug`, verify with `verifyPaparazziDebug`.
- Amount color rules (spec): expense `onSurface`, income = extended income green (the only green), transfer `colorScheme.primary`, upcoming = extended warning, overdue `colorScheme.error`.

---

### Task 1: Extended semantic colors (`IvyExtendedColors`)

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/design/system/IvyExtendedColors.kt`
- Modify: `shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt`

**Interfaces:**
- Produces: `LocalIvyExtendedColors: ProvidableCompositionLocal<IvyExtendedColors>` with fields `income: Color`, `warning: Color`, `warningContainer: Color`, `onWarningContainer: Color`. Provided automatically inside `IvyMaterial3Theme` (dark-aware). Tasks 2, 3, 5 consume it.

- [ ] **Step 1: Create `IvyExtendedColors.kt`**

```kotlin
package com.ivy.design.system

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors Material 3 schemes don't guarantee: dynamic color may
 * produce a scheme with no green or orange role at all. Provided by
 * [IvyMaterial3Theme] via [LocalIvyExtendedColors].
 */
@Immutable
data class IvyExtendedColors(
    val income: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val LightExtendedColors: IvyExtendedColors = IvyExtendedColors(
    income = Color(0xFF1E7C46),
    warning = Color(0xFF8A5100),
    warningContainer = Color(0xFFFFDDB8),
    onWarningContainer = Color(0xFF2C1600),
)

internal val DarkExtendedColors: IvyExtendedColors = IvyExtendedColors(
    income = Color(0xFF7ADC9E),
    warning = Color(0xFFFFB95C),
    warningContainer = Color(0xFF693C00),
    onWarningContainer = Color(0xFFFFDDB8),
)

val LocalIvyExtendedColors: ProvidableCompositionLocal<IvyExtendedColors> =
    staticCompositionLocalOf { LightExtendedColors }
```

- [ ] **Step 2: Provide it from `IvyMaterial3Theme`**

Wrap the existing `MaterialExpressiveTheme` call:

```kotlin
    val colorScheme = ivyColorScheme(colorSource, dark).applyTrueBlack(isTrueBlack)
    CompositionLocalProvider(
        LocalIvyExtendedColors provides if (dark) DarkExtendedColors else LightExtendedColors
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = Shapes(),
            typography = ivyExpressiveTypography(),
            content = content,
        )
    }
```

Add import `androidx.compose.runtime.CompositionLocalProvider`.

- [ ] **Step 3: Compile + existing module tests**

Run: `./gradlew :shared:ui:core:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (rendering coverage lands with Task 2's Paparazzi test).

- [ ] **Step 4: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/design/system/
git commit -m "feat: add IvyExtendedColors semantic colors to IvyMaterial3Theme"
```

---

### Task 2: `TransactionItem` row component

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/transaction/TransactionItemUi.kt`
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/transaction/TransactionItem.kt`
- Test: `shared/ui/core/src/test/java/com/ivy/ui/component/transaction/TransactionComponentsPaparazziTest.kt`

**Interfaces:**
- Consumes: `LocalIvyExtendedColors` (Task 1).
- Produces (Tasks 4/5 rely on exact shapes):

```kotlin
@Immutable
data class TransactionItemUi(
    val id: String,
    val title: String,
    val supportingText: String?,   // "Groceries · Cash" or "Cash → Revolut"; null hides line
    val categoryColor: Color?,     // raw category color; null = neutral bubble
    val amountText: String,        // signed + currency, e.g. "-32.51 USD"
    val amountKind: TransactionAmountKind,
    val secondaryText: String?,    // time "14:05" or cross-currency "510 EUR"
    val dueText: String?,          // "Due on Fri, Jul 24"; non-null renders the due chip
)
enum class TransactionAmountKind { Expense, Income, Transfer, Upcoming, Overdue }
enum class TransactionItemPosition { Single, First, Middle, Last }

@Composable
fun TransactionItem(
    ui: TransactionItemUi,
    position: TransactionItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onPayOrGet: (() -> Unit)? = null,
    payOrGetText: String? = null,
    icon: @Composable () -> Unit,
)
```

- [ ] **Step 1: Create `TransactionItemUi.kt`**

```kotlin
package com.ivy.ui.component.transaction

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TransactionItemUi(
    val id: String,
    val title: String,
    val supportingText: String?,
    val categoryColor: Color?,
    val amountText: String,
    val amountKind: TransactionAmountKind,
    val secondaryText: String?,
    val dueText: String?,
)

enum class TransactionAmountKind { Expense, Income, Transfer, Upcoming, Overdue }

enum class TransactionItemPosition { Single, First, Middle, Last }
```

- [ ] **Step 2: Create `TransactionItem.kt`**

```kotlin
package com.ivy.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.design.system.LocalIvyExtendedColors
import com.ivy.ui.R
import androidx.compose.ui.res.stringResource

private val LargeCorner = 24.dp
private val SmallCorner = 8.dp

fun TransactionItemPosition.toShape(): RoundedCornerShape = when (this) {
    TransactionItemPosition.Single -> RoundedCornerShape(LargeCorner)
    TransactionItemPosition.First ->
        RoundedCornerShape(LargeCorner, LargeCorner, SmallCorner, SmallCorner)
    TransactionItemPosition.Middle -> RoundedCornerShape(SmallCorner)
    TransactionItemPosition.Last ->
        RoundedCornerShape(SmallCorner, SmallCorner, LargeCorner, LargeCorner)
}

@Composable
fun TransactionItem(
    ui: TransactionItemUi,
    position: TransactionItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onPayOrGet: (() -> Unit)? = null,
    payOrGetText: String? = null,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(position.toShape())
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag("transaction_item"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBubble(categoryColor = ui.categoryColor, icon = icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ui.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    ui.dueText != null -> DueChip(
                        text = ui.dueText,
                        overdue = ui.amountKind == TransactionAmountKind.Overdue,
                    )
                    ui.supportingText != null -> Text(
                        text = ui.supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ui.amountText,
                    style = MaterialTheme.typography.titleMedium,
                    color = ui.amountKind.amountColor(),
                )
                if (ui.secondaryText != null) {
                    Text(
                        text = ui.secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (onSkip != null || onPayOrGet != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                if (onSkip != null) {
                    FilledTonalButton(onClick = onSkip) {
                        Text(text = stringResource(R.string.skip))
                    }
                }
                if (onPayOrGet != null) {
                    Button(onClick = onPayOrGet) {
                        Text(text = payOrGetText ?: stringResource(R.string.pay))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionAmountKind.amountColor(): Color = when (this) {
    TransactionAmountKind.Expense -> MaterialTheme.colorScheme.onSurface
    TransactionAmountKind.Income -> LocalIvyExtendedColors.current.income
    TransactionAmountKind.Transfer -> MaterialTheme.colorScheme.primary
    TransactionAmountKind.Upcoming -> LocalIvyExtendedColors.current.warning
    TransactionAmountKind.Overdue -> MaterialTheme.colorScheme.error
}

private const val ContainerAlpha = 0.24f
private const val ContentBlend = 0.45f

@Composable
private fun CategoryIconBubble(
    categoryColor: Color?,
    icon: @Composable () -> Unit,
) {
    val container = if (categoryColor != null) {
        categoryColor.copy(alpha = ContainerAlpha)
            .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (categoryColor != null) {
        lerp(categoryColor, MaterialTheme.colorScheme.onSurface, ContentBlend)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            icon()
        }
    }
}

@Composable
private fun DueChip(text: String, overdue: Boolean) {
    val background = if (overdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        LocalIvyExtendedColors.current.warningContainer
    }
    val contentColor = if (overdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        LocalIvyExtendedColors.current.onWarningContainer
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
```

- [ ] **Step 3: Write the Paparazzi test**

Mirror `SettingsComponentsPaparazziTest`. One snapshot with every state, per theme:

```kotlin
package com.ivy.ui.component.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import com.ivy.ui.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class TransactionComponentsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot transaction items`() {
        snapshot(theme) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TransactionItem(
                    ui = expense.copy(title = "Lidl groceries"),
                    position = TransactionItemPosition.First,
                    onClick = {},
                    icon = { CategoryIcon() },
                )
                TransactionItem(
                    ui = expense.copy(
                        id = "2",
                        title = "Salary — July",
                        supportingText = "Salary · DSK Bank",
                        categoryColor = Color(0xFF14CC9E),
                        amountText = "+8,049.70 USD",
                        amountKind = TransactionAmountKind.Income,
                    ),
                    position = TransactionItemPosition.Middle,
                    onClick = {},
                    icon = { CategoryIcon() },
                )
                TransactionItem(
                    ui = expense.copy(
                        id = "3",
                        title = "Top-up Revolut",
                        supportingText = "Cash → Revolut",
                        categoryColor = null,
                        amountText = "40.00 USD",
                        amountKind = TransactionAmountKind.Transfer,
                        secondaryText = "36.50 EUR",
                    ),
                    position = TransactionItemPosition.Last,
                    onClick = {},
                    icon = { TransferIcon() },
                )
                TransactionItem(
                    ui = expense.copy(
                        id = "4",
                        title = "Rent",
                        supportingText = null,
                        amountText = "-500.00 USD",
                        amountKind = TransactionAmountKind.Upcoming,
                        secondaryText = null,
                        dueText = "Due on Fri, Jul 24",
                    ),
                    position = TransactionItemPosition.Single,
                    onClick = {},
                    onSkip = {},
                    onPayOrGet = {},
                    payOrGetText = "Pay",
                    icon = { CategoryIcon() },
                )
                TransactionItem(
                    ui = expense.copy(
                        id = "5",
                        title = "Spotify",
                        supportingText = null,
                        amountText = "-5.99 USD",
                        amountKind = TransactionAmountKind.Overdue,
                        secondaryText = null,
                        dueText = "Due on Jul 15",
                    ),
                    position = TransactionItemPosition.Single,
                    onClick = {},
                    onSkip = {},
                    onPayOrGet = {},
                    payOrGetText = "Pay",
                    icon = { CategoryIcon() },
                )
            }
        }
    }

    companion object {
        private val expense = TransactionItemUi(
            id = "1",
            title = "Expense",
            supportingText = "Groceries · Cash",
            categoryColor = Color(0xFFFF9235),
            amountText = "-32.51 USD",
            amountKind = TransactionAmountKind.Expense,
            secondaryText = "14:05",
            dueText = null,
        )
    }
}

@androidx.compose.runtime.Composable
private fun CategoryIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_custom_category_s),
        contentDescription = null,
    )
}

@androidx.compose.runtime.Composable
private fun TransferIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_transfer),
        contentDescription = null,
    )
}
```

(Move the two private icon composables above the companion if detekt complains about ordering; plain `@Composable` import instead of the fully-qualified annotation.)

- [ ] **Step 4: Record baselines and eyeball them**

Run: `./gradlew :shared:ui:core:recordPaparazziDebug`
Expected: BUILD SUCCESSFUL; new PNGs under `shared/ui/core/src/test/snapshots/images/` matching `TransactionComponentsPaparazziTest` (Light + Dark). Open both PNGs and check: segmented corners (24/8dp), legible icon tint on its bubble in both themes, green income amount, warn/error due chips, button pair on due rows.

- [ ] **Step 5: Verify**

Run: `./gradlew :shared:ui:core:verifyPaparazziDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/ui/component/transaction/ shared/ui/core/src/test/
git commit -m "feat: add M3 segmented TransactionItem component"
```

---

### Task 3: `TransactionDayHeader` + `TransactionSectionHeader`

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/transaction/TransactionDayHeader.kt`
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/transaction/TransactionSectionHeader.kt`
- Modify (extend test): `shared/ui/core/src/test/java/com/ivy/ui/component/transaction/TransactionComponentsPaparazziTest.kt`

**Interfaces (produced, consumed by Task 5):**

```kotlin
@Composable
fun TransactionDayHeader(title: String, netText: String, modifier: Modifier = Modifier)

@Composable
fun TransactionSectionHeader(
    title: String,
    titleColor: Color,
    subtitle: String?,
    expanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
)
```

- [ ] **Step 1: Create `TransactionDayHeader.kt`**

```kotlin
package com.ivy.ui.component.transaction

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TransactionDayHeader(
    title: String,
    netText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = netText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

- [ ] **Step 2: Create `TransactionSectionHeader.kt`**

```kotlin
package com.ivy.ui.component.transaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ivy.ui.R

private const val ExpandedRotation = 180f

@Composable
fun TransactionSectionHeader(
    title: String,
    titleColor: Color,
    subtitle: String?,
    expanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSetExpanded(!expanded) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
        val rotation by animateFloatAsState(
            targetValue = if (expanded) ExpandedRotation else 0f,
            label = "chevron",
        )
        Icon(
            painter = painterResource(R.drawable.ic_expand_more),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotation),
        )
    }
}
```

- [ ] **Step 3: Extend the Paparazzi test**

Add a second test to `TransactionComponentsPaparazziTest`:

```kotlin
    @Test
    fun `snapshot headers`() {
        snapshot(theme) {
            Column(modifier = Modifier.padding(16.dp)) {
                TransactionDayHeader(title = "Today", netText = "-83.26 USD")
                TransactionSectionHeader(
                    title = "Upcoming",
                    titleColor = LocalIvyExtendedColors.current.warning,
                    subtitle = "+120.00 USD · -500.00 USD",
                    expanded = false,
                    onSetExpanded = {},
                )
                TransactionSectionHeader(
                    title = "Overdue",
                    titleColor = MaterialTheme.colorScheme.error,
                    subtitle = "-5.99 USD",
                    expanded = true,
                    onSetExpanded = {},
                    trailing = {
                        TextButton(onClick = {}) { Text("Skip all") }
                    },
                )
            }
        }
    }
```

Imports to add: `androidx.compose.material3.MaterialTheme`, `androidx.compose.material3.Text`, `androidx.compose.material3.TextButton`, `com.ivy.design.system.LocalIvyExtendedColors`.

- [ ] **Step 4: Record, eyeball, verify**

Run: `./gradlew :shared:ui:core:recordPaparazziDebug` then `./gradlew :shared:ui:core:verifyPaparazziDebug`
Expected: BUILD SUCCESSFUL both; new `snapshot headers` PNGs look right (chevron rotated when expanded, warning/error titles, trailing button).

- [ ] **Step 5: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/ui/component/transaction/ shared/ui/core/src/test/
git commit -m "feat: add transaction day and section header components"
```

---

### Task 4: Home mapper (`HomeTransactionMapper`) — TDD

**Files:**
- Create: `feature/home/src/main/java/com/ivy/home/transactionlist/HomeTrnListItem.kt`
- Create: `feature/home/src/main/java/com/ivy/home/transactionlist/HomeTransactionMapper.kt`
- Test: `feature/home/src/test/java/com/ivy/home/transactionlist/HomeTransactionMapperTest.kt`

**Interfaces:**
- Consumes: `TransactionItemUi`, `TransactionAmountKind`, `TransactionItemPosition` (Task 2); legacy `Transaction`, `AppBaseData`, `TransactionHistoryItem`, `TransactionHistoryDateDivider`, `IncomeExpensePair`; `TimeConverter`/`TimeFormatter` (`com.ivy.base.time` / `com.ivy.ui.time`).
- Produces (Task 5 relies on):

```kotlin
sealed interface HomeTrnListItem {
    data class DayHeader(val key: String, val title: String, val netText: String) : HomeTrnListItem
    data class Trn(
        val ui: TransactionItemUi,
        val position: TransactionItemPosition,
        val iconAsset: String?,
        val trn: Transaction,
    ) : HomeTrnListItem
}

class HomeTransactionMapper(
    baseData: AppBaseData,
    timeConverter: TimeConverter,
    timeFormatter: TimeFormatter,
    deletedText: String,          // R.string.deleted
    dueOnFormat: String,          // R.string.due_on, "Due on %1$s"
    expenseFallback: String,      // R.string.expense
    incomeFallback: String,       // R.string.income
    transferFallback: String,     // R.string.transfer
    formatAmount: (Double, String) -> String = { amount, currency -> amount.format(currency) },
) {
    fun mapHistory(history: List<TransactionHistoryItem>): ImmutableList<HomeTrnListItem>
    fun mapDueSection(trns: List<Transaction>, overdue: Boolean): ImmutableList<HomeTrnListItem.Trn>
    fun sectionSubtitle(stats: IncomeExpensePair, currency: String): String?
}
```

- [ ] **Step 1: Create `HomeTrnListItem.kt`** (needed for the test to compile)

```kotlin
package com.ivy.home.transactionlist

import androidx.compose.runtime.Immutable
import com.ivy.base.legacy.Transaction
import com.ivy.ui.component.transaction.TransactionItemPosition
import com.ivy.ui.component.transaction.TransactionItemUi

@Immutable
sealed interface HomeTrnListItem {
    @Immutable
    data class DayHeader(
        val key: String,
        val title: String,
        val netText: String,
    ) : HomeTrnListItem

    @Immutable
    data class Trn(
        val ui: TransactionItemUi,
        val position: TransactionItemPosition,
        val iconAsset: String?,
        val trn: Transaction,
    ) : HomeTrnListItem
}
```

- [ ] **Step 2: Write the failing tests**

Follow the repo's Given-When-Then + sentence-name conventions, MockK for `TimeFormatter`/`TimeConverter`, fixtures at the bottom:

```kotlin
package com.ivy.home.transactionlist

import androidx.compose.ui.graphics.Color
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.domain.pure.data.IncomeExpensePair
import com.ivy.ui.component.transaction.TransactionAmountKind
import com.ivy.ui.component.transaction.TransactionItemPosition
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class HomeTransactionMapperTest {

    private val timeFormatter = mockk<TimeFormatter> {
        every { any<java.time.LocalDateTime>().format(any()) } returns "Today"
        every { any<Instant>().formatLocal(any()) } returns "Fri, Jul 24"
        every { any<LocalTime>().format() } returns "14:05"
    }
    private val timeConverter = mockk<TimeConverter> {
        every { any<Instant>().toLocalTime() } returns LocalTime.of(14, 5)
    }

    private fun mapper(
        accounts: List<Account> = listOf(cash),
        categories: List<Category> = listOf(food),
    ): HomeTransactionMapper = HomeTransactionMapper(
        baseData = AppBaseData(
            baseCurrency = "USD",
            accounts = persistentListOf(*accounts.toTypedArray()),
            categories = persistentListOf(*categories.toTypedArray()),
        ),
        timeConverter = timeConverter,
        timeFormatter = timeFormatter,
        deletedText = "deleted",
        dueOnFormat = "Due on %1\$s",
        expenseFallback = "Expense",
        incomeFallback = "Income",
        transferFallback = "Transfer",
        formatAmount = { amount, currency -> "$amount $currency" },
    )

    @Test
    fun `assigns First Middle Last positions within a day`() {
        // Given
        val history = listOf(
            divider(income = 0.0, expenses = 43.26),
            expenseTrn("a"), expenseTrn("b"), expenseTrn("c"),
        )

        // When
        val items = mapper().mapHistory(history)

        // Then
        items.filterIsInstance<HomeTrnListItem.Trn>().map { it.position } shouldBe listOf(
            TransactionItemPosition.First,
            TransactionItemPosition.Middle,
            TransactionItemPosition.Last,
        )
    }

    @Test
    fun `a lone transaction in a day is Single`() {
        val items = mapper().mapHistory(listOf(divider(0.0, 10.0), expenseTrn("a")))

        (items[1] as HomeTrnListItem.Trn).position shouldBe TransactionItemPosition.Single
    }

    @Test
    fun `day header carries formatted date and signed net total`() {
        val items = mapper().mapHistory(listOf(divider(income = 100.0, expenses = 40.0)))

        val header = items.first() as HomeTrnListItem.DayHeader
        header.title shouldBe "Today"
        header.netText shouldBe "+60.0 USD"
    }

    @Test
    fun `untitled expense falls back to category name and keeps only account in supporting text`() {
        val trn = expenseTrn("a").copy(title = null)

        val item = mapper().mapHistory(listOf(divider(0.0, 10.0), trn))[1] as HomeTrnListItem.Trn

        item.ui.title shouldBe "Food"
        item.ui.supportingText shouldBe "Cash"
    }

    @Test
    fun `titled expense shows category and account in supporting text`() {
        val item = mapper()
            .mapHistory(listOf(divider(0.0, 10.0), expenseTrn("a")))[1] as HomeTrnListItem.Trn

        item.ui.supportingText shouldBe "Food · Cash"
        item.ui.amountKind shouldBe TransactionAmountKind.Expense
        item.ui.amountText shouldBe "-32.51 USD"
        item.ui.secondaryText shouldBe "14:05"
        item.ui.categoryColor shouldBe Color(food.color.value)
        item.iconAsset shouldBe null
    }

    @Test
    fun `transfer builds from-to supporting text and cross-currency secondary text`() {
        val trn = Transaction(
            accountId = cash.id,
            toAccountId = revolut.id,
            type = TransactionType.TRANSFER,
            amount = BigDecimal("40.0"),
            toAmount = BigDecimal("36.5"),
            title = "Top-up",
            dateTime = Instant.EPOCH,
        )

        val item = mapper(accounts = listOf(cash, revolut))
            .mapHistory(listOf(divider(0.0, 0.0), trn))[1] as HomeTrnListItem.Trn

        item.ui.supportingText shouldBe "Cash → Revolut"
        item.ui.amountKind shouldBe TransactionAmountKind.Transfer
        item.ui.amountText shouldBe "40.0 USD"
        item.ui.secondaryText shouldBe "36.5 EUR"
    }

    @Test
    fun `due section rows carry due chip text and upcoming or overdue kind`() {
        val due = expenseTrn("a").copy(dateTime = null, dueDate = Instant.EPOCH)

        val upcoming = mapper().mapDueSection(listOf(due), overdue = false).first()
        val overdue = mapper().mapDueSection(listOf(due), overdue = true).first()

        upcoming.ui.dueText shouldBe "Due on Fri, Jul 24"
        upcoming.ui.amountKind shouldBe TransactionAmountKind.Upcoming
        upcoming.ui.secondaryText shouldBe null
        upcoming.position shouldBe TransactionItemPosition.Single
        overdue.ui.amountKind shouldBe TransactionAmountKind.Overdue
    }

    @Test
    fun `unknown account renders deleted text`() {
        val trn = expenseTrn("a").copy(accountId = UUID.randomUUID())

        val item = mapper().mapHistory(listOf(divider(0.0, 10.0), trn))[1] as HomeTrnListItem.Trn

        item.ui.supportingText shouldBe "Food · deleted"
    }

    @Test
    fun `section subtitle joins non-zero income and expenses and is null when both are zero`() {
        val m = mapper()

        m.sectionSubtitle(IncomeExpensePair(BigDecimal("120.0"), BigDecimal("500.0")), "USD") shouldBe
            "+120.0 USD · -500.0 USD"
        m.sectionSubtitle(IncomeExpensePair(BigDecimal.ZERO, BigDecimal("5.99")), "USD") shouldBe
            "-5.99 USD"
        m.sectionSubtitle(IncomeExpensePair.zero(), "USD") shouldBe null
    }

    companion object {
        private val cash = Account(name = "Cash", color = 0xFF00FF00.toInt())
        private val revolut = Account(name = "Revolut", currency = "EUR", color = 0xFF0000FF.toInt())
        private val food = Category(
            name = NotBlankTrimmedString.unsafe("Food"),
            color = ColorInt(0xFFFF9235.toInt()),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

        private fun divider(income: Double, expenses: Double): TransactionHistoryDateDivider =
            TransactionHistoryDateDivider(
                date = LocalDate.of(2026, 7, 19),
                income = income,
                expenses = expenses,
            )

        private fun expenseTrn(seed: String): Transaction = Transaction(
            accountId = cash.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("32.51"),
            title = "Trn $seed",
            categoryId = food.id.value,
            dateTime = Instant.EPOCH,
        )
    }
}
```

Note: MockK mocking of extension-function interfaces (`every { any<Instant>().formatLocal(any()) }`) must be written inside `with(timeFormatter) { ... }`-style scope; if the `mockk {}` builder syntax fights the receiver, fall back to:

```kotlin
private val timeFormatter = mockk<TimeFormatter>()
private val timeConverter = mockk<TimeConverter>()

init {
    with(timeFormatter) {
        every { any<java.time.LocalDateTime>().format(any()) } returns "Today"
        every { any<Instant>().formatLocal(any()) } returns "Fri, Jul 24"
        every { any<LocalTime>().format() } returns "14:05"
    }
    with(timeConverter) {
        every { any<Instant>().toLocalTime() } returns LocalTime.of(14, 5)
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :feature:home:testDebugUnitTest --tests "com.ivy.home.transactionlist.HomeTransactionMapperTest"`
Expected: FAIL — `HomeTransactionMapper` unresolved.

- [ ] **Step 4: Implement `HomeTransactionMapper.kt`**

```kotlin
package com.ivy.home.transactionlist

import androidx.compose.ui.graphics.Color
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.domain.pure.data.IncomeExpensePair
import com.ivy.legacy.utils.format
import com.ivy.ui.component.transaction.TransactionAmountKind
import com.ivy.ui.component.transaction.TransactionItemPosition
import com.ivy.ui.component.transaction.TransactionItemUi
import com.ivy.ui.time.TimeFormatter
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import java.math.BigDecimal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class HomeTransactionMapper(
    private val baseData: AppBaseData,
    private val timeConverter: TimeConverter,
    private val timeFormatter: TimeFormatter,
    private val deletedText: String,
    private val dueOnFormat: String,
    private val expenseFallback: String,
    private val incomeFallback: String,
    private val transferFallback: String,
    private val formatAmount: (Double, String) -> String = { amount, currency ->
        amount.format(currency)
    },
) {
    fun mapHistory(history: List<TransactionHistoryItem>): ImmutableList<HomeTrnListItem> {
        val result = mutableListOf<HomeTrnListItem>()
        val run = mutableListOf<Transaction>()

        fun flushRun() {
            run.forEachIndexed { index, trn ->
                result += mapTransaction(trn, position(index, run.size), dueKind = null)
            }
            run.clear()
        }

        for (item in history) {
            when (item) {
                is TransactionHistoryDateDivider -> {
                    flushRun()
                    result += mapDayHeader(item)
                }

                is Transaction -> run += item
            }
        }
        flushRun()
        return result.toImmutableList()
    }

    fun mapDueSection(
        trns: List<Transaction>,
        overdue: Boolean,
    ): ImmutableList<HomeTrnListItem.Trn> = trns.mapIndexed { index, trn ->
        mapTransaction(
            trn = trn,
            position = position(index, trns.size),
            dueKind = if (overdue) TransactionAmountKind.Overdue else TransactionAmountKind.Upcoming,
        )
    }.toImmutableList()

    fun sectionSubtitle(stats: IncomeExpensePair, currency: String): String? {
        val parts = buildList {
            if (stats.income > BigDecimal.ZERO) {
                add("+${formatAmount(stats.income.toDouble(), currency)} $currency")
            }
            if (stats.expense > BigDecimal.ZERO) {
                add("-${formatAmount(stats.expense.abs().toDouble(), currency)} $currency")
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun mapDayHeader(divider: TransactionHistoryDateDivider): HomeTrnListItem.DayHeader {
        val net = divider.income - divider.expenses
        val sign = if (net >= 0) "+" else "-"
        return HomeTrnListItem.DayHeader(
            key = divider.date.toString(),
            title = with(timeFormatter) {
                divider.date.atStartOfDay().format(
                    TimeFormatter.Style.DateOnly(includeWeekDay = true)
                )
            },
            netText = "$sign${formatAmount(kotlin.math.abs(net), baseData.baseCurrency)} " +
                baseData.baseCurrency,
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mapTransaction(
        trn: Transaction,
        position: TransactionItemPosition,
        dueKind: TransactionAmountKind?,
    ): HomeTrnListItem.Trn {
        val account = baseData.accounts.find { it.id == trn.accountId }
        val toAccount = baseData.accounts.find { it.id == trn.toAccountId }
        val currency = account?.currency ?: baseData.baseCurrency
        val toCurrency = toAccount?.currency ?: baseData.baseCurrency
        val category = baseData.categories.find { it.id.value == trn.categoryId }

        val accountName = account?.name ?: deletedText
        val isTransfer = trn.type == TransactionType.TRANSFER
        val titledByCategory = trn.title.isNullOrBlank() && category != null
        val title = when {
            !trn.title.isNullOrBlank() -> trn.title!!
            category != null -> category.name.value
            else -> when (trn.type) {
                TransactionType.INCOME -> incomeFallback
                TransactionType.EXPENSE -> expenseFallback
                TransactionType.TRANSFER -> transferFallback
            }
        }
        val supporting = if (isTransfer) {
            "$accountName → ${toAccount?.name ?: deletedText}"
        } else {
            listOfNotNull(
                category?.name?.value?.takeIf { !titledByCategory },
                accountName,
            ).joinToString(" · ").ifBlank { null }
        }

        val amountKind = dueKind ?: when (trn.type) {
            TransactionType.INCOME -> TransactionAmountKind.Income
            TransactionType.EXPENSE -> TransactionAmountKind.Expense
            TransactionType.TRANSFER -> TransactionAmountKind.Transfer
        }
        val amountSign = when {
            dueKind != null || trn.type == TransactionType.EXPENSE -> "-"
            trn.type == TransactionType.INCOME -> "+"
            else -> ""
        }
        val amountText = "$amountSign${formatAmount(trn.amount.toDouble(), currency)} $currency"

        val secondaryText = when {
            dueKind != null -> null
            isTransfer && toCurrency != currency ->
                "${formatAmount(trn.toAmount.toDouble(), toCurrency)} $toCurrency"
            else -> trn.dateTime?.let { instant ->
                with(timeFormatter) {
                    with(timeConverter) { instant.toLocalTime() }.format()
                }
            }
        }

        val dueText = if (dueKind != null && trn.dueDate != null) {
            String.format(
                dueOnFormat,
                with(timeFormatter) {
                    trn.dueDate!!.formatLocal(TimeFormatter.Style.DateOnly(includeWeekDay = true))
                },
            )
        } else {
            null
        }

        return HomeTrnListItem.Trn(
            ui = TransactionItemUi(
                id = trn.id.toString(),
                title = title,
                supportingText = supporting,
                categoryColor = categoryColor(category?.color?.value, account, isTransfer),
                amountText = amountText,
                amountKind = amountKind,
                secondaryText = secondaryText,
                dueText = dueText,
            ),
            position = position,
            iconAsset = category?.icon?.id ?: if (isTransfer) null else account?.icon,
            trn = trn,
        )
    }

    private fun categoryColor(categoryColor: Int?, account: Account?, isTransfer: Boolean): Color? =
        when {
            categoryColor != null -> Color(categoryColor)
            isTransfer -> null
            else -> account?.color?.let(::Color)
        }

    private fun position(index: Int, size: Int): TransactionItemPosition = when {
        size == 1 -> TransactionItemPosition.Single
        index == 0 -> TransactionItemPosition.First
        index == size - 1 -> TransactionItemPosition.Last
        else -> TransactionItemPosition.Middle
    }
}
```

Note: `IncomeExpensePair` comparisons — `stats.income > BigDecimal.ZERO` uses `compareTo`, which handles scale differences correctly (unlike `==`).

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :feature:home:testDebugUnitTest --tests "com.ivy.home.transactionlist.HomeTransactionMapperTest"`
Expected: PASS (all tests). Fix expectation mismatches by reading the failure diff — do not weaken assertions.

- [ ] **Step 6: Commit**

```bash
git add feature/home/src/main/java/com/ivy/home/transactionlist/ feature/home/src/test/java/com/ivy/home/transactionlist/
git commit -m "feat: add HomeTransactionMapper for M3 transaction list"
```

---

### Task 5: Home wiring — render the new list on `HomeTab`

**Files:**
- Create: `feature/home/src/main/java/com/ivy/home/transactionlist/HomeTransactionsList.kt`
- Modify: `feature/home/src/main/java/com/ivy/home/HomeTab.kt` (the `HomeLazyColumn` composable, ~lines 320–412)
- Modify (re-record): `feature/home/src/test/snapshots/` (Home Paparazzi baselines)

**Interfaces:**
- Consumes: everything from Tasks 1–4; legacy `ItemIconSDefaultIcon` (`com.ivy.wallet.ui.theme.components`, from `temp/legacy-code` — already a `feature/home` dependency); `LocalTimeConverter`/`LocalTimeFormatter` (`com.ivy.design.api`); existing `HomeLazyColumn` callbacks (`onPayOrGet`, `onSkipTransaction`, `onSkipAllTransactions`, `setUpcomingExpanded`, `setOverdueExpanded`).
- Produces: `rememberHomeTransactionMapper(baseData): HomeTransactionMapper` and `LazyListScope.homeTransactionsList(...)` — Home-internal, no downstream consumers.

- [ ] **Step 1: Create `HomeTransactionsList.kt`**

```kotlin
package com.ivy.home.transactionlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.system.LocalIvyExtendedColors
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.ui.R
import com.ivy.ui.component.transaction.TransactionDayHeader
import com.ivy.ui.component.transaction.TransactionItem
import com.ivy.ui.component.transaction.TransactionItemPosition
import com.ivy.ui.component.transaction.TransactionSectionHeader
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import androidx.compose.material3.LocalContentColor
import kotlinx.collections.immutable.ImmutableList

private const val FewTransactionsThreshold = 5

@Composable
fun rememberHomeTransactionMapper(baseData: AppBaseData): HomeTransactionMapper {
    val timeConverter = LocalTimeConverter.current
    val timeFormatter = LocalTimeFormatter.current
    val deletedText = stringResource(R.string.deleted)
    val dueOnFormat = stringResource(R.string.due_on)
    val expenseFallback = stringResource(R.string.expense)
    val incomeFallback = stringResource(R.string.income)
    val transferFallback = stringResource(R.string.transfer)
    return remember(baseData, timeConverter, timeFormatter) {
        HomeTransactionMapper(
            baseData = baseData,
            timeConverter = timeConverter,
            timeFormatter = timeFormatter,
            deletedText = deletedText,
            dueOnFormat = dueOnFormat,
            expenseFallback = expenseFallback,
            incomeFallback = incomeFallback,
            transferFallback = transferFallback,
        )
    }
}

@Suppress("LongParameterList", "LongMethod")
fun LazyListScope.homeTransactionsList(
    historyItems: ImmutableList<HomeTrnListItem>,
    upcoming: LegacyDueSection?,
    upcomingRows: ImmutableList<HomeTrnListItem.Trn>,
    upcomingSubtitle: String?,
    overdue: LegacyDueSection?,
    overdueRows: ImmutableList<HomeTrnListItem.Trn>,
    overdueSubtitle: String?,
    emptyStateTitle: String,
    emptyStateText: String,
    onTransactionClick: (Transaction) -> Unit,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    setUpcomingExpanded: (Boolean) -> Unit,
    setOverdueExpanded: (Boolean) -> Unit,
) {
    if (upcoming != null && upcoming.trns.isNotEmpty()) {
        item(key = "upcoming_section") {
            TransactionSectionHeader(
                title = stringResource(R.string.upcoming),
                titleColor = LocalIvyExtendedColors.current.warning,
                subtitle = upcomingSubtitle,
                expanded = upcoming.expanded,
                onSetExpanded = setUpcomingExpanded,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
            )
        }
        if (upcoming.expanded) {
            dueRows(upcomingRows, onTransactionClick, onPayOrGet, onSkipTransaction)
        }
    }

    if (overdue != null && overdue.trns.isNotEmpty()) {
        item(key = "overdue_section") {
            val overdueTrns = overdue.trns
            TransactionSectionHeader(
                title = stringResource(R.string.overdue),
                titleColor = MaterialTheme.colorScheme.error,
                subtitle = overdueSubtitle,
                expanded = overdue.expanded,
                onSetExpanded = setOverdueExpanded,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
                trailing = {
                    TextButton(onClick = { onSkipAllTransactions(overdueTrns) }) {
                        Text(text = stringResource(R.string.skip_all))
                    }
                },
            )
        }
        if (overdue.expanded) {
            dueRows(overdueRows, onTransactionClick, onPayOrGet, onSkipTransaction)
        }
    }

    items(
        items = historyItems,
        key = {
            when (it) {
                is HomeTrnListItem.DayHeader -> it.key
                is HomeTrnListItem.Trn -> it.ui.id
            }
        },
    ) { item ->
        when (item) {
            is HomeTrnListItem.DayHeader -> TransactionDayHeader(
                title = item.title,
                netText = item.netText,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
            )

            is HomeTrnListItem.Trn -> TrnRow(
                item = item,
                onClick = { onTransactionClick(item.trn) },
            )
        }
    }

    val trnCount = historyItems.count { it is HomeTrnListItem.Trn }
        .plus(if (upcoming?.expanded == true) upcomingRows.size else 0)
        .plus(if (overdue?.expanded == true) overdueRows.size else 0)
    if (
        trnCount == 0 &&
        (upcoming == null || upcoming.trns.isEmpty()) &&
        (overdue == null || overdue.trns.isEmpty())
    ) {
        item {
            HomeTransactionsEmptyState(title = emptyStateTitle, text = emptyStateText)
        }
    }

    item {
        // scroll hack: keep the last items reachable above the FAB/bottom bar
        Spacer(Modifier.height(if (trnCount <= FewTransactionsThreshold) 300.dp else 150.dp))
    }
}

private fun LazyListScope.dueRows(
    rows: ImmutableList<HomeTrnListItem.Trn>,
    onTransactionClick: (Transaction) -> Unit,
    onPayOrGet: (Transaction) -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
) {
    items(items = rows, key = { it.ui.id }) { item ->
        val isExpense = item.trn.type == TransactionType.EXPENSE
        TrnRow(
            item = item,
            onClick = { onTransactionClick(item.trn) },
            onSkip = { onSkipTransaction(item.trn) },
            onPayOrGet = { onPayOrGet(item.trn) },
            payOrGetText = stringResource(if (isExpense) R.string.pay else R.string.get),
        )
    }
}

@Composable
private fun TrnRow(
    item: HomeTrnListItem.Trn,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onPayOrGet: (() -> Unit)? = null,
    payOrGetText: String? = null,
) {
    val topGap = when (item.position) {
        TransactionItemPosition.Single, TransactionItemPosition.First -> 8.dp
        TransactionItemPosition.Middle, TransactionItemPosition.Last -> 4.dp
    }
    TransactionItem(
        ui = item.ui,
        position = item.position,
        onClick = onClick,
        onSkip = onSkip,
        onPayOrGet = onPayOrGet,
        payOrGetText = payOrGetText,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = topGap),
        icon = {
            ItemIconSDefaultIcon(
                iconName = item.iconAsset,
                defaultIcon = if (item.ui.amountKind ==
                    com.ivy.ui.component.transaction.TransactionAmountKind.Transfer
                ) {
                    R.drawable.ic_transfer
                } else {
                    R.drawable.ic_custom_category_s
                },
                tint = LocalContentColor.current,
            )
        },
    )
}

@Composable
private fun HomeTransactionsEmptyState(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
```

(Clean up the two fully-qualified `TransactionAmountKind` references with a normal import if detekt/ktlint flags them.)

- [ ] **Step 2: Swap the renderer in `HomeTab.kt`'s `HomeLazyColumn`**

Before the `LazyColumn`, map the data (the composable already has `baseData`, `history`, `upcoming`, `overdue` params):

```kotlin
    val mapper = rememberHomeTransactionMapper(baseData)
    val historyItems = remember(mapper, history) { mapper.mapHistory(history) }
    val upcomingRows = remember(mapper, upcoming) {
        mapper.mapDueSection(upcoming?.trns.orEmpty(), overdue = false)
    }
    val overdueRows = remember(mapper, overdue) {
        mapper.mapDueSection(overdue?.trns.orEmpty(), overdue = true)
    }
    val upcomingSubtitle = remember(mapper, upcoming) {
        upcoming?.let { mapper.sectionSubtitle(it.stats, baseData.baseCurrency) }
    }
    val overdueSubtitle = remember(mapper, overdue) {
        overdue?.let { mapper.sectionSubtitle(it.stats, baseData.baseCurrency) }
    }
    val nav = navigation()
```

Replace the whole `transactions(...)` call inside the `LazyColumn` block with:

```kotlin
        homeTransactionsList(
            historyItems = historyItems,
            upcoming = upcoming,
            upcomingRows = upcomingRows,
            upcomingSubtitle = upcomingSubtitle,
            overdue = overdue,
            overdueRows = overdueRows,
            overdueSubtitle = overdueSubtitle,
            emptyStateTitle = stringRes(R.string.no_transactions),
            emptyStateText = stringRes(
                R.string.no_transactions_description,
                period.toDisplayLong(
                    startDateOfMonth = ivyContext.startDayOfMonth,
                    timeProvider = timeProvider,
                    timeConverter = timeConverter,
                    timeFormatter = timeFormatter,
                )
            ),
            onTransactionClick = { trn ->
                nav.navigateTo(
                    EditTransactionScreen(initialTransactionId = trn.id, type = trn.type)
                )
            },
            onPayOrGet = onPayOrGet,
            onSkipTransaction = onSkipTransaction,
            onSkipAllTransactions = onSkipAllTransactions,
            setUpcomingExpanded = setUpcomingExpanded,
            setOverdueExpanded = setOverdueExpanded,
        )
```

Add imports (`com.ivy.home.transactionlist.*`, `com.ivy.navigation.EditTransactionScreen`, `com.ivy.navigation.navigation`, `androidx.compose.runtime.remember`) and remove the now-unused `com.ivy.legacy.ui.component.transaction.transactions` import. Keep `TransactionsDividerLine` and everything else untouched.

- [ ] **Step 3: Unit tests still pass**

Run: `./gradlew :feature:home:testDebugUnitTest`
Expected: mapper tests PASS; `HomePaparazziTest` FAILS on snapshot diff (that's the visual change — expected).

- [ ] **Step 4: Re-record Home baselines and eyeball**

Run: `./gradlew :feature:home:recordPaparazziDebug` then `./gradlew :feature:home:verifyPaparazziDebug`
Expected: BUILD SUCCESSFUL; inspect the updated `HomePaparazziTest` PNGs — segmented list rendered, header/customer-journey unchanged.

- [ ] **Step 5: Detekt + full unit tests**

Run: `./gradlew detekt` and `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, no new detekt violations.

- [ ] **Step 6: Commit**

```bash
git add feature/home/
git commit -m "feat: render M3 segmented transaction list on Home tab"
```

---

### Task 6: Full verification + device sanity check

**Files:** none new.

- [ ] **Step 1: Full CI mirror**

Run (each from repo root, with `JAVA_HOME` exported):

```bash
./gradlew testDebugUnitTest
./gradlew detekt
./gradlew lintR
./gradlew verifyPaparazziDebug
```

Expected: all BUILD SUCCESSFUL. Lint report at `build/reports/lint/lint.html` — no new errors (baseline covers pre-existing).

- [ ] **Step 2: Build + run on emulator/device (if available)**

```bash
./gradlew assembleDebug
```

Install and check on the Home tab: history rows grouped per day with segmented corners; upcoming/overdue collapse and expand; Pay/Skip/Skip-all still trigger their confirmation flows; tapping a row opens the edit screen; empty state renders when the period has no transactions; dark theme + true-black look right.

- [ ] **Step 3: Commit any fixups**

```bash
git add -A && git commit -m "fix: address lint/detekt fallout from home transaction list"
```

(Skip if nothing changed.)

## Self-review notes

- Spec coverage: extended colors (T1), row + due variant + chip + buttons (T2), day/section headers incl. Skip all (T3), mapper incl. positions/fallbacks/net totals/subtitles (T4), Home wiring incl. empty state + scroll spacer + navigation + Paparazzi re-record (T5), CI mirror (T6). Simplifications need no code: description/tags/chip-tap/account-color are simply not mapped.
- Legacy `transactions()` untouched — other screens unaffected (verified: only `HomeTab` call site changes).
- Type consistency: `TransactionItemUi`/`TransactionAmountKind`/`TransactionItemPosition` (T2) are the exact types consumed in T4's mapper and T5's wiring; `HomeTrnListItem` field names (`ui`, `position`, `iconAsset`, `trn`) match across T4/T5.
