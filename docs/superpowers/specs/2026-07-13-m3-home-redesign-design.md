# M3 Expressive Migration — Phase 3: Home Screen Redesign

## Vision

Restyle the Home tab and its shared shell (bottom navigation, add-transaction
FAB, MoreMenu) in Material 3 Expressive, continuing the Settings redesign's
flatter direction: drop the hand-drawn gradient/shadow cards in favor of M3
tonal surfaces, and where a stock Expressive component now exists for
something that's hand-built today (the FAB menu), replace the bespoke
implementation with the stock one instead of just reskinning it.

Home is the app's single most-used screen and lives inside `MainScreen` (the
Home/Accounts tab host). The Phase 1 roadmap flagged `MainScreen` as unable to
flip `isLegacy = false` safely, because `NavigationRoot` cleared the
activity-level `ViewModelStore` on every non-legacy screen exit. That specific
danger has since been removed on `main` (commit `c146fd95`, landed as part of
the Settings redesign work) — but no scoped per-screen `ViewModelStoreOwner`
exists yet, so VMs now simply live for the Activity's lifetime instead of
being destroyed. This round deliberately still keeps `MainScreen.isLegacy =
true`: a visual-only reskin is the right size of change for how deeply
`HomeTab`'s content is wired into the legacy design system today (`UI.colors`,
`UI.typo`, `IvyOutlinedButton`, custom `Gradient` brushes, `drawColoredShadow`).
Flipping the flag isn't needed to reach M3 tokens anyway: `IvyUI` already
themes every screen, legacy or not, through `IvyTheme` → `IvyMaterial3Theme`,
so `MaterialTheme.colorScheme`/`typography`/`shapes` are already available
inside `HomeTab`. `isLegacy` only controls an extra `Surface` wrapper.

## Scope decisions (settled during brainstorming)

- **Visual-only reskin; `MainScreen.isLegacy` stays `true`.** No navigation or
  ViewModel-scoping work this round.
- **Shell is in scope.** `MainBottomBar` (tab row + add-transaction FAB, in
  `feature/main`) and `HomeMoreMenu` (in `feature/home`) are restyled
  alongside `HomeTab`'s own content — they're always on screen together, and
  a legacy/M3 visual mismatch between them would be worse than leaving
  everything legacy.
- **Gradient cards become flat M3 tonal cards.** The income/expense cards
  (`HeaderCard` in `HomeHeader.kt`) and `CustomerJourneyCard` drop their
  custom `Gradient` brush + `drawColoredShadow` in favor of `colorScheme`
  container roles (e.g. `primaryContainer` / `tertiaryContainer` /
  `errorContainer`) and standard M3 elevation.
- **FAB rebuilt on stock M3 Expressive `FloatingActionButtonMenu`.** Verified
  directly against the pinned `androidx.compose.material3:material3-android:
  1.5.0-alpha23` sources: `FloatingActionButtonMenu`, `ToggleFloatingActionButton`,
  and `FloatingActionButtonMenuItem` all exist and carry no
  `@ExperimentalMaterial3ExpressiveApi` gate. This replaces `MainBottomBar`'s
  hand-rolled pixel-math button positioning and radial reveal animation. The
  drag-up gesture shortcuts (drag up/left/right on the FAB for
  expense/income/transfer) are dropped: they're undiscoverable UI today, and
  the stock component doesn't support them — tap-to-expand plus a
  `FloatingActionButtonMenuItem` per action (income/expense/transfer/planned
  payment) covers the same functionality.
- **MoreMenu rebuilt as a stock M3 `ModalBottomSheet`.** Replaces
  `HomeMoreMenu`'s custom full-screen circular-reveal `Canvas` transition and
  swipe-up-to-close gesture with the standard bottom-sheet slide-up and
  swipe-down-to-dismiss. Content (search entry, quick-access icon grid,
  buffer battery, open-source link) is preserved, grouped into sections in
  the Settings redesign's row/section visual style. Reuses
  `SettingsSectionTitle` from `shared/ui/core` for section headers where it
  fits; the quick-access grid's icon+label buttons are a new, Home-specific
  composable (not extracted to `shared/ui/core` — no other current screen
  needs an icon-grid button, so it stays local per YAGNI).
- **Bottom tab row rebuilt on stock M3 `NavigationBar` / `NavigationBarItem`**
  (2 items: Home, Accounts), replacing the custom `Tab` composable's manual
  selected-state coloring.
- **Shared legacy components stay legacy.** The transaction list rendering
  (`temp/legacy-code`'s `transactions()` and the upcoming/overdue due
  sections) and the ephemeral picker modals still used by other legacy
  screens (`CurrencyModal`, `ChoosePeriodModal`, `BufferModal`) are untouched
  this round — restyling them in isolation would either visually mismatch
  their other legacy consumers (Accounts, Reports, Budgets, etc.) or drag
  those screens into this round's scope. The one exception: the
  skip-all-planned-transactions confirmation (`DeleteModal` usage in
  `HomeTab`) is swapped for a stock M3 `AlertDialog`, mirroring exactly what
  the Settings redesign already did for its own delete confirmation.
- **`HomeHeader`'s scroll-linked collapse animation is kept as-is
  mechanically**, just re-skinned to M3 color/typography/shape tokens rather
  than rebuilt around `TopAppBarScrollBehavior` — it's small, self-contained,
  and already works, unlike the FAB/MoreMenu.
- **All existing gesture behavior is preserved**: swipe down on the
  balance/cashflow area opens MoreMenu (now a bottom sheet instead of a
  circular reveal), horizontal swipe on Home switches to the Accounts tab.

## Screen layout

### HomeTab content

`HomeHeader`'s sticky row (greeting / mini balance row, month-period button)
keeps its current structure and collapse-on-scroll behavior, re-skinned:
`IvyOutlinedButton` → M3 `OutlinedButton` (or `FilledTonalButton`), text
styles → `MaterialTheme.typography`, colors → `MaterialTheme.colorScheme`.

`CashFlowInfo`'s balance row and the income/expense `HeaderCard`s become flat
M3 tonal cards (see above) with `MaterialTheme.shapes` corner radii instead of
`UI.shapes.r4`. The cashflow delta line (`+X` / `-X`) uses
`colorScheme.primary`/`error` instead of the current `Green`/`Gray` constants.

`CustomerJourneyCard` becomes a tonal card matching the income/expense cards'
new style, keeping its title/description/CTA/dismiss layout unchanged.

The transaction list itself (`HomeLazyColumn`'s `transactions()` call,
upcoming/overdue sections) is unchanged — see scope decisions above.

### Shell

- `MainBottomBar`: `NavigationBar` with two `NavigationBarItem`s (Home,
  Accounts) sized to leave room for the centered FAB, matching the current
  layout's visual balance. The add-transaction FAB becomes a
  `FloatingActionButtonMenu`: a `ToggleFloatingActionButton` as the trigger,
  expanding into `FloatingActionButtonMenuItem`s for Income, Expense,
  Transfer, and Planned Payment on the Home tab; on the Accounts tab the FAB
  keeps its current single-tap "add account" behavior (no menu).
- `HomeMoreMenu`: `ModalBottomSheet` triggered the same way as today (swipe
  down on the cashflow area, or tapping the existing floating trigger
  button — which itself becomes a standard `FloatingActionButton` or icon
  button instead of the hand-positioned `CircleButtonFilled`). Sheet content,
  top to bottom: search entry row, "Quick access" section
  (`SettingsSectionTitle` + icon-grid of Settings / Categories / Theme
  (cycles via existing `HomeEvent.SwitchTheme`) / Planned Payments / Share /
  Reports / Budgets / Loans), buffer battery row, open-source link row.

## Architecture & state

MVI shape unchanged (`ComposeViewModel<HomeState, HomeEvent>`). No new events
or state fields are needed — every interaction this redesign touches already
has a corresponding `HomeEvent` (`SetExpanded`, `SwitchTheme`, `SetBuffer`,
`SetCurrency`, `SetPeriod`, `SkipAllPlanned`, etc.); this is purely a
view-layer rebuild of `HomeTab.kt`, `HomeHeader.kt`, `HomeMoreMenu.kt`, and
`MainBottomBar.kt`/`MainScreen.kt`.

`HomeEvent.SwitchTheme` (tap-to-cycle) is kept as-is for the MoreMenu
quick-access button — unlike Settings' dedicated `SetTheme(Theme)` radio
dialog, this is a fast shortcut in a transient sheet, a different UX context
that doesn't need an explicit chooser.

## Error handling

No new error states. All existing ViewModel logic and event handling is
unchanged.

## Testing

- **Screenshot**: re-record `HomePaparazziTest` baselines (`HomeUiTest` /
  `PreviewHomeTab`) against the new visuals. Add new Paparazzi coverage for
  `MainBottomBar` (collapsed and expanded FAB-menu states, both tabs) and
  `HomeMoreMenu` (bottom-sheet content) — neither currently has a
  `*PaparazziTest.kt`, only `@Preview`s.
- **Unit**: no `HomeViewModel` contract changes, so no new unit tests are
  required beyond keeping existing ones green.
- **Static**: `detekt` and Android Lint stay clean with no new baseline
  entries.
- **CI mirror**: `testDebugUnitTest`, `detekt`, `lintR`, `verifyPaparazziDebug`,
  compose-stability — all green before done.
- **Manual emulator pass**: FAB menu opens/closes and each action navigates
  correctly on both tabs; MoreMenu bottom sheet opens via swipe-down and
  trigger button, every row/quick-access action still works, buffer battery
  and theme-cycle still function; Home↔Accounts swipe and tab-tap navigation
  unaffected; skip-all-planned confirmation still two-step; legacy screens
  reached from Home (Categories, Budgets, Loans, etc.) render unaffected.

## Out of scope

- Flipping `MainScreen.isLegacy` (needs a scoped `ViewModelStoreOwner` design
  — still not built, only the crash-causing store-clear was removed).
- Restyling the transaction list (`transactions()`, upcoming/overdue
  sections) — shared with other still-legacy screens.
- Restyling `CurrencyModal`, `ChoosePeriodModal`, or `BufferModal` — shared
  with other still-legacy screens; only the skip-all confirmation
  (`DeleteModal` usage) is swapped, matching the Settings redesign precedent.
- Restyling the Accounts tab itself (`AccountsTab`) beyond the shared shell
  it renders inside.
- Any drag-gesture shortcuts on the new FAB — dropped, not reintroduced in a
  new form.
- Deleting `temp/old-design`, `shared/base/legacy/Theme.kt`, or `IvyColors` —
  still depended on by every other legacy screen.
