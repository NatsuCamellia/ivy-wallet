# M3 Expressive Migration — Phase 2: Settings Screen Redesign

## Vision

Rebuild `SettingsScreen` in the visual style of ReadYou's settings pages
(the reference app already used to guide Phase 1's toolchain choices),
making it the second screen migrated off the legacy design system and the
first *legacy* screen flipped to `isLegacy = false`. The redesign also
completes a Phase 1 roadmap item: a user-facing toggle for dynamic color
vs. Ivy brand color.

This is a view-layer redesign. The screen keeps its single-page structure
(no ReadYou-style hub-and-spoke subpages), keeps every existing setting,
and keeps the existing ViewModel logic except where the new UI needs a
different contract (theme selection, dynamic color).

## Scope decisions (settled during brainstorming)

- **Single page**, ReadYou visual style — not ReadYou's hub-and-spoke
  navigation structure.
- **Reorganized sections** (see layout below) — same items, cleaner groups.
  Nothing is pruned.
- **Legacy bottom-sheet modals are replaced** with Material 3 dialogs.
- **Appearance gains both** a proper theme selector dialog (replacing
  tap-to-cycle) and a new dynamic-color switch.
- **Shared building blocks live in `shared/ui/core`** (Approach A): written
  fresh in Ivy conventions, not ported from ReadYou's GPL sources, because
  every later screen migration will reuse them.

## Screen layout

`SettingsScreen` becomes a plain M3 screen: `Scaffold` with a transparent
top bar containing only the back arrow, and a `LazyColumn` whose first item
is a large display-style "Settings" title (`displaySmall`-scale, ReadYou's
`DisplayText` look) with the app version string as its description line.
Tapping the version still opens `ReleasesScreen` (preserving today's
shortcut).

Seven groups follow. Each is a small section title (`labelLarge`, primary
color) above full-width setting rows (24dp start padding, title + optional
grey description + optional trailing control). Rows are text-only — the
legacy per-row icons (`ic_custom_*`) are dropped, matching ReadYou's inner
settings pages. Switch rows toggle when tapped anywhere on the row.

1. **Profile** — Name (current name as description; opens text input
   dialog) · Currency (code as description; opens currency picker dialog).
2. **Appearance** — Theme (current theme as description; radio dialog:
   Auto / Light / Dark / AMOLED) · Dynamic color (switch; row visible only
   on Android 12+) · Language (visible under the existing
   `languageOptionVisible` flag; keeps current switch-language behavior).
3. **Behavior** — Start date of month (day as description; radio dialog
   1–31) · Treat transfers as income/expense (switch, keeps its
   description) · Exchange rates (→ `ExchangeRatesScreen`) · Advanced
   features (→ `FeaturesScreen`).
4. **Privacy** — Lock app · Show notifications · Hide balance · Hide income
   (all switches; hide balance/income keep their descriptions).
5. **Import & Export** — Import data (→ `ImportScreen`) · Backup data ·
   Export to CSV (keeps its "not for backup purposes" description).
6. **About & Support** — Rate us on Google Play · Share Ivy Wallet ·
   GitHub (open source) · Telegram · Help center · Releases · Report bug ·
   Request a feature · Contact support · Contributors · Attributions ·
   Terms & Conditions · Privacy policy. All become plain rows — the
   gradient buttons and pill-shaped T&C/privacy buttons disappear.
7. **Danger zone** — Delete all user data (title tinted
   `MaterialTheme.colorScheme.error`).

## Shared components (`shared/ui/core`)

New composables, written from scratch following Ivy conventions, each with
`@Preview`s and Paparazzi coverage:

- `SettingsItem(title, description?, enabled, onClick, trailing)` — the row.
- `SettingsSectionTitle(text, color = primary)` — the group header.
- `ScreenDisplayTitle(text, description?)` — the large screen header.
- `RadioSelectionDialog(title, options, selectedIndex, onSelect)` — used by
  the theme selector and start-date picker.
- `TextInputDialog(title, initialValue, onConfirm)` — used by the name
  editor.
- `ProgressDialog(title, description)` — non-dismissible, Expressive
  `LoadingIndicator`; replaces `ProgressModal` for the export flow.

Delete confirmations use stock M3 `AlertDialog`, preserving the existing
two-step confirmation flow.

The **currency picker dialog stays private to `feature/settings`**: it
depends on `IvyCurrency` (legacy code), and `shared/ui/core` must not gain
a legacy dependency. It is a searchable list dialog — text field filtering
a `LazyColumn` of currencies, grouped fiat/crypto.

Legacy modals (`CurrencyModal`, `NameModal`, `ChooseStartDateOfMonthModal`,
`DeleteModal`, `ProgressModal`) are **not deleted** — other legacy screens
still use them; this screen just stops calling them.

## Architecture & state

MVI shape unchanged (`ComposeViewModel<SettingsState, SettingsEvent>`).
Contract changes only:

- `SettingsState` gains `dynamicColorEnabled: Boolean` and
  `dynamicColorAvailable: Boolean` (ViewModel checks
  `Build.VERSION.SDK_INT >= S`; the composable stays dumb).
- `SettingsEvent.SwitchTheme` is **replaced** by `SetTheme(Theme)` — the
  radio dialog sends an explicit choice. New event:
  `SetDynamicColor(Boolean)`.
- All other events, ViewModel logic, and persistence paths (name, currency,
  toggles, backup/export, delete flows) stay as they are.

### Dynamic-color persistence

A new boolean preference in the existing DataStore (`shared/data/core`),
default `true`, exposed via a small datasource with `Flow<Boolean>` and a
suspend setter.

Plumbing note: non-legacy screens are themed through `temp/old-design`'s
`IvyTheme` (which wraps `IvyMaterial3Theme`), and `RootActivity` has a
second `IvyMaterial3Theme` call site for the date-time picker. Rather than
threading a parameter through legacy code, `shared/ui/core` gains a
`LocalIvyColorSource` CompositionLocal (default `IvyColorSource.Dynamic`)
and `IvyMaterial3Theme`'s `colorSource` parameter defaults to it. Consumers:

- `RootActivity` collects the preference and provides `LocalIvyColorSource`
  (`Dynamic` when enabled, `BrandSeed(IvyColors.Purple.primary)` when
  disabled) once, above `setContent`'s tree — no existing call site changes.
  This completes the Phase 1 roadmap item.
- `SettingsViewModel` reads the preference for `uiState()` and writes it on
  `SetDynamicColor`.

### Legacy flip

`SettingsScreen.isLegacy` → `false` in `Screens.kt`, so `NavigationRoot`
wraps it in `IvyMaterial3Theme` automatically. The composable drops its
`BoxWithConstraintsScope` receiver; the `IvyNavGraph` call site updates
accordingly.

## Error handling

No new error states. Existing ViewModel paths keep their behavior. The
DataStore write is a fire-and-forget suspend call in `viewModelScope`; a
failed write leaves the previous value.

## Testing

- **Unit**: extend `SettingsViewModel` tests for `SetTheme` and
  `SetDynamicColor` (Given-When-Then, MockK, Kotest `shouldBe`).
- **Screenshot**: re-record settings Paparazzi baselines (`SettingsUiTest`
  renders the new design); new Paparazzi tests for each shared component
  in `shared/ui/core`.
- **Static**: `detekt` and Android Lint stay clean with no new baseline
  entries.
- **CI mirror**: `testDebugUnitTest`, `detekt`, `lintR`,
  `verifyPaparazziDebug`, compose-stability — all green before done.
- **Manual emulator pass**: every row and dialog works; the dynamic-color
  switch visibly re-themes the screen; delete flow remains two-step; legacy
  screens are unaffected.

## Out of scope

- Hub-and-spoke settings subpages.
- Pruning or merging any settings item.
- Migrating any other legacy screen.
- Deleting legacy modal code from `temp/old-design`.
- Changing the `Theme` enum or AMOLED/true-black behavior (the theme dialog
  exposes the existing four options as-is).
