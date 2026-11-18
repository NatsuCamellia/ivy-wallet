# M3 Expressive Migration — Phase 1: Design Token Foundation

## Vision

Ivy Wallet currently ships two parallel design systems: the deprecated custom
system (`IvyColors` / `UI.colors`, `shared/base/legacy/Theme.kt`,
`temp/old-design`) used by every `isLegacy = true` screen, and a thin
Compose Material 3 layer (`IvyMaterial3Theme`) used by a handful of
already-migrated screens. The end goal is to retire the old design system
entirely and make native Material 3 Expressive the app's only design system.

This is a multi-round migration. **This spec covers Phase 1 only**: building
the M3 Expressive design-token foundation and validating it on one screen.
Later rounds migrate individual `isLegacy` screens onto these tokens and
eventually delete the old design system's code.

### Why start here (context from prior attempt)

An earlier attempt at this migration (3 commits: toolchain bump, theme
rewrite, Home shell rewrite) was deliberately reset because the instructions
given to the coding agent were unclear and it made unexpected changes. This
spec exists to nail down a clear, reviewed scope before any implementation
starts again.

## Phase 1 scope

1. Toolchain upgrade (one-shot).
2. Rewrite `IvyMaterial3Theme` to carry full M3 Expressive design tokens:
   color, shape, typography, motion.
3. Validate the tokens visually on one small, already-non-legacy screen
   (`AttributionsScreen`), including updated Paparazzi baselines.

### Out of scope for Phase 1

- Flipping `isLegacy` for any other screen (`MainScreen`, `SettingsScreen`,
  etc. all remain legacy).
- A user-facing Settings toggle for dynamic-color vs. brand-color. The
  architecture supports both; this round hardcodes the choice to dynamic
  color at the call site.
- Deleting any old-design-system code (`temp/old-design`,
  `shared/base/legacy/Theme.kt`, `IvyColors`) — it keeps serving legacy
  screens until they're migrated in later rounds.
- Changing AMOLED_DARK/true-black UX or the `Theme` enum — `isTrueBlack` is
  re-plumbed into the new color pipeline as-is, no behavior change.
- Introducing a seed-based brand palette generator library if one isn't
  already available in the pinned Compose alpha (see Color tokens below) —
  if it's not available, the brand-color fallback path may need a follow-up
  spec.

## Toolchain

Verified directly (not from stale notes) against `androidx.compose.material3`
1.4.0 sources: `MaterialExpressiveTheme` and `MotionScheme` are literally
`internal` in the current stable release. The public,
`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`-gated versions only exist
starting in the `1.5.0-alpha` line. `ReadYou` (the reference app) confirms
this is the intended path — its `app/build.gradle.kts` depends on
`platform(libs.compose.bom.alpha)` for the main app target, with
`compileSdk = 36`, AGP `8.13.0`, Kotlin `2.2.0`.

Ivy Wallet's current toolchain (`compileSdk 34`, AGP `8.5.2`, Kotlin
`2.0.20`, `compose-material3 1.2.1` pinned directly rather than via BOM) is
far behind what's needed. Upgrade in one commit per dependency (no stepwise
bumps):

- `compileSdk`: 34 → 36 (`minSdk` 28 stays; `targetSdk` follows `compileSdk`
  per current convention).
- AGP: 8.5.2 → 8.13.0.
- Kotlin: 2.0.20 → 2.2.0 (required for compose compiler compatibility with
  the target Compose version).
- Compose: replace the direct `compose-material3` version pin with
  `compose-bom-alpha`, pinned to the newest alpha release where
  `material3`'s `MaterialExpressiveTheme`/`MotionScheme`/expressive
  components are public and which compiles cleanly against `compileSdk 36`
  — confirm the exact version number at implementation time (verify against
  compileSdk requirements the same way this spec verified 1.4.0 vs.
  1.5.0-alpha; don't assume ReadYou's exact pin is still current).

Each of the above is a single commit, semantic/conventional style
(`build: ...`), no long commit bodies.

## Design tokens

All of the below live in `IvyMaterial3Theme`
(`shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt`),
which wraps content in `MaterialExpressiveTheme` behind
`@OptIn(ExperimentalMaterial3ExpressiveApi::class)` — following the
codebase's existing convention of per-usage `@OptIn` rather than a blanket
compiler flag.

### Color

- New signature accepts a color-source input (architecture: a sealed type
  with `Dynamic` and `BrandSeed` cases), but every call site this round
  passes `Dynamic`. No user-facing switch is built yet.
- `Dynamic`: Android 12+ (`Build.VERSION.SDK_INT >= S`) uses
  `dynamicColorScheme` derived from the device wallpaper. Below API 31,
  falls back to `BrandSeed`.
- `BrandSeed`: a tonal-palette-generated `ColorScheme` seeded from
  `IvyColors.Purple.primary` (`#5C3DF5`), replacing today's fully
  hand-authored `ivyLightColorScheme()`/`ivyDarkColorScheme()` functions.
  The exact seed→scheme generation API needs to be confirmed against
  whichever Compose alpha gets pinned (some alpha lines expose this
  directly in `material3`; otherwise a color-utilities dependency may be
  needed — resolve during implementation).
- `isTrueBlack: Boolean` parameter is preserved on `IvyMaterial3Theme`:
  when true, overrides `background`/`surface`/`surfaceContainer*` to pure
  black after the scheme (dynamic or brand-seed) is generated. Behavior
  parity with today, not a new feature.

### Shape

- Drop the codebase's scattered hardcoded shapes (e.g.
  `RoundedCornerShape(12.dp)` on individual `Card`s) in favor of M3's
  Expressive default `Shapes()`, passed into `MaterialExpressiveTheme`.
  Components read `MaterialTheme.shapes.*` instead of inlining corner radii.

### Typography

- Base `Typography` on the Expressive default `Typography()` (wider type
  scale than stable M3), with every text style's `fontFamily` overridden to
  Open Sans — the brand font already bundled as font resources for the
  legacy design system (`temp/old-design`). This keeps the migrated UI
  visually consistent with not-yet-migrated legacy screens instead of
  falling back to Roboto.

### Motion

- `MotionScheme.expressive()` passed into `MaterialExpressiveTheme`, so
  every stock M3 component (buttons, cards, app bars, etc.) picks up
  spring-based default animation automatically — no per-component animation
  code needed.

## Validation: AttributionsScreen

`feature/attributions/src/main/java/com/ivy/attributions/AttributionsScreen.kt`
is the Phase 1 proof point: already `isLegacy = false`, already plain
Compose M3 (`Scaffold` + `TopAppBar` + `LazyColumn` of `Card`s), already has
a Paparazzi test (`AttributionsScreenPaparazziTest`).

Changes:

- Remove the hardcoded `shape = RoundedCornerShape(12.dp)` on `AttributionCard`
  — let it use the theme's default `Card` shape.
- No other structural changes to this screen; the point is to observe the
  new color/shape/type/motion tokens flow through unmodified M3 component
  usage.
- Regenerate Paparazzi baselines (`recordPaparazziDebug`) once the new
  tokens are in place, then run `verifyPaparazziDebug` to confirm they're
  stable.

## Testing / verification

- `./gradlew :feature:attributions:testDebugUnitTest` and
  `:shared:ui:core:testDebugUnitTest` (or whichever module hosts
  `IvyMaterial3Theme`) still pass.
- `./gradlew detekt` — no new violations (baseline only covers
  pre-existing ones).
- `./gradlew verifyPaparazziDebug` passes after baselines are re-recorded.
- Manual emulator check: launch the app, confirm legacy screens (still on
  the old design system) render unaffected, and navigate to
  Settings → Attributions to see the new tokens in effect.

## Roadmap beyond Phase 1 (not speced here)

- Per-screen migration: flip `isLegacy = false` screen by screen, starting
  with small/simple screens (mirroring the "smallest first" approach used
  to pick `AttributionsScreen` here).
  `MainScreen` is a special case — flipping it requires a scoped
  ViewModelStore design first, since `NavigationRoot` currently clears the
  activity-level `ViewModelStore` on every Home→X navigation.
- A Settings UI toggle for dynamic-color vs. brand-color, once there's a
  second migrated screen to make it meaningful to test.
- Deleting `temp/old-design`, `shared/base/legacy/Theme.kt`, and `IvyColors`
  once no screen depends on them anymore.
