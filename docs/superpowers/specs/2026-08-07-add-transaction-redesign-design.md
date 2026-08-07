# M3 Expressive Migration — Add/Edit Transaction Redesign

## Vision

Rebuild the add/edit transaction screen in the flat Material 3 idiom the
Settings, Home, Accounts and transaction-list rounds established: a
`Scaffold` with a transparent `TopAppBar`, the amount as the screen's display
headline, and the transaction's attributes as flat rows on the bare surface —
ReadYou's page anatomy, applied to a form.

This is the last high-traffic screen still on the deprecated design system.
Today it is 719 lines of screen sitting on ~3,240 lines of legacy edit
components, including an 869-line hand-rolled draggable `EditBottomSheet`, a
600dp spacer used to keep that sheet from covering content, and six
full-screen legacy modals — none of it screenshot-tested.

Chosen during brainstorming from three mocked-up directions (A: flat
ReadYou-style rows with a keypad bottom sheet, B: one-screen permanent
keypad, C: segmented tonal blocks matching the transaction list).
**Direction A** won: highest fidelity to ReadYou, most reuse of components
already shipped (the `SettingsItem` row above all), and the only direction
that reads equally well in add and edit mode.

## Decisions settled during brainstorming

- **The keypad auto-opens** for a new transaction, exactly as today's
  `AmountModal` does. Fast entry is the screen's best property and it stays.
- **The guided focus chain stays**: amount entered → category picker if no
  category → title focused. Same rules as today
  (`shouldFocusCategory` / `shouldFocusTitle` / `shouldFocusAmount`).
- **Add and edit remain one screen.** Edit mode adds Delete and Duplicate to
  the top-bar overflow and changes the commit button's label.
- **View-layer rewrite only.** `EditTransactionViewModel`,
  `EditTransactionViewEvent` and `EditTransactionViewState` are unchanged, so
  behaviour parity is verifiable event by event.

## Scope

### In scope

`feature/edit-transaction` — `EditTransactionScreen.kt` is rewritten; the
screen flips to `isLegacy = false` in `Screens.kt`. New reusable components
land in `shared/ui/core` so `EditPlannedScreen` can adopt them in a later
round.

Replaced this round:

| Legacy component | Replacement |
| --- | --- |
| `Toolbar` (gradient, all-caps) | `TopAppBar` + overflow `DropdownMenu` |
| `ChangeTransactionTypeModal` | `SingleChoiceSegmentedButtonRow` |
| `Title` + `IvyTitleTextField` | Borderless `TextField` + `SuggestionChip` row |
| `Category`, `Description`, `TransactionDateTime`, `DueDate` | `SettingsItem` rows |
| `AddPrimaryAttributeButton` (planned payment) | Top-bar overflow item |
| `EditBottomSheet` (869 lines) | `AmountKeypadSheet` + an account row |
| `AmountModal` + `CalculatorModal` | `AmountKeypadSheet` (one surface, arithmetic included) |
| `ChooseCategoryModal` | `CategoryPickerSheet` |
| `DescriptionModal` | `TextInputDialog` (already in `shared/ui/core`) |
| `DeleteModal` (×2 usages) | M3 `AlertDialog` |
| `ProgressModal` | `ProgressDialog` (already in `shared/ui/core`) |
| `CustomExchangeRateCard` | Exchange-rate row on the warning container |

### Deliberately kept legacy (deferred)

These are creation/management flows reached *from* the new surfaces, not part
of the add-transaction path, and two of them are shared with other legacy
screens. Restyling them would drag those screens along — the same reasoning
the transaction-list round used for the shared renderer.

- `CategoryModal` — create/edit a category (icon + colour pickers).
- `AccountModal` — create an account (icon, colour, currency, balance).
- `ShowTagModal` — tag search/create/edit; also used by `FilterOverlay` in
  `feature/reports`.

They keep working: `IvyUI` wraps the whole nav graph regardless of
`isLegacy`, so legacy modals still resolve `UI.colors`. Only
`includeSurface` changes, which is what the new screen wants — it paints its
own M3 surface.

### Out of scope

- `EditPlannedScreen`, which shares several of the legacy components above.
- Any ViewModel, domain or data-layer change.
- Deleting the legacy components themselves — `EditPlannedScreen` and
  `FilterOverlay` still reference most of them. They stop being referenced
  from this screen; deletion happens when the last caller migrates.

## Screen anatomy

Top to bottom, inside `Scaffold`:

### Top app bar

Transparent `TopAppBar`, no title text (the amount is the display headline).

- **Navigation icon**: back arrow → `nav.back()`.
- **Overflow** (`MoreVert` → `DropdownMenu`), items present only when they
  apply:
  - *Duplicate* — edit mode only.
  - *Delete* — edit mode only; opens the delete `AlertDialog`.
  - *Make it planned* — new transaction, non-transfer, no date and no due
    date; navigates to `EditPlannedScreen` carrying the current draft, same
    payload as today's `AddPrimaryAttributeButton`.

When `loanData.isLoanRecord`, the overflow shows nothing but *Duplicate* and
the type selector is hidden — the current screen's rule, preserved.

### Type selector

`SingleChoiceSegmentedButtonRow` with Expense / Income / Transfer, 24dp
horizontal padding. Emits `OnSetTransactionType`. Hidden for loan records.
This deletes the `ChangeTransactionTypeModal` round-trip.

### Amount headline

A clickable row: amount in `displayLarge` with `tabular-nums`, currency code
in `headlineSmall` `onSurfaceVariant`. Supporting line below in
`labelMedium`:

- expense/income: the selected account's name, or "Select account" when none.
- transfer: `≈ <converted amount> <currency>` when a conversion applies.

Amount colour follows the transaction-list rules already shipped: expense
`onSurface`, income `LocalIvyExtendedColors.current.income`, transfer
`colorScheme.primary`.

Tapping anywhere on the headline opens `AmountKeypadSheet`. The click target
carries an `onClickLabel` so TalkBack announces "Edit amount".

### Title field

Borderless `TextField` (transparent container colours, no indicator) with
`headlineSmall` text, 24dp padding, and a 1dp `outlineVariant` rule beneath.
Placeholder keeps the existing per-type strings (`expense_title`,
`income_title`, `transfer_title`). `ImeAction.Next` runs the focus chain's
next step.

Title suggestions render as a `FlowRow` of `SuggestionChip`s directly below
the field whenever the suggestion set is non-empty and the field has focus,
capped at `SUGGESTIONS_LIMIT`. This replaces the legacy vertical list gated
on keyboard visibility — chips don't push the rows off screen.

### Attribute rows

`SettingsItem` from `shared/ui/core`, unchanged, one per attribute. Each row
is `title` = the current value, `description` = the attribute's name, so a
filled-in screen reads as a summary and an empty one reads as a prompt.

| Row | Title | Description | Tap |
| --- | --- | --- | --- |
| Category | category name / "Choose category" | "Category" | `CategoryPickerSheet` |
| Account (expense, income) | account name / "Select account" | "Account · CUR" | `AccountPickerSheet` |
| From, To (transfer) | account names | "From · CUR", "To · CUR" | `AccountPickerSheet` |
| Date & time | formatted date-time / "Set date & time" | "Date & time" | `OnChangeDate`, then `OnChangeTime` |
| Due date (planned only) | formatted due date | "Due date" | existing date picker |
| Description | description first line / "Add description" | "Description" | `TextInputDialog` |
| Tags | "N tags" / "Add tags" | "Tags" | `ShowTagModal` (legacy) |

The category row's leading icon is the category's icon inside a tonal circle
tinted from the category colour, reusing the container/content derivation
already written for `TransactionItem` (`CategoryIconBubble`) — extracted to
a shared internal helper rather than duplicated.

### Exchange-rate row

Transfer only, when `customExchangeRateState.showCard` is true. A
`SettingsItem` variant on `warningContainer` (from `IvyExtendedColors`):
title "Custom exchange rate", description the current rate, trailing refresh
`IconButton` that emits `onExchangeRateChange(null)`. Tapping the row opens
`AmountKeypadSheet` in rate mode.

This replaces `CustomExchangeRateCard` *and* the `onGloballyPositioned` +
`animateScrollTo` scroll hack that currently drags the card into view — as a
row in the normal flow it needs no special handling.

### Bottom bar

`Scaffold`'s `bottomBar`: a full-width filled `Button` with
`navigationBarsPadding()` and IME padding. Label by mode, matching today's
`ActionButton` logic exactly:

- new transaction → "Add"
- editing, no due date → "Save"
- editing a planned payment with unsaved changes → "Save"
- editing a planned payment with no changes → "Pay" (expense) / "Get"
  (income), emitting `OnPayPlannedPayment`

## New components (`shared/ui/core`)

### `AmountKeypadSheet` — `com.ivy.ui.component.amount`

`ModalBottomSheet` containing, top to bottom:

1. **Amount display** — the live input, `displayMedium`, tabular, with the
   currency code trailing. When the input holds an arithmetic expression the
   expression is shown instead, with the evaluated result in
   `bodyMedium onSurfaceVariant` beneath it.
2. **Account chips** — `FilterChip` per account plus a trailing "New" chip
   opening the legacy `AccountModal`. Present only in amount mode, so the
   always-visible account row that today's drag sheet provides survives.
   Omitted in rate mode.
3. **Keypad** — a 4-column grid of tonal keys on `surfaceContainerHigh`,
   18dp corners, 52dp tall:

   ```
   7  8  9  ÷
   4  5  6  ×
   1  2  3  −
   .  0  ⌫  +
   [        Done        ]
   ```

   Merging the operators into the keypad is what lets `CalculatorModal`
   disappear: there is no second surface, just keys. The expression is
   evaluated with Keval (`libs.keval`, already in the version catalog) on
   *Done*; an unparseable expression disables *Done* rather than dismissing
   with a stale value.

   Decimal separator, decimal-count limit and input formatting reuse the
   existing helpers (`localDecimalSeparator`, `formatInputAmount`,
   `amountToDoubleOrNull`) so currency behaviour is unchanged.

The sheet is a dumb component: it takes an initial amount, a currency, an
account list and callbacks. Keval is added as a dependency of
`shared/ui/core`.

### `CategoryPickerSheet` — `com.ivy.ui.component.picker`

`ModalBottomSheet` listing categories as segmented tonal blocks — the
24/8dp corner rhythm with 4dp gaps already shipped on the transaction list —
each with its tonal category icon; the selected one carries a trailing check.
First entry is "No category"; last is "New category", opening the legacy
`CategoryModal`. Long-press is not carried over; editing a category stays in
its own screen.

### `AccountPickerSheet` — `com.ivy.ui.component.picker`

Same block treatment, listing accounts with name and currency, selected one
checked, trailing "New account" entry opening the legacy `AccountModal`.
Used by the account, From and To rows.

Both pickers take pre-formatted, immutable UI models — no legacy
`Account`/`Category` types cross into `shared/ui/core`, per the project's
screen-architecture rules. Mapping happens in `feature/edit-transaction`.

## Behaviour parity

Every existing event keeps a trigger. The rewrite is complete only when each
row of this table is wired:

| Event | Triggered by |
| --- | --- |
| `OnAmountChanged` | keypad sheet *Done* |
| `OnTitleChanged` | title field, suggestion chip |
| `OnDescriptionChanged` | description dialog |
| `OnCategoryChanged` | category picker sheet |
| `OnAccountChanged` | account row, keypad account chips |
| `OnToAccountChanged` | "To" row (transfer) |
| `OnDueDateChanged` | due-date row |
| `OnChangeDate`, `OnChangeTime` | date & time row |
| `OnSetTransactionType` | segmented button row |
| `OnPayPlannedPayment` | bottom bar in Pay/Get state |
| `Delete` | overflow → delete dialog confirm |
| `Duplicate` | overflow |
| `CreateCategory`, `CreateAccount` | legacy modals, launched from the pickers |
| `Save`, `SetHasChanges` | bottom bar; `Save(false)` for planned-payment saves |
| `UpdateExchangeRate` | exchange-rate row and its refresh button |
| `TagEvent.*` | `ShowTagModal`, unchanged |

Preserved behaviours that are easy to lose in a rewrite, called out so the
review can check them:

- Keypad auto-opens when `initialTransactionId == null`.
- Focus chain: amount → category (if unset) → title (if unset).
- Loan-record transactions hide the type selector and show the loan caption
  under the amount.
- Changing account currency on a loan transaction raises the existing
  confirmation dialog before `OnAccountChanged`.
- `backgroundProcessingStarted` shows the blocking progress dialog.

## Verification

The screen has no screenshot coverage today. This round adds:

- `AmountKeypadSheetPaparazziTest`, `CategoryPickerSheetPaparazziTest`,
  `AccountPickerSheetPaparazziTest` — light and dark, via the existing
  `PaparazziScreenshotTest` + `@TestParameter` harness.
- `EditTransactionScreenPaparazziTest` — light and dark, covering new
  expense, edit with everything filled, and transfer with a custom rate,
  through an `EditTransactionUiTest(isDark:)` entry point in main source, the
  convention `SettingsUiTest` established.
- `detekt`, `ktlint` and the repo's compose-stability check must pass; no new
  baseline entries.

Definition of done: every table row above wired, screenshots recorded and
verified, lint clean, and the screen rendering with `isLegacy = false`.
