# Settings Screen Redesign (M3 Phase 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `SettingsScreen` in ReadYou's Material 3 visual style (single page, reorganized sections, M3 dialogs), flip it to `isLegacy = false`, and add a user-facing dynamic-color toggle wired into `IvyMaterial3Theme`.

**Architecture:** New reusable settings components live in `shared/ui/core` (`com.ivy.ui.component.settings` / `.dialog`). The dynamic-color preference is a DataStore boolean surfaced through `LocalIvyColorSource` (a CompositionLocal provided once in `RootActivity`), so no legacy theme call sites change. `feature/settings` keeps its MVI shape; only the view layer and two event contracts change. Spec: `docs/superpowers/specs/2026-07-12-settings-redesign-design.md`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 (Expressive, alpha BOM), Hilt, DataStore Preferences, Molecule + Turbine + MockK + Kotest (VM tests), Paparazzi (screenshots).

## Global Constraints

- Every `./gradlew` call needs: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` first. Run from the repo root (the worktree root).
- Commits: single-line conventional commits (`feat:`/`fix:`/`test:`/`docs:`/`refactor:`), no bodies. **Never push** — the user pushes manually.
- No new detekt or Android Lint baseline entries. `./gradlew detekt` must pass after every task.
- Detekt Compose rules are active: public composables need a `modifier: Modifier = Modifier` parameter; previews must be private (except the `*UiTest` screenshot entry points); required params before optional ones.
- The `ComposeParameterOrder` Android Lint check is disabled project-wide (crashes under the current toolchain) — do not re-enable it.
- Paparazzi: record with `recordPaparazziDebug`, then confirm `verifyPaparazziDebug` passes. `AlertDialog`-based composables render into a separate window Paparazzi cannot capture — dialogs get `@Preview`s only (documented deviation from the spec's blanket Paparazzi claim).
- String resources live in `shared/ui/core/src/main/res/values/strings.xml` (imported everywhere as `com.ivy.ui.R`). English only; translated `values-*` files are out of scope.

---

## File Structure

| File | Responsibility |
|---|---|
| `shared/data/core/.../datasource/LocalAppearanceDataSource.kt` (new) | DataStore-backed dynamic-color preference (`Flow<Boolean>` + setter) |
| `shared/ui/core/.../design/system/IvyColorSource.kt` (modify) | + `LocalIvyColorSource` CompositionLocal |
| `shared/ui/core/.../design/system/IvyMaterial3Theme.kt` (modify) | `colorSource` defaults to `LocalIvyColorSource.current` |
| `shared/ui/core/.../ui/component/settings/SettingsItem.kt` (new) | ReadYou-style setting row |
| `shared/ui/core/.../ui/component/settings/SettingsSectionTitle.kt` (new) | Colored section header |
| `shared/ui/core/.../ui/component/settings/ScreenDisplayTitle.kt` (new) | Large display-text screen header |
| `shared/ui/core/.../ui/component/dialog/RadioSelectionDialog.kt` (new) | Radio-list picker dialog |
| `shared/ui/core/.../ui/component/dialog/TextInputDialog.kt` (new) | Single text field dialog |
| `shared/ui/core/.../ui/component/dialog/ProgressDialog.kt` (new) | Non-dismissible progress dialog |
| `app/.../wallet/RootActivity.kt` (modify) | Collect preference → provide `LocalIvyColorSource` |
| `feature/settings/.../SettingsState.kt` (modify) | + `dynamicColorEnabled`, `dynamicColorAvailable` |
| `feature/settings/.../SettingsEvent.kt` (modify) | `SwitchTheme` → `SetTheme(Theme)`, + `SetDynamicColor(Boolean)` |
| `feature/settings/.../SettingsViewModel.kt` (modify) | Implement the two new events, expose preference in state |
| `feature/settings/.../CurrencyPickerDialog.kt` (new) | Searchable currency dialog (needs legacy `IvyCurrency`, stays feature-private) |
| `feature/settings/.../SettingsScreen.kt` (rewrite) | The new single-page M3 UI |
| `shared/ui/navigation/.../Screens.kt` (modify) | `SettingsScreen.isLegacy = false` |
| `config/detekt/config.yml` (modify) | Allowlist `LocalIvyColorSource` |
| `feature/settings/src/test/.../SettingsViewModelTest.kt` (new) | VM tests for new events |
| `feature/settings/src/test/.../SettingsScreenPaparazziTest.kt` (new) | Screen snapshots |
| `shared/ui/core/src/test/.../component/settings/SettingsComponentsPaparazziTest.kt` (new) | Component snapshots |

---

### Task 1: Dynamic-color preference datasource

**Files:**
- Create: `shared/data/core/src/main/java/com/ivy/data/datasource/LocalAppearanceDataSource.kt`

**Interfaces:**
- Consumes: Hilt-provided `DataStore<Preferences>` (see `DatastoreModule`).
- Produces: `class LocalAppearanceDataSource` with `val dynamicColor: Flow<Boolean>` (default `true`) and `suspend fun setDynamicColor(enabled: Boolean)`. Tasks 3 and 6 inject it.

No direct unit test: it is a two-line DataStore wrapper, mirroring the untested `LocalLegalDataSource` next to it; behavior is covered through `SettingsViewModelTest` (Task 6, mocked) and the manual QA pass (Task 8).

- [ ] **Step 1: Write the datasource**

```kotlin
package com.ivy.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalAppearanceDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val dynamicColor: Flow<Boolean> = dataStore.data
        .map { it[DynamicColorKey] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit {
            it[DynamicColorKey] = enabled
        }
    }

    companion object {
        private val DynamicColorKey = booleanPreferencesKey("dynamic_color_enabled")
    }
}
```

- [ ] **Step 2: Compile + static checks**

Run: `./gradlew :shared:data:core:compileDebugKotlin detekt`
Expected: BUILD SUCCESSFUL, no new detekt findings.

- [ ] **Step 3: Commit**

```bash
git add shared/data/core/src/main/java/com/ivy/data/datasource/LocalAppearanceDataSource.kt
git commit -m "feat: add dynamic color preference datasource"
```

---

### Task 2: `LocalIvyColorSource` CompositionLocal

**Files:**
- Modify: `shared/ui/core/src/main/java/com/ivy/design/system/IvyColorSource.kt`
- Modify: `shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt` (signature only)
- Modify: `config/detekt/config.yml:1066` (CompositionLocalAllowlist)

**Interfaces:**
- Produces: `val LocalIvyColorSource: ProvidableCompositionLocal<IvyColorSource>` (default `IvyColorSource.Dynamic`) in package `com.ivy.design.system`. `IvyMaterial3Theme`'s `colorSource` parameter now defaults to `LocalIvyColorSource.current`. Existing explicit call sites (Paparazzi bases pass `BrandSeed`) are unaffected.

- [ ] **Step 1: Add the CompositionLocal to `IvyColorSource.kt`**

Append to the existing file (add the two imports):

```kotlin
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * App-wide color source for [IvyMaterial3Theme]. Provided once in RootActivity
 * from the persisted dynamic-color preference so legacy theme call sites
 * don't need to plumb it through.
 */
val LocalIvyColorSource: ProvidableCompositionLocal<IvyColorSource> =
    compositionLocalOf { IvyColorSource.Dynamic }
```

- [ ] **Step 2: Default `IvyMaterial3Theme`'s parameter to it**

In `IvyMaterial3Theme.kt` change:

```kotlin
    colorSource: IvyColorSource = IvyColorSource.Dynamic,
```

to:

```kotlin
    colorSource: IvyColorSource = LocalIvyColorSource.current,
```

- [ ] **Step 3: Allowlist the new CompositionLocal in detekt**

In `config/detekt/config.yml`, under `Compose:` → `CompositionLocalAllowlist:` (line ~1066), add:

```yaml
  CompositionLocalAllowlist:
    active: true
    allowedCompositionLocals: 'LocalIvyColorSource'
```

(Existing locals like `LocalIvyColors` stay covered by the checked-in baseline.)

- [ ] **Step 4: Verify**

Run: `./gradlew :shared:ui:core:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL (Paparazzi tests in this module pass `colorSource` explicitly, so snapshots are unchanged).

- [ ] **Step 5: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/design/system/ config/detekt/config.yml
git commit -m "feat: default theme color source from LocalIvyColorSource"
```

---

### Task 3: RootActivity provides the color source

**Files:**
- Modify: `app/src/main/java/com/ivy/wallet/RootActivity.kt` (~line 95–155, inside `setContent`)

**Interfaces:**
- Consumes: `LocalAppearanceDataSource` (Task 1), `LocalIvyColorSource` (Task 2).

- [ ] **Step 1: Inject the datasource**

Next to the existing `@Inject lateinit var` fields in `RootActivity`:

```kotlin
    @Inject
    lateinit var appearanceDataSource: LocalAppearanceDataSource
```

Imports to add: `com.ivy.data.datasource.LocalAppearanceDataSource`, `com.ivy.design.system.IvyColorSource`, `com.ivy.design.system.LocalIvyColorSource`, `com.ivy.design.system.colors.IvyColors`, `androidx.compose.runtime.CompositionLocalProvider`, `androidx.compose.runtime.collectAsState`, `androidx.compose.runtime.getValue`.

- [ ] **Step 2: Provide the CompositionLocal around the whole compose tree**

Inside `setContent { ... }`, right after `val isSystemInDarkTheme = isSystemInDarkTheme()`, add the collection, then wrap **everything** that follows in `setContent` (the `when (appLocked)` block *and* the trailing `IvyMaterial3Theme { dateTimePicker.Content() }`) in the provider:

```kotlin
val dynamicColor by appearanceDataSource.dynamicColor.collectAsState(initial = true)
CompositionLocalProvider(
    LocalIvyColorSource provides if (dynamicColor) {
        IvyColorSource.Dynamic
    } else {
        IvyColorSource.BrandSeed(IvyColors.Purple.primary)
    }
) {
    // ...existing when (appLocked) { ... } and IvyMaterial3Theme(dateTimePicker) unchanged...
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew :app:assembleDebug detekt`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ivy/wallet/RootActivity.kt
git commit -m "feat: wire dynamic color preference into app theme"
```

---

### Task 4: Settings display components (shared/ui/core)

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/settings/SettingsItem.kt`
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/settings/SettingsSectionTitle.kt`
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/settings/ScreenDisplayTitle.kt`
- Test: `shared/ui/core/src/test/java/com/ivy/ui/component/settings/SettingsComponentsPaparazziTest.kt`

**Interfaces (produced, used by Task 7):**
- `SettingsItem(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null, enabled: Boolean = true, titleColor: Color = Color.Unspecified, trailing: (@Composable () -> Unit)? = null)`
- `SettingsSectionTitle(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary)`
- `ScreenDisplayTitle(text: String, modifier: Modifier = Modifier, description: String? = null, onDescriptionClick: (() -> Unit)? = null)`

- [ ] **Step 1: `SettingsItem.kt`**

```kotlin
package com.ivy.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.design.system.IvyMaterial3Theme

private const val DisabledAlpha = 0.5f

@Composable
fun SettingsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                maxLines = if (description == null) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            )
            if (description != null) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (trailing != null) {
            Box(modifier = Modifier.padding(start = 16.dp)) {
                trailing()
            }
        }
    }
}

@Preview
@Composable
private fun SettingsItemPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        SettingsItem(
            title = "Currency",
            description = "USD",
            onClick = {},
        )
    }
}
```

- [ ] **Step 2: `SettingsSectionTitle.kt`**

```kotlin
package com.ivy.ui.component.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMaterial3Theme

@Composable
fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        modifier = modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Preview
@Composable
private fun SettingsSectionTitlePreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        SettingsSectionTitle(text = "Appearance")
    }
}
```

- [ ] **Step 3: `ScreenDisplayTitle.kt`**

```kotlin
package com.ivy.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMaterial3Theme

@Composable
fun ScreenDisplayTitle(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onDescriptionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
            Text(
                modifier = if (onDescriptionClick != null) {
                    Modifier.clickable(onClick = onDescriptionClick)
                } else {
                    Modifier
                },
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun ScreenDisplayTitlePreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        ScreenDisplayTitle(text = "Settings", description = "1.0.0 (100)")
    }
}
```

- [ ] **Step 4: Paparazzi test**

`shared/ui/core/src/test/java/com/ivy/ui/component/settings/SettingsComponentsPaparazziTest.kt` (this module's own base class is `com.ivy.ui.PaparazziScreenshotTest`):

```kotlin
package com.ivy.ui.component.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SettingsComponentsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot settings components`() {
        snapshot(theme) {
            Column {
                ScreenDisplayTitle(text = "Settings", description = "1.0.0 (100)")
                SettingsSectionTitle(text = "Appearance")
                SettingsItem(title = "Theme", description = "Auto", onClick = {})
                SettingsItem(
                    title = "Dynamic color",
                    description = "Use colors from your wallpaper",
                    onClick = {},
                ) {
                    Switch(checked = true, onCheckedChange = {})
                }
                SettingsItem(title = "Disabled row", enabled = false, onClick = {})
            }
        }
    }
}
```

- [ ] **Step 5: Record baselines, then verify**

Run: `./gradlew :shared:ui:core:recordPaparazziDebug`
Then: `./gradlew :shared:ui:core:verifyPaparazziDebug detekt`
Expected: BUILD SUCCESSFUL both times; new golden images appear under `shared/ui/core/src/test/snapshots/`.

- [ ] **Step 6: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/ui/component/settings/ shared/ui/core/src/test/java/com/ivy/ui/component/settings/ shared/ui/core/src/test/snapshots/
git commit -m "feat: add ReadYou-style settings display components"
```

---

### Task 5: Shared M3 dialogs (shared/ui/core)

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/dialog/RadioSelectionDialog.kt`
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/dialog/TextInputDialog.kt`
- Create: `shared/ui/core/src/main/java/com/ivy/ui/component/dialog/ProgressDialog.kt`
- Modify: `shared/ui/core/src/main/res/values/strings.xml` (add `cancel`)

**Interfaces (produced, used by Task 7):**
- `RadioSelectionDialog(title: String, options: ImmutableList<String>, selectedIndex: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier)` — `onSelect` fires with the tapped index; caller dismisses.
- `TextInputDialog(title: String, initialValue: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier)`
- `ProgressDialog(title: String, description: String, modifier: Modifier = Modifier)` — not dismissible.

No Paparazzi (dialog windows aren't captured — see Global Constraints); each gets a `@Preview` and is exercised in Task 8's manual QA.

- [ ] **Step 1: Add the `cancel` string**

In `shared/ui/core/src/main/res/values/strings.xml`, next to `confirm` (line ~94):

```xml
<string name="cancel">Cancel</string>
```

(`confirm` already exists; do not duplicate it.)

- [ ] **Step 2: `RadioSelectionDialog.kt`**

```kotlin
package com.ivy.ui.component.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun RadioSelectionDialog(
    title: String,
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            LazyColumn {
                itemsIndexed(options) { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = index == selectedIndex, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun RadioSelectionDialogPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        RadioSelectionDialog(
            title = "Theme",
            options = persistentListOf("Auto", "Light", "Dark"),
            selectedIndex = 0,
            onSelect = {},
            onDismiss = {},
        )
    }
}
```

- [ ] **Step 3: `TextInputDialog.kt`**

```kotlin
package com.ivy.ui.component.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.R

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun TextInputDialogPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        TextInputDialog(
            title = "Name",
            initialValue = "Ivy",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
```

- [ ] **Step 4: `ProgressDialog.kt`**

```kotlin
package com.ivy.ui.component.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.ivy.design.system.IvyMaterial3Theme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressDialog(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        icon = { LoadingIndicator() },
        title = { Text(text = title) },
        text = { Text(text = description) },
        confirmButton = {},
    )
}

@Preview
@Composable
private fun ProgressDialogPreview() {
    IvyMaterial3Theme(isTrueBlack = false) {
        ProgressDialog(title = "Exporting data", description = "Please wait…")
    }
}
```

(If `LoadingIndicator` is unresolved in the pinned BOM, use `androidx.compose.material3.CircularProgressIndicator` instead and drop the `@OptIn` — verify which exists before guessing.)

- [ ] **Step 5: Verify**

Run: `./gradlew :shared:ui:core:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add shared/ui/core/src/main/java/com/ivy/ui/component/dialog/ shared/ui/core/src/main/res/values/strings.xml
git commit -m "feat: add shared M3 dialogs for settings"
```

---

### Task 6: ViewModel contract — `SetTheme` + `SetDynamicColor` (TDD)

**Files:**
- Modify: `feature/settings/src/main/java/com/ivy/settings/SettingsEvent.kt`
- Modify: `feature/settings/src/main/java/com/ivy/settings/SettingsState.kt`
- Modify: `feature/settings/src/main/java/com/ivy/settings/SettingsViewModel.kt`
- Test: `feature/settings/src/test/java/com/ivy/settings/SettingsViewModelTest.kt` (new file, new directory)

**Interfaces:**
- Consumes: `LocalAppearanceDataSource` (Task 1).
- Produces (used by Task 8's UI): `SettingsState(..., dynamicColorEnabled: Boolean, dynamicColorAvailable: Boolean)`; `SettingsEvent.SetTheme(val theme: Theme)`; `SettingsEvent.SetDynamicColor(val enabled: Boolean)`. `SettingsEvent.SwitchTheme` no longer exists.

Note: `SettingsScreen.kt` still references `SwitchTheme` until Task 8 — to keep this task compiling, Step 4 includes a one-line bridge in the old screen.

- [ ] **Step 1: Write the failing test**

Create `feature/settings/src/test/java/com/ivy/settings/SettingsViewModelTest.kt`:

```kotlin
package com.ivy.settings

import android.content.Context
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Theme
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.datasource.LocalAppearanceDataSource
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.write.WriteSettingsDao
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.domain.usecase.csv.ExportCsvUseCase
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.LogoutLogic
import com.ivy.legacy.datamodel.Settings
import com.ivy.legacy.domain.action.settings.UpdateSettingsAct
import com.ivy.ui.testing.ComposeViewModelTest
import com.ivy.ui.testing.runTest
import com.ivy.wallet.domain.action.global.StartDayOfMonthAct
import com.ivy.wallet.domain.action.global.UpdateStartDayOfMonthAct
import com.ivy.wallet.domain.action.settings.SettingsAct
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest : ComposeViewModelTest() {

    private val settingsDao = mockk<SettingsDao>()
    private val ivyContext = mockk<IvyWalletCtx>(relaxed = true)
    private val logoutLogic = mockk<LogoutLogic>(relaxed = true)
    private val sharedPrefs = mockk<SharedPrefs>()
    private val backupDataUseCase = mockk<BackupDataUseCase>(relaxed = true)
    private val startDayOfMonthAct = mockk<StartDayOfMonthAct>()
    private val updateStartDayOfMonthAct = mockk<UpdateStartDayOfMonthAct>(relaxed = true)
    private val syncExchangeRatesUseCase = mockk<SyncExchangeRatesUseCase>(relaxed = true)
    private val settingsAct = mockk<SettingsAct>()
    private val updateSettingsAct = mockk<UpdateSettingsAct>(relaxed = true)
    private val settingsWriter = mockk<WriteSettingsDao>(relaxed = true)
    private val exportCsvUseCase = mockk<ExportCsvUseCase>(relaxed = true)
    private val appearanceDataSource = mockk<LocalAppearanceDataSource>()
    private val context = mockk<Context>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        coEvery { settingsDao.findFirst() } returns SettingsEntity(
            theme = Theme.AUTO,
            currency = "USD",
            bufferAmount = 0.0,
            name = "Test",
        )
        coEvery { settingsAct(Unit) } returns InitialSettings
        coEvery { startDayOfMonthAct(Unit) } returns 1
        every { sharedPrefs.getBoolean(any(), any()) } answers { secondArg() }
        every { appearanceDataSource.dynamicColor } returns flowOf(true)
        coEvery { appearanceDataSource.setDynamicColor(any()) } just Runs

        viewModel = SettingsViewModel(
            settingsDao = settingsDao,
            ivyContext = ivyContext,
            logoutLogic = logoutLogic,
            sharedPrefs = sharedPrefs,
            backupDataUseCase = backupDataUseCase,
            startDayOfMonthAct = startDayOfMonthAct,
            updateStartDayOfMonthAct = updateStartDayOfMonthAct,
            syncExchangeRatesUseCase = syncExchangeRatesUseCase,
            settingsAct = settingsAct,
            updateSettingsAct = updateSettingsAct,
            settingsWriter = settingsWriter,
            exportCsvUseCase = exportCsvUseCase,
            appearanceDataSource = appearanceDataSource,
            context = context,
        )
    }

    @Test
    fun `sets an explicit theme`() {
        // given
        coEvery { updateSettingsAct(any()) } returns InitialSettings.copy(theme = Theme.DARK)

        // when-then
        viewModel.runTest(events = listOf(SettingsEvent.SetTheme(Theme.DARK))) {
            currentTheme shouldBe Theme.DARK
        }
        coVerify { updateSettingsAct(match { it.theme == Theme.DARK }) }
        verify { ivyContext.switchTheme(Theme.DARK) }
    }

    @Test
    fun `disabling dynamic color persists the preference`() {
        viewModel.runTest(events = listOf(SettingsEvent.SetDynamicColor(false))) {
            // state comes from the (static) mocked flow; the write is the contract
        }
        coVerify { appearanceDataSource.setDynamicColor(false) }
    }

    @Test
    fun `reflects a disabled dynamic color preference in state`() {
        every { appearanceDataSource.dynamicColor } returns flowOf(false)

        viewModel.runTest {
            dynamicColorEnabled shouldBe false
        }
    }

    companion object {
        private val InitialSettings = Settings(
            theme = Theme.AUTO,
            baseCurrency = "USD",
            bufferAmount = BigDecimal.ZERO,
            name = "Test",
        )
    }
}
```

(Do not assert `dynamicColorAvailable` — it reads `Build.VERSION.SDK_INT`, whose value under Paparazzi's JVM environment is not a behavior we own.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :feature:settings:testDebugUnitTest`
Expected: FAILS to compile — `SetTheme`, `SetDynamicColor`, `dynamicColorEnabled`, and the `appearanceDataSource` constructor parameter don't exist yet.

- [ ] **Step 3: Implement the contract**

`SettingsEvent.kt` — replace `data object SwitchTheme : SettingsEvent` with:

```kotlin
    data class SetTheme(val theme: Theme) : SettingsEvent
    data class SetDynamicColor(val enabled: Boolean) : SettingsEvent
```

and add `import com.ivy.base.legacy.Theme`.

`SettingsState.kt` — add two properties:

```kotlin
    val dynamicColorEnabled: Boolean,
    val dynamicColorAvailable: Boolean,
```

`SettingsViewModel.kt`:

1. Constructor: add `private val appearanceDataSource: LocalAppearanceDataSource,` after `exportCsvUseCase` (import `com.ivy.data.datasource.LocalAppearanceDataSource`, `androidx.compose.runtime.collectAsState`, `androidx.compose.runtime.getValue`).
2. In `uiState()` add to the returned `SettingsState`:

```kotlin
            dynamicColorEnabled = getDynamicColorEnabled(),
            dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
```

3. Add the getter next to the other `get...()` helpers:

```kotlin
    @Composable
    private fun getDynamicColorEnabled(): Boolean {
        val enabled by appearanceDataSource.dynamicColor.collectAsState(initial = true)
        return enabled
    }
```

4. In `onEvent`, replace `SettingsEvent.SwitchTheme -> switchTheme()` with:

```kotlin
            is SettingsEvent.SetTheme -> setTheme(event.theme)
            is SettingsEvent.SetDynamicColor -> setDynamicColor(event.enabled)
```

5. Replace `private fun switchTheme()` with:

```kotlin
    private fun setTheme(theme: Theme) {
        viewModelScope.launch {
            val updated = settingsAct(Unit).copy(theme = theme)
            updateSettingsAct(updated)
            ivyContext.switchTheme(theme)
            currentTheme.value = theme
        }
    }

    private fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            appearanceDataSource.setDynamicColor(enabled)
        }
    }
```

- [ ] **Step 4: Bridge the old screen so the module compiles**

In the old `SettingsScreen.kt` (still legacy until Task 8), change the one `SwitchTheme` dispatch:

```kotlin
        onSwitchTheme = {
            viewModel.onEvent(SettingsEvent.SetTheme(uiState.currentTheme))
        },
```

The legacy screen's `Preview` builds `UI(...)` from individual parameters (not `SettingsState`), so this event bridge is the only change the old file needs to keep compiling.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :feature:settings:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL, 3 tests pass.

- [ ] **Step 6: Commit**

```bash
git add feature/settings/ 
git commit -m "feat: add explicit theme and dynamic color events to settings"
```

---

### Task 7: New Settings UI (screen rewrite + currency picker + legacy flip)

**Files:**
- Create: `feature/settings/src/main/java/com/ivy/settings/CurrencyPickerDialog.kt`
- Rewrite: `feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt`
- Modify: `shared/ui/core/src/main/res/values/strings.xml`
- Modify: `shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt:93-96`
- Test: `feature/settings/src/test/java/com/ivy/settings/SettingsScreenPaparazziTest.kt`

**Interfaces:**
- Consumes: all Task 4/5 components, Task 6 contract, `BackButton` (`com.ivy.ui.component`), `IvyCurrency` (`com.ivy.legacy.domain.data`), `Constants` (`com.ivy.legacy`).
- Produces: `SettingsScreen()` — plain composable, **no** `BoxWithConstraintsScope` receiver, no `@ExperimentalFoundationApi`; `SettingsUiTest(isDark: Boolean)` kept as the screenshot entry point. The `IvyNavGraph` call site (`app/src/main/java/com/ivy/IvyNavGraph.kt:74`, `is SettingsScreen -> SettingsScreen()`) compiles unchanged.

- [ ] **Step 1: Add the new strings**

In `shared/ui/core/src/main/res/values/strings.xml` (none of these keys exist yet — verified):

```xml
<string name="profile">Profile</string>
<string name="appearance">Appearance</string>
<string name="behavior">Behavior</string>
<string name="privacy">Privacy</string>
<string name="about_and_support">About &amp; Support</string>
<string name="name">Name</string>
<string name="currency">Currency</string>
<string name="theme">Theme</string>
<string name="dynamic_color">Dynamic color</string>
<string name="dynamic_color_description">Use colors from your wallpaper (Material You)</string>
<string name="search">Search</string>
<string name="fiat_currencies">Fiat</string>
<string name="crypto_currencies">Crypto</string>
```

- [ ] **Step 2: `CurrencyPickerDialog.kt`**

```kotlin
package com.ivy.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency

private val CurrencyListMaxHeight = 400.dp

@Composable
internal fun CurrencyPickerDialog(
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val allCurrencies = remember { IvyCurrency.getAvailable() }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            allCurrencies
        } else {
            allCurrencies.filter {
                it.code.contains(q, ignoreCase = true) ||
                    it.name.contains(q, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.set_currency)) },
        text = {
            Column {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(text = stringResource(R.string.search)) },
                )
                LazyColumn(modifier = Modifier.heightIn(max = CurrencyListMaxHeight)) {
                    currencyGroup(
                        titleResId = R.string.fiat_currencies,
                        currencies = filtered.filterNot(IvyCurrency::isCrypto),
                        selectedCode = selectedCode,
                        onSelect = onSelect,
                    )
                    currencyGroup(
                        titleResId = R.string.crypto_currencies,
                        currencies = filtered.filter(IvyCurrency::isCrypto),
                        selectedCode = selectedCode,
                        onSelect = onSelect,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.currencyGroup(
    titleResId: Int,
    currencies: List<IvyCurrency>,
    selectedCode: String,
    onSelect: (String) -> Unit,
) {
    if (currencies.isEmpty()) return
    item(key = "group_$titleResId") {
        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            text = stringResource(titleResId),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    items(currencies, key = { it.code }) { currency ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(currency.code) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "${currency.code} — ${currency.name}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (currency.code == selectedCode) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
```

(If detekt/compose flags the fully-qualified `LazyListScope` receiver, import `androidx.compose.foundation.lazy.LazyListScope` instead.)

- [ ] **Step 3: Rewrite `SettingsScreen.kt`**

Replace the entire file with:

```kotlin
package com.ivy.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.legacy.Theme
import com.ivy.legacy.Constants
import com.ivy.legacy.rootScreen
import com.ivy.navigation.AttributionsScreen
import com.ivy.navigation.ContributorsScreen
import com.ivy.navigation.ExchangeRatesScreen
import com.ivy.navigation.FeaturesScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.ReleasesScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.BackButton
import com.ivy.ui.component.dialog.ProgressDialog
import com.ivy.ui.component.dialog.RadioSelectionDialog
import com.ivy.ui.component.dialog.TextInputDialog
import com.ivy.ui.component.settings.ScreenDisplayTitle
import com.ivy.ui.component.settings.SettingsItem
import com.ivy.ui.component.settings.SettingsSectionTitle
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val DaysInMonth = 31

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    val rootScreen = rootScreen()

    SettingsUi(
        uiState = uiState,
        versionText = "${rootScreen.buildVersionName} (${rootScreen.buildVersionCode})",
        onEvent = viewModel::onEvent,
        onBackupData = { viewModel.onEvent(SettingsEvent.BackupData(rootScreen)) },
        onExportToCsv = { viewModel.onEvent(SettingsEvent.ExportToCsv(rootScreen)) },
        onRateUs = { rootScreen.reviewIvyWallet(dismissReviewCard = false) },
        onShareIvyWallet = { rootScreen.shareIvyWallet() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "LongParameterList")
private fun SettingsUi(
    uiState: SettingsState,
    versionText: String,
    onEvent: (SettingsEvent) -> Unit,
    onBackupData: () -> Unit,
    onExportToCsv: () -> Unit,
    onRateUs: () -> Unit,
    onShareIvyWallet: () -> Unit,
) {
    val nav = navigation()

    var nameDialogVisible by remember { mutableStateOf(false) }
    var currencyDialogVisible by remember { mutableStateOf(false) }
    var themeDialogVisible by remember { mutableStateOf(false) }
    var startDateDialogVisible by remember { mutableStateOf(false) }
    var deleteAllDataDialogVisible by remember { mutableStateOf(false) }
    var deleteAllDataFinalDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    BackButton(onClick = { nav.onBackPressed() })
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("settings_lazy_column"),
        ) {
            item {
                ScreenDisplayTitle(
                    text = stringResource(R.string.settings),
                    description = versionText,
                    onDescriptionClick = { nav.navigateTo(ReleasesScreen) },
                )
            }
            profileSection(
                uiState = uiState,
                onNameClick = { nameDialogVisible = true },
                onCurrencyClick = { currencyDialogVisible = true },
            )
            appearanceSection(
                uiState = uiState,
                onEvent = onEvent,
                onThemeClick = { themeDialogVisible = true },
            )
            behaviorSection(
                uiState = uiState,
                onEvent = onEvent,
                onStartDateClick = { startDateDialogVisible = true },
                onExchangeRatesClick = { nav.navigateTo(ExchangeRatesScreen) },
                onAdvancedFeaturesClick = { nav.navigateTo(FeaturesScreen) },
            )
            privacySection(uiState = uiState, onEvent = onEvent)
            importExportSection(
                uiState = uiState,
                onImportClick = {
                    nav.navigateTo(ImportScreen(launchedFromOnboarding = false))
                },
                onBackupData = onBackupData,
                onExportToCsv = onExportToCsv,
            )
            aboutSection(
                onRateUs = onRateUs,
                onShareIvyWallet = onShareIvyWallet,
                onReleasesClick = { nav.navigateTo(ReleasesScreen) },
                onContributorsClick = { nav.navigateTo(ContributorsScreen) },
                onAttributionsClick = { nav.navigateTo(AttributionsScreen) },
            )
            dangerZoneSection(onDeleteAllData = { deleteAllDataDialogVisible = true })
            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }

    SettingsDialogs(
        uiState = uiState,
        onEvent = onEvent,
        nameDialogVisible = nameDialogVisible,
        onNameDialogVisible = { nameDialogVisible = it },
        currencyDialogVisible = currencyDialogVisible,
        onCurrencyDialogVisible = { currencyDialogVisible = it },
        themeDialogVisible = themeDialogVisible,
        onThemeDialogVisible = { themeDialogVisible = it },
        startDateDialogVisible = startDateDialogVisible,
        onStartDateDialogVisible = { startDateDialogVisible = it },
        deleteAllDataDialogVisible = deleteAllDataDialogVisible,
        onDeleteAllDataDialogVisible = { deleteAllDataDialogVisible = it },
        deleteAllDataFinalDialogVisible = deleteAllDataFinalDialogVisible,
        onDeleteAllDataFinalDialogVisible = { deleteAllDataFinalDialogVisible = it },
    )
}

private fun LazyListScope.profileSection(
    uiState: SettingsState,
    onNameClick: () -> Unit,
    onCurrencyClick: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.profile))
    }
    item {
        SettingsItem(
            title = stringResource(R.string.name),
            description = uiState.name.ifBlank { stringResource(R.string.anonymous) },
            onClick = onNameClick,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.currency),
            description = uiState.currencyCode,
            onClick = onCurrencyClick,
        )
    }
}

private fun LazyListScope.appearanceSection(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onThemeClick: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.appearance))
    }
    item {
        SettingsItem(
            title = stringResource(R.string.theme),
            description = themeLabel(uiState.currentTheme),
            onClick = onThemeClick,
        )
    }
    if (uiState.dynamicColorAvailable) {
        item {
            SettingsItem(
                title = stringResource(R.string.dynamic_color),
                description = stringResource(R.string.dynamic_color_description),
                onClick = { onEvent(SettingsEvent.SetDynamicColor(!uiState.dynamicColorEnabled)) },
            ) {
                Switch(
                    checked = uiState.dynamicColorEnabled,
                    onCheckedChange = { onEvent(SettingsEvent.SetDynamicColor(it)) },
                )
            }
        }
    }
    if (uiState.languageOptionVisible) {
        item {
            SettingsItem(
                title = stringResource(R.string.language),
                description = Locale.getDefault().displayName,
                onClick = { onEvent(SettingsEvent.SwitchLanguage) },
            )
        }
    }
}

private fun LazyListScope.behaviorSection(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onStartDateClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onAdvancedFeaturesClick: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.behavior))
    }
    item {
        SettingsItem(
            title = stringResource(R.string.start_date_of_month),
            description = uiState.startDateOfMonth,
            onClick = onStartDateClick,
        )
    }
    item {
        SwitchItem(
            title = stringResource(R.string.transfers_as_income_expense),
            description = stringResource(R.string.transfers_as_income_expense_description),
            checked = uiState.treatTransfersAsIncomeExpense,
            onCheckedChange = { onEvent(SettingsEvent.SetTransfersAsIncomeExpense(it)) },
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.exchange_rates),
            onClick = onExchangeRatesClick,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.advanced_features),
            onClick = onAdvancedFeaturesClick,
        )
    }
}

private fun LazyListScope.privacySection(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.privacy))
    }
    item {
        SwitchItem(
            title = stringResource(R.string.lock_app),
            checked = uiState.lockApp,
            onCheckedChange = { onEvent(SettingsEvent.SetLockApp(it)) },
        )
    }
    item {
        SwitchItem(
            title = stringResource(R.string.show_notifications),
            checked = uiState.showNotifications,
            onCheckedChange = { onEvent(SettingsEvent.SetShowNotifications(it)) },
        )
    }
    item {
        SwitchItem(
            title = stringResource(R.string.hide_balance),
            description = stringResource(R.string.hide_balance_description),
            checked = uiState.hideCurrentBalance,
            onCheckedChange = { onEvent(SettingsEvent.SetHideCurrentBalance(it)) },
        )
    }
    item {
        SwitchItem(
            title = stringResource(R.string.hide_income),
            description = stringResource(R.string.hide_income_description),
            checked = uiState.hideIncome,
            onCheckedChange = { onEvent(SettingsEvent.SetHideIncome(it)) },
        )
    }
}

private fun LazyListScope.importExportSection(
    uiState: SettingsState,
    onImportClick: () -> Unit,
    onBackupData: () -> Unit,
    onExportToCsv: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.import_export))
    }
    item {
        SettingsItem(
            title = stringResource(R.string.import_data),
            onClick = onImportClick,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.backup_data),
            onClick = onBackupData,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.export_to_csv),
            description = stringResource(R.string.do_not_use_for_backup_purposes),
            onClick = onExportToCsv,
        )
    }
}

private fun LazyListScope.aboutSection(
    onRateUs: () -> Unit,
    onShareIvyWallet: () -> Unit,
    onReleasesClick: () -> Unit,
    onContributorsClick: () -> Unit,
    onAttributionsClick: () -> Unit,
) {
    item {
        SettingsSectionTitle(text = stringResource(R.string.about_and_support))
    }
    item {
        SettingsItem(title = stringResource(R.string.rate_us_on_google_play), onClick = onRateUs)
    }
    item {
        SettingsItem(title = stringResource(R.string.share_ivy_wallet), onClick = onShareIvyWallet)
    }
    item {
        UrlItem(
            title = stringResource(R.string.ivy_wallet_is_opensource),
            url = Constants.URL_IVY_WALLET_REPO,
        )
    }
    item {
        UrlItem(
            title = stringResource(R.string.ivy_telegram),
            url = Constants.URL_IVY_TELEGRAM_INVITE,
        )
    }
    item {
        UrlItem(
            title = stringResource(R.string.help_center),
            url = Constants.URL_HELP_CENTER,
        )
    }
    item {
        SettingsItem(title = stringResource(R.string.releases), onClick = onReleasesClick)
    }
    item {
        UrlItem(
            title = stringResource(R.string.report_bug),
            url = Constants.URL_GITHUB_NEW_ISSUE,
        )
    }
    item {
        UrlItem(
            title = stringResource(R.string.request_a_feature),
            url = Constants.URL_GITHUB_NEW_ISSUE,
        )
    }
    item {
        UrlItem(
            title = stringResource(R.string.contact_support),
            url = Constants.URL_IVY_TELEGRAM_INVITE,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.project_contributors),
            onClick = onContributorsClick,
        )
    }
    item {
        SettingsItem(title = stringResource(R.string.attributions), onClick = onAttributionsClick)
    }
    item {
        UrlItem(title = stringResource(R.string.terms_conditions), url = Constants.URL_TC)
    }
    item {
        UrlItem(title = stringResource(R.string.privacy_policy), url = Constants.URL_PRIVACY_POLICY)
    }
}

private fun LazyListScope.dangerZoneSection(onDeleteAllData: () -> Unit) {
    item {
        SettingsSectionTitle(
            text = stringResource(R.string.danger_zone),
            color = MaterialTheme.colorScheme.error,
        )
    }
    item {
        SettingsItem(
            title = stringResource(R.string.delete_all_user_data),
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onDeleteAllData,
        )
    }
}

@Composable
private fun SwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    SettingsItem(
        title = title,
        description = description,
        onClick = { onCheckedChange(!checked) },
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun UrlItem(title: String, url: String) {
    val uriHandler = LocalUriHandler.current
    SettingsItem(
        title = title,
        onClick = { uriHandler.openUri(url) },
    )
}

@Composable
private fun themeLabel(theme: Theme): String = stringResource(
    when (theme) {
        Theme.LIGHT -> R.string.light_mode
        Theme.DARK -> R.string.dark_mode
        Theme.AMOLED_DARK -> R.string.amoled_mode
        Theme.AUTO -> R.string.auto_mode
    }
)

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun SettingsDialogs(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    nameDialogVisible: Boolean,
    onNameDialogVisible: (Boolean) -> Unit,
    currencyDialogVisible: Boolean,
    onCurrencyDialogVisible: (Boolean) -> Unit,
    themeDialogVisible: Boolean,
    onThemeDialogVisible: (Boolean) -> Unit,
    startDateDialogVisible: Boolean,
    onStartDateDialogVisible: (Boolean) -> Unit,
    deleteAllDataDialogVisible: Boolean,
    onDeleteAllDataDialogVisible: (Boolean) -> Unit,
    deleteAllDataFinalDialogVisible: Boolean,
    onDeleteAllDataFinalDialogVisible: (Boolean) -> Unit,
) {
    if (nameDialogVisible) {
        TextInputDialog(
            title = stringResource(R.string.name),
            initialValue = uiState.name,
            onConfirm = {
                onEvent(SettingsEvent.SetName(it))
                onNameDialogVisible(false)
            },
            onDismiss = { onNameDialogVisible(false) },
        )
    }
    if (currencyDialogVisible) {
        CurrencyPickerDialog(
            selectedCode = uiState.currencyCode,
            onSelect = {
                onEvent(SettingsEvent.SetCurrency(it))
                onCurrencyDialogVisible(false)
            },
            onDismiss = { onCurrencyDialogVisible(false) },
        )
    }
    if (themeDialogVisible) {
        val themeOptions = remember {
            persistentListOf(Theme.AUTO, Theme.LIGHT, Theme.DARK, Theme.AMOLED_DARK)
        }
        RadioSelectionDialog(
            title = stringResource(R.string.theme),
            options = themeOptions.map { themeLabel(it) }.toImmutableList(),
            selectedIndex = themeOptions.indexOf(uiState.currentTheme),
            onSelect = { index ->
                onEvent(SettingsEvent.SetTheme(themeOptions[index]))
                onThemeDialogVisible(false)
            },
            onDismiss = { onThemeDialogVisible(false) },
        )
    }
    if (startDateDialogVisible) {
        RadioSelectionDialog(
            title = stringResource(R.string.choose_start_date_of_month),
            options = (1..DaysInMonth).map(Int::toString).toImmutableList(),
            selectedIndex = (uiState.startDateOfMonth.toIntOrNull() ?: 1) - 1,
            onSelect = { index ->
                onEvent(SettingsEvent.SetStartDateOfMonth(index + 1))
                onStartDateDialogVisible(false)
            },
            onDismiss = { onStartDateDialogVisible(false) },
        )
    }
    if (deleteAllDataDialogVisible) {
        AlertDialog(
            onDismissRequest = { onDeleteAllDataDialogVisible(false) },
            title = { Text(text = stringResource(R.string.delete_all_user_data_question)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_all_user_data_warning,
                        stringResource(R.string.your_account),
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllDataDialogVisible(false)
                        onDeleteAllDataFinalDialogVisible(true)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onDeleteAllDataDialogVisible(false) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
    if (deleteAllDataFinalDialogVisible) {
        AlertDialog(
            onDismissRequest = { onDeleteAllDataFinalDialogVisible(false) },
            title = {
                Text(
                    text = stringResource(
                        R.string.confirm_all_userd_data_deletion,
                        stringResource(R.string.all_of_your_data),
                    )
                )
            },
            text = { Text(text = stringResource(R.string.final_deletion_warning)) },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsEvent.DeleteAllUserData) }) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onDeleteAllDataFinalDialogVisible(false) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
    if (uiState.progressState) {
        ProgressDialog(
            title = stringResource(R.string.exporting_data),
            description = stringResource(R.string.exporting_data_description),
        )
    }
}

@Preview
@Composable
private fun Preview(dark: Boolean = false) {
    IvyPreview(dark = dark) {
        SettingsUi(
            uiState = SettingsState(
                currencyCode = "USD",
                name = "Ivy",
                currentTheme = Theme.AUTO,
                lockApp = false,
                showNotifications = true,
                hideCurrentBalance = false,
                hideIncome = false,
                treatTransfersAsIncomeExpense = false,
                startDateOfMonth = "1",
                progressState = false,
                languageOptionVisible = true,
                dynamicColorEnabled = true,
                dynamicColorAvailable = true,
            ),
            versionText = "1.0.0 (100)",
            onEvent = {},
            onBackupData = {},
            onExportToCsv = {},
            onRateUs = {},
            onShareIvyWallet = {},
        )
    }
}

/** For screenshot testing */
@Composable
fun SettingsUiTest(isDark: Boolean) {
    Preview(dark = isDark)
}
```

Notes for the implementer:
- The dead `DeleteCloudUserData` flow (its modal was never reachable in the old UI) gets **no** row — the VM event stays untouched.
- The old file's imports of `CurrencyModal`, `NameModal`, `ChooseStartDateOfMonthModal`, `DeleteModal`, `ProgressModal`, `IvyToolbar`, `IvySwitch`, gradients, and `IvyIconScaled` all disappear; do not delete those components from `temp/old-design`.
- Check string plurals/args compile: `delete_all_user_data_warning` and `confirm_all_userd_data_deletion` (sic) take one format arg each — exactly as the old code used them.

- [ ] **Step 4: Flip the screen to non-legacy**

`shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt:93`:

```kotlin
data object SettingsScreen : Screen {
    override val isLegacy: Boolean
        get() = false
}
```

Remove `@ExperimentalFoundationApi` from the settings entry in `app/src/main/java/com/ivy/IvyNavGraph.kt` only if the compiler complains; the call `is SettingsScreen -> SettingsScreen()` itself is unchanged.

- [ ] **Step 5: Screen Paparazzi test**

`feature/settings/src/test/java/com/ivy/settings/SettingsScreenPaparazziTest.kt` (feature modules use `com.ivy.ui.testing.PaparazziScreenshotTest`):

```kotlin
package com.ivy.settings

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SettingsScreenPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot Settings screen`() {
        snapshot(theme) {
            SettingsUiTest(isDark = theme == PaparazziTheme.Dark)
        }
    }
}
```

- [ ] **Step 6: Record + verify**

Run: `./gradlew :feature:settings:recordPaparazziDebug`
Then: `./gradlew :feature:settings:verifyPaparazziDebug :feature:settings:testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL; VM tests still green; new snapshots under `feature/settings/src/test/snapshots/`.

- [ ] **Step 7: Full app build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (catches the nav-graph call site and any missed legacy import).

- [ ] **Step 8: Commit**

```bash
git add feature/settings/ shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt shared/ui/core/src/main/res/values/strings.xml app/src/main/java/com/ivy/IvyNavGraph.kt
git commit -m "feat: rebuild settings screen in M3 Expressive style"
```

---

### Task 8: Full verification sweep + manual QA

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

Install `assembleDebug` on an API 31+ emulator and verify:

- Settings opens with the new design; legacy screens (Home etc.) unaffected.
- Every row triggers its dialog/navigation/toggle; switches toggle on full-row tap.
- Theme dialog: all four options apply immediately and persist across restart.
- Dynamic color switch: toggling visibly re-themes to/from the purple brand scheme, persists across restart; on an API < 31 emulator the row is absent.
- Name and currency dialogs persist; currency search filters both code and name.
- Start date dialog shows 1–31, persists.
- Delete flow remains two-step and actually deletes.
- Export CSV/backup show the progress dialog and share sheet.
- Tapping the version line under "Settings" opens Releases.

- [ ] **Step 3: Report**

Summarize results (with any deviations) to the user. **Do not push** — the user decides when and where to push.
