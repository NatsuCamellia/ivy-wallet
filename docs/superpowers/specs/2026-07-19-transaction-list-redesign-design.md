# M3 Expressive Migration — Home Transaction List Redesign

## Vision

Replace the legacy gradient `TransactionCard` on the Home tab with an M3
Expressive **segmented list**: compact rows grouped per day, each transaction
its own tonal block with 4dp gaps — large (24dp) corners on the ends of a day
group, small (8dp) corners between rows. This is the list treatment the
Phase 3 home-redesign spec (`2026-07-13-m3-home-redesign-design.md`)
explicitly deferred; it continues the same visual language (flat
`colorScheme` tonal surfaces, no gradients, no all-caps labels) that the
Settings and Home-header redesigns established.

Chosen during brainstorming from three mocked-up directions (A: continuous
grouped card with hairline dividers, B: segmented rows, C: modernized rich
card). **Option B** won: near-maximal density while every transaction keeps
its own shape and tap target, and due/overdue items expand in place without
breaking the group rhythm.

## Scope decisions (settled during brainstorming)

- **Home tab only.** The legacy `transactions()` renderer
  (`temp/legacy-code`) is shared with Search, Transactions, and Reports —
  those screens keep it untouched. New components are built reusable in
  `shared/ui/core` so later screen redesigns can adopt them, but only
  `HomeTab` switches now. This resolves the July 13 spec's reason for
  deferring (restyling the shared renderer would drag other legacy screens
  along) by not touching the shared renderer at all.
- **Dumb components + pre-formatted UI model.** Per the project's screen
  architecture rules, the new composables take an immutable UI model with
  already-formatted strings — no legacy `Transaction`/`AppBaseData` types in
  `shared/ui/core`. Mapping from legacy types happens in `feature/home`.
- **Deliberate simplifications** (approved):
  - Description and tags no longer render on the row — they remain visible
    on the edit/detail screen only.
  - Category/account are no longer separate tap targets; the whole row opens
    the transaction. The legacy chip-tap → filtered `TransactionsScreen`
    shortcut goes away on Home (still available via Accounts/Categories).
  - The "account-specific color in transactions" setting becomes a no-op on
    Home: account names render as plain supporting text.

## Components (`shared/ui/core`, package `com.ivy.ui.component.transaction`)

### `TransactionItem`

One row, `surfaceContainer` background, full-row ripple + `onClick`:

- **Leading**: 40dp circular tonal icon container carrying the category
  color. The container/content pair is derived from the raw category ARGB,
  harmonized with the current theme (container blended toward the surface,
  icon tint contrast-adjusted; exact blend math decided in the plan, with
  the acceptance criterion that the icon stays legible on its container in
  both light and dark). The icon itself is a slot
  (`icon: @Composable () -> Unit`) so the legacy item-icon resolver stays in
  the caller and out of `shared/ui/core`.
- **Middle**: title (`bodyLarge`, medium weight, single line ellipsized)
  and a supporting line (`Category · Account`, or `From → To` for
  transfers) in `onSurfaceVariant`.
- **Trailing**: signed formatted amount plus a small time label; transfers
  with cross-currency show the received amount as the secondary line
  instead of the time.
- **Shape**: driven by a `position` parameter — `Single` (24dp all),
  `First` (24/24/8/8), `Middle` (8dp all), `Last` (8/8/24/24). Groups are
  laid out with 4dp vertical gaps.

Amount color by kind: expense `onSurface` (calm, not alarming), income the
extended income green (the only green on screen), transfer
`colorScheme.primary`, upcoming the extended warning color, overdue
`colorScheme.error`.

### Due variant

When the transaction is a planned payment (due date, not yet paid) the row
gains a due chip under the title — tonal warning container for upcoming
("Due Fri, Jul 24"), error container for overdue — and a trailing button row
inside the same block: `FilledTonalButton` **Skip** and filled `Button`
**Pay**/**Get**, replacing the legacy gradient button pair.

### `TransactionDayHeader`

Replaces the legacy `HistoryDateDivider`: date label ("Today", "Yesterday",
otherwise a formatted date) with the day's net total (income − expense,
signed, base currency) right-aligned in `onSurfaceVariant`. Plain text on the
screen background — no divider line.

### `TransactionSectionHeader`

Replaces the legacy `SectionDivider` for Upcoming/Overdue: title colored by
severity (warning / error), income–expense totals as supporting text,
expand/collapse chevron, whole header toggles the existing expanded state
from the ViewModel. The Overdue header carries a trailing **Skip all**
`TextButton` (keeps the existing confirmation dialog flow).

### Extended semantic colors

Dynamic color schemes guarantee no green or orange role, so a small
`IvyExtendedColors` holder (income green + warning orange, light/dark pairs)
is provided via a `LocalIvyExtendedColors` CompositionLocal from
`IvyMaterial3Theme`, mirroring the `LocalIvyColorSource` precedent.

## Home wiring (`feature/home`)

- A mapper (pure functions or a small injected class) converts legacy
  `Transaction` + `AppBaseData` + `LegacyDueSection` + the time
  formatter/converter locals into the UI models: formatted amounts (existing
  format utils), titles with category-name fallback, supporting lines,
  due-chip text, and **group positions** computed from the flat
  `TransactionHistoryItem` list's day segments.
- A new `LazyListScope` extension in `feature/home` replaces the legacy
  `transactions(...)` call in `HomeTab`'s `HomeLazyColumn`, preserving:
  - lazy item keys (`transaction.id`, date strings) as today;
  - Upcoming and Overdue collapsible sections driven by the existing
    `HomeEvent`s (`SetUpcomingExpanded`, `SetOverdueExpanded`);
  - Pay/Get, Skip, and Skip-all flows through the existing event handlers
    and confirmation dialogs — no ViewModel behavior changes;
  - row click → `EditTransactionScreen` navigation;
  - the empty state (restyled minimally to `MaterialTheme` typography and
    `onSurfaceVariant`) and the trailing scroll-hack spacer (still needed
    for FAB clearance).
- `CashFlowInfo`, `TransactionsDividerLine` handling, customer-journey
  cards, and everything else in `HomeTab` are untouched by this round.

## Testing

- Paparazzi screenshot tests for the new components alongside the existing
  shared-component tests: expense / income / transfer / cross-currency
  transfer / upcoming-with-buttons / overdue states, each light + dark,
  with `IvyColorSource.BrandSeed` pinned for determinism.
- Unit tests for the mapper: title fallback, transfer supporting line,
  group-position computation (single-item day, first/middle/last), day net
  total formatting, due-chip text selection (upcoming vs overdue).

## Out of scope

- Adopting the new components on Search, Transactions, Reports, or the
  planned-payments screens (each in its own later redesign round).
- Deleting the legacy `TransactionCard`/`transactions()` — still used by
  those screens.
- Rendering description/tags on the row, or restoring per-chip tap targets.
- Any ViewModel/state refactoring beyond the view-layer mapper.
- Swipe actions, shape-morph press animations, or other Expressive motion
  extras (possible follow-ups once the static design lands).
