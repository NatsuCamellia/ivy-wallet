# M3 Expressive Migration — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade Ivy Wallet's toolchain to a version that can run real Material 3 Expressive APIs, rewrite `IvyMaterial3Theme` to carry full Expressive design tokens (color, shape, typography, motion), and prove the tokens work end-to-end on `AttributionsScreen`.

**Architecture:** `IvyMaterial3Theme` (in `shared/ui/core`) is the single theme entry point every non-legacy screen (and the Paparazzi test harness) already wraps its content in. This plan upgrades the toolchain those APIs require, then rewrites that one function to source colors from either the device wallpaper (`Dynamic`, production default) or a seeded tonal palette (`BrandSeed`, used by tests and pre-Android-12 devices), while wiring Expressive shapes, an Open-Sans-flavored Expressive type scale, and `MotionScheme.expressive()`.

**Tech Stack:** Kotlin 2.2.10+, AGP 9.1.x, Gradle 9.1.x, Jetpack Compose (`compose-bom-alpha` 2026.06.01), `androidx.compose.material3:material3:1.5.0-alpha23`, `com.materialkolor:material-kolor:4.1.1`, Paparazzi 2.0.0-alpha02.

Spec: `docs/superpowers/specs/2026-07-11-m3-expressive-phase1-design.md`

## Toolchain revision (recorded during Task 3, supersedes the original Global Constraints below)

Task 1 originally targeted `compileSdk 36` / AGP `8.13.0` on the belief that `material3 1.5.0-alpha23` had dropped its `compileSdk 37` requirement. During Task 3, direct inspection of the published AARs' `aar-metadata.properties` (not release-note prose) showed this was wrong: every `material3-android` release from `1.5.0-alpha19` through the current latest (`1.5.0-alpha23`) requires `minCompileSdk=37` / `minAndroidGradlePluginVersion=9.1.0`. Only `1.5.0-alpha18` and earlier work with compileSdk ≤36/AGP 8.x, and alpha18 lacks a clean public API gate for `MaterialExpressiveTheme`/`MotionScheme` (marked `@Material3ExpressiveApi`/`@RestrictTo(LIBRARY_GROUP_PREFIX)`, a lint-only restriction, not `@RequiresOptIn`).

Human decision: keep the literal `material3 1.5.0-alpha23` pin and bump the floor instead. Revised targets for the remainder of this plan:

- `compileSdk` / `targetSdk`: **37** (was 36). `minSdk` stays 28.
- AGP: **9.1.x** (was 8.13.0). Gradle wrapper: **9.1.x** (AGP 9.1.0's documented minimum; Gradle 9 itself requires JDK 17 minimum, already satisfied by this environment's `JAVA_HOME`).
- Kotlin: **2.2.10 or newer** (was 2.2.0) — AGP 9.0+ has a runtime dependency on KGP 2.2.10 minimum and will force-upgrade anything lower.
- AGP 9.0+ has a documented breaking DSL change: `CommonExtension` parameterization was removed, so any convention-plugin code written against the parameterized `CommonExtension<...>` type needs updating to the per-variant extension types (`ApplicationExtension`, `LibraryExtension`, etc.). Check every file under `buildSrc/src/main/kotlin/ivy.*.gradle.kts` for this pattern.
- AGP 9.0+ also builds in first-class Kotlin support and no longer requires applying `org.jetbrains.kotlin.android`/`kotlin-android` separately — this may or may not need to be removed from existing `plugins {}` blocks; verify empirically rather than assuming.
- Every dependency already bumped in Task 1/2 (Room `2.8.4`, Hilt `2.58`, KSP `2.2.0-2.0.2`, Molecule `2.2.0`) needs re-verifying against the new Gradle 9/AGP 9 floor. In particular, **Hilt was deliberately capped at `2.58` in Task 2 specifically to avoid requiring AGP 9** (`2.59+` hard-requires AGP `9.0.0`+ for the Hilt Gradle plugin) — now that AGP 9 is happening anyway, re-evaluate whether Hilt should move to a newer release; do not leave it stale at 2.58 for a reason that no longer applies without checking.
- Everything else in the original Global Constraints below (Paparazzi, material-kolor, commit-per-dependency discipline, `JAVA_HOME`, out-of-scope list) still applies unchanged.

## Global Constraints (original; see revision above for the toolchain floor actually in effect)

- `compileSdk` / `targetSdk`: 36 (`minSdk` stays 28).
- AGP: `8.13.0`. Gradle wrapper: `8.13` (AGP 8.13.0's documented minimum).
- Kotlin: `2.2.0`. KSP: `2.2.0-2.0.2` (must track Kotlin exactly).
- `androidx.compose.material3:material3`: pinned to `1.5.0-alpha23` — the first alpha that dropped its `compileSdk 37` requirement (verified against `compose-material3` release notes; earlier 1.5.0-alpha lines required `compileSdk 37`, which AGP 8.13.0 can't even target — its documented max API level is 36.1).
- All other BOM-covered Compose artifacts (`foundation`, `ui`, `runtime`, `animation`, `material-icons-extended`, `ui-tooling`, `runtime-livedata`) move from the direct `compose = "1.6.8"` pin onto `androidx.compose:compose-bom-alpha:2026.06.01`, so they stay in lockstep with the new `material3`.
- `app.cash.paparazzi`: `2.0.0-alpha02` (not 1.3.5 — incompatible with Gradle 8.13 at runtime; not 2.0.0-alpha03+ — ships Kotlin 2.3 metadata that Gradle 8.13's buildSrc classpath reader can't parse).
- New dependency `com.materialkolor:material-kolor:4.1.1`, added only to `shared/ui/core`, for seed-based `ColorScheme` generation (`androidx.compose.material3` itself only exposes wallpaper-based `dynamicLightColorScheme`/`dynamicDarkColorScheme`; its own `TonalPalette` seed-generation code is `internal`).
- Every distinct dependency bump is its own commit, semantic/conventional style (`build: ...`), single-line message, no long bodies, no comments in code that merely narrate what a line does.
- `JAVA_HOME` must point at Android Studio's bundled JBR for every `./gradlew` invocation in this environment: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Out of scope (do not touch): any screen's `isLegacy` flag, a Settings UI toggle for color source, deleting `temp/old-design`/`shared/base/legacy/Theme.kt`/`IvyColors`, AMOLED/`Theme` enum UX changes beyond re-plumbing `isTrueBlack`.

---

### Task 1: Bump Gradle wrapper, AGP, and compileSdk/targetSdk

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `gradle/libs.versions.toml:19` (`compile-sdk`), `gradle/libs.versions.toml:29` (`android-gradle-plugin`)

**Interfaces:**
- Produces: a buildable project at `compileSdk 36` / AGP `8.13.0`, which every later task assumes.

- [ ] **Step 1: Bump the Gradle wrapper to 8.13**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew wrapper --gradle-version 8.13 --distribution-type bin
```
Expected: `gradle/wrapper/gradle-wrapper.properties`'s `distributionUrl` now ends in `gradle-8.13-bin.zip`, and `distributionSha256Sum` is refreshed to match.

- [ ] **Step 2: Verify the new wrapper runs**

Run: `./gradlew --version`
Expected: output shows `Gradle 8.13`.

- [ ] **Step 3: Commit the wrapper bump**

```bash
git add gradle/wrapper/gradle-wrapper.properties
git commit -m "build: bump Gradle wrapper to 8.13"
```

- [ ] **Step 4: Bump AGP to 8.13.0**

In `gradle/libs.versions.toml:29`, change:
```toml
android-gradle-plugin = { module = "com.android.tools.build:gradle", version = "8.5.2" }
```
to:
```toml
android-gradle-plugin = { module = "com.android.tools.build:gradle", version = "8.13.0" }
```

- [ ] **Step 5: Verify the project still configures with the new AGP**

Run: `./gradlew help`
Expected: `BUILD SUCCESSFUL` (this just forces Gradle to resolve `buildSrc`, which pulls in the new AGP, without building any modules yet).

- [ ] **Step 6: Commit the AGP bump**

```bash
git add gradle/libs.versions.toml
git commit -m "build: bump Android Gradle Plugin to 8.13.0"
```

- [ ] **Step 7: Bump compileSdk/targetSdk to 36**

In `gradle/libs.versions.toml:19`, change:
```toml
compile-sdk = "34"
```
to:
```toml
compile-sdk = "36"
```
(`minSdk` at line 18 stays `"28"`; `targetSdk` in `app/build.gradle.kts:21` already reads `libs.versions.compile.sdk.get().toInt()`, so it follows automatically.)

- [ ] **Step 8: Verify the app module builds against compileSdk 36**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. If it fails asking to install `SDK Platform 36`, run `sdkmanager "platforms;android-36"` (or accept Android Studio's prompt) and retry.

- [ ] **Step 9: Commit the compileSdk bump**

```bash
git add gradle/libs.versions.toml
git commit -m "build: bump compileSdk/targetSdk to 36"
```

---

### Task 2: Bump Kotlin and KSP to 2.2.0

**Files:**
- Modify: `gradle/libs.versions.toml:2` (`kotlin`), `gradle/libs.versions.toml:144` (`ksp-plugin`)

**Interfaces:**
- Consumes: Task 1's `compileSdk 36` / AGP `8.13.0` baseline.
- Produces: a project compiling under Kotlin 2.2.0, which the compose compiler plugin (`compose-compiler-plugin`, already `version.ref = "kotlin"`) and `kotlinx-serialization-plugin` (also `version.ref = "kotlin"`) pick up automatically.

- [ ] **Step 1: Bump Kotlin**

In `gradle/libs.versions.toml:2`, change:
```toml
kotlin = "2.0.20"
```
to:
```toml
kotlin = "2.2.0"
```

- [ ] **Step 2: Bump KSP to match**

In `gradle/libs.versions.toml:144`, change:
```toml
ksp-plugin = { module = "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin", version = "2.0.20-1.0.24" }
```
to:
```toml
ksp-plugin = { module = "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin", version = "2.2.0-2.0.2" }
```

- [ ] **Step 3: Verify Hilt/Room annotation processing still works under the new Kotlin/KSP**

Run: `./gradlew :app:compileDebugKotlin :shared:data:core:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. If KSP fails with a version-mismatch error, check https://github.com/google/ksp/releases for the exact KSP patch matching Kotlin `2.2.0` and use that version instead.

If `app.cash.molecule` (used by `ivy.compose.gradle.kts`) fails to resolve/compile against Kotlin 2.2.0's compose compiler, bump `cashapp-molecule-plugin` (`gradle/libs.versions.toml`) to the newest 1.4.x/1.5.x release and re-run this step — Molecule releases track the Kotlin compose-compiler version closely.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: bump Kotlin and KSP to 2.2.0"
```

---

### Task 3: Migrate to compose-bom-alpha and pin material3 to 1.5.0-alpha23

**Files:**
- Modify: `gradle/libs.versions.toml` (versions block + `[libraries]` compose entries)
- Modify: `buildSrc/src/main/kotlin/ivy.compose.gradle.kts`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Consumes: Task 1/2's toolchain baseline.
- Produces: `androidx.compose.material3.MaterialExpressiveTheme`, `MotionScheme`, and the Expressive `Shapes()`/`Typography()` defaults become public, non-`internal` APIs available to `shared/ui/core` (gated behind `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`).

- [ ] **Step 1: Replace the direct `compose`/`compose-material3` version pins with the alpha BOM**

In `gradle/libs.versions.toml`, in the `[versions]` block, remove:
```toml
compose = "1.6.8"
compose-material3 = "1.2.1"
```
and add in their place:
```toml
compose-material3 = "1.5.0-alpha23"
compose-bom-alpha = "2026.06.01"
```

- [ ] **Step 2: Add the BOM library entry and drop explicit versions from BOM-covered libraries**

In `gradle/libs.versions.toml`, in the `[libraries]` block, add:
```toml
compose-bom-alpha = { module = "androidx.compose:compose-bom-alpha", version.ref = "compose-bom-alpha" }
```

Change these existing entries (drop `version.ref = "compose"` — the BOM supplies the version now):
```toml
compose-animation = { module = "androidx.compose.animation:animation" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-material3-windowsize = { module = "androidx.compose.material3:material3-window-size-class" }
compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
compose-runtime = { module = "androidx.compose.runtime:runtime" }
compose-runtime-livedate-temp = { module = "androidx.compose.runtime:runtime-livedata" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-tooling = { module = "androidx.compose.ui:ui-tooling" }
```

Leave `compose-material3` itself keeping its explicit `version.ref`, so it overrides whatever the BOM recommends:
```toml
compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "compose-material3" }
```

(`compose-activity`, `compose-viewmodel`, `compose-coil`, `glance*` are not part of the Compose BOM and keep their existing standalone versions untouched.)

- [ ] **Step 3: Import the BOM platform in the `ivy.compose` convention plugin**

In `buildSrc/src/main/kotlin/ivy.compose.gradle.kts`, change:
```kotlin
dependencies {
    implementation(libs.bundles.compose)

    lintChecks(libs.slack.lint.compose)
}
```
to:
```kotlin
dependencies {
    implementation(platform(libs.compose.bom.alpha))
    implementation(libs.bundles.compose)

    lintChecks(libs.slack.lint.compose)
}
```

- [ ] **Step 4: Import the BOM platform in `app/build.gradle.kts`**

`app` doesn't apply `id("ivy.compose")` — it declares Compose dependencies directly. In `app/build.gradle.kts`, change:
```kotlin
    implementation(libs.bundles.compose)
```
to:
```kotlin
    implementation(platform(libs.compose.bom.alpha))
    implementation(libs.bundles.compose)
```

- [ ] **Step 5: Build and resolve any compileSdk/version fallout**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

If the build fails complaining about `compileSdk 37` (from a *different* BOM-covered artifact than `material3`, e.g. `foundation` or `ui` at whatever version `2026.06.01` resolves them to), drop `compose-bom-alpha` back one or two releases (e.g. `2026.05.02`, then `2026.05.00`) until it compiles cleanly against `compileSdk 36`, keeping the explicit `compose-material3 = "1.5.0-alpha23"` override in place regardless of which BOM version is picked.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml buildSrc/src/main/kotlin/ivy.compose.gradle.kts app/build.gradle.kts
git commit -m "build: migrate Compose to compose-bom-alpha, pin material3 to 1.5.0-alpha23"
```

---

### Task 4: Fix the 5 Paparazzi crashes surfaced by Task 3, then establish a clean baseline

**Revised during Task 3** (this supersedes the original version of this task below): Paparazzi could not stay at `1.3.3`/wait for this task — its Gradle plugin hard-fails at Gradle/AGP-9 configuration time (depends on the removed `TestedExtension` DSL), so Task 3 had to bump it just to get the project to configure at all. It landed on `2.0.0-alpha05` (`gradle/libs.versions.toml`, already committed in `91be14ef`) — newer than this task's original `2.0.0-alpha02` target, because `alpha02`–`alpha04` don't fix `TestedExtension` under this toolchain. **There is nothing left to bump.** Running the full-repo suite after Task 3's commit surfaces 5 real crashes (not rendering diffs) that must be root-caused and fixed before any baseline is meaningful:

1. `feature:attributions`, `feature:disclaimer`, `feature:contributors`, `feature:poll:impl` (4 modules, likely more once the full suite runs — Task 3 only ran a partial pass): `java.lang.IllegalArgumentException: Invalid ID: 60` in `android.graphics.ColorSpace.get`, thrown from `androidx.compose.material3.AppBarKt`'s draw path. All 4 modules use `Scaffold`/`TopAppBar`. Likely cause: `material3 1.5.0-alpha23`'s `TopAppBar` uses a `ColorSpace` that Paparazzi `2.0.0-alpha05`'s bundled Layoutlib doesn't recognize.
2. `feature:search`: `java.lang.NoSuchMethodError: 'int java.lang.Thread.setPosixNicenessInternal(int)'`. Reproduces in isolation (`./gradlew :feature:search:testDebugUnitTest`). Unrelated to material3/ColorSpace — a separate Paparazzi-alpha05-on-this-JVM issue.

**Files:**
- Unknown until investigated — likely `buildSrc/src/main/kotlin/ivy.paparazzi.gradle.kts` (Paparazzi/Layoutlib configuration) and/or `gradle/libs.versions.toml` (if a different Paparazzi alpha turns out necessary), possibly JVM/test-runner flags. Do not assume; the investigation steps below determine this.

**Interfaces:**
- Consumes: Task 3's toolchain (Gradle 9.3.1, AGP 9.1.1, compileSdk 37, Paparazzi 2.0.0-alpha05, material3 1.5.0-alpha23).
- Produces: a working `verifyPaparazziDebug` across the whole repo (before any theme-token changes), so Task 8's later re-record has a clean starting point.

- [ ] **Step 1: Reproduce and isolate both crash signatures**

Run: `./gradlew :feature:attributions:testDebugUnitTest :feature:search:testDebugUnitTest`
Expected: both fail, reproducing the `ColorSpace`/`AppBarKt` crash and the `Thread.setPosixNicenessInternal` crash respectively, matching the signatures above. Capture full stack traces for both — they're the starting point for root-causing.

- [ ] **Step 2: Investigate the ColorSpace/AppBarKt crash**

This needs real investigation, not a guess. Things to try, in order, stopping at the first that works:
- Check the Paparazzi GitHub issue tracker (`github.com/cashapp/paparazzi/issues`) for `ColorSpace` + `Invalid ID` reports against Layoutlib versions bundled with `2.0.0-alpha03`/`alpha04`/`alpha05` — this is exactly the kind of Layoutlib/material3-version compatibility gap Paparazzi tracks issues for.
- If a specific Paparazzi patch (newer or, if the alpha05 Layoutlib is the regression, an older 2.0.0-alphaNN) fixes it without reintroducing the `TestedExtension` AGP-9 failure Task 3 hit, use that version instead of alpha05 and update `gradle/libs.versions.toml` accordingly.
- If it's a Paparazzi `DeviceConfig` issue (e.g. a color-mode/night-mode flag), check `shared/ui/testing/.../PaparazziScreenshotTest.kt` and `shared/ui/core/src/test/java/com/ivy/ui/PaparazziScreenshotTest.kt`'s `Paparazzi(...)` rule configuration for a `DeviceConfig` that might need adjusting for this Layoutlib version.
- If none of the above resolves it, treat it as a genuine upstream Paparazzi/material3-alpha compatibility bug: file the minimal repro info in the report and escalate (NEEDS_CONTEXT) rather than silently working around it with something fragile.

- [ ] **Step 3: Investigate the Thread.setPosixNicenessInternal crash**

This is a JVM/Robolectric-shadow-layer issue, not a Compose/material3 one — likely fixed by a JVM flag (e.g. an `--add-opens`/`--add-exports` the newer JDK needs that Paparazzi's test JVM args don't yet include) or a Paparazzi/JUnit runner version detail. Check Paparazzi's own CI configuration or issue tracker for `setPosixNicenessInternal` — this is a known class of "newer JDK breaks reflection into java.lang.Thread internals" issue.

- [ ] **Step 4: Verify both fixes**

Run: `./gradlew :feature:attributions:testDebugUnitTest :feature:search:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, no crashes (snapshot mismatches are fine and expected at this point — Task 3's toolchain changes will have shifted rendering; crashes are not).

- [ ] **Step 5: Run the full existing Paparazzi suite and establish the clean baseline**

Run: `./gradlew testDebugUnitTest --continue` (full repo) to confirm the other modules flagged in Task 3's report (`feature:contributors`, `feature:poll:impl`, `feature:disclaimer`, and check for others Task 3 didn't reach) are also fixed by the same change, not separate issues.

Then run: `./gradlew recordPaparazziDebug` (full repo) to establish new baselines against the Task 3 toolchain — expect essentially every module's baseline to change at least slightly (new Layoutlib rendering engine), not just the 5 that crashed.

Then run: `./gradlew verifyPaparazziDebug` to confirm the recorded baselines are stable and reproducible.

- [ ] **Step 6: Spot-check a sample of the newly-recorded baselines**

Same standard as Task 8's spot-check below: open a handful of the changed snapshot PNGs (at least one from each of the 5 previously-crashing modules, plus 2-3 others) and confirm they render sane UI, not blank/garbled/partial frames — a silent renderer failure can produce a "valid" but empty image that `recordPaparazziDebug` would happily accept as a new baseline.

- [ ] **Step 7: Commit**

```bash
git add '**/src/test/snapshots/**'
# plus whatever files Step 2/3's actual fix touched
git commit -m "fix: resolve Paparazzi crashes under Gradle 9/material3 1.5.0-alpha23 and re-record baselines"
```

---

<details>
<summary>Original Task 4 (superseded — kept for history, do not execute)</summary>

Bump Paparazzi from `1.3.3` to `2.0.0-alpha02` in `gradle/libs.versions.toml:15`, run `./gradlew verifyPaparazziDebug`, expect no diffs since this was meant to be toolchain-only with no visual change yet. This assumed Paparazzi would still be at `1.3.3` when this task started and that the only needed change was a routine version bump — both assumptions were invalidated by Task 3.

</details>

---

### Task 5: Add the IvyColorSource token (Dynamic / BrandSeed) with a true-black unit test

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/design/system/IvyColorSource.kt`
- Create: `shared/ui/core/src/main/java/com/ivy/design/system/TrueBlack.kt`
- Create: `shared/ui/core/src/test/java/com/ivy/design/system/TrueBlackTest.kt`
- Modify: `shared/ui/core/build.gradle.kts`

**Interfaces:**
- Produces: `IvyColorSource` (sealed interface, `Dynamic` / `BrandSeed(seedColor: Color)`) and `ColorScheme.applyTrueBlack(isTrueBlack: Boolean): ColorScheme`, both consumed by Task 7's `IvyMaterial3Theme` rewrite.

- [ ] **Step 1: Add the material-kolor dependency**

In `gradle/libs.versions.toml`, in `[libraries]`, add:
```toml
materialkolor = { module = "com.materialkolor:material-kolor", version = "4.1.1" }
```

In `shared/ui/core/build.gradle.kts`, change:
```kotlin
dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)
}
```
to:
```kotlin
dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)

    implementation(libs.materialkolor)
}
```

- [ ] **Step 2: Write the failing test for the true-black helper**

Create `shared/ui/core/src/test/java/com/ivy/design/system/TrueBlackTest.kt`:
```kotlin
package com.ivy.design.system

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.Test

class TrueBlackTest {

    private val scheme = darkColorScheme(
        background = Color(0xFF1C1C1F),
        surface = Color(0xFF1C1C1F),
    )

    @Test
    fun `leaves background and surface untouched when isTrueBlack is false`() {
        val result = scheme.applyTrueBlack(isTrueBlack = false)

        result.background shouldBe scheme.background
        result.surface shouldBe scheme.surface
    }

    @Test
    fun `forces background and surface to pure black when isTrueBlack is true`() {
        val result = scheme.applyTrueBlack(isTrueBlack = true)

        result.background shouldBe Color(0xFF000000)
        result.surface shouldBe Color(0xFF000000)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :shared:ui:core:testDebugUnitTest --tests "com.ivy.design.system.TrueBlackTest"`
Expected: FAIL — `applyTrueBlack` is unresolved.

- [ ] **Step 4: Implement `IvyColorSource`**

Create `shared/ui/core/src/main/java/com/ivy/design/system/IvyColorSource.kt`:
```kotlin
package com.ivy.design.system

import androidx.compose.ui.graphics.Color
import com.ivy.design.system.colors.IvyColors

sealed interface IvyColorSource {
    data object Dynamic : IvyColorSource

    data class BrandSeed(
        val seedColor: Color = IvyColors.Purple.primary
    ) : IvyColorSource
}
```

- [ ] **Step 5: Implement the true-black helper**

Create `shared/ui/core/src/main/java/com/ivy/design/system/TrueBlack.kt`:
```kotlin
package com.ivy.design.system

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

private val PureBlack = Color(0xFF000000)

internal fun ColorScheme.applyTrueBlack(isTrueBlack: Boolean): ColorScheme {
    if (!isTrueBlack) return this
    return copy(background = PureBlack, surface = PureBlack)
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :shared:ui:core:testDebugUnitTest --tests "com.ivy.design.system.TrueBlackTest"`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml shared/ui/core/build.gradle.kts \
  shared/ui/core/src/main/java/com/ivy/design/system/IvyColorSource.kt \
  shared/ui/core/src/main/java/com/ivy/design/system/TrueBlack.kt \
  shared/ui/core/src/test/java/com/ivy/design/system/TrueBlackTest.kt
git commit -m "feat: add IvyColorSource and true-black color scheme override"
```

---

### Task 6: Add the Open Sans Expressive Typography

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/design/system/IvyTypography.kt`

**Interfaces:**
- Consumes: font resources already present at `shared/ui/core/src/main/res/font/opensans_*.ttf` (already bundled in this module — no new resources needed).
- Produces: `ivyExpressiveTypography(): Typography`, consumed by Task 7.

- [ ] **Step 1: Implement the Open Sans font family and Expressive typography**

Create `shared/ui/core/src/main/java/com/ivy/design/system/IvyTypography.kt`:
```kotlin
package com.ivy.design.system

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ivy.ui.R

val OpenSans: FontFamily = FontFamily(
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_light, FontWeight.Light),
    Font(R.font.opensans_semibold, FontWeight.SemiBold),
    Font(R.font.opensans_bold, FontWeight.Bold),
    Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ivyExpressiveTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = OpenSans),
        displayMedium = base.displayMedium.copy(fontFamily = OpenSans),
        displaySmall = base.displaySmall.copy(fontFamily = OpenSans),
        displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = OpenSans),
        displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = OpenSans),
        displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = OpenSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = OpenSans),
        headlineMedium = base.headlineMedium.copy(fontFamily = OpenSans),
        headlineSmall = base.headlineSmall.copy(fontFamily = OpenSans),
        headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = OpenSans),
        headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = OpenSans),
        headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = OpenSans),
        titleLarge = base.titleLarge.copy(fontFamily = OpenSans),
        titleMedium = base.titleMedium.copy(fontFamily = OpenSans),
        titleSmall = base.titleSmall.copy(fontFamily = OpenSans),
        titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = OpenSans),
        titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = OpenSans),
        titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = OpenSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = OpenSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = OpenSans),
        bodySmall = base.bodySmall.copy(fontFamily = OpenSans),
        bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = OpenSans),
        bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = OpenSans),
        bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = OpenSans),
        labelLarge = base.labelLarge.copy(fontFamily = OpenSans),
        labelMedium = base.labelMedium.copy(fontFamily = OpenSans),
        labelSmall = base.labelSmall.copy(fontFamily = OpenSans),
        labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = OpenSans),
        labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = OpenSans),
        labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = OpenSans),
    )
}
```

- [ ] **Step 2: Compile-check**

Run: `./gradlew :shared:ui:core:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. If any `*Emphasized` parameter name doesn't match (API drift from what this plan verified against `1.5.0-alpha18`/`alpha23` sources), the compiler error names the correct parameter — fix the mismatched name(s) only.

- [ ] **Step 3: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/design/system/IvyTypography.kt
git commit -m "feat: add Open Sans Expressive typography"
```

---

### Task 7: Rewrite IvyMaterial3Theme to wire color/shape/typography/motion tokens

**Important context from Task 4**: while investigating Paparazzi crashes, Task 4 root-caused (via bytecode decompilation, independently verified by review) a real, pre-existing bug that this task's rewrite is expected to fix as a side effect: the *current* `IvyMaterial3Theme.kt` builds `ColorScheme(...)` via the legacy ~29-parameter compatibility constructor, which leaves 19 newer color roles (`surfaceContainer`, `surfaceContainerHigh/Low/Lowest/Highest`, `surfaceBright`, `surfaceDim`, the `*Fixed` roles, etc.) as `Color.Unspecified`. `material3 1.5.0-alpha23`'s `TopAppBar` reads `surfaceContainer` for its default scrolled-state color, and feeding it `Color.Unspecified` crashes (a real compose-ui-graphics bug converting `Unspecified` to a "color long" — reproduces on real Android devices, not just Paparazzi/Layoutlib). This currently crashes `feature:attributions`, `feature:contributors`, `feature:disclaimer`, `feature:poll:impl`'s Paparazzi tests (all use a bare `TopAppBar(...)` with no explicit `colors`), and those 4 modules were deliberately left unbaselined by Task 4 pending this fix. The rewrite below uses `dynamicLightColorScheme`/`dynamicDarkColorScheme`/`rememberDynamicColorScheme` — all "full" scheme generators that populate every `ColorScheme` field, not the legacy partial constructor — so this bug should be fixed as a natural side effect, not something requiring extra code. **Step 3 below adds an explicit check that it actually is.**

**Files:**
- Modify: `shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt`

**Interfaces:**
- Consumes: `IvyColorSource` (Task 5), `ColorScheme.applyTrueBlack` (Task 5), `ivyExpressiveTypography()` (Task 6).
- Produces: `IvyMaterial3Theme(isTrueBlack: Boolean, dark: Boolean = isSystemInDarkTheme(), colorSource: IvyColorSource = IvyColorSource.Dynamic, content: @Composable () -> Unit)` — same call signature every existing call site already uses (`dark`, `isTrueBlack`, `content`), with `colorSource` added as a defaulted trailing parameter so `RootActivity.kt` and `IvyPreview.kt` don't need to change.

- [ ] **Step 1: Replace the file contents**

Replace all of `shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt` with:
```kotlin
package com.ivy.design.system

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IvyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    colorSource: IvyColorSource = IvyColorSource.Dynamic,
    content: @Composable () -> Unit
) {
    val colorScheme = ivyColorScheme(colorSource, dark).applyTrueBlack(isTrueBlack)
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes(),
        typography = ivyExpressiveTypography(),
        content = content,
    )
}

@Composable
private fun ivyColorScheme(colorSource: IvyColorSource, dark: Boolean): ColorScheme =
    when (colorSource) {
        is IvyColorSource.Dynamic -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            ivyBrandColorScheme(IvyColorSource.BrandSeed(), dark)
        }

        is IvyColorSource.BrandSeed -> ivyBrandColorScheme(colorSource, dark)
    }

@Composable
private fun ivyBrandColorScheme(brandSeed: IvyColorSource.BrandSeed, dark: Boolean): ColorScheme =
    rememberDynamicColorScheme(seedColor = brandSeed.seedColor, isDark = dark)
```

This deletes `ivyLightColorScheme()`/`ivyDarkColorScheme()` (the old hand-authored `ColorScheme(...)` literals) entirely — they're fully superseded by `ivyColorScheme`/`ivyBrandColorScheme`.

- [ ] **Step 2: Compile-check**

Run: `./gradlew :shared:ui:core:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. If `rememberDynamicColorScheme` requires an experimental opt-in annotation from `material-kolor`, add the matching `@OptIn(...)` to `ivyBrandColorScheme` (the compiler error names the exact annotation).

- [ ] **Step 3: Full project compile-check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL` — this confirms `RootActivity.kt`'s existing `IvyMaterial3Theme(dark = ..., isTrueBlack = ...)` call still compiles unchanged against the new signature (relying on the `colorSource` default).

- [ ] **Step 3.5: Confirm Task 4's `ColorSpace` crash is actually resolved**

Run: `./gradlew :feature:attributions:testDebugUnitTest :feature:contributors:testDebugUnitTest :feature:disclaimer:testDebugUnitTest :feature:poll:impl:testDebugUnitTest`
Expected: these 4 modules — left crashing by Task 4 with `java.lang.IllegalArgumentException: Invalid ID: 60` in `AppBarKt`, because the *old* `IvyMaterial3Theme` left `surfaceContainer` as `Color.Unspecified` — now run without that crash (snapshot *mismatches* are fine and expected here, since the whole point of this task is changing colors/shapes/typography/motion; a hard crash is not).

If any of the 4 still crash with the same signature: `dynamicLightColorScheme`/`dynamicDarkColorScheme`/`rememberDynamicColorScheme` are documented to populate every `ColorScheme` field, so a persisting crash likely means `ivyBrandColorScheme`'s fallback path (pre-Android-12 devices/tests) isn't actually being exercised the way expected in the test environment — check which branch of `ivyColorScheme` the test harness hits (Task 8 will pin it to `IvyColorSource.BrandSeed()` explicitly; until Task 8 lands, the test harness still calls the *old* `IvyMaterial3Theme` signature's default, so this check may need to wait for Task 8's harness update to be meaningful — if so, note that in your report and defer final confirmation to Task 8's own verification step instead of blocking this task on it).

- [ ] **Step 4: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt
git commit -m "feat: rewrite IvyMaterial3Theme with M3 Expressive color/shape/type/motion tokens"
```

---

### Task 8: Point both Paparazzi test harnesses at BrandSeed and regenerate baselines repo-wide

**Files:**
- Modify: `shared/ui/testing/src/main/java/com/ivy/ui/testing/PaparazziScreenshotTest.kt`
- Modify: `shared/ui/core/src/test/java/com/ivy/ui/PaparazziScreenshotTest.kt`

**Interfaces:**
- Consumes: Task 7's new `IvyMaterial3Theme(colorSource = ...)` parameter.
- Produces: deterministic screenshot rendering — `IvyColorSource.Dynamic` reads the (headless, unpredictable) test-environment wallpaper via `dynamicLightColorScheme`/`dynamicDarkColorScheme`, which is unsuitable for reproducible baselines, so both harnesses pin `IvyColorSource.BrandSeed()` explicitly instead of relying on the production `Dynamic` default.

- [ ] **Step 1: Update the shared harness (`shared/ui/testing`)**

In `shared/ui/testing/src/main/java/com/ivy/ui/testing/PaparazziScreenshotTest.kt`, change:
```kotlin
import com.ivy.design.system.IvyMaterial3Theme
```
to:
```kotlin
import com.ivy.design.system.IvyColorSource
import com.ivy.design.system.IvyMaterial3Theme
```
and change:
```kotlin
    protected fun snapshot(theme: PaparazziTheme, content: @Composable () -> Unit) {
        paparazzi.snapshot {
            IvyMaterial3Theme(
                dark = when (theme) {
                    PaparazziTheme.Light -> false
                    PaparazziTheme.Dark -> true
                },
                isTrueBlack = false
            ) {
                content()
            }
        }
    }
```
to:
```kotlin
    protected fun snapshot(theme: PaparazziTheme, content: @Composable () -> Unit) {
        paparazzi.snapshot {
            IvyMaterial3Theme(
                dark = when (theme) {
                    PaparazziTheme.Light -> false
                    PaparazziTheme.Dark -> true
                },
                isTrueBlack = false,
                colorSource = IvyColorSource.BrandSeed(),
            ) {
                content()
            }
        }
    }
```

- [ ] **Step 2: Update the module-local harness (`shared/ui/core`)**

Apply the identical change to `shared/ui/core/src/test/java/com/ivy/ui/PaparazziScreenshotTest.kt` (same before/after as Step 1, same import addition).

- [ ] **Step 3: Regenerate every Paparazzi baseline in the repo**

Run: `./gradlew recordPaparazziDebug`
Expected: `BUILD SUCCESSFUL`. This rewrites snapshots under every module's `src/test/snapshots/images/` — not just `AttributionsScreen`'s. Affected test classes (confirmed via `grep -rl PaparazziScreenshotTest`):
`OpenSourceCardPaparazziTest`, `DemoPaparazziTest`, `LoanScreenPaparazziTest`, `HomePaparazziTest`,
`DisclaimerScreenPaparazziTest`, `ContributorsScreenPaparazziTest`, `SearchPaparazziTest`,
`TransactionsPaparazziTest`, `AccountsTabPaparazziTest`, `ExchangeRatesScreenPaparazziTest`,
`AttributionsScreenPaparazziTest`, `CategoriesScreenPaparazziTest`, `PollScreenshotTest`,
`ReportPaparazziTest`.

- [ ] **Step 4: Spot-check the diffs**

Run: `git status --short -- '**/src/test/snapshots/**'` to list every changed snapshot PNG, then open a handful (at least `AttributionsScreenPaparazziTest`'s and 2–3 others) to confirm they look like sane M3 Expressive renders (new colors, new shapes, no broken/blank/crashed layouts) — not just diff noise. A blank or garbled snapshot means a runtime crash was silently swallowed by Paparazzi; investigate before proceeding rather than committing a broken baseline.

**Give `AttributionsScreenPaparazziTest`, `ContributorsScreenPaparazziTest`, `DisclaimerScreenPaparazziTest`, and `PollScreenshotTest` extra scrutiny here specifically**: Task 4 left these 4 unbaselined (they were crashing with `Invalid ID: 60` due to the old theme's incomplete `ColorScheme`, per Task 7's fix). This is their *first* successful baseline under the new theme, not a re-record of a known-good image — if Task 7's Step 3.5 check passed, this should just work, but confirm each of the 4 actually shows real rendered UI (not the blank dark-gray rectangle Task 4 found and deleted for one of these exact modules).

- [ ] **Step 5: Verify the new baselines are stable**

Run: `./gradlew verifyPaparazziDebug`
Expected: `BUILD SUCCESSFUL` (recording and verifying immediately after should always match).

- [ ] **Step 6: Commit**

```bash
git add shared/ui/testing/src/main/java/com/ivy/ui/testing/PaparazziScreenshotTest.kt \
  shared/ui/core/src/test/java/com/ivy/ui/PaparazziScreenshotTest.kt \
  '**/src/test/snapshots/**'
git commit -m "test: pin Paparazzi harnesses to BrandSeed and re-record baselines"
```

---

### Task 9: Migrate AttributionsScreen off its hardcoded shape

**Files:**
- Modify: `feature/attributions/src/main/java/com/ivy/attributions/AttributionsScreen.kt`

**Interfaces:**
- Consumes: Task 7's `Shapes()` now wired into the theme, so `Card`'s own default shape (`MaterialTheme.shapes.medium`-derived) is already Expressive — no explicit shape needs passing anymore.

- [ ] **Step 1: Remove the hardcoded shape**

In `feature/attributions/src/main/java/com/ivy/attributions/AttributionsScreen.kt`, remove the now-unused import:
```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
```
and change:
```kotlin
    Card(
        shape = RoundedCornerShape(12.dp),
        onClick = {
            browser.openUri(attribution.link)
        }
    ) {
```
to:
```kotlin
    Card(
        onClick = {
            browser.openUri(attribution.link)
        }
    ) {
```

- [ ] **Step 2: Re-record just this screen's baseline**

Run: `./gradlew :feature:attributions:recordPaparazziDebug`
Expected: `BUILD SUCCESSFUL`, and `feature/attributions/src/test/snapshots/images/...AttributionsScreenPaparazziTest_snapshot Attribution Screen[Light].png` / `[Dark].png` change again (Card now renders with the theme's default shape instead of the old fixed 12dp corner radius).

- [ ] **Step 3: Verify**

Run: `./gradlew :feature:attributions:verifyPaparazziDebug :feature:attributions:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add feature/attributions/src/main/java/com/ivy/attributions/AttributionsScreen.kt \
  feature/attributions/src/test/snapshots/
git commit -m "refactor: drop AttributionsScreen's hardcoded card shape"
```

---

### Task 10: Manual verification and lint/detekt gate

**Files:** none (verification only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Detekt**

Run: `./gradlew detekt`
Expected: `BUILD SUCCESSFUL`, no new violations (the baseline at `config/detekt/baseline.yml` only covers pre-existing findings — anything new here is a real regression to fix, most likely `@OptIn`-related or import-order issues in the files this plan touched).

- [ ] **Step 3: Android Lint**

Run: `./gradlew lintR`
Expected: `BUILD SUCCESSFUL`. Check `build/reports/lint/lint.html` if it fails.

- [ ] **Step 4: Full Paparazzi verification**

Run: `./gradlew verifyPaparazziDebug`
Expected: `BUILD SUCCESSFUL` (re-confirms Task 8/9's recorded baselines are still exactly reproducible after the lint/detekt/test runs above, which shouldn't have touched any source).

- [ ] **Step 5: Manual emulator smoke test**

Run: `./gradlew assembleDebug`, install the resulting APK on an emulator or device (`adb install -r app/build/outputs/apk/debug/app-debug.apk`), then:
1. Launch the app and confirm it doesn't crash on startup (this exercises `RootActivity`'s `IvyMaterial3Theme` call with the real `Dynamic` color source on a real/emulated device).
2. Confirm legacy screens (e.g. the Home tab, Accounts, Settings) still render — they should look unchanged, since they still route through the old `isLegacy` design system wrapper.
3. Navigate to Settings → About/Attributions (`AttributionsScreen`) and confirm: the card list renders with a visibly different shape/color/motion feel than before (Expressive corner radii, dynamic-color-derived palette, Open Sans type), and tapping a card still opens the browser link correctly.
4. Toggle the device's dark/light theme (or the app's own theme setting, if reachable) and confirm both render correctly without crashing.

There is no scripted assertion for this step — it's a human check that the Expressive tokens actually look and behave like Expressive M3 on a real render target, not just in Paparazzi's Layoutlib-based rendering.
