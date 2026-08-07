# Add/Edit Transaction M3 Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `EditTransactionScreen` in flat Material 3 (ReadYou idiom) — `Scaffold` + transparent `TopAppBar`, amount as display headline, attributes as `SettingsItem` rows, keypad in a `ModalBottomSheet` — deleting every legacy-design-system component from this screen's composition.

**Architecture:** Reusable dumb components land in `shared/ui/core` (keypad, pickers, icon bubble) taking pre-formatted immutable UI models. `feature/edit-transaction` keeps the mapping from legacy `Account`/`Category` types and owns the screen composition, split into a header file, a rows file, a pure-Kotlin mapper file and a composition root. `EditTransactionViewModel`, `EditTransactionViewEvent` and `EditTransactionViewState` are **not modified** — every existing event keeps a trigger.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (`androidx.compose.material3`), Hilt, `kotlinx.collections.immutable`, Keval (`libs.keval`) for arithmetic, Paparazzi for screenshot tests, kotest assertions + JUnit4 for unit tests.

**Spec:** `docs/superpowers/specs/2026-08-07-add-transaction-redesign-design.md` — read it before Task 1. The parity table in that spec is the acceptance checklist for Task 8.

## Global Constraints

- **Do not modify** `EditTransactionViewModel.kt`, `EditTransactionViewEvent.kt`, or `EditTransactionViewState` (in `EditTransactionViewEvent.kt`). View-layer only.
- **No legacy design system in new code.** No `com.ivy.design.l0_system.UI`, no `UI.colors`/`UI.typo`/`UI.shapes`, no `com.ivy.wallet.ui.theme.*` components, no gradients. Colors come from `MaterialTheme.colorScheme` and `LocalIvyExtendedColors.current`; type from `MaterialTheme.typography`.
- **Three legacy modals stay and keep being called** from the new screen: `CategoryModal` (create category), `AccountModal` (create account), `ShowTagModal` (tags). Do not restyle or replace them.
- **Do not touch** `EditPlannedScreen`, `feature/reports/FilterOverlay.kt`, or any file under `temp/legacy-code` / `temp/old-design`.
- **`shared/ui/core` must not import legacy types.** No `com.ivy.legacy.datamodel.Account`, no `com.ivy.data.model.Category`, no `com.ivy.base.legacy.*` in new `shared/ui/core` files. Pass pre-formatted strings and `Color`s.
- **Strings** go in `shared/ui/core/src/main/res/values/strings.xml`, referenced via `com.ivy.ui.R`. Before adding a string, grep that file for an existing one with the same text and reuse it. English only.
- **Naming:** Compose parameter naming rules are enforced by the repo's detekt/compose rules — trailing lambda is the content slot; event callbacks are `onXxxChange`/`onXxx`, never `setXxx`; `modifier: Modifier = Modifier` is the first optional parameter.
- **Paparazzi cannot render `ModalBottomSheet`** (it composes into a separate window). Every sheet is split into a public `XxxContent` composable (snapshot-tested) and a thin `XxxSheet` wrapper that puts `XxxContent` inside `ModalBottomSheet` (not snapshot-tested).
- **Commit style:** conventional commits, single-line subject, no body. Example: `feat: add M3 amount keypad sheet`.
- **Every task ends green:** the task's own tests pass before committing.
- **`detekt` cannot run in this environment.** The repo's detekt setup pulls the
  `com.github.Ivy-Apps:detekt-explicit` rule set from `jitpack.io`, which this
  session's egress policy blocks (403 on CONNECT). `detekt` is also a
  root-project task, not a per-module one — `:module:detekt` does not exist.
  Where a task step below says to run detekt, run
  `./gradlew :<module>:compileDebugKotlin` instead and note in your report that
  detekt was skipped for this reason. Static analysis runs in CI on push, where
  jitpack is reachable. Do not attempt to work around the block, and do not
  remove the rule set from the build config.
- **Only one Gradle build at a time.** Concurrent builds in this project
  corrupt each other's KSP outputs (`Failed to create MD5 hash for file ...`).
  Never start a Gradle command while another is running.

## File Structure

**Created — `shared/ui/core`:**

| File | Responsibility |
| --- | --- |
| `com/ivy/ui/component/transaction/CategoryIconBubble.kt` | Tonal circular icon container tinted from a category color (moved out of `TransactionItem.kt`, made public) |
| `com/ivy/ui/component/amount/AmountKeypadInput.kt` | Pure keypad input state + arithmetic evaluation. No Compose. |
| `com/ivy/ui/component/amount/AmountKeypadSheet.kt` | `AmountKeypadContent` + `AmountKeypadSheet`, account chips, key grid |
| `com/ivy/ui/component/picker/PickerSheet.kt` | `PickerItemUi`, `PickerContent` + `PickerSheet` — segmented tonal block list used for category and account selection |

**Created — `feature/edit-transaction`:**

| File | Responsibility |
| --- | --- |
| `com/ivy/transaction/EditTransactionUiMapper.kt` | Pure mapping: commit-action choice, overflow items, row labels, legacy types → `PickerItemUi` |
| `com/ivy/transaction/EditTransactionHeader.kt` | Type selector, amount headline, title field, suggestion chips |
| `com/ivy/transaction/EditTransactionRows.kt` | Attribute rows (category, account(s), date & time, due date, description, tags) + exchange-rate row |

**Modified:**

| File | Change |
| --- | --- |
| `shared/ui/core/.../transaction/TransactionItem.kt` | `CategoryIconBubble` moved out; call site unchanged |
| `shared/ui/core/build.gradle.kts` | `implementation(libs.keval)` |
| `shared/ui/core/src/main/res/values/strings.xml` | New strings |
| `feature/edit-transaction/.../EditTransactionScreen.kt` | Rewritten |
| `shared/ui/navigation/.../Screens.kt` | `EditTransactionScreen.isLegacy` → `false` |
| `app/src/main/java/com/ivy/IvyNavGraph.kt` | Only if the screen's receiver type changes (see Task 8) |

**Tests created:**

- `shared/ui/core/src/test/java/com/ivy/ui/component/amount/AmountKeypadInputTest.kt`
- `shared/ui/core/src/test/java/com/ivy/ui/component/amount/AmountKeypadPaparazziTest.kt`
- `shared/ui/core/src/test/java/com/ivy/ui/component/picker/PickerPaparazziTest.kt`
- `feature/edit-transaction/src/test/java/com/ivy/transaction/EditTransactionUiMapperTest.kt`
- `feature/edit-transaction/src/test/java/com/ivy/transaction/EditTransactionPaparazziTest.kt`

---

### Task 1: Extract `CategoryIconBubble` into a shared component

The category icon bubble is currently private inside `TransactionItem.kt`. The new rows and pickers need the same treatment, so it moves to its own file and becomes public. Rendering must not change — the existing transaction-item golden images are the test.

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/transaction/CategoryIconBubble.kt`
- Modify: `shared/ui/core/src/main/java/com/ivy/ui/component/transaction/TransactionItem.kt` (remove `CategoryIconBubble`, `ContainerAlpha`, `ContentBlend`; keep the call site)
- Test: existing `shared/ui/core/src/test/java/com/ivy/ui/component/transaction/TransactionComponentsPaparazziTest.kt` (unchanged)

**Interfaces:**
- Consumes: nothing.
- Produces: `@Composable fun CategoryIconBubble(categoryColor: Color?, modifier: Modifier = Modifier, size: Dp = 40.dp, icon: @Composable () -> Unit)`

- [ ] **Step 1: Verify the current goldens pass before touching anything**

Run: `./gradlew :shared:ui:core:verifyPaparazziDebug`
Expected: PASS. If it fails, stop — the baseline is stale and that is a separate problem.

- [ ] **Step 2: Create the new file, moving the code verbatim**

Move `ContainerAlpha` (`0.24f`), `ContentBlend` (`0.45f`) and the body of `CategoryIconBubble` exactly as they are today. The only changes: it becomes public, gains `modifier` and `size` parameters, and `Modifier.size(40.dp)` becomes `modifier.size(size)`.

```kotlin
package com.ivy.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val ContainerAlpha = 0.24f
private const val ContentBlend = 0.45f

/**
 * Circular tonal container carrying a category's color, with the icon tinted
 * for legibility on it. Shared by the transaction list, the edit-transaction
 * rows and the category picker.
 */
@Composable
fun CategoryIconBubble(
    categoryColor: Color?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
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
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            icon()
        }
    }
}
```

- [ ] **Step 3: Delete the old copy from `TransactionItem.kt`**

Remove the private `CategoryIconBubble` function and the `ContainerAlpha` / `ContentBlend` constants. Remove now-unused imports (`Box`, `size`, `CircleShape`, `CompositionLocalProvider`, `LocalContentColor`, `compositeOver`, `lerp`, `clip`, `background` — only if no other usage remains in the file; check each). The `CategoryIconBubble(categoryColor = ui.categoryColor, icon = icon)` call site stays as-is.

- [ ] **Step 4: Verify rendering is unchanged**

Run: `./gradlew :shared:ui:core:verifyPaparazziDebug`
Expected: PASS with no re-recorded images. If any image differs, the move was not verbatim — fix the code, do not re-record.

- [ ] **Step 5: Compile check**

Run: `./gradlew :shared:ui:core:compileDebugKotlin`
Expected: PASS (detekt is skipped — see Global Constraints)

- [ ] **Step 6: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/ui/component/transaction/
git commit -m "refactor: extract CategoryIconBubble into a shared component"
```

---

### Task 2: Keypad input logic (pure Kotlin, TDD)

All keypad behaviour that can be tested without Compose lives here: building the input string, the decimal rules, and evaluating arithmetic. Written test-first.

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/amount/AmountKeypadInput.kt`
- Modify: `shared/ui/core/build.gradle.kts`
- Test: `shared/ui/core/src/test/java/com/ivy/ui/component/amount/AmountKeypadInputTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `enum class AmountKeypadKey { Digit0..Digit9, Decimal, Backspace, Plus, Minus, Times, Divide }` — digits are `Digit0`…`Digit9`
  - `@Immutable data class AmountKeypadInput(val text: String)` with `val isExpression: Boolean`
  - `fun AmountKeypadInput.press(key: AmountKeypadKey, decimalCountMax: Int, decimalSeparator: Char): AmountKeypadInput`
  - `fun AmountKeypadInput.evaluate(decimalSeparator: Char): Double?`
  - `fun amountKeypadInputOf(amount: Double?, decimalSeparator: Char): AmountKeypadInput`

- [ ] **Step 1: Add the Keval dependency**

In `shared/ui/core/build.gradle.kts`, inside `dependencies`, after `implementation(libs.materialkolor)`:

```kotlin
    implementation(libs.keval)
```

- [ ] **Step 2: Write the failing tests**

Create `shared/ui/core/src/test/java/com/ivy/ui/component/amount/AmountKeypadInputTest.kt`:

```kotlin
package com.ivy.ui.component.amount

import io.kotest.matchers.shouldBe
import org.junit.Test

class AmountKeypadInputTest {

    private val sep = '.'

    private fun input(text: String) = AmountKeypadInput(text)

    private fun AmountKeypadInput.press(vararg keys: AmountKeypadKey): AmountKeypadInput =
        keys.fold(this) { acc, key -> acc.press(key, decimalCountMax = 2, decimalSeparator = sep) }

    @Test
    fun `appends digits`() {
        input("").press(AmountKeypadKey.Digit4, AmountKeypadKey.Digit2).text shouldBe "42"
    }

    @Test
    fun `appends the decimal separator once per number`() {
        input("42").press(AmountKeypadKey.Decimal, AmountKeypadKey.Decimal).text shouldBe "42."
    }

    @Test
    fun `starts a decimal with a leading zero when the input is empty`() {
        input("").press(AmountKeypadKey.Decimal).text shouldBe "0."
    }

    @Test
    fun `rejects digits beyond decimalCountMax`() {
        input("42.50").press(AmountKeypadKey.Digit9).text shouldBe "42.50"
    }

    @Test
    fun `allows a second decimal separator after an operator`() {
        input("1.5+2").press(AmountKeypadKey.Decimal, AmountKeypadKey.Digit5).text shouldBe "1.5+2.5"
    }

    @Test
    fun `backspace removes the last character`() {
        input("42").press(AmountKeypadKey.Backspace).text shouldBe "4"
    }

    @Test
    fun `backspace on empty input is a no-op`() {
        input("").press(AmountKeypadKey.Backspace).text shouldBe ""
    }

    @Test
    fun `ignores a leading operator`() {
        input("").press(AmountKeypadKey.Times).text shouldBe ""
    }

    @Test
    fun `replaces a trailing operator instead of stacking operators`() {
        input("42").press(AmountKeypadKey.Plus, AmountKeypadKey.Times).text shouldBe "42*"
    }

    @Test
    fun `isExpression is false for a plain number`() {
        input("42.50").isExpression shouldBe false
    }

    @Test
    fun `isExpression is true once an operator is present`() {
        input("42+8").isExpression shouldBe true
    }

    @Test
    fun `evaluates a plain decimal`() {
        input("42.50").evaluate(sep) shouldBe 42.5
    }

    @Test
    fun `evaluates a comma decimal separator`() {
        AmountKeypadInput("42,50").evaluate(',') shouldBe 42.5
    }

    @Test
    fun `evaluates an expression honouring precedence`() {
        input("12+3*2").evaluate(sep) shouldBe 18.0
    }

    @Test
    fun `returns null for an incomplete expression`() {
        input("12+").evaluate(sep) shouldBe null
    }

    @Test
    fun `returns null for empty input`() {
        input("").evaluate(sep) shouldBe null
    }

    @Test
    fun `returns null for division by zero`() {
        input("12/0").evaluate(sep) shouldBe null
    }

    @Test
    fun `amountKeypadInputOf renders an amount with the given separator`() {
        amountKeypadInputOf(42.5, ',').text shouldBe "42,5"
    }

    @Test
    fun `amountKeypadInputOf maps null and zero to empty input`() {
        amountKeypadInputOf(null, '.').text shouldBe ""
        amountKeypadInputOf(0.0, '.').text shouldBe ""
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :shared:ui:core:testDebugUnitTest --tests "*AmountKeypadInputTest*"`
Expected: FAIL — unresolved reference `AmountKeypadInput`.

- [ ] **Step 4: Write the implementation**

Create `shared/ui/core/src/main/java/com/ivy/ui/component/amount/AmountKeypadInput.kt`:

```kotlin
package com.ivy.ui.component.amount

import androidx.compose.runtime.Immutable
import com.notkamui.keval.Keval

private const val Operators = "+-*/"

enum class AmountKeypadKey {
    Digit0, Digit1, Digit2, Digit3, Digit4,
    Digit5, Digit6, Digit7, Digit8, Digit9,
    Decimal, Backspace, Plus, Minus, Times, Divide,
}

private val AmountKeypadKey.digit: Char?
    get() = when (this) {
        AmountKeypadKey.Digit0 -> '0'
        AmountKeypadKey.Digit1 -> '1'
        AmountKeypadKey.Digit2 -> '2'
        AmountKeypadKey.Digit3 -> '3'
        AmountKeypadKey.Digit4 -> '4'
        AmountKeypadKey.Digit5 -> '5'
        AmountKeypadKey.Digit6 -> '6'
        AmountKeypadKey.Digit7 -> '7'
        AmountKeypadKey.Digit8 -> '8'
        AmountKeypadKey.Digit9 -> '9'
        else -> null
    }

private val AmountKeypadKey.operator: Char?
    get() = when (this) {
        AmountKeypadKey.Plus -> '+'
        AmountKeypadKey.Minus -> '-'
        AmountKeypadKey.Times -> '*'
        AmountKeypadKey.Divide -> '/'
        else -> null
    }

/**
 * The raw text on the keypad's display. May be a plain amount ("42.50") or an
 * arithmetic expression ("12+3*2"); [evaluate] resolves both.
 */
@Immutable
data class AmountKeypadInput(val text: String) {
    val isExpression: Boolean
        get() = text.drop(1).any { it in Operators }
}

fun amountKeypadInputOf(amount: Double?, decimalSeparator: Char): AmountKeypadInput {
    if (amount == null || amount == 0.0) return AmountKeypadInput("")
    val plain = if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        amount.toString()
    }
    return AmountKeypadInput(plain.replace('.', decimalSeparator))
}

@Suppress("ReturnCount")
fun AmountKeypadInput.press(
    key: AmountKeypadKey,
    decimalCountMax: Int,
    decimalSeparator: Char,
): AmountKeypadInput {
    key.digit?.let { digit ->
        if (decimalsExceeded(decimalCountMax, decimalSeparator)) return this
        return AmountKeypadInput(text + digit)
    }
    key.operator?.let { operator ->
        if (text.isEmpty()) return this
        val withoutTrailingOperator = text.trimEnd { it in Operators }
        if (withoutTrailingOperator.isEmpty()) return this
        return AmountKeypadInput(withoutTrailingOperator + operator)
    }
    return when (key) {
        AmountKeypadKey.Backspace -> AmountKeypadInput(text.dropLast(1))
        AmountKeypadKey.Decimal -> appendDecimal(decimalSeparator)
        else -> this
    }
}

private fun AmountKeypadInput.appendDecimal(decimalSeparator: Char): AmountKeypadInput {
    val segment = currentSegment()
    return when {
        segment.contains(decimalSeparator) -> this
        segment.isEmpty() -> AmountKeypadInput("${text}0$decimalSeparator")
        else -> AmountKeypadInput(text + decimalSeparator)
    }
}

private fun AmountKeypadInput.decimalsExceeded(
    decimalCountMax: Int,
    decimalSeparator: Char,
): Boolean {
    val segment = currentSegment()
    val separatorIndex = segment.indexOf(decimalSeparator)
    return separatorIndex >= 0 && segment.length - separatorIndex - 1 >= decimalCountMax
}

/** The number currently being typed — everything after the last operator. */
private fun AmountKeypadInput.currentSegment(): String =
    text.takeLastWhile { it !in Operators }

fun AmountKeypadInput.evaluate(decimalSeparator: Char): Double? {
    val normalized = text.replace(decimalSeparator, '.')
    if (normalized.isBlank()) return null
    return runCatching { Keval.eval(normalized) }
        .getOrNull()
        ?.takeIf { it.isFinite() }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :shared:ui:core:testDebugUnitTest --tests "*AmountKeypadInputTest*"`
Expected: PASS (19 tests). If `division by zero` returns `Infinity` rather than throwing, the `isFinite()` filter already handles it; if `12+` throws a Keval exception, `runCatching` handles it. Do not weaken a test to make it pass — fix the implementation.

- [ ] **Step 6: Compile check**

Run: `./gradlew :shared:ui:core:compileDebugKotlin`
Expected: PASS (detekt is skipped — see Global Constraints)

- [ ] **Step 7: Commit**

```bash
git add shared/ui/core/build.gradle.kts shared/ui/core/src/main/java/com/ivy/ui/component/amount/ shared/ui/core/src/test/java/com/ivy/ui/component/amount/
git commit -m "feat: add keypad input state with inline arithmetic"
```

---

### Task 3: `AmountKeypadSheet` component

The bottom sheet that replaces `EditBottomSheet`, `AmountModal` and `CalculatorModal`. Content is a separate public composable so Paparazzi can snapshot it.

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/amount/AmountKeypadSheet.kt`
- Modify: `shared/ui/core/src/main/res/values/strings.xml`
- Test: `shared/ui/core/src/test/java/com/ivy/ui/component/amount/AmountKeypadPaparazziTest.kt`

**Interfaces:**
- Consumes: `AmountKeypadInput`, `AmountKeypadKey`, `press`, `evaluate`, `amountKeypadInputOf` (Task 2).
- Produces:
  - `@Immutable data class KeypadAccountUi(val id: String, val name: String, val selected: Boolean)`
  - `@Composable fun AmountKeypadContent(currency: String, initialAmount: Double?, accounts: ImmutableList<KeypadAccountUi>, onAccountClick: (String) -> Unit, onAddAccountClick: () -> Unit, onDone: (Double) -> Unit, modifier: Modifier = Modifier, decimalCountMax: Int = 2)`
  - `@Composable fun AmountKeypadSheet(...same params..., onDismiss: () -> Unit)`

- [ ] **Step 1: Add strings**

In `shared/ui/core/src/main/res/values/strings.xml`, add (grep first — reuse any that already exist, and if `done` or `new_account` are already defined, delete the duplicate from this list):

```xml
    <string name="done">Done</string>
    <string name="new_account">New account</string>
    <string name="backspace">Backspace</string>
    <string name="edit_amount">Edit amount</string>
```

- [ ] **Step 2: Write the component**

Create `shared/ui/core/src/main/java/com/ivy/ui/component/amount/AmountKeypadSheet.kt`. Structure — a column of: amount display, account chips (only when `accounts` is non-empty), key grid, Done button.

```kotlin
package com.ivy.ui.component.amount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList
import java.text.DecimalFormatSymbols
```

Requirements the implementation must satisfy — decide layout details yourself, but all of these are non-negotiable:

1. `decimalSeparator` is `DecimalFormatSymbols.getInstance().decimalSeparator`, read once via `remember`.
2. Input state: `var input by remember(initialAmount) { mutableStateOf(amountKeypadInputOf(initialAmount, decimalSeparator)) }`.
3. Display row: `input.text` (or `"0"` when empty) in `MaterialTheme.typography.displayMedium`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`, followed by `currency` in `titleMedium` / `onSurfaceVariant`. When `input.isExpression`, show the evaluated result beneath in `bodyMedium` / `onSurfaceVariant` (blank when it does not evaluate).
4. Account chips: `LazyRow` of `FilterChip(selected = account.selected, onClick = { onAccountClick(account.id) }, label = { Text(account.name) })`, then a trailing chip labelled `R.string.new_account` calling `onAddAccountClick`. The whole row is omitted when `accounts.isEmpty()`.
5. Key grid: four columns, using `Column` of `Row`s (not `LazyVerticalGrid` — the grid is fixed and must not scroll), 8dp gaps, each key 52dp tall and `weight(1f)` wide, `MaterialTheme.shapes.large` on `surfaceContainerHigh`, digit labels in `headlineSmall`. Layout, row by row:

   ```
   7 8 9 ÷
   4 5 6 ×
   1 2 3 −
   . 0 ⌫ +
   ```

   The backspace key uses `Icons.AutoMirrored.Outlined.Backspace` with `contentDescription = stringResource(R.string.backspace)`; every other key is text. Operator keys use `onSurfaceVariant` content color to read as secondary.
6. Each key press calls `input = input.press(key, decimalCountMax, decimalSeparator)`.
7. Done: full-width `Button` labelled `R.string.done`, `enabled = input.evaluate(decimalSeparator) != null`, calling `onDone(value)` with the evaluated value.
8. The content column has `navigationBarsPadding()` and 16dp horizontal padding.
9. `AmountKeypadSheet` wraps `AmountKeypadContent` in `ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true))` and nothing else. Annotate with `@OptIn(ExperimentalMaterial3Api::class)`.

- [ ] **Step 3: Add previews**

Two `@Preview` composables in the same file, both wrapped in `IvyMaterial3Theme(isTrueBlack = false)` (import `com.ivy.design.system.IvyMaterial3Theme`), matching the convention in `SettingsItem.kt`: one with a plain amount and three accounts, one mid-expression (`initialAmount = null`, no accounts). Plus a screenshot entry point:

```kotlin
/** For screenshot testing */
@Composable
fun AmountKeypadUiTest(withAccounts: Boolean) { /* renders AmountKeypadContent */ }
```

- [ ] **Step 4: Write the screenshot test**

Create `shared/ui/core/src/test/java/com/ivy/ui/component/amount/AmountKeypadPaparazziTest.kt`:

```kotlin
package com.ivy.ui.component.amount

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AmountKeypadPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun `snapshot amount keypad with accounts`() {
        snapshot(theme) {
            AmountKeypadUiTest(withAccounts = true)
        }
    }

    @Test
    fun `snapshot amount keypad without accounts`() {
        snapshot(theme) {
            AmountKeypadUiTest(withAccounts = false)
        }
    }
}
```

Check `shared/ui/core/src/test/java/com/ivy/ui/component/transaction/TransactionComponentsPaparazziTest.kt` for how it wraps content on a `Surface` and copy that wrapping — snapshots must not render on a transparent background.

- [ ] **Step 5: Record and verify the goldens**

Run: `./gradlew :shared:ui:core:recordPaparazziDebug --tests "*AmountKeypadPaparazziTest*"`
Then: `./gradlew :shared:ui:core:verifyPaparazziDebug`
Expected: PASS. Open the recorded PNGs and confirm: keys aligned in a 4-column grid, no clipped text, both themes legible.

- [ ] **Step 6: Compile check**

Run: `./gradlew :shared:ui:core:compileDebugKotlin`
Expected: PASS (detekt is skipped — see Global Constraints)

- [ ] **Step 7: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/ui/component/amount/ shared/ui/core/src/test/java/com/ivy/ui/component/amount/ shared/ui/core/src/main/res/values/strings.xml shared/ui/core/src/test/snapshots/
git commit -m "feat: add M3 amount keypad sheet"
```

---

### Task 4: `PickerSheet` component

One generic picker used for both categories and accounts, rendering items as segmented tonal blocks with the corner rhythm already shipped on the transaction list.

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/picker/PickerSheet.kt`
- Test: `shared/ui/core/src/test/java/com/ivy/ui/component/picker/PickerPaparazziTest.kt`

**Interfaces:**
- Consumes: `CategoryIconBubble` (Task 1), `TransactionItemPosition` + `toShape()` from `com.ivy.ui.component.transaction`.
- Produces:
  - `@Immutable data class PickerItemUi(val id: String, val title: String, val supportingText: String? = null, val color: Color? = null, val selected: Boolean = false)`
  - `@Composable fun PickerContent(title: String, items: ImmutableList<PickerItemUi>, onItemClick: (String) -> Unit, addLabel: String, onAddClick: () -> Unit, modifier: Modifier = Modifier, icon: @Composable (PickerItemUi) -> Unit = {})`
  - `@Composable fun PickerSheet(...same params..., onDismiss: () -> Unit)`

- [ ] **Step 1: Write the component**

Requirements:

1. `PickerContent` is a `LazyColumn` with 4dp `verticalArrangement` spacing and 16dp horizontal padding, headed by a non-scrolling title `Text` in `titleLarge` with 24dp start / 8dp bottom padding.
2. Each item is a `Row` on `MaterialTheme.colorScheme.surfaceContainer`, clipped to `position.toShape()` where `position` is computed from the item's index across the full list **including** the trailing add-entry: single item → `TransactionItemPosition.Single`, first → `First`, last → `Last`, otherwise `Middle`.
3. Item content: `CategoryIconBubble(categoryColor = item.color, size = 32.dp) { icon(item) }`, then a `Column` with `title` (`bodyLarge`) and `supportingText` (`bodySmall`, `onSurfaceVariant`, omitted when null), then — when `item.selected` — a trailing `Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)`.
4. The add entry is the last block: bubble with `Icons.Outlined.Add` and `color = null`, title `addLabel`, calling `onAddClick`.
5. Row padding 16dp horizontal / 12dp vertical; whole block `clickable`.
6. Content has `navigationBarsPadding()`.
7. `PickerSheet` wraps `PickerContent` in `ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true))`, `@OptIn(ExperimentalMaterial3Api::class)`.

- [ ] **Step 2: Add previews and the screenshot entry point**

A category-shaped preview (4 items with colors, one selected, "New category" add label) and an account-shaped preview (3 items with currency supporting text, no colors, "New account"), both in `IvyMaterial3Theme(isTrueBlack = false)`. Then:

```kotlin
/** For screenshot testing */
@Composable
fun PickerUiTest(accounts: Boolean) { /* renders the matching PickerContent */ }
```

- [ ] **Step 3: Write the screenshot test**

Create `shared/ui/core/src/test/java/com/ivy/ui/component/picker/PickerPaparazziTest.kt`, same shape as `AmountKeypadPaparazziTest` from Task 3, with two tests: `snapshot category picker` (`PickerUiTest(accounts = false)`) and `snapshot account picker` (`PickerUiTest(accounts = true)`), both parameterised over `PaparazziTheme` and wrapped on a `Surface` the same way.

- [ ] **Step 4: Record and verify**

Run: `./gradlew :shared:ui:core:recordPaparazziDebug --tests "*PickerPaparazziTest*"`
Then: `./gradlew :shared:ui:core:verifyPaparazziDebug`
Expected: PASS. Confirm in the PNGs: corner rhythm reads as one grouped block (large corners only at the ends), 4dp gaps, check mark on the selected item only.

- [ ] **Step 5: Compile check**

Run: `./gradlew :shared:ui:core:compileDebugKotlin`
Expected: PASS (detekt is skipped — see Global Constraints)

- [ ] **Step 6: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/ui/component/picker/ shared/ui/core/src/test/java/com/ivy/ui/component/picker/ shared/ui/core/src/test/snapshots/
git commit -m "feat: add M3 picker sheet with segmented blocks"
```

---

### Task 5: Screen decision logic (pure Kotlin, TDD)

Everything about the screen that is a decision rather than a layout: which commit action the bottom bar shows, which overflow items exist, and how legacy models become `PickerItemUi`. Written test-first so the rewrite in Task 8 can't quietly change behaviour.

**Files:**
- Create: `feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionUiMapper.kt`
- Test: `feature/edit-transaction/src/test/java/com/ivy/transaction/EditTransactionUiMapperTest.kt`

**Interfaces:**
- Consumes: `PickerItemUi` (Task 4), `TransactionType`, `Account`, `Category` (existing legacy types — allowed here, this is the feature module).
- Produces:
  - `enum class CommitAction { Add, Save, Pay, Get }`
  - `fun commitAction(isNewTransaction: Boolean, hasDueDate: Boolean, hasChanges: Boolean, type: TransactionType): CommitAction`
  - `enum class OverflowItem { Duplicate, Delete, MakePlanned }`
  - `fun overflowItems(isNewTransaction: Boolean, isLoanRecord: Boolean, type: TransactionType, hasDateTime: Boolean, hasDueDate: Boolean): List<OverflowItem>`
  - `fun Account.toPickerItem(selectedId: UUID?): PickerItemUi`
  - `fun Category.toPickerItem(selectedId: UUID?): PickerItemUi`

- [ ] **Step 1: Write the failing tests**

Create `feature/edit-transaction/src/test/java/com/ivy/transaction/EditTransactionUiMapperTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :feature:edit-transaction:testDebugUnitTest --tests "*EditTransactionUiMapperTest*"`
Expected: FAIL — unresolved reference `commitAction`. If it fails because kotest isn't on the test classpath, add `testImplementation(libs.bundles.kotest)` to `feature/edit-transaction/build.gradle.kts` (check `feature/settings/build.gradle.kts` for the exact accessor name used in this project).

- [ ] **Step 3: Write the implementation**

Create `feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionUiMapper.kt`. The rules, taken from the current screen:

```kotlin
package com.ivy.transaction

import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.component.picker.PickerItemUi
import com.ivy.wallet.ui.theme.toComposeColor
import java.util.UUID

enum class CommitAction { Add, Save, Pay, Get }

fun commitAction(
    isNewTransaction: Boolean,
    hasDueDate: Boolean,
    hasChanges: Boolean,
    type: TransactionType,
): CommitAction = when {
    isNewTransaction -> CommitAction.Add
    !hasDueDate || hasChanges -> CommitAction.Save
    type == TransactionType.EXPENSE -> CommitAction.Pay
    else -> CommitAction.Get
}

enum class OverflowItem { Duplicate, Delete, MakePlanned }

fun overflowItems(
    isNewTransaction: Boolean,
    isLoanRecord: Boolean,
    type: TransactionType,
    hasDateTime: Boolean,
    hasDueDate: Boolean,
): List<OverflowItem> = buildList {
    if (!isNewTransaction) {
        add(OverflowItem.Duplicate)
        if (!isLoanRecord) add(OverflowItem.Delete)
    }
    val plannedAvailable = type != TransactionType.TRANSFER && !hasDateTime && !hasDueDate
    if (isNewTransaction && plannedAvailable) add(OverflowItem.MakePlanned)
}

fun Account.toPickerItem(selectedId: UUID?): PickerItemUi = PickerItemUi(
    id = id.toString(),
    title = name,
    supportingText = currency,
    color = color.toComposeColor(),
    selected = id == selectedId,
)

fun Category.toPickerItem(selectedId: UUID?): PickerItemUi = PickerItemUi(
    id = id.value.toString(),
    title = name.value,
    color = color.value.toComposeColor(),
    selected = id.value == selectedId,
)
```

Check `Account`/`Category` property names against `com.ivy.legacy.datamodel.Account` and `com.ivy.data.model.Category` before writing — `Category.name` is a `NotBlankTrimmedString` and `Category.color` is a `ColorInt`; adjust the `.value` accessors to whatever those types actually expose. `toComposeColor()` lives in `com.ivy.wallet.ui.theme` and takes an `Int`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :feature:edit-transaction:testDebugUnitTest --tests "*EditTransactionUiMapperTest*"`
Expected: PASS (10 tests).

- [ ] **Step 5: Commit**

```bash
git add feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionUiMapper.kt feature/edit-transaction/src/test/java/com/ivy/transaction/EditTransactionUiMapperTest.kt feature/edit-transaction/build.gradle.kts
git commit -m "feat: add edit-transaction commit action and overflow rules"
```

---

### Task 6: Screen header composable

Type selector, amount headline, title field and suggestion chips — the top of the screen, independently previewable.

**Files:**
- Create: `feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionHeader.kt`
- Modify: `shared/ui/core/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `LocalIvyExtendedColors` (`com.ivy.design.system`), existing strings `expense`, `income`, `transfer`, `expense_title`, `income_title`, `transfer_title`.
- Produces:
  - `@Composable fun TransactionTypeSelector(type: TransactionType, onTypeChange: (TransactionType) -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun AmountHeadline(amountText: String, currency: String, type: TransactionType, supportingText: String?, onClick: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun TitleField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, type: TransactionType, suggestions: ImmutableSet<String>, onSuggestionClick: (String) -> Unit, onNext: () -> Unit, focusRequester: FocusRequester, modifier: Modifier = Modifier)`

- [ ] **Step 1: Add strings**

```xml
    <string name="select_account">Select account</string>
    <string name="date_and_time">Date &amp; time</string>
    <string name="set_date_and_time">Set date &amp; time</string>
    <string name="add_tags">Add tags</string>
    <string name="custom_exchange_rate">Custom exchange rate</string>
    <string name="reset_exchange_rate">Reset exchange rate</string>
    <string name="make_it_planned">Make it planned</string>
    <string name="no_category">No category</string>
    <string name="new_category">New category</string>
```

Grep first; `no_category`, `new_category` and `duplicate` may already exist. Reuse rather than duplicate. `date_and_time` and the rest are used in Task 7 — add them all now so Task 7 doesn't touch this file.

- [ ] **Step 2: Write `TransactionTypeSelector`**

`SingleChoiceSegmentedButtonRow` (`@OptIn(ExperimentalMaterial3Api::class)`), `fillMaxWidth()`, 24dp horizontal padding, one `SegmentedButton` per `TransactionType.entries` in the order EXPENSE, INCOME, TRANSFER, with `shape = SegmentedButtonDefaults.itemShape(index, count)`, `selected = type == entry`, `onClick = { onTypeChange(entry) }`, label = the existing `R.string.expense` / `income` / `transfer`.

- [ ] **Step 3: Write `AmountHeadline`**

A `Column` with `Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.edit_amount), onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp)` containing:

- a `Row` with `Alignment.Bottom`: `amountText` in `displayLarge` colored by type — `TransactionType.EXPENSE` → `colorScheme.onSurface`, `INCOME` → `LocalIvyExtendedColors.current.income`, `TRANSFER` → `colorScheme.primary` — then 8dp spacer, then `currency` in `headlineSmall` / `onSurfaceVariant`.
- `supportingText` when non-null, in `labelMedium` / `onSurfaceVariant`.

`amountText` arrives pre-formatted; this composable never formats.

- [ ] **Step 4: Write `TitleField`**

- `TextField` with `Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(focusRequester)`, `textStyle = MaterialTheme.typography.headlineSmall`, `placeholder` = the per-type title string, `singleLine = true`, `colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant, unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant)`, `keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)`, `keyboardActions = KeyboardActions(onNext = { onNext() })`.
- Below it, when `suggestions.isNotEmpty()`: a `FlowRow` (`androidx.compose.foundation.layout.FlowRow`, `@OptIn(ExperimentalLayoutApi::class)`) with 8dp gaps and 24dp horizontal padding of `SuggestionChip(onClick = { onSuggestionClick(it) }, label = { Text(it) })`, capped at `SUGGESTIONS_LIMIT` (import from `com.ivy.wallet.domain.deprecated.logic.SmartTitleSuggestionsLogic`'s package — it's a top-level constant, `com.ivy.wallet.domain.deprecated.logic.SUGGESTIONS_LIMIT`).

- [ ] **Step 5: Add previews**

Three `@Preview`s wrapped in `IvyPreview(dark = false)` (`com.ivy.navigation.IvyPreview`, the wrapper `SettingsScreen.kt` uses) inside a `Surface(color = MaterialTheme.colorScheme.background)`: an expense header with amount and suggestions, an income header, a transfer header. Verify them by running the preview-less check in the next step; visual confirmation happens in Task 8's screen snapshots.

- [ ] **Step 6: Compile check**

Run: `./gradlew :feature:edit-transaction:compileDebugKotlin`
Expected: PASS (detekt is skipped — see Global Constraints)

- [ ] **Step 7: Commit**

```bash
git add feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionHeader.kt shared/ui/core/src/main/res/values/strings.xml
git commit -m "feat: add M3 header components for edit transaction"
```

---

### Task 7: Attribute rows

Every attribute row, built on the shipped `SettingsItem`, plus the exchange-rate row.

**Files:**
- Create: `feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionRows.kt`

**Interfaces:**
- Consumes: `SettingsItem` (`com.ivy.ui.component.settings`), `CategoryIconBubble` (Task 1), `LocalIvyExtendedColors`.
- Produces:
  - `@Composable fun CategoryRow(categoryName: String?, categoryColor: Color?, categoryIcon: String?, onClick: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun AccountRow(accountName: String?, currency: String?, label: String, onClick: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun DateTimeRow(dateTimeText: String?, onClick: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun DueDateRow(dueDateText: String, onClick: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun DescriptionRow(description: String?, onClick: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun TagsRow(tagCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun ExchangeRateRow(rateText: String, onClick: () -> Unit, onReset: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the rows**

Each row is a thin `SettingsItem` call. `title` is the value or the empty-state prompt; `description` is the attribute name; `icon` is the outlined Material icon (the repo standardised on `Icons.Outlined.*` in commit `69aac85`):

| Row | title | description | icon |
| --- | --- | --- | --- |
| `CategoryRow` | `categoryName ?: stringResource(R.string.choose_category)` | `stringResource(R.string.category)` | `CategoryIconBubble(categoryColor, size = 24.dp) { ItemIconSDefaultIcon(iconName = categoryIcon, defaultIcon = R.drawable.ic_custom_category_s, tint = LocalContentColor.current) }` passed via `SettingsItem`'s `trailing`-free path — see step 2 |
| `AccountRow` | `accountName ?: stringResource(R.string.select_account)` | `label` (caller passes "Account", "From" or "To"), suffixed `" · $currency"` when currency is non-null | `Icons.Outlined.AccountBalanceWallet` |
| `DateTimeRow` | `dateTimeText ?: stringResource(R.string.set_date_and_time)` | `stringResource(R.string.date_and_time)` | `Icons.Outlined.Schedule` |
| `DueDateRow` | `dueDateText` | `stringResource(R.string.due_date)` (grep — add if missing) | `Icons.Outlined.Event` |
| `DescriptionRow` | `description ?: stringResource(R.string.add_description)` | `stringResource(R.string.description)` | `Icons.Outlined.Notes` |
| `TagsRow` | `if (tagCount == 0) stringResource(R.string.add_tags) else pluralised count` | `stringResource(R.string.tags)` | `Icons.Outlined.Sell` |

Empty-state titles use `MaterialTheme.colorScheme.onSurfaceVariant` via `SettingsItem`'s `titleColor`; filled values leave `titleColor` unspecified.

For the tag count, add a plural resource rather than string concatenation:

```xml
    <plurals name="tag_count">
        <item quantity="one">%d tag</item>
        <item quantity="other">%d tags</item>
    </plurals>
```

- [ ] **Step 2: Handle the category row's icon**

`SettingsItem`'s `icon` parameter is an `ImageVector?`, which cannot carry the tonal bubble. Rather than widening `SettingsItem`'s API, build `CategoryRow` as its own `Row` mirroring `SettingsItem`'s metrics exactly — `fillMaxWidth`, `clickable`, `padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)`, icon then 24dp gap, `titleLarge` at 20sp for the title, `bodyMedium` / `onSurfaceVariant` for the description — with `CategoryIconBubble(categoryColor = categoryColor, size = 24.dp)` in the leading slot. Add a short comment saying why it doesn't reuse `SettingsItem`.

- [ ] **Step 3: Write `ExchangeRateRow`**

A `Row` on `LocalIvyExtendedColors.current.warningContainer`, `MaterialTheme.shapes.large`, 16dp horizontal margins, containing: `Icons.Outlined.CurrencyExchange` tinted `onWarningContainer`, a `Column` with `stringResource(R.string.custom_exchange_rate)` (`titleMedium`) and `rateText` (`bodySmall`), and a trailing `IconButton(onClick = onReset)` with `Icons.Outlined.Refresh` and `contentDescription = stringResource(R.string.reset_exchange_rate)`. The row itself is `clickable(onClick = onClick)`. All content colors are `onWarningContainer`.

- [ ] **Step 4: Add previews**

One `@Preview` in `IvyPreview(dark = false)` + `Surface` rendering every row in a column, filled state; one rendering the empty states. These get snapshotted as part of the screen in Task 8.

- [ ] **Step 5: Compile check**

Run: `./gradlew :feature:edit-transaction:compileDebugKotlin`
Expected: PASS (detekt is skipped — see Global Constraints)

- [ ] **Step 6: Commit**

```bash
git add feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionRows.kt shared/ui/core/src/main/res/values/strings.xml
git commit -m "feat: add M3 attribute rows for edit transaction"
```

---

### Task 8: Rewrite the screen

Compose the pieces into the screen, wire every event, flip the screen off the legacy design system, and snapshot it.

**Files:**
- Modify: `feature/edit-transaction/src/main/java/com/ivy/transaction/EditTransactionScreen.kt` (rewrite)
- Modify: `shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt:26-34`
- Modify: `app/src/main/java/com/ivy/IvyNavGraph.kt:70` (only if the receiver type changes)
- Test: `feature/edit-transaction/src/test/java/com/ivy/transaction/EditTransactionPaparazziTest.kt`

**Interfaces:**
- Consumes: everything from Tasks 1–7.
- Produces: `@Composable fun EditTransactionUiTest(isDark: Boolean, state: EditTransactionPreviewState)` for screenshot testing, where `EditTransactionPreviewState` is an enum `{ NewExpense, EditFilled, TransferWithRate }`.

- [ ] **Step 1: Rewrite the screen**

Keep the existing `EditTransactionScreen(screen)` entry point and its `viewModel.uiState()` / `viewModel.onEvent(...)` wiring — that part of the file is fine. Replace the private `UI(...)` composable's body.

Structure:

```kotlin
@Composable
private fun EditTransactionUi(
    // same parameters as today's UI(), minus the BoxWithConstraintsScope receiver
) {
    Scaffold(
        topBar = { /* TopAppBar: BackButton + overflow DropdownMenu from overflowItems(...) */ },
        bottomBar = { /* full-width Button labelled by commitAction(...) */ },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            TransactionTypeSelector(...)      // hidden when loanData.isLoanRecord
            AmountHeadline(...)
            // loan caption, when loanData.loanCaption != null, bodyMedium/onSurfaceVariant, 24dp padding
            TitleField(...)
            CategoryRow(...)
            AccountRow(...)                    // or From/To pair when type == TRANSFER
            DateTimeRow(...)
            DueDateRow(...)                    // only when dueDate != null
            DescriptionRow(...)
            TagsRow(...)
            ExchangeRateRow(...)               // only when TRANSFER && customExchangeRateState.showCard
            Spacer(Modifier.height(24.dp))
        }
    }
    // sheets + dialogs
}
```

Hard requirements:

1. **Delete** every import of `com.ivy.design.l0_system.*`, `com.ivy.wallet.ui.edit.core.*`, `com.ivy.wallet.ui.theme.modal.*` (except the three kept legacy modals), `com.ivy.legacy.ui.component.edit.*`, `AddPrimaryAttributeButton`, `CustomExchangeRateCard`, `ChangeTransactionTypeModal`, and the `Spacer(Modifier.height(600.dp))` scroll hack together with the `customExchangeRatePosition` / `onGloballyPositioned` / `animateScrollTo` block.
2. **Keep calling** `CategoryModal`, `AccountModal` and `ShowTagModal` exactly as today — launched from `PickerSheet`'s add entry and the tags row respectively.
3. `statusBarsPadding()` / `navigationBarsPadding()` are no longer applied manually — `Scaffold` handles insets. Keep `navigationBarsPadding()` on the bottom bar only if the snapshot shows it is needed.
4. **Auto-open the keypad**: keep the existing `onScreenStart { if (screen.initialTransactionId == null) amountModalShown = true }`, now driving `AmountKeypadSheet`'s visibility.
5. **Focus chain**, unchanged from today:
   - keypad `onDone` → `onAmountChange(it)`; then if `shouldFocusCategory(category)` show the category sheet, else if `shouldFocusTitle(titleTextFieldValue, transactionType)` `titleFocus.requestFocus()`.
   - category picker `onItemClick` → `onCategoryChange(it)`; then if `shouldFocusTitle(...)` request title focus, else if `shouldFocusAmount(amount)` show the keypad.
   - title field `onNext` → if `shouldFocusAmount(amount)` show the keypad, else `onSave(true)`.
   The three `shouldFocusX` helpers already exist at the bottom of the file — keep them.
6. **Account currency guard**: the account picker's `onItemClick` keeps today's rule — when `loanData.isLoan && account?.currency != selected.currency`, stash the account and raise the confirmation dialog instead of emitting `OnAccountChanged`.
7. **Dialogs**: description → `TextInputDialog`; delete confirm and account-change confirm → `AlertDialog` with `confirmButton`/`dismissButton` `TextButton`s (reuse the existing `confirm_deletion`, `transaction_confirm_deletion_description`, `confirm_account_change`, `confirm_account_change_description` strings); background processing → `ProgressDialog`.
8. **Exchange rate**: the row's `onClick` opens `AmountKeypadSheet` with `accounts = persistentListOf()`, `currency = ""`, `decimalCountMax = 4`, `initialAmount = customExchangeRateState.exchangeRate`, `onDone = onExchangeRateChange`. The reset button emits `onExchangeRateChange(null)`.
9. Every row of the spec's parity table must have a live trigger. Walk the table before moving to the next step.

- [ ] **Step 2: Flip the screen off the legacy design system**

In `shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt`, `EditTransactionScreen`:

```kotlin
    override val isLegacy: Boolean
        get() = false
```

If `EditTransactionUi` no longer needs `BoxWithConstraintsScope` (it shouldn't — nothing in the new screen measures the window), drop the receiver from the public `EditTransactionScreen` composable too and remove the now-unneeded `@ExperimentalFoundationApi`. `IvyNavGraph.kt:70` calls it from inside a `BoxWithConstraintsScope`, which still compiles for a non-receiver function, so that line needs no change — verify by compiling.

- [ ] **Step 3: Add the screenshot entry point**

At the bottom of `EditTransactionScreen.kt`, following the `SettingsUiTest` convention:

```kotlin
enum class EditTransactionPreviewState { NewExpense, EditFilled, TransferWithRate }

/** For screenshot testing */
@Composable
fun EditTransactionUiTest(isDark: Boolean, state: EditTransactionPreviewState) {
    EditTransactionPreview(dark = isDark, state = state)
}
```

`EditTransactionPreview` is a `@Preview`-annotated private composable wrapping `EditTransactionUi` in `IvyPreview(dark = dark)` with hardcoded arguments per state:

- `NewExpense` — no title, amount `0.0`, no category, account "Revolut" (USD), no date, empty callbacks.
- `EditFilled` — title "Groceries", amount `42.50`, category "Food & Drink", account "Revolut", a date-time, description "Weekly shop", 2 tags.
- `TransferWithRate` — `TransactionType.TRANSFER`, amount `250.0`, from "Revolut" (USD), to "N26" (EUR), `CustomExchangeRateState(showCard = true, exchangeRate = 0.9176, toCurrencyCode = "EUR", convertedAmount = 229.4)` — check the real constructor before writing.

- [ ] **Step 4: Write the screenshot test**

Create `feature/edit-transaction/src/test/java/com/ivy/transaction/EditTransactionPaparazziTest.kt`:

```kotlin
package com.ivy.transaction

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class EditTransactionPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun `snapshot new expense`() {
        snapshot(theme) {
            EditTransactionUiTest(
                isDark = theme == PaparazziTheme.Dark,
                state = EditTransactionPreviewState.NewExpense,
            )
        }
    }

    @Test
    fun `snapshot filled transaction`() {
        snapshot(theme) {
            EditTransactionUiTest(
                isDark = theme == PaparazziTheme.Dark,
                state = EditTransactionPreviewState.EditFilled,
            )
        }
    }

    @Test
    fun `snapshot transfer with custom rate`() {
        snapshot(theme) {
            EditTransactionUiTest(
                isDark = theme == PaparazziTheme.Dark,
                state = EditTransactionPreviewState.TransferWithRate,
            )
        }
    }
}
```

- [ ] **Step 5: Record and review the goldens**

Run: `./gradlew :feature:edit-transaction:recordPaparazziDebug`
Then: `./gradlew :feature:edit-transaction:verifyPaparazziDebug`
Expected: PASS. Then **open all six PNGs** and check: no legacy gradient or all-caps text anywhere; amount headline dominant; rows aligned on a single 24dp start edge; bottom button not clipped; dark theme legible.

- [ ] **Step 6: Compile the app module**

Run: `./gradlew :app:compileDebugKotlin`
Expected: PASS — this catches a broken `IvyNavGraph` call site.

- [ ] **Step 7: Commit**

```bash
git add feature/edit-transaction/ shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt app/src/main/java/com/ivy/IvyNavGraph.kt
git commit -m "feat: rebuild add transaction screen in Material 3"
```

---

### Task 9: Full verification

**Files:** none created; fixes only.

- [ ] **Step 1: Confirm no legacy edit components remain in the module**

Run: `grep -rn "EditBottomSheet\|AmountModal\|CalculatorModal\|ChooseCategoryModal\|DescriptionModal\|ChangeTransactionTypeModal\|CustomExchangeRateCard\|AddPrimaryAttributeButton\|DeleteModal\|ProgressModal\|com.ivy.design.l0_system" feature/edit-transaction/src`
Expected: no matches. `CategoryModal`, `AccountModal` and `ShowTagModal` are expected to still appear and are not part of this grep.

- [ ] **Step 2: Unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 3: Screenshot tests**

Run: `./gradlew verifyPaparazziDebug`
Expected: PASS

- [ ] **Step 4: Static analysis**

`detekt` cannot run here (jitpack blocked — see Global Constraints); it runs in CI on push. Run `./gradlew lint` and report the result. If `lint` itself fails on a blocked dependency, record that and move on — do not work around the egress policy. If `lint` runs and reports new baseline entries, fix the code rather than regenerating the baseline.

- [ ] **Step 5: Compose stability**

Run the check the `compose_stability.yml` workflow runs (read that file for the exact task name) and confirm no new unstable-parameter or value-returning-composable findings for the new files.

- [ ] **Step 6: Commit any fixes**

```bash
git add -A
git commit -m "fix: address lint findings in the transaction redesign"
```

Skip this step if steps 1–5 were clean.

---

## Self-Review

**Spec coverage:** Screen shell → Task 8. Top bar + overflow → Tasks 5 (rules) and 8 (UI). Type selector, amount headline, title field + suggestions → Task 6. Attribute rows → Task 7. Exchange-rate row → Task 7. Bottom bar → Tasks 5 and 8. `AmountKeypadSheet` → Tasks 2 and 3. `CategoryPickerSheet` / `AccountPickerSheet` → Task 4 (one generic `PickerSheet`, two usages in Task 8). `CategoryIconBubble` reuse → Task 1. Behaviour-parity table → Task 8 step 1 requirement 9, re-checked in Task 9. Verification → Tasks 3, 4, 8, 9. `isLegacy = false` → Task 8 step 2.

**Type consistency:** `PickerItemUi` is produced in Task 4 and consumed in Tasks 5 and 8. `AmountKeypadInput`/`AmountKeypadKey`/`press`/`evaluate`/`amountKeypadInputOf` are produced in Task 2 and consumed in Task 3 only. `CommitAction`/`OverflowItem` are produced in Task 5 and consumed in Task 8. `KeypadAccountUi` is produced in Task 3 and consumed in Task 8. `CategoryIconBubble(categoryColor, modifier, size, icon)` is produced in Task 1 and consumed in Tasks 4 and 7 with the same signature.

**Known judgement calls left to the implementer**, each with an explicit acceptance criterion rather than a free choice: exact keypad key sizing (criterion: 4-column grid, no clipped labels in either theme), whether the bottom bar needs `navigationBarsPadding()` (criterion: not clipped in the snapshot), and the exact `Account`/`Category` property accessors in Task 5 (criterion: compiles against the real types).
