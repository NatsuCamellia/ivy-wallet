# Material 3 Expressive 遷移實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 刪除 Ivy design system，全 App 改用語意化的原生 M3 Expressive，並產出截圖 diff 和行為改變報告。

**Architecture:** Strangler 遷移。舊 `UI.*` 和新的 `MaterialTheme` 並存到 S6 才刪舊模組，每個階段結束時 build 和測試都是綠的。`ActionSheet` 是唯一的版面 wrapper；共用元件只允許帶 domain 邏輯或資料的 composable。

**Tech Stack:** Kotlin 2.4.21、AGP 9.3.3、Gradle 9.6.1、Compose 1.12.1、material3 1.5.0-rc01、Paparazzi 2.0.0-alpha05.1、Hilt 2.60.1、detekt 1.23.8。

**Spec:** `docs/superpowers/specs/2026-11-07-m3-expressive-design.md`。每個 agent 都要先讀這份 spec，元件對照、token 對照和行為改變表都以它為準，本計畫不重複。

## Global Constraints

- material3 `1.5.0-rc01`；minSdk 31；compileSdk 37；targetSdk 36；JVM 21。
- App 一律動態色；Ivy 紫 `0xFF6B4DFF` 種子 scheme 只給 `LocalInspectionMode` 下的測試和 Preview。
- 不改 domain、data、ViewModel 邏輯，UI 必要的調整除外；不動 Glance 和 RemoteViews 外觀；不加新功能。
- 非截圖測試的斷言不得修改。若非改不可，停下來問使用者。
- 不建命名的間距常數、自訂 design token，或只替 M3 元件設預設參數的函式。
- 間距：`spacedBy`、`contentPadding`、`padding`、偶爾的 `Spacer`，數值限 4、8、12、16、24；每模組每 100 行新增最多 3 個數字 `.dp`。
- 測試類別和方法名稱從 S0 起凍結，新測試名稱不含空白。
- 改動行為時，同一個 commit 更新 `docs/m3-migration/LEDGER.md`（欄位：id、stage、part、test key、old、new、reason、evidence）。
- 依階段 commit 到目前分支，不 push、不開 PR。Commit 訊息結尾加 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`。
- Shell 是 zsh：`--include` 的 glob 要加引號。

## 執行協定

- **實作：** 每個 task 派一個實作 agent，預設 `sonnet`；標記 `[haiku]` 的 task 用 `haiku`。
- **驗收：** 每個 task 完成後派一個全新的驗收 agent（`sonnet`），只看 spec、該 task 和 diff，回報通過或失敗清單，不代為修改。驗收項目見 spec 的「驗收流程」。
- **連續 3 輪未過：** 主 session 親自診斷並換實作 agent。視覺或間距爭議可記入 `docs/m3-migration/GAPS.md` 後繼續，測試失敗不行。
- **階段結束：** 跑全量 `testDebugUnitTest`、`verifyPaparazziDebug`、`detekt`、`lintRelease`。

## Review Focus

1. 文字很長或字體放大時，`ActionSheet` 的固定動作列不能被內容擠掉，內容要能捲動。測試見 Task 2.4。
2. 使用者選了極淺或極深的顏色時，徽章內的圖示仍要看得清楚。測試見 Task 4.1。
3. 金額位數很多時，主視覺金額和交易列不能裁切或擠壓名稱。測試見 Task 4.1。
4. AMOLED 模式下，卡片和 sheet 要能和純黑背景區分。測試見 Task 2.1。
5. 沒有帳戶、交易或分類時，空狀態要能正常顯示，FAB 仍可使用。測試見 Task 4.3、4.4。

---

## S0 基準

### Task 0.1: 確認舊工具鏈的測試基準 [haiku]

**Files:** 不改程式碼。

- [ ] **Step 1:** 執行 `./gradlew :feature:accounts:verifyPaparazziDebug`。Expected: BUILD SUCCESSFUL。失敗且原因是 JDK 21 時，安裝 Temurin 17 到 `~/.local/share/jdk/17`，後續用 `JAVA_HOME` 指定。
- [ ] **Step 2:** 執行 `./gradlew testDebugUnitTest verifyPaparazziDebug detekt`。Expected: BUILD SUCCESSFUL。原本就失敗的項目逐一回報給主 session，不修改。

### Task 0.2: 補齊畫面截圖覆蓋

**Files:**
- Modify: settings、edit-transaction、budgets、onboarding、main、planned-payments、piechart、features、import-data、releases 的畫面檔
- Create: 上述模組的 `src/test/java/.../*PaparazziTest.kt`，以及 feature/balance 的 `BalanceScreenPaparazziTest`
- Delete: `feature/accounts/src/test/snapshots/images/` 裡沒有對應測試的 `accountTab composable` golden

**Interfaces:**
- Produces: 每個畫面有 `/** For screenshot testing */ @Composable fun XUiTest(isDark: Boolean)`，沿用 `feature/home/.../HomeTab.kt` 的寫法，呼叫既有的 private Preview。
- Produces: 展開狀態（Main 的 FAB、Home 的 more menu）走 Preview 參數，各有獨立測試方法。

- [ ] **Step 1:** 對每個模組加 `XUiTest` 和測試類別，繼承 `com.ivy.ui.testing.PaparazziScreenshotTest`，用 `PaparazziTheme` 參數化 Light、Dark。方法名不含空白，例如 `snapshotSettingsScreen`。features、releases 補範例 state。
- [ ] **Step 2:** 執行 `./gradlew recordPaparazziDebug`，用 Read 檢查新 golden 確實渲染出畫面內容，不是空白。
- [ ] **Step 3:** 執行 `./gradlew verifyPaparazziDebug`。Expected: BUILD SUCCESSFUL。
- [ ] **Step 4:** Commit。

### Task 0.3: Modal 截圖覆蓋

**Files:**
- Modify: `temp/legacy-code/build.gradle.kts`（`testImplementation(projects.shared.ui.testing)`）、`AmountKeyboard` 所在檔案、`shared/ui/testing/.../PaparazziScreenshotTest.kt`
- Create: `temp/legacy-code/src/test/java/com/ivy/legacy/ModalsPaparazziTest.kt`、feature 模組內 modal 的測試（`BudgetModal`、`LoanModal` 等）

**Interfaces:**
- Produces: `PaparazziScreenshotTest.snapshot` 以 `LocalInspectionMode provides true` 包住內容。
- Produces: `AmountKeyboard` 在 `LocalInspectionMode.current` 為 true 時跳過 `EntryPointAccessors`。
- Produces: `ModalsPaparazziTest` 用 enum 參數化，方法 `snapshotModal`，每個 modal 有 `XModalUiTest(isDark: Boolean)`，以 `visible = true` 渲染。

- [ ] **Step 1:** 完成上述改動，覆蓋全部 23 個 modal 檔案和 feature 模組內的 modal。
- [ ] **Step 2:** 執行 `./gradlew recordPaparazziDebug verifyPaparazziDebug`，抽查 5 張確認 modal 完全展開。Expected: BUILD SUCCESSFUL。
- [ ] **Step 3:** Commit，然後 `git tag m3-baseline`。

## S1 工具鏈

### Task 1.1: 升級工具鏈（單一 commit）

**Files:** `gradle/libs.versions.toml`、`gradle/wrapper/*`、`buildSrc/**`、`app/build.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`.github/workflows/*`、`ci-actions/**`、`shared/ui/testing/**`

版本和已知處理項目見 spec 的「S1 目標版本」，不得更動版本。先載入 `agp-9-upgrade` skill。

- [ ] **Step 1:** 改好版本和 build 設定，加 `testOptions.targetSdk = 36`，執行 `./gradlew assembleDebug`。Expected: BUILD SUCCESSFUL。
- [ ] **Step 2: Paparazzi 閘門。** 在 `shared/ui/testing` 加暫時測試，錄製一個 Compose 畫面和一個 `ModalBottomSheet`。sheet 需要時間偏移就加 `snapshotSettled(theme, offsetMillis: Long = 1000, content)` 到 `PaparazziScreenshotTest`。Expected: 兩張圖內容正確。失敗時依 spec 退路改 Roborazzi；兩者都失敗就停下來問使用者。
- [ ] **Step 3:** 執行 `./gradlew testDebugUnitTest detekt lintRelease`，修到通過，不改測試斷言。
- [ ] **Step 4:** Commit。

### Task 1.2: 重錄 golden 並建立 ledger [haiku]

**Files:** Create `docs/m3-migration/LEDGER.md`、`docs/m3-migration/GAPS.md`；Modify 全部 golden。

- [ ] **Step 1:** 刪除舊 golden（檔名含空白者會換名），執行 `./gradlew recordPaparazziDebug verifyPaparazziDebug`。Expected: BUILD SUCCESSFUL，golden 數量和 S0 相同。
- [ ] **Step 2:** 在 `LEDGER.md` 的「S1 渲染差異」段落記錄有明顯變化的畫面。
- [ ] **Step 3:** 建立 AVD 後執行 `:shared:data:core:connectedDebugAndroidTest`、`:shared:domain:connectedDebugAndroidTest`。Expected: BUILD SUCCESSFUL。
- [ ] **Step 4:** Commit。

## S2 基礎

### Task 2.1: WalletTheme

**Files:**
- Create: `shared/ui/core/src/main/java/com/ivy/ui/theme/WalletTheme.kt`、`SeedColorSchemes.kt`
- Test: `shared/ui/core/src/test/java/com/ivy/ui/theme/WalletThemeTest.kt`、`WalletThemePaparazziTest.kt`
- Modify: `temp/old-design/.../l0_system/IvyTheme.kt`（內部改呼叫 `WalletTheme`）

**Interfaces:**
- Produces: `@Composable fun WalletTheme(theme: Theme, content: @Composable () -> Unit)`
- Produces: `fun Theme.isDark(systemDark: Boolean): Boolean`、`fun ColorScheme.trueBlack(): ColorScheme`

- [ ] **Step 1:** 寫失敗測試：`LIGHT`、`DARK`、`AMOLED_DARK` 不看系統，`AUTO` 跟系統；`trueBlack()` 後 `background`、`surface`、`surfaceDim`、`surfaceContainerLowest` 為 `Color.Black`，`surfaceContainerHigh` 不是黑色。
- [ ] **Step 2:** 執行 `./gradlew :shared:ui:core:testDebugUnitTest`。Expected: FAIL（unresolved reference）。
- [ ] **Step 3:** 實作，內容見 spec S2。種子 scheme 用 `npx @material/material-color-utilities` 或等效工具產生一次後寫成 literal。移植 `IvyTheme` 的 `WindowCompat` 狀態列處理。
- [ ] **Step 4:** 加 `WalletThemePaparazziTest.snapshotAmoledSurfaces`：黑底上放 `Card` 和 sheet 內容。測試通過後 commit。

### Task 2.2: 搬移非設計程式碼

**Files:** `com.ivy.ui.color`、`com.ivy.ui.time`、`com.ivy.ui.util`（`shared/ui/core`）；`temp/legacy-code/.../IvyWalletCtx.kt`；所有 import 處。

**Interfaces:**
- Produces: `com.ivy.ui.color.findContrastTextColor`、調色盤和 `IVY_COLOR_PICKER_COLORS_*`（值不變）；`com.ivy.ui.time.LocalTimeConverter/Provider/Formatter`；`com.ivy.ui.util.thenIf`；`LocalWalletCtx` 取代 `LocalIvyContext`。

- [ ] **Step 1:** 用 IDE 等效的批次改名搬移，`IvyContext` 併入 `IvyWalletCtx`。
- [ ] **Step 2:** 執行 `./gradlew testDebugUnitTest verifyPaparazziDebug`。Expected: BUILD SUCCESSFUL，golden 無變化。
- [ ] **Step 3:** Commit。

### Task 2.3: WalletRoot、WalletPreview 和間距檢查

**Files:**
- Create: `temp/legacy-code/src/main/java/com/ivy/legacy/ui/WalletRoot.kt`、`WalletPreview.kt`、`scripts/m3-check.sh`、`docs/m3-migration/dp-allowlist.tsv`
- Modify: `app/src/main/java/com/ivy/wallet/RootActivity.kt`、`shared/ui/testing/.../PaparazziScreenshotTest.kt`

**Interfaces:**
- Produces: `@Composable fun WalletRoot(theme: Theme, timeConverter: TimeConverter, timeProvider: TimeProvider, timeFormatter: TimeFormatter, content: @Composable () -> Unit)`
- Produces: `@Composable fun WalletPreview(theme: Theme = Theme.LIGHT, content: @Composable () -> Unit)`
- Produces: `scripts/m3-check.sh <base-ref>`：掃描 base 之後新增的非測試行，違規時 exit 1，規則見 spec「無硬編間距規則」。

- [ ] **Step 1:** 實作 `WalletRoot`，`RootActivity` 兩個 `IvyUI` 合併為一個外層呼叫，並移除第二個 `IvyMaterial3Theme`。舊畫面仍需 `BoxWithConstraints` 時，由 `IvyUI` 暫時保留給舊畫面，S6 刪除。
- [ ] **Step 2:** 以含違規和不違規範例的暫時 commit 測 `m3-check.sh`：`.offset(3.dp)`、`fontSize = 12.sp` 要失敗，`spacedBy(8.dp)` 要通過。
- [ ] **Step 3:** 執行 `./gradlew assembleDebug testDebugUnitTest verifyPaparazziDebug`。Expected: BUILD SUCCESSFUL。Commit。

### Task 2.4: ActionSheet

**Files:**
- Create: `temp/legacy-code/src/main/java/com/ivy/legacy/ui/sheet/ActionSheet.kt`
- Test: `temp/legacy-code/src/test/java/com/ivy/legacy/ui/sheet/ActionSheetPaparazziTest.kt`

**Interfaces:**
```kotlin
@Composable fun ActionSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetGesturesEnabled: Boolean = true,
    actions: @Composable RowScope.(dismiss: () -> Unit) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
)
```
`dismiss` 先呼叫 `sheetState.hide()` 再 `onDismiss()`，保留退場動畫。

- [ ] **Step 1:** 寫 `snapshotShortContent`、`snapshotLongContentKeepsActions`（50 列內容，動作列必須可見）、`snapshotLargeFontScale`（fontScale 1.5）。
- [ ] **Step 2:** 實作：`ModalBottomSheet(skipPartiallyExpanded = true)`，內容 `Column(Modifier.weight(1f, false).verticalScroll(...))`，固定 `FlexibleBottomAppBar`，並加 `imePadding`。
- [ ] **Step 3:** 錄製並檢視三張圖，動作列都要可見。Commit。

## S3 Sheet

### Task 3.1: IvyModal 改建於 ActionSheet，刪除 UUID 返回堆疊

**Files:** `temp/legacy-code/.../legacy/ui/theme/modal/IvyModal.kt`、`shared/ui/navigation/.../Navigation.kt`、`app/.../RootActivity.kt`

- [ ] **Step 1:** `IvyModal` 內部改為 `if (visible) key(id) { ActionSheet(...) }`，參數暫時不變；刪除 `modalBackHandling` 和鍵盤 listener。
- [ ] **Step 2:** 新增 `scripts/m3-smoke.sh`：建立 AVD、`installDebug`，依 spec 的流程用 `adb`、`uiautomator dump`、`screencap` 操作，logcat 出現 `FATAL EXCEPTION` 即 exit 1。先涵蓋 Account 到 Amount 到 Calculator 的逐層返回。
- [ ] **Step 3:** 重錄 modal golden，跑 `m3-smoke.sh`。Expected: 每次返回只關最上層。在 ledger 記錄 Modal 關閉、巢狀、鍵盤三項。Commit。

### Task 3.2: 改寫交易和金額相關 Modal

**Files:** `legacy/ui/theme/modal/edit/**`（`AmountModal`、`CalculatorModal`、`AccountModal`、`CategoryModal` 等）移到 `com/ivy/legacy/ui/sheet/`

**Interfaces:**
- Consumes: `ActionSheet`
- Produces: `AmountKeypad` 為 `FilledTonalButton` 格狀鍵盤；移除 `BoxScope` receiver。呼叫處在 `if (modal != null) key(modal.id) { ... }` 內直接用 `ActionSheet`。

- [ ] **Step 1:** 改寫，直接用 M3 元件，對照 spec 元件表。
- [ ] **Step 2:** 執行 `scripts/m3-check.sh` 和受影響模組的 `recordPaparazziDebug`，檢視 golden。Commit。

### Task 3.3: 改寫選擇器 Modal

**Files:** `ChooseIconModal`、顏色選擇、`CurrencyPicker` 相關 modal、`ReorderModal`。

- [ ] **Step 1:** 顏色和圖示用 `LazyVerticalGrid` + `FilledIconToggleButton`；`ReorderModal` 保留 RecyclerView，列改 `ListItem`，`sheetGesturesEnabled = false`。
- [ ] **Step 2:** `m3-check.sh`、重錄並檢視 golden。Commit。

### Task 3.4: 改寫期間、週期、日期 Modal 和其餘呼叫

**Files:** `ChoosePeriodModal`、`RecurringRuleModal`、`PeriodSelector`、`DeleteModal` 等其餘 `IvyModal` 呼叫；`shared/ui/core/.../AndroidDateTimePicker.kt`；`app/.../RootActivity.kt`。

**Interfaces:** 日期和時間改用既有 `DateTimePicker` 的 `DatePickerDialog`、`TimePickerDialog`；刪 `MaterialDatePicker`、`setupDatePicker`、`setupTimePicker`。不加可選範圍限制；取消時不呼叫 `onDatePicked`。

- [ ] **Step 1:** 改寫。`DeleteModal` 這類確認視窗用 `AlertDialog`，記入 ledger。
- [ ] **Step 2:** 刪除 `IvyModal.kt`。確認 `git grep -n 'IvyModal' -- '*.kt'` 為空。
- [ ] **Step 3:** 全量測試，`m3-smoke.sh` 加入日期選擇和 reorder 流程。Commit。

## S4 外殼

### Task 4.1: 共用 domain 元件

**Files:** Create `temp/legacy-code/src/main/java/com/ivy/legacy/ui/component/{TransactionRow,ItemBadge,AmountText,TransactionColors}.kt`；Test 同路徑 `*PaparazziTest.kt`。Delete `TransactionCard.kt` 中被取代的部分。

**Interfaces:**
- Produces: `@Composable fun TransactionType.color(): Color`（income `tertiary`、expense `error`、transfer `primary`）
- Produces: `@Composable fun ItemBadge(icon: String?, color: Color, modifier: Modifier = Modifier)`：解析 `ic_custom_*`，底色為自選色，圖示色用 `findContrastTextColor`。
- Produces: `@Composable fun AmountText(amount: Double, currency: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current)`
- Produces: `@Composable fun TransactionRow(...)`，以 `SegmentedListItem` 組成，參數沿用 `TransactionCard`。

- [ ] **Step 1:** 寫測試：`snapshotBadgeLightUserColor`（白色）、`snapshotBadgeDarkUserColor`（黑色）、`snapshotRowHugeAmount`（`9999999999.99`，名稱要仍可見）、`snapshotRowTypes`（三種類型都有正負號和圖示）。
- [ ] **Step 2:** 實作後錄製並檢視。Commit。

### Task 4.2: Main 外殼

**Files:** `feature/main/**`。

- [ ] **Step 1:** `MainBottomBar` 改成 `Scaffold` + `ShortNavigationBar` + `FloatingActionButtonMenu`，移除拖曳。返回鍵收合選單。
- [ ] **Step 2:** `m3-check.sh`、重錄 golden、`m3-smoke.sh` 跑 FAB 流程。更新 ledger。Commit。

### Task 4.3: Home 和 More menu

**Files:** `feature/home/**`。

- [ ] **Step 1:** 加 `snapshotHomeEmpty`（無交易、無帳戶的狀態）。`HomeMoreMenu` 改 `ActionSheet` + `SegmentedListItem`，項目不變。頂部用 `LargeFlexibleTopAppBar`。圖示換 `Icons.*`。
- [ ] **Step 2:** `m3-check.sh`、重錄 golden，確認 `git grep -n 'UI\.' -- 'feature/home/*.kt' 'feature/main/*.kt'` 無 `UI.colors|typo|shapes`。Commit。

### Task 4.4: Accounts

**Files:** `feature/accounts/**`。

- [ ] **Step 1:** 加 `snapshotAccountsEmpty`。彩色卡片改 `Card` + `ListItem` + `ItemBadge`。`RoundedCornerShape` 用元件預設。
- [ ] **Step 2:** `m3-check.sh`、重錄 golden。階段全量測試，`m3-smoke.sh`。Commit。

## S5 畫面

三波各在自己的 worktree 平行進行，波內依序。規則相同：對照 spec 元件表和 token 表、圖示換 `Icons.*`、自選色只留徽章、`BackBottomBar` 換 `TopAppBar`、移除 `BoxScope` receiver、完成後模組內無 `UI.colors|typo|shapes`。每個 task 的結束動作相同：`m3-check.sh`、重錄並檢視該模組 golden、更新 ledger、commit。

### Wave A

- [ ] **Task 5.A1 edit-transaction：** `EditBottomSheet` 換 `Scaffold` bottom bar + 金額 `ActionSheet` + `FilterChip` 帳戶列。
- [ ] **Task 5.A2 transactions、search：** 列表用 `TransactionRow`，搜尋用 `SearchBar`。
- [ ] **Task 5.A3 planned-payments。**
- [ ] **Task 5.A4 reports：** `FilterOverlay` 換 `ActionSheet` + `FilterChip`。
- [ ] **Task 5.A5 piechart：** 保留 `PieChartView`，扇區維持分類色，周邊改 M3。

### Wave B

- [ ] **Task 5.B1 categories。**
- [ ] **Task 5.B2 budgets：** Battery 換 `LinearWavyProgressIndicator`，超支用 `error`。
- [ ] **Task 5.B3 loans：** `LoanBottomBar` 換 `TopAppBar` + `ExtendedFloatingActionButton`。
- [ ] **Task 5.B4 exchange-rates、balance。**

### Wave C

- [ ] **Task 5.C1 settings。**
- [ ] **Task 5.C2 onboarding：** splash 位置動畫改一般版面。
- [ ] **Task 5.C3 import-data。**
- [ ] **Task 5.C4 其餘 M3 畫面：** attributions、contributors、disclaimer、features、poll、releases 換 `WalletPreview` 和圖示，移除 `com.ivy.design.system` import。

### Task 5.Z: 合併三波 [haiku]

- [ ] **Step 1:** 合併三個 worktree 到分支，解決 `LEDGER.md` 衝突。
- [ ] **Step 2:** 全量測試和 `m3-smoke.sh`。Expected: 全部通過。Commit。

## S6 刪除

### Task 6.1: 刪除舊 design system

**Files:** Delete `temp/old-design/`、`temp/legacy-code/.../legacy/ui/theme/**`、舊的 `IvyTheme`、`IvyUI`、`IvyColors`；Modify `settings.gradle.kts`、所有 `oldDesign` 相依；`shared/ui/core` 移除 `com.ivy.design` package。

- [ ] **Step 1:** 刪除並修到 `./gradlew assembleDebug` 成功。
- [ ] **Step 2:** 執行 spec「已刪除的定義」的 4 條 grep。Expected: 前 3 條為空；第 4 條只剩非介面用途，寫入 `GAPS.md`。
- [ ] **Step 3:** 全量測試。Commit。

### Task 6.2: 資源和靜態檢查

**Files:** `shared/ui/core/src/main/res/**`、`app/src/main/res/values/styles.xml`、`config/detekt/*`、compose stability baseline。

- [ ] **Step 1:** 刪除未使用的 drawable（保留 `ic_custom_*`）和字型（只留 widget XML 用到的）；`styles.xml` parent 換 Material3 theme。
- [ ] **Step 2:** detekt 開 `ForbiddenImport`，禁止 `com.ivy.design` 和 `com.ivy.wallet.ui.theme`。Baseline 只能縮小；重產 stability baseline。
- [ ] **Step 3:** 全量測試、`assembleDebug`、`m3-smoke.sh`。Commit。

## S7 收尾

### Task 7.1: 完整驗證

- [ ] **Step 1:** 執行 `./gradlew testDebugUnitTest verifyPaparazziDebug detekt lintRelease assembleDemo`、兩個 `connectedDebugAndroidTest`。Expected: 全部成功。
- [ ] **Step 2:** 在 demo build 上跑 `m3-smoke.sh`；嘗試 android-31 image，裝不到就記入 `GAPS.md`。

### Task 7.2: 報告

**Files:** Create `scripts/m3-report.py`；輸出到 scratchpad。

**Interfaces:** `m3-report.py --base m3-baseline --out <dir> --max-bytes <n>`：配對 key 為 `filename.replace(' ', '_')`，輸出 270px 寬的 before、after、heatmap JPEG 和 `report.json`。

- [ ] **Step 1:** 產生圖片，未配對項目明確列出。
- [ ] **Step 2:** 主 session 載入 `artifact-design`，用 LEDGER 和 GAPS 組頁面，依模組分部分；S1 渲染差異另列；「待 review 項目」第一項是 `tertiary` 作為收入色。
- [ ] **Step 3:** 用 Artifact 工具發布。
