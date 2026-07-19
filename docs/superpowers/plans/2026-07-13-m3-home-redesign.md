# M3 Home Screen Redesign (M3 Phase 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle the Home tab and its shared shell (bottom nav, add-transaction FAB, MoreMenu) to Material 3 Expressive, replacing hand-built gradients/animations with stock M3 components where one now exists, while keeping `MainScreen.isLegacy = true`.

**Architecture:** Pure view-layer rebuild — no ViewModel/state/navigation contract changes. `HomeHeader.kt` and `CustomerJourney.kt` swap legacy `UI.colors`/`UI.typo`/`Gradient` tokens for `MaterialTheme.colorScheme`/`typography`/`shapes`. `HomeMoreMenu.kt`'s custom circular-reveal drawer becomes a stock `ModalBottomSheet` wrapping a separately-testable content composable. `MainBottomBar.kt`'s hand-positioned FAB becomes a stock `FloatingActionButtonMenu`/`ToggleFloatingActionButton`, and its tab row becomes a stock `NavigationBar`. Spec: `docs/superpowers/specs/2026-07-13-m3-home-redesign-design.md`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 (Expressive, alpha BOM), Hilt, Paparazzi (screenshots).

## Global Constraints

- Every `./gradlew` call needs: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` first. Run from the repo root (the worktree root).
- Commits: single-line conventional commits (`feat:`/`fix:`/`test:`/`docs:`/`refactor:`), no bodies. **Never push** — the user pushes manually.
- No new detekt or Android Lint baseline entries. `./gradlew detekt` must pass after every task.
- Detekt Compose rules are active: public composables need a `modifier: Modifier = Modifier` parameter; previews must be private (except the `*UiTest` screenshot entry points); required params before optional ones.
- The `ComposeParameterOrder` Android Lint check is disabled project-wide (crashes under the current toolchain) — do not re-enable it.
- **Paparazzi cannot capture `AlertDialog` or `ModalBottomSheet` content directly** — both render through `androidx.compose.ui.window.Dialog`-like separate windows (verified against `ModalBottomSheet.kt`'s `ModalBottomSheetDialog` in the pinned `material3:1.5.0-alpha23` sources). Where this plan adds one, the actual visual content is extracted into its own composable and Paparazzi-tested directly (unwrapped); the `AlertDialog`/`ModalBottomSheet` wrapper itself gets a `@Preview` only.
- **`com.ivy.legacy.rootScreen()` does an unchecked cast** (`LocalContext.current as RootScreen`) and throws under Paparazzi/any host whose `Context` isn't a real `RootScreen` Activity. `CustomerJourney.kt` already guards this with `if (LocalContext.current is RootScreen)`. Any new code that needs `rootScreen()` inside a composable that will be Paparazzi-tested must capture `val context = LocalContext.current` at composition time and defer the `context is RootScreen` check into the click lambda — never call `rootScreen()` unconditionally at composition time in code that might render under test.
- `feature/home` and `feature/main`'s existing Paparazzi tests for legacy (`isLegacy = true`) screens wrap content in `com.ivy.legacy.IvyWalletPreview` (which itself provides `NavigationRoot`/`LocalNavigation` and the legacy `IvyContext`), not the lighter `com.ivy.navigation.IvyPreview` used by already-non-legacy screens like Settings. Match whichever convention the file already uses.
- String resources live in `shared/ui/core/src/main/res/values/strings.xml` (imported everywhere as `com.ivy.ui.R`). English only; translated `values-*` files are out of scope. `R.string.quick_access`, `R.string.cancel`, `R.string.confirm`, `R.string.home`, `R.string.accounts`, and all existing MoreMenu quick-access labels already exist — verified directly, do not re-add them.

---

## File Structure

| File | Responsibility |
|---|---|
| `feature/home/src/main/java/com/ivy/home/HomeHeader.kt` (rewrite) | Sticky greeting/period row + income/expense/cashflow cards restyled to M3 tokens; drops the now-dead `percentExpanded`/shadow plumbing |
| `feature/home/src/main/java/com/ivy/home/HomeTab.kt` (modify, 2 separate edits) | Task 1: drop the now-removed `percentExpanded` argument. Task 3: skip-all-planned confirmation swapped from legacy `DeleteModal` to stock M3 `AlertDialog` |
| `feature/home/src/main/java/com/ivy/home/customerjourney/CustomerJourney.kt` (modify) | `CustomerJourneyCard` restyled to a flat M3 card (data-driven per-card color kept, gradient brush + shadow dropped) |
| `feature/home/src/test/java/com/ivy/home/customerjourney/CustomerJourneyPaparazziTest.kt` (new) | Component snapshot |
| `feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt` (rewrite) | Custom circular-reveal drawer replaced by a stock M3 `ModalBottomSheet` wrapping a new `MoreMenuContent` composable |
| `feature/home/src/test/java/com/ivy/home/HomeMoreMenuPaparazziTest.kt` (new) | Component snapshot of `MoreMenuContent` (not the sheet wrapper — see Global Constraints) |
| `feature/main/build.gradle.kts` (modify) | + `testImplementation(projects.shared.ui.testing)` for the new Paparazzi test |
| `feature/main/src/main/java/com/ivy/main/MainBottomBar.kt` (rewrite) | Tab row → stock `NavigationBar`; FAB → stock `FloatingActionButtonMenu`/`ToggleFloatingActionButton` |
| `feature/main/src/test/java/com/ivy/main/MainBottomBarPaparazziTest.kt` (new) | Component snapshot, both tabs, collapsed state |
| `feature/home/src/test/java/com/ivy/home/HomePaparazziTest.kt` (baseline re-record only, no code change) | Existing Home screen snapshot against the new visuals |
| `shared/ui/core/src/main/res/values/strings.xml` (modify, 2 separate edits) | + `more_options` (Task 4), + `add_transaction` (Task 5) |

---

### Task 1: Restyle `HomeHeader.kt` to M3 tokens

**Files:**
- Rewrite: `feature/home/src/main/java/com/ivy/home/HomeHeader.kt`
- Modify: `feature/home/src/main/java/com/ivy/home/HomeTab.kt` (one call site)
- Test: re-record `feature/home/src/test/java/com/ivy/home/HomePaparazziTest.kt` baseline (no code change to that file)

**Interfaces:**
- Consumes: nothing new.
- Produces: `HomeHeader(...)` and `CashFlowInfo(...)` keep the same public signatures **except** `CashFlowInfo` drops its `percentExpanded: Float` parameter (see rationale below) — Task 3 and later tasks don't touch this file again, so no other task is affected.

The old `HeaderCard`'s `percentVisible` parameter only existed to gate the colored drop-shadow (`thenIf(percentVisible == 1f) { drawColoredShadow(...) }`), which this task removes entirely in favor of a flat card. Once that's gone, `percentVisible` (and the `percentExpanded` that fed it, all the way up through `IncomeExpenses` and `CashFlowInfo`) is dead — and at `CashFlowInfo`'s one production call site (`HomeTab.kt`'s `HomeLazyColumn`) it was already hardcoded to `1f`. Removing the parameter chain avoids a detekt `UnusedPrivateMember`/`UnusedParameter` finding and is a straightforward dead-code cleanup enabled by this task, not a separate refactor.

- [ ] **Step 1: Rewrite `HomeHeader.kt`**

Replace the entire file with:

```kotlin
package com.ivy.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.transaction.TransactionsDividerLine
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.springBounce
import com.ivy.legacy.utils.verticalSwipeListener
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import kotlin.math.absoluteValue

@ExperimentalAnimationApi
@Composable
internal fun HomeHeader(
    expanded: Boolean,
    name: String,
    period: TimePeriod,
    currency: String,
    balance: Double,
    onShowMonthModal: () -> Unit,
    onBalanceClick: () -> Unit,
    onSelectNextMonth: () -> Unit,
    hideBalance: Boolean,
    onHiddenBalanceClick: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
) {
    Column {
        val percentExpanded by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = springBounce(
                stiffness = Spring.StiffnessLow
            ),
            label = "Home Header Expand Collapse"
        )

        Spacer(Modifier.height(20.dp))

        HeaderStickyRow(
            percentExpanded = percentExpanded,
            name = name,
            period = period,
            currency = currency,
            balance = balance,
            hideBalance = hideBalance,

            onShowMonthModal = onShowMonthModal,
            onBalanceClick = onBalanceClick,
            onHiddenBalanceClick = onHiddenBalanceClick,
            onSelectNextMonth = onSelectNextMonth,
            onSelectPreviousMonth = onSelectPreviousMonth,
        )

        Spacer(Modifier.height(16.dp))

        if (percentExpanded < 0.5f) {
            TransactionsDividerLine(
                modifier = Modifier.alpha(1f - percentExpanded),
                paddingHorizontal = 0.dp
            )
        }
    }
}

@Composable
private fun HeaderStickyRow(
    percentExpanded: Float,
    name: String,
    period: TimePeriod,
    currency: String,
    balance: Double,
    onShowMonthModal: () -> Unit,
    onBalanceClick: () -> Unit,
    onSelectNextMonth: () -> Unit,
    hideBalance: Boolean,
    onHiddenBalanceClick: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                modifier = Modifier
                    .alpha(percentExpanded)
                    .testTag("home_greeting_text"),
                text = if (name.isNotNullOrBlank()) {
                    stringResource(
                        R.string.hi_name,
                        name,
                    )
                } else {
                    stringResource(R.string.hi)
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            // Balance mini row
            if (percentExpanded < 1f) {
                BalanceRowMini(
                    modifier = Modifier
                        .alpha(alpha = 1f - percentExpanded)
                        .clickableNoIndication(rememberInteractionSource()) {
                            if (hideBalance) {
                                onHiddenBalanceClick()
                            } else {
                                onBalanceClick()
                            }
                        },
                    currency = currency,
                    balance = balance,
                    shortenBigNumbers = true,
                    hiddenMode = hideBalance,
                    doubleRowDisplay = true,
                )
            }
        }

        OutlinedButton(
            modifier = Modifier.horizontalSwipeListener(
                sensitivity = 75,
                state = rememberSwipeListenerState(),
                onSwipeLeft = {
                    onSelectNextMonth()
                },
                onSwipeRight = {
                    onSelectPreviousMonth()
                },
            ),
            onClick = onShowMonthModal,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = period.toDisplayShort(
                    startDateOfMonth = ivyWalletCtx().startDayOfMonth,
                    timeConverter = LocalTimeConverter.current,
                    timeProvider = LocalTimeProvider.current,
                    timeFormatter = LocalTimeFormatter.current,
                ),
            )
        }

        Spacer(Modifier.width(12.dp))

        Spacer(Modifier.width(40.dp)) // settings menu button spacer
    }
}

@ExperimentalAnimationApi
@Composable
fun CashFlowInfo(
    currency: String,
    balance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideBalance: Boolean,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    onBalanceClick: () -> Unit,
    onHiddenBalanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalSwipeListener(
                sensitivity = Constants.SWIPE_DOWN_THRESHOLD_OPEN_MORE_MENU,
                state = rememberSwipeListenerState(),
                onSwipeDown = {
                    onOpenMoreMenu()
                },
            ),
    ) {
        BalanceRow(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clickableNoIndication(rememberInteractionSource()) {
                    if (hideBalance) {
                        onHiddenBalanceClick()
                    } else {
                        onBalanceClick()
                    }
                }
                .testTag("home_balance"),
            currency = currency,
            balance = balance,
            shortenBigNumbers = true,
            hiddenMode = hideBalance
        )

        Spacer(modifier = Modifier.height(24.dp))

        IncomeExpenses(
            currency = currency,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            hideIncome = hideIncome,
            onHiddenIncomeClick = onHiddenIncomeClick
        )

        val cashflow = monthlyIncome - monthlyExpenses
        if (cashflow != 0.0 && !hideBalance) {
            Spacer(Modifier.height(12.dp))

            Text(
                modifier = Modifier.padding(
                    start = 24.dp,
                ),
                text = stringResource(
                    R.string.cashflow,
                    (if (cashflow > 0) "+" else ""),
                    cashflow.format(currency),
                    currency,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (cashflow < 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )

            Spacer(Modifier.height(4.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IncomeExpenses(
    currency: String,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))

        val nav = navigation()

        HeaderCard(
            icon = R.drawable.ic_income,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            label = stringResource(R.string.income),
            currency = currency,
            amount = monthlyIncome,
            testTag = "home_card_income"
        ) {
            if (hideIncome) {
                onHiddenIncomeClick()
            } else {
                nav.navigateTo(
                    PieChartStatisticScreen(
                        type = TransactionType.INCOME,
                    ),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        HeaderCard(
            icon = R.drawable.ic_expense,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            label = stringResource(R.string.expenses),
            currency = currency,
            amount = monthlyExpenses.absoluteValue,
            testTag = "home_card_expense",
        ) {
            nav.navigateTo(
                PieChartStatisticScreen(
                    type = TransactionType.EXPENSE,
                ),
            )
        }

        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun RowScope.HeaderCard(
    @DrawableRes icon: Int,
    containerColor: Color,
    label: String,
    currency: String,
    amount: Double,
    testTag: String,
    onClick: () -> Unit,
) {
    val contentColor = contentColorFor(containerColor)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .testTag(testTag)
            .clickable(
                onClick = onClick,
            ),
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(16.dp))

            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = contentColor,
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(20.dp))

            AmountCurrencyB1(
                amount = amount,
                currency = currency,
                textColor = contentColor,
                shortenBigNumbers = true,
            )

            Spacer(Modifier.width(4.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}
```

Notes on the color-role choice: income uses `tertiaryContainer`, expenses use `secondaryContainer`. `errorContainer` was deliberately **not** used for expenses — that role signals an actual error state to accessibility services and M3 theming, and a normal expense entry isn't one.

- [ ] **Step 2: Update the one call site in `HomeTab.kt`**

Find this block inside `HomeLazyColumn` (in `feature/home/src/main/java/com/ivy/home/HomeTab.kt`):

```kotlin
                onOpenMoreMenu = onOpenMoreMenu,
                onBalanceClick = onBalanceClick,
                onHiddenBalanceClick = onHiddenBalanceClick,
                percentExpanded = 1f,
                hideIncome = hideIncome,
                onHiddenIncomeClick = onHiddenIncomeClick
```

Remove the `percentExpanded = 1f,` line:

```kotlin
                onOpenMoreMenu = onOpenMoreMenu,
                onBalanceClick = onBalanceClick,
                onHiddenBalanceClick = onHiddenBalanceClick,
                hideIncome = hideIncome,
                onHiddenIncomeClick = onHiddenIncomeClick
```

- [ ] **Step 3: Record + verify**

Run: `./gradlew :feature:home:recordPaparazziDebug`
Then: `./gradlew :feature:home:verifyPaparazziDebug :feature:home:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL both times; updated golden images under `feature/home/src/test/snapshots/`.

- [ ] **Step 4: Commit**

```bash
git add feature/home/src/main/java/com/ivy/home/HomeHeader.kt feature/home/src/main/java/com/ivy/home/HomeTab.kt feature/home/src/test/snapshots/
git commit -m "feat: restyle Home header and cashflow cards to M3 tokens"
```

---

### Task 2: Restyle `CustomerJourneyCard` to a flat M3 card

**Files:**
- Modify: `feature/home/src/main/java/com/ivy/home/customerjourney/CustomerJourney.kt`
- Test: `feature/home/src/test/java/com/ivy/home/customerjourney/CustomerJourneyPaparazziTest.kt` (new)

**Interfaces:**
- `CustomerJourneyCardModel.background: Gradient` (from `CustomerJourneyCardsProvider.kt`) is **unchanged** — each of the 6 existing cards already calls `Gradient.solid(color)` with one flat color, so this task only changes how `CustomerJourneyCard` renders that color (flat background instead of gradient brush + shadow), not the data model.
- `CustomerJourneyCard(cardData, onDismiss, modifier, onCTA)` keeps its exact signature — no other file calls it besides `CustomerJourney.kt`'s own `CustomerJourney(...)` loop, which is unchanged.

- [ ] **Step 1: Rewrite `CustomerJourneyCard`**

Replace the whole `CustomerJourneyCard` composable (and its imports) in `feature/home/src/main/java/com/ivy/home/customerjourney/CustomerJourney.kt`. Full new file:

```kotlin
package com.ivy.home.customerjourney

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.domain.RootScreen
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.rootScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.wallet.ui.theme.findContrastTextColor
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CustomerJourney(
    customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,
    modifier: Modifier = Modifier,
    onDismiss: (CustomerJourneyCardModel) -> Unit,
) {
    val ivyContext = ivyWalletCtx()
    val nav = navigation()
    // Check is added for Paparazzi Test where context is different
    if (LocalContext.current is RootScreen) {
        val rootScreen = rootScreen()

        if (customerJourneyCards.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
        }

        for (card in customerJourneyCards) {
            Spacer(Modifier.height(12.dp))

            CustomerJourneyCard(
                modifier = modifier,
                cardData = card,
                onDismiss = {
                    onDismiss(card)
                }
            ) {
                card.onAction(nav, ivyContext, rootScreen)
            }
        }
    } else {
        Box(modifier)
    }
}

@Composable
fun CustomerJourneyCard(
    cardData: CustomerJourneyCardModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCTA: () -> Unit,
) {
    val containerColor = cardData.background.startColor
    val contentColor = findContrastTextColor(containerColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .clickable {
                onCTA()
            }
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp, end = 16.dp),
                text = cardData.title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )

            if (cardData.hasDismiss) {
                Icon(
                    modifier = Modifier
                        .clickable {
                            onDismiss()
                        }
                        .padding(8.dp), // enlarge click area
                    painter = painterResource(id = com.ivy.ui.R.drawable.ic_dismiss),
                    tint = contentColor,
                    contentDescription = "prompt_dismiss",
                )

                Spacer(Modifier.width(20.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 32.dp),
            text = cardData.description,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )

        Spacer(Modifier.height(32.dp))

        if (cardData.cta != null) {
            Button(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 20.dp)
                    .testTag("cta_prompt_${cardData.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = contentColor,
                    contentColor = containerColor,
                ),
                onClick = onCTA,
            ) {
                Icon(
                    painter = painterResource(id = cardData.ctaIcon),
                    contentDescription = null,
                    tint = containerColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(text = cardData.cta, color = containerColor)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Preview
@Composable
private fun PreviewCard() {
    IvyPreview {
        CustomerJourneyCard(
            cardData = CustomerJourneyCardsProvider.adjustBalanceCard(),
            onCTA = { },
            onDismiss = {}
        )
    }
}
```

This drops `IvyButton`, `IvyIcon`, `Gradient`'s brush/shadow rendering, and the `dynamicContrast()` calls (the original mixed `findContrastTextColor` for text with `.dynamicContrast()` for the dismiss icon tint — this rewrite standardizes on `findContrastTextColor` everywhere for one consistent contrast color per card).

- [ ] **Step 2: Add the Paparazzi test**

Create `feature/home/src/test/java/com/ivy/home/customerjourney/CustomerJourneyPaparazziTest.kt`:

```kotlin
package com.ivy.home.customerjourney

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CustomerJourneyPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot customer journey card`() {
        snapshot(theme) {
            CustomerJourneyCard(
                cardData = CustomerJourneyCardsProvider.adjustBalanceCard(),
                onCTA = {},
                onDismiss = {},
            )
        }
    }
}
```

`CustomerJourneyCard` itself doesn't call `rootScreen()` or `navigation()` (only the outer `CustomerJourney` loop does, which this test doesn't exercise), so it needs no `IvyWalletPreview`/`IvyPreview` wrapping beyond what `PaparazziScreenshotTest.snapshot()` already provides.

- [ ] **Step 3: Record + verify**

Run: `./gradlew :feature:home:recordPaparazziDebug`
Then: `./gradlew :feature:home:verifyPaparazziDebug :feature:home:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL both times.

- [ ] **Step 4: Commit**

```bash
git add feature/home/src/main/java/com/ivy/home/customerjourney/CustomerJourney.kt feature/home/src/test/java/com/ivy/home/customerjourney/CustomerJourneyPaparazziTest.kt feature/home/src/test/snapshots/
git commit -m "feat: restyle customer journey card to a flat M3 card"
```

---

### Task 3: Skip-all-planned confirmation → stock M3 `AlertDialog`

**Files:**
- Modify: `feature/home/src/main/java/com/ivy/home/HomeTab.kt`

**Interfaces:** no signature changes — `HomeUi`'s internal `skipAllModalVisible` state and its trigger (`onSkipAllTransactions = { skipAllModalVisible = true }`) are unchanged; only the modal itself changes.

- [ ] **Step 1: Replace the `DeleteModal` block**

In `feature/home/src/main/java/com/ivy/home/HomeTab.kt`, find:

```kotlin
    DeleteModal(
        visible = skipAllModalVisible,
        title = stringResource(R.string.confirm_skip_all),
        description = stringResource(R.string.confirm_skip_all_description),
        dismiss = {
            skipAllModalVisible = false
        }
    ) {
        onEvent(HomeEvent.SkipAllPlanned(uiState.overdue.trns))
        skipAllModalVisible = false
    }
```

Replace it with:

```kotlin
    if (skipAllModalVisible) {
        AlertDialog(
            onDismissRequest = { skipAllModalVisible = false },
            title = { Text(text = stringResource(R.string.confirm_skip_all)) },
            text = { Text(text = stringResource(R.string.confirm_skip_all_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(HomeEvent.SkipAllPlanned(uiState.overdue.trns))
                        skipAllModalVisible = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { skipAllModalVisible = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
```

- [ ] **Step 2: Update imports**

Remove (no longer used in this file):

```kotlin
import com.ivy.wallet.ui.theme.modal.DeleteModal
```

Add:

```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
```

`R.string.confirm` and `R.string.cancel` already exist in `shared/ui/core`'s `strings.xml` (the latter was added during the Settings redesign) — `HomeTab.kt` already imports `com.ivy.ui.R`, so no new import is needed for them.

- [ ] **Step 3: Verify**

Run: `./gradlew :feature:home:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL. (No Paparazzi re-record needed — the dialog only renders when `skipAllModalVisible` is true, which the default Home screenshot never triggers, exactly like the removed `DeleteModal` before it.)

- [ ] **Step 4: Commit**

```bash
git add feature/home/src/main/java/com/ivy/home/HomeTab.kt
git commit -m "feat: swap skip-all-planned confirmation to a stock M3 alert dialog"
```

---

### Task 4: Rebuild `HomeMoreMenu.kt` on a stock M3 `ModalBottomSheet`

**Files:**
- Rewrite: `feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt`
- Modify: `shared/ui/core/src/main/res/values/strings.xml` (add `more_options`)
- Test: `feature/home/src/test/java/com/ivy/home/HomeMoreMenuPaparazziTest.kt` (new)

**Interfaces:**
- `MoreMenu(expanded, balance, buffer, currency, theme, setExpanded, onSwitchTheme, onBufferClick, onCurrencyClick, modifier)` keeps its exact signature and `BoxWithConstraintsScope` receiver — `HomeTab.kt`'s call site is unaffected.
- New: `private fun MoreMenuContent(...)` holds the sheet's actual content (everything that used to be `Content()`), so it can be Paparazzi-tested directly without going through the un-capturable `ModalBottomSheet` window (see Global Constraints).
- New: `fun MoreMenuContentUiTest(isDark: Boolean)` — the screenshot entry point Task 4's test calls, mirroring `HomeTab.kt`'s existing `HomeUiTest` convention.

- [ ] **Step 1: Add the `more_options` string**

In `shared/ui/core/src/main/res/values/strings.xml`, next to `quick_access` (line ~103):

```xml
<string name="more_options">More options</string>
```

- [ ] **Step 2: Rewrite `HomeMoreMenu.kt`**

Replace the entire file with:

```kotlin
package com.ivy.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.domain.RootScreen
import com.ivy.legacy.Constants
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.openUrl
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.component.settings.SettingsSectionTitle
import com.ivy.wallet.ui.theme.components.BufferBattery
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1

@Composable
fun BoxWithConstraintsScope.MoreMenu(
    expanded: Boolean,

    balance: Double,
    buffer: Double,
    currency: String,
    theme: Theme,

    setExpanded: (Boolean) -> Unit,
    onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = { setExpanded(true) },
        modifier = modifier
            .statusBarsPadding()
            .align(Alignment.TopEnd)
            .padding(top = 20.dp, end = 24.dp)
            .testTag("home_more_menu_arrow"),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_expandarrow),
            contentDescription = stringResource(R.string.more_options),
        )
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { setExpanded(false) },
        ) {
            MoreMenuContent(
                balance = balance,
                buffer = buffer,
                currency = currency,
                theme = theme,
                onSwitchTheme = onSwitchTheme,
                onBufferClick = onBufferClick,
            )
        }
    }
}

@Composable
private fun MoreMenuContent(
    balance: Double,
    buffer: Double,
    currency: String,
    theme: Theme,
    onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit,
) {
    val nav = navigation()
    // rootScreen() unsafely casts LocalContext.current — capture the context here (always
    // safe) and defer the RootScreen check into the click lambda so this composable can still
    // be Paparazzi-tested without a real RootScreen host.
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .testTag("home_more_menu_content"),
    ) {
        SearchButton {
            nav.navigateTo(SearchScreen)
        }

        Spacer(Modifier.height(24.dp))

        SettingsSectionTitle(text = stringResource(R.string.quick_access))

        QuickAccessGrid(
            theme = theme,
            onSwitchTheme = onSwitchTheme,
            onSettingsClick = { nav.navigateTo(SettingsScreen) },
            onCategoriesClick = { nav.navigateTo(CategoriesScreen) },
            onPlannedPaymentsClick = { nav.navigateTo(PlannedPaymentsScreen) },
            onShareClick = {
                if (context is RootScreen) {
                    context.shareIvyWallet()
                }
            },
            onReportsClick = { nav.navigateTo(ReportScreen) },
            onBudgetsClick = { nav.navigateTo(BudgetScreen) },
            onLoansClick = { nav.navigateTo(LoansScreen) },
        )

        Spacer(Modifier.height(24.dp))

        Buffer(
            buffer = buffer,
            currency = currency,
            balance = balance,
            onBufferClick = onBufferClick,
        )

        Spacer(Modifier.height(16.dp))

        OpenSource()

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SearchButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = stringResource(R.string.search_transactions),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun QuickAccessGrid(
    theme: Theme,
    onSwitchTheme: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onPlannedPaymentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onLoansClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickAccessButton(
                icon = R.drawable.home_more_menu_settings,
                label = stringResource(R.string.settings),
                onClick = onSettingsClick,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_categories,
                label = stringResource(R.string.categories),
                onClick = onCategoriesClick,
            )
            QuickAccessButton(
                icon = when (theme) {
                    Theme.LIGHT -> R.drawable.home_more_menu_light_mode
                    Theme.DARK -> R.drawable.home_more_menu_dark_mode
                    Theme.AMOLED_DARK -> R.drawable.home_more_menu_amoled_dark_mode
                    Theme.AUTO -> R.drawable.home_more_menu_auto_mode
                },
                label = when (theme) {
                    Theme.LIGHT -> stringResource(R.string.light_mode)
                    Theme.DARK -> stringResource(R.string.dark_mode)
                    Theme.AMOLED_DARK -> stringResource(R.string.amoled_mode)
                    Theme.AUTO -> stringResource(R.string.auto_mode)
                },
                onClick = onSwitchTheme,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_planned_payments,
                label = stringResource(R.string.planned_payments),
                onClick = onPlannedPaymentsClick,
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickAccessButton(
                icon = R.drawable.home_more_menu_share,
                label = stringResource(R.string.share_ivy),
                onClick = onShareClick,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_reports,
                label = stringResource(R.string.reports),
                onClick = onReportsClick,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_budgets,
                label = stringResource(R.string.budgets),
                onClick = onBudgetsClick,
            )
            QuickAccessButton(
                icon = R.drawable.home_more_menu_loans,
                label = stringResource(R.string.loans),
                onClick = onLoansClick,
            )
        }
    }
}

@Composable
private fun QuickAccessButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(painter = painterResource(icon), contentDescription = label)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun Buffer(
    buffer: Double,
    currency: String,
    balance: Double,
    onBufferClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndication(rememberInteractionSource()) {
                onBufferClick()
            }
            .padding(horizontal = 24.dp)
            .testTag("savings_goal_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.savings_goal),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.weight(1f))

        AmountCurrencyB1(
            amount = buffer,
            currency = currency,
        )
    }

    Spacer(Modifier.height(12.dp))

    BufferBattery(
        modifier = Modifier.padding(horizontal = 16.dp),
        buffer = buffer,
        currency = currency,
        balance = balance,
    ) {
        onBufferClick()
    }
}

@Composable
private fun OpenSource() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                openUrl(
                    uriHandler = uriHandler,
                    url = Constants.URL_IVY_WALLET_REPO
                )
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))

        Icon(
            painter = painterResource(R.drawable.github_logo),
            contentDescription = null,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.ivy_wallet_open_source),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = Constants.URL_IVY_WALLET_REPO,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewCollapsed() {
    IvyWalletPreview {
        MoreMenu(
            expanded = false,
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            setExpanded = {},
            onSwitchTheme = {},
            onBufferClick = {},
            onCurrencyClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewExpanded() {
    IvyWalletPreview {
        MoreMenu(
            expanded = true,
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            setExpanded = {},
            onSwitchTheme = {},
            onBufferClick = {},
            onCurrencyClick = {},
        )
    }
}

/** For screenshot testing */
@Composable
fun MoreMenuContentUiTest(isDark: Boolean) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    IvyWalletPreview(theme) {
        MoreMenuContent(
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            onSwitchTheme = {},
            onBufferClick = {},
        )
    }
}
```

Notes for the implementer:
- `onCurrencyClick` stays an unused parameter on `MoreMenu`, exactly matching current (pre-existing) behavior: neither the old `Content()` nor this new `MoreMenuContent` ever calls it — `HomeTab.kt`'s `currencyModalVisible` state is consequently unreachable through the Home UI today. That's a pre-existing gap, not introduced or fixed by this task; leave it alone.
- `IvyWalletPreview`'s content lambda has a `BoxWithConstraintsScope` receiver (same as `HomeUiTest` elsewhere in this module), which is why `MoreMenu` keeps its own `BoxWithConstraintsScope` receiver too.
- This drops `AddModalBackHandling`, the `Canvas`-drawn circular reveal, `colorLerp`/`lerp`/`rotate`/`zIndex`/pixel-math `layout {}` blocks, and the swipe-up-to-close gesture — `ModalBottomSheet` provides equivalent back-press/scrim-tap/swipe-down dismissal natively.

- [ ] **Step 3: Add the Paparazzi test**

Create `feature/home/src/test/java/com/ivy/home/HomeMoreMenuPaparazziTest.kt`:

```kotlin
package com.ivy.home

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class HomeMoreMenuPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot more menu content`() {
        snapshot(theme) {
            MoreMenuContentUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
```

- [ ] **Step 4: Record + verify**

Run: `./gradlew :feature:home:recordPaparazziDebug`
Then: `./gradlew :feature:home:verifyPaparazziDebug :feature:home:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL both times.

- [ ] **Step 5: Commit**

```bash
git add feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt feature/home/src/test/java/com/ivy/home/HomeMoreMenuPaparazziTest.kt shared/ui/core/src/main/res/values/strings.xml feature/home/src/test/snapshots/
git commit -m "feat: rebuild HomeMoreMenu on a stock M3 modal bottom sheet"
```

---

### Task 5: Rebuild `MainBottomBar.kt` on stock M3 `NavigationBar` + `FloatingActionButtonMenu`

**Files:**
- Modify: `feature/main/build.gradle.kts`
- Rewrite: `feature/main/src/main/java/com/ivy/main/MainBottomBar.kt`
- Modify: `shared/ui/core/src/main/res/values/strings.xml` (add `add_transaction`)
- Test: `feature/main/src/test/java/com/ivy/main/MainBottomBarPaparazziTest.kt` (new)

**Interfaces:** `BottomBar(tab, selectTab, onAddIncome, onAddExpense, onAddTransfer, onAddPlannedPayment, showAddAccountModal)` keeps its exact signature and `BoxWithConstraintsScope` receiver — `MainScreen.kt`'s call site needs no changes.

Verified directly against the pinned `androidx.compose.material3:material3-android:1.5.0-alpha23` sources: `FloatingActionButtonMenu`, `ToggleFloatingActionButton`, `FloatingActionButtonMenuItem`, and `NavigationBar`/`NavigationBarItem` all exist with no `@ExperimentalMaterial3ExpressiveApi` gate. `RowScope.NavigationBarItem` applies `Modifier.weight(1f)` to itself internally, so a plain `Spacer(Modifier.weight(1f))` between the two `NavigationBarItem`s creates an even three-way split (item / gap / item) — the standard, widely-used way to leave a centered notch for an overlaid FAB.

- [ ] **Step 1: Add the test dependency**

In `feature/main/build.gradle.kts`, add to the `dependencies` block (matching `feature/home`'s and `feature/settings`'s existing pattern):

```kotlin
    testImplementation(projects.shared.ui.testing)
```

- [ ] **Step 2: Add the `add_transaction` string**

In `shared/ui/core/src/main/res/values/strings.xml`, next to `quick_access`:

```xml
<string name="add_transaction">Add transaction</string>
```

- [ ] **Step 3: Rewrite `MainBottomBar.kt`**

Replace the entire file with:

```kotlin
package com.ivy.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ivy.legacy.data.model.MainTab
import com.ivy.ui.R

@Composable
fun BoxWithConstraintsScope.BottomBar(
    tab: MainTab,
    selectTab: (MainTab) -> Unit,

    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,

    showAddAccountModal: () -> Unit,
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(tab) {
        fabMenuExpanded = false
    }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    NavigationBar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
    ) {
        NavigationBarItem(
            selected = tab == MainTab.HOME,
            onClick = { selectTab(MainTab.HOME) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = stringResource(R.string.home),
                )
            },
            label = { Text(text = stringResource(R.string.home)) },
            modifier = Modifier.testTag("home"),
        )

        Spacer(modifier = Modifier.weight(1f, fill = true))

        NavigationBarItem(
            selected = tab == MainTab.ACCOUNTS,
            onClick = { selectTab(MainTab.ACCOUNTS) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_accounts),
                    contentDescription = stringResource(R.string.accounts),
                )
            },
            label = { Text(text = stringResource(R.string.accounts)) },
            modifier = Modifier.testTag("accounts"),
        )
    }

    FloatingActionButtonMenu(
        modifier = Modifier.align(Alignment.BottomCenter),
        expanded = fabMenuExpanded && tab == MainTab.HOME,
        horizontalAlignment = Alignment.CenterHorizontally,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier.testTag("fab_add"),
                checked = fabMenuExpanded,
                onCheckedChange = {
                    if (tab == MainTab.HOME) {
                        fabMenuExpanded = !fabMenuExpanded
                    } else {
                        showAddAccountModal()
                    }
                },
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (tab == MainTab.HOME && checkedProgress > 0.5f) {
                            Icons.Filled.Close
                        } else {
                            Icons.Filled.Add
                        }
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = stringResource(R.string.add_transaction),
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onAddIncome()
            },
            icon = {
                Icon(painter = painterResource(R.drawable.ic_income), contentDescription = null)
            },
            text = { Text(text = stringResource(R.string.add_income_uppercase)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onAddExpense()
            },
            icon = {
                Icon(painter = painterResource(R.drawable.ic_expense), contentDescription = null)
            },
            text = { Text(text = stringResource(R.string.add_expense_uppercase)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onAddTransfer()
            },
            icon = {
                Icon(painter = painterResource(R.drawable.ic_transfer), contentDescription = null)
            },
            text = { Text(text = stringResource(R.string.account_transfer)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onAddPlannedPayment()
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_planned_payments),
                    contentDescription = null,
                )
            },
            text = { Text(text = stringResource(R.string.add_planned_payment)) },
        )
    }
}
```

This drops `TRN_BUTTON_CLICK_AREA_HEIGHT`, `FAB_BUTTON_SIZE`, the `TransactionButtons`/`AddIncomeButton`/`AddExpenseButton`/`AddTransferButton`/`Tab` private composables, `IvyCircleButton`, `pureBlur()`, `AddModalBackHandling`, and the `detectDragGestures`-based drag-up shortcuts entirely — all superseded by `NavigationBar`/`FloatingActionButtonMenu`'s stock behavior. `R.string.home` and `R.string.accounts` already exist (verified — they were passed as plain strings to the old private `Tab` composable already).

- [ ] **Step 4: Add the Paparazzi test**

Create `feature/main/src/test/java/com/ivy/main/MainBottomBarPaparazziTest.kt`:

```kotlin
package com.ivy.main

import androidx.compose.foundation.layout.BoxWithConstraints
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.legacy.data.model.MainTab
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class MainBottomBarPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot bottom bar - home tab`() {
        snapshot(theme) {
            BoxWithConstraints {
                BottomBar(
                    tab = MainTab.HOME,
                    selectTab = {},
                    onAddIncome = {},
                    onAddExpense = {},
                    onAddTransfer = {},
                    onAddPlannedPayment = {},
                    showAddAccountModal = {},
                )
            }
        }
    }

    @Test
    fun `snapshot bottom bar - accounts tab`() {
        snapshot(theme) {
            BoxWithConstraints {
                BottomBar(
                    tab = MainTab.ACCOUNTS,
                    selectTab = {},
                    onAddIncome = {},
                    onAddExpense = {},
                    onAddTransfer = {},
                    onAddPlannedPayment = {},
                    showAddAccountModal = {},
                )
            }
        }
    }
}
```

Only the collapsed-FAB state is captured here (`fabMenuExpanded` is private `rememberSaveable` state with no way to force it true from outside) — the expanded FAB-menu state is covered by Task 6's manual QA pass instead, the same limitation this plan already documents for `AlertDialog`/`ModalBottomSheet`.

- [ ] **Step 5: Record + verify**

Run: `./gradlew :feature:main:recordPaparazziDebug`
Then: `./gradlew :feature:main:verifyPaparazziDebug :feature:main:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL both times; new golden images under `feature/main/src/test/snapshots/`.

- [ ] **Step 6: Full app build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (catches `MainScreen.kt`'s call site and any missed import across modules).

- [ ] **Step 7: Commit**

```bash
git add feature/main/build.gradle.kts feature/main/src/main/java/com/ivy/main/MainBottomBar.kt feature/main/src/test/java/com/ivy/main/MainBottomBarPaparazziTest.kt shared/ui/core/src/main/res/values/strings.xml feature/main/src/test/snapshots/
git commit -m "feat: rebuild MainBottomBar on stock M3 NavigationBar and FAB menu"
```

---

### Task 6: Full verification sweep + manual QA

**Files:** none (verification only; fix-forward anything found, amending nothing — new `fix:` commits).

- [ ] **Step 1: CI mirror**

Run each; all must pass:

```bash
./gradlew testDebugUnitTest
./gradlew detekt
./gradlew lintR
./gradlew verifyPaparazziDebug
./gradlew assembleDemo -PcomposeCompilerReports=true && ./gradlew :ci-actions:compose-stability:run
```

- [ ] **Step 2: Manual emulator pass**

Install `assembleDebug` on an emulator and verify:

- Home screen renders with flat M3 income/expense cards and restyled greeting/period row; scroll-collapse behavior (greeting ↔ mini balance row) still animates smoothly.
- Customer journey cards (if any are active) render as flat cards; dismiss and CTA both work.
- Swipe down on the balance/cashflow area opens the MoreMenu bottom sheet; the corner button also opens it; swipe down or tap-outside dismisses it.
- Inside the bottom sheet: search opens `SearchScreen`; every quick-access icon (Settings, Categories, Theme cycle, Planned Payments, Share, Reports, Budgets, Loans) does its action; buffer battery still opens the buffer modal; open-source row opens the GitHub URL.
- Bottom nav: tapping Home/Accounts switches tabs and updates the selected item's label/icon; horizontal swipe on Home still switches to Accounts.
- FAB on Home: tap expands into Income/Expense/Transfer/Planned-Payment menu items with the stock M3 animation; each opens the correct `EditTransactionScreen`/`EditPlannedScreen`; tapping the FAB again, tapping a menu item, or pressing back all collapse it.
- FAB on Accounts: single tap opens the add-account modal directly, no menu.
- Switching Home → Accounts while the FAB menu is expanded collapses it (no stuck "×" icon on the Accounts tab).
- Skip-all-planned confirmation (via the overdue section) still shows a two-choice dialog and actually skips all on confirm.
- Legacy screens reached from Home (Categories, Budgets, Loans, Settings, Search, Reports, PlannedPayments) render unaffected.

- [ ] **Step 3: Report**

Summarize results (with any deviations) to the user. **Do not push** — the user decides when and where to push.

---

### Task 8 (post-review revision): Replace the notch-FAB with a floating toolbar + a corner FAB

**Context:** After Task 6's review, the user asked to remove the FAB embedded in/overlapping the `NavigationBar`'s notch, and to try M3 Expressive's floating-toolbar pattern as the nav-bar replacement instead, falling back to "a traditional big FAB in the lower right corner" if combining the toolbar with the existing FAB menu proves too awkward.

Verified directly against the pinned `material3:1.5.0-alpha23` sources: `HorizontalFloatingToolbar` exists (two overloads — a plain one with `leadingContent`/`trailingContent`/`content`, and one that also takes a `floatingActionButton` slot), gated only behind `@OptIn(ExperimentalMaterial3Api::class)` (same opt-in Task 4 already needed for `ModalBottomSheet`, not a new toolchain requirement). However, the `floatingActionButton`-slot overload expects a single plain FAB whose "size is controlled by the floating toolbar and animates according to its state" — nesting the existing `FloatingActionButtonMenu` (which does its own internal layout/positioning for its button + expanding item column) inside that slot fights with the toolbar's own FAB sizing/animation. So this task takes the explicitly-authorized fallback for the FAB specifically, while still adopting the toolbar for the tab row (which isn't the hard part):

- The tab row moves from `NavigationBar`/`NavigationBarItem` onto the **plain** `HorizontalFloatingToolbar` overload (no `floatingActionButton` param), centered at the bottom, containing two custom tab items (icon + label-when-selected, replacing `NavigationBarItem`).
- The existing `FloatingActionButtonMenu`/`ToggleFloatingActionButton`/`FloatingActionButtonMenuItem` block (built in Task 5) is **kept exactly as-is internally** — same 4 menu items, same tab-switch-collapse `LaunchedEffect`, same Home/Accounts branching — just repositioned from `Alignment.BottomCenter` (overlapping the old nav bar's notch) to `Alignment.BottomEnd` (a standalone corner FAB), and its `horizontalAlignment` override is dropped so the expanding item list uses the default `Alignment.End` (stacking above a corner FAB reads naturally; centering it no longer makes sense once the FAB isn't centered).

**Files:**
- Modify: `feature/main/src/main/java/com/ivy/main/MainBottomBar.kt`
- Test: re-record `feature/main/src/test/java/com/ivy/main/MainBottomBarPaparazziTest.kt` baselines (no code change to the test file itself — it already exercises `BottomBar` for both tabs)

**Interfaces:** `BottomBar`'s signature is unchanged; `MainScreen.kt`'s call site needs no changes (same as Task 5).

- [ ] **Step 1: Replace the `NavigationBar` block with `HorizontalFloatingToolbar`**

In `feature/main/src/main/java/com/ivy/main/MainBottomBar.kt`, replace:

```kotlin
    NavigationBar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
    ) {
        NavigationBarItem(
            selected = tab == MainTab.HOME,
            onClick = { selectTab(MainTab.HOME) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = stringResource(R.string.home),
                )
            },
            label = { Text(text = stringResource(R.string.home)) },
            modifier = Modifier.testTag("home"),
        )

        Spacer(modifier = Modifier.weight(1f, fill = true))

        NavigationBarItem(
            selected = tab == MainTab.ACCOUNTS,
            onClick = { selectTab(MainTab.ACCOUNTS) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_accounts),
                    contentDescription = stringResource(R.string.accounts),
                )
            },
            label = { Text(text = stringResource(R.string.accounts)) },
            modifier = Modifier.testTag("accounts"),
        )
    }
```

with:

```kotlin
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        ToolbarTab(
            icon = R.drawable.ic_home,
            label = stringResource(R.string.home),
            selected = tab == MainTab.HOME,
            onClick = { selectTab(MainTab.HOME) },
        )
        ToolbarTab(
            icon = R.drawable.ic_accounts,
            label = stringResource(R.string.accounts),
            selected = tab == MainTab.ACCOUNTS,
            onClick = { selectTab(MainTab.ACCOUNTS) },
        )
    }
```

Add the `ToolbarTab` composable (new, private, replaces `NavigationBarItem`'s job inside the toolbar's plain `RowScope` content):

```kotlin
@Composable
private fun RowScope.ToolbarTab(
    @DrawableRes icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(label.lowercase()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(text = label, color = MaterialTheme.colorScheme.primary)
        }
    }
}
```

Treat the exact padding/offset values (`bottom = 16.dp`, the toolbar's internal `contentPadding`/`shape` defaults) as a starting point, not a pixel-exact requirement — `HorizontalFloatingToolbar` positions itself as a raw floating overlay here (there's no `Scaffold` in this screen to hand inset/offset handling to), so use the recorded Paparazzi snapshot in Step 4 to visually confirm the toolbar and the corner FAB (Step 2) sit at a consistent, sensible height above the screen edge, and adjust the padding if the snapshot looks cramped or misaligned.

- [ ] **Step 2: Move the FAB to the bottom-right corner**

Change the `FloatingActionButtonMenu` call's `modifier` and drop its `horizontalAlignment` override:

```kotlin
    FloatingActionButtonMenu(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(16.dp),
        expanded = fabMenuExpanded && tab == MainTab.HOME,
        button = {
```

(i.e. remove `horizontalAlignment = Alignment.CenterHorizontally,` entirely — the default `Alignment.End` is correct for a corner FAB — and change `.align(Alignment.BottomCenter)` to `.align(Alignment.BottomEnd)` plus add `.navigationBarsPadding().padding(16.dp)` so it doesn't sit flush against the screen edge or under the system gesture bar). Everything else inside `FloatingActionButtonMenu` (the `button` slot, the 4 `FloatingActionButtonMenuItem`s) stays exactly as Task 5 built it.

- [ ] **Step 3: Update imports**

Remove (no longer used): `androidx.compose.material3.NavigationBar`, `androidx.compose.material3.NavigationBarItem`, `androidx.compose.foundation.layout.fillMaxWidth`.

Add: `androidx.activity.compose.BackHandler` (already present, no change), `androidx.compose.foundation.clickable`, `androidx.compose.foundation.layout.Row`, `androidx.compose.foundation.layout.RowScope` (already present), `androidx.compose.foundation.layout.navigationBarsPadding`, `androidx.compose.foundation.layout.padding`, `androidx.compose.foundation.layout.width` (already present), `androidx.compose.material3.ExperimentalMaterial3Api`, `androidx.compose.material3.FloatingToolbarDefaults` (only if you use it explicitly — the plain overload's defaults resolve on their own, so this import is only needed if you reference `FloatingToolbarDefaults` directly), `androidx.compose.material3.HorizontalFloatingToolbar`, `androidx.compose.material3.LocalContentColor`, `androidx.compose.material3.MaterialTheme`, `androidx.compose.ui.draw.clip`, `androidx.annotation.DrawableRes`.

Add `@OptIn(ExperimentalMaterial3Api::class)` on the `BottomBar` function itself (same pattern Task 4 used for `MoreMenu` and `ModalBottomSheet` — if the compiler says a different/no opt-in is actually required, trust the compiler over this brief, exactly as Task 4 and Task 5's implementers already had to do twice each).

- [ ] **Step 4: Record + verify**

Run: `./gradlew :feature:main:recordPaparazziDebug`
Then: `./gradlew :feature:main:verifyPaparazziDebug :feature:main:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL both times. Look at the updated `bottom_bar_-_home_tab`/`bottom_bar_-_accounts_tab` snapshots (light and dark) to confirm the toolbar and the corner FAB read as two distinct, sensibly-spaced floating elements — not overlapping, not flush against the screen edges.

- [ ] **Step 5: Full app build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add feature/main/src/main/java/com/ivy/main/MainBottomBar.kt feature/main/src/test/snapshots/
git commit -m "feat: replace notch FAB with a floating toolbar and a corner FAB"
```
