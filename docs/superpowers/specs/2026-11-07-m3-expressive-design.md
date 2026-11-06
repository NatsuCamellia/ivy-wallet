# Ivy Wallet：Ivy design system 全面遷移至 Material 3 Expressive

## Context

Ivy Wallet 的 UI 建立在一套自製 design system 上，散落在 4 個模組，約 41k 行 Compose UI 受影響，24 個畫面有 18 個是舊版。使用者要刪除整套 design system，換成語意化、原生的 M3 Expressive，並保持程式碼簡潔，不為了模仿舊版面硬編間距。

完成後交付一份 artifact 報告，分部分列出截圖 diff 和 UI 導致的行為改變。使用者之後再 review，並要求細部修改。

## 已定案的決策

| 項目 | 決定 | 來源 |
|---|---|---|
| Material 版本 | material3 `1.5.0-rc01`（Stable 1.4.0 沒有 Expressive 元件） | 使用者 |
| 最低版本 | minSdk 28 改為 31（Android 12） | 使用者 |
| 配色 | App 一律動態色；Ivy 紫種子 ColorScheme 只給截圖測試和 Preview | 使用者 |
| 使用者自選顏色 | 帳戶、分類、借貸顏色縮成圖示徽章，卡片和頁首改用語意 role；圓餅圖扇區仍用分類色（那是資料） | 使用者 |
| 介面圖示 | 約 119 個介面 drawable 換成 `material-icons-extended`；使用者選的 `ic_custom_*` 保留 | 使用者 |
| 字體 | Compose 用 M3 預設 Typography；只保留 widget XML 需要的字型 | 我的決定 |
| 收支顏色 | income=`tertiary`、expense=`error`、transfer=`primary`，集中在一個 extension；正負號和圖示一定要有 | 我的決定，報告中列為第一個 review 項目 |
| 測試框架 | 續用 Paparazzi 2.0.0-alpha05.1，失敗時退回 Roborazzi | 我的決定 |

**考慮過的路線：** `1.5.0-alpha18` 可以留在 AGP 8，但會停在 2026 年 4 月的 alpha，41k 行之後可能要再改一輪，所以不採用。

## 目標與界線

**目標**
- 刪除 `temp/old-design`、`com.ivy.design`、`com.ivy.wallet.ui.theme` 全部內容。
- 所有畫面使用 M3 元件和語意 role（`ColorScheme`、`Typography`、`Shapes`）。
- 單元測試、截圖測試、detekt、lint、整合測試全部通過。

**非目標**
- 不改 domain、data 和 ViewModel 邏輯，UI 必要的調整除外。
- 不動 Glance 和 RemoteViews widget 的外觀（它們不使用 design package），只保留所需資源。
- 不做新功能，不重排資訊架構。

**反目標（技術上能動也算失敗）**
- 為了貼近舊版面加入硬編 margin、spacing。
- 另建一層自訂 design token 或 wrapper 元件。
- 為了過測試而錄下有裁切、對比或重疊問題的 golden。

**Wrapper 判準（我的決定）：** 共用 composable 必須帶有 domain 邏輯或 domain 資料，例如 `TransactionRow`、含輸入邏輯的金額欄位、顏色選擇器。只替 M3 元件設預設參數的函式一律不允許，所以 `IvyButton`、`IvyIcon` 改成在呼叫處直接用 `Button`、`Icon`。`ModalTitle` 變成 `ActionSheet` 的 `title` 參數，`ModalSave` 這類動作放進 `ActionSheet` 的 actions slot。`ActionSheet` 是唯一的版面 wrapper，因為 M3 沒有固定動作列的 slot。共用元件在 S3、S4 建好，S5 才平行展開，避免 worktree 互相衝突。

## 階段

實作和驗收一律由不同 agent 負責。全程用 strangler 方式：舊 design system 留到 S6 才刪，每個階段結束時 build 和測試都要是綠的。

| # | 內容 | 結束條件 | 平行 |
|---|---|---|---|
| S0 基準 | 在舊工具鏈補齊 Paparazzi 覆蓋，打 tag `m3-baseline` | 全部測試綠；每個畫面和 modal 都有 golden | 依模組 |
| S1 工具鏈 | 升版、minSdk 31，不改 UI | 全部測試綠；golden 重錄；渲染差異記入 ledger | 否 |
| S2 基礎 | `WalletTheme`、`WalletRoot`、`ActionSheet`、搬移非設計程式碼、`m3-check.sh`、ledger | App 可執行，舊畫面正常 | 否 |
| S3 Sheet | `IvyModal` 改建於 `ModalBottomSheet`，改寫 29 處呼叫，刪 `modalBackHandling` | 沒有 `IvyModal`；模擬器上巢狀返回正常 | 先做引擎，再分 3 個 agent |
| S4 外殼 | main、home、accounts、more menu、`TransactionCard`、共用列 | 這些模組沒有 `UI.*` | 否 |
| S5 畫面 | **A：** edit-transaction、transactions、search、planned-payments、reports、piechart。**B：** categories、budgets、loans、exchange-rates、balance。**C：** settings、onboarding、import-data、其餘 M3 畫面 | 各波模組沒有 `UI.*` | A、B、C 各自 worktree |
| S6 刪除 | 移除 `temp/old-design`，清字型和資源，換 `styles.xml` parent，開 detekt `ForbiddenImport` | 下方 grep 全空；全部測試綠 | 否 |
| S7 收尾 | 完整驗證、R8 demo build、報告 | artifact 發布 | 否 |

### S0 重點
- 先跑 `:feature:accounts:verifyPaparazziDebug` 確認 Paparazzi 1.3.3 能在 JDK 21 上執行；不行就裝 Temurin 17 給 Gradle 用。
- 沒有 `*UiTest` 的畫面沿用既有模式：`/** For screenshot testing */ fun XUiTest(isDark)`，呼叫現有的 private Preview。features 和 releases 需要補範例 state。
- Modal 用 `visible = true` 直接渲染。在 `temp/legacy-code` 加參數化的 `ModalsPaparazziTest`，feature 模組的 modal 在各自模組測。
- `AmountKeyboard` 的 Hilt `EntryPointAccessors` 加 `LocalInspectionMode` 分支，測試基底提供 `true`。
- 補上 balance 測試；刪掉沒有測試的 `accountTab composable` golden。
- **從 S0 起凍結測試類別和方法名稱**，新測試名稱不含空白。Paparazzi 2.0 會把空白換成 `_`，報告用 `filename.replace(' ', '_')` 配對 before/after。

### S1 目標版本（單一原子 commit）

| 工具 | 版本 |
|---|---|
| AGP / Gradle | 9.3.3 / 9.6.1 |
| Kotlin / KSP | 2.4.21 / 2.3.13 |
| Hilt / Room | 2.60.1 / 2.8.5 |
| Compose / material3 | 1.12.1（維持無 BOM 寫法）/ 1.5.0-rc01；material-icons-extended 1.7.8 |
| Paparazzi | 2.0.0-alpha05.1 |
| Kover / google-services / Crashlytics | 0.9.11 / 4.5.0 / 3.0.8 |
| module-graph / wrapper-upgrade | 2.9.1 / 0.12 |
| Slack compose lint | 1.6.1 |
| detekt / compose-rules | 1.23.8（只在 root）/ 維持 0.4.x |
| compileSdk / targetSdk / minSdk / JVM | 37 / 36 / 31 / 21 |

- 使用 `agp-9-upgrade` skill。已知要處理：移除 `kotlin-android` plugin、`kotlin.srcDir`、`resValues`、`ivy.room` 的 assets、Molecule plugin 改 `molecule-runtime`、ci-actions 的 `context(...)`、13 個 workflow 的 `java-version`、`org.gradle.jvmargs`、在 `ivy.compose` 統一加 Material3 opt-in。
- **Paparazzi 閘門先跑：** 設 `testOptions.targetSdk = 36`，證明 Compose 1.12 畫面和 `ModalBottomSheet` 都能錄製（必要時加 `snapshotSettled` helper）。
- **退路：** Paparazzi 不行就改 Roborazzi 1.77 + Robolectric 4.17，輸出到相同路徑。Sheet 錄不到就只拍 body。detekt 不相容就改 `dev.detekt` 2.0.0-alpha.6 + compose-rules 0.6.8。

### S2 主題基礎（`shared/ui/core`，`com.ivy.ui.theme`）
- `WalletTheme(theme, content)`：`LocalInspectionMode` 時用 Ivy 紫種子 scheme，否則用 `dynamicDark/LightColorScheme`。外層是 `MaterialExpressiveTheme`，搭配 `MotionScheme.expressive()`。
- `AMOLED_DARK` 用 `scheme.trueBlack()`，把 `background`、`surface`、`surfaceDim`、`surfaceContainerLowest` 設為黑色。`Theme` enum 和持久化值不變。
- 種子 scheme 是以 `0xFF6B4DFF` 用 material-color-utilities 一次產生的靜態 literal。
- `WalletRoot` 取代 `IvyUI`（locals、`WalletTheme`、全尺寸 `Surface`，無 `BoxWithConstraints`）；`WalletPreview` 取代兩個舊 Preview wrapper。
- 舊 `IvyTheme` 內部改呼叫 `WalletTheme`，讓 `UI.*` 和 `MaterialTheme` 暫時並存。
- 搬移：`findContrastTextColor` 和調色盤到 `com.ivy.ui.color`（值不變）、time locals 到 `com.ivy.ui.time`、`thenIf` 到 `com.ivy.ui.util`、`IvyContext` 併入 `IvyWalletCtx`。
- `Screen.isLegacy` 控制 ViewModel store 清理，不是設計程式碼，保留。
- 改寫後的共用 UI 留在 `temp/legacy-code`，改放 `com.ivy.legacy.ui.{sheet,component}`，不開新模組。
- Insets 只由最外層 `Scaffold` 處理。`BoxScope` receiver 逐函式移除。

## 元件對照

| 舊 | 新 |
|---|---|
| `IvyModal` | `ActionSheet`（唯一 wrapper）：`ModalBottomSheet` + 可捲動內容 + 固定 `FlexibleBottomAppBar` |
| `MainBottomBar` | `Scaffold` + `ShortNavigationBar` + `FloatingActionButtonMenu` |
| `LoanBottomBar`、`BackBottomBar` | `TopAppBar` 返回 + `ExtendedFloatingActionButton` |
| `HomeMoreMenu` | `ActionSheet` + `SegmentedListItem` |
| `EditBottomSheet` | `Scaffold` bottom bar + 金額 `ActionSheet` + `FilterChip` 帳戶列 |
| Amount、Calculator | `FilledTonalButton` 格狀鍵盤 |
| Budget、Buffer battery | `LinearWavyProgressIndicator` |
| `TransactionCard` | `SegmentedListItem`，移除漸層 |
| 漸層和 glow 按鈕 | `Button`、`FilledTonalButton`、`OutlinedButton`、`TextButton`、`ButtonGroup` |
| 顏色、圖示選擇器 | `LazyVerticalGrid` + `FilledIconToggleButton`，清單資料保留 |
| 期間、週期選擇 | `SingleChoiceSegmentedButtonRow`、`DatePickerDialog`、`ButtonGroup` |
| `FilterOverlay` | `ActionSheet` + `FilterChip` |
| `WrapContentRow` | `FlowRow` |
| 載入 | `LoadingIndicator`、`ContainedLoadingIndicator` |
| 搜尋 | `SearchBar` |
| 日期、時間 | `DatePickerDialog`、`TimePickerDialog`；刪 `MaterialDatePicker` |
| 頂部列 | `TopAppBar`；一級畫面用 `LargeFlexibleTopAppBar` |
| `PieChartView` | **保留**自訂 View，扇區維持分類色，周邊 UI 改 M3 |
| 帳戶、分類、借貸的彩色卡片和頁首 | `Card`、`ListItem` 用語意 role，自選色只在圖示徽章；`findContrastTextColor` 只用於徽章內的圖示 |
| 介面 `R.drawable.ic_*`、`IvyIcon` | `Icon(Icons.*)`；沒有對應圖示時用最接近的替代，並記入 ledger |
| `ReorderModal` | **保留** RecyclerView + ItemTouchHelper，列改 `ListItem`，關閉 sheet 手勢 |

**Token 對照**
- 顏色：`pure`/`pureInverse` 對應 `surface`/`onSurface`（通常直接刪，由元件提供）；`medium` 對應 `surfaceContainerHigh`；`gray` 文字用 `onSurfaceVariant`、邊框用 `outlineVariant`；`green` 對應 `tertiary`；`orange` 對應 `secondary`；`red` 對應 `error`；各 `*1` 對應 `*Container`。
- 字體：`h1` 對應 `displaySmall`，`h2` 對應 `headlineLarge`，`b1` 對應 `titleLarge`，`b2` 對應 `bodyLarge`，`c` 對應 `labelMedium`；金額主視覺用 `*Emphasized`。優先用元件 slot 的預設樣式。
- 形狀：優先用元件預設。

## 預期的行為改變

| 區域 | 新行為 |
|---|---|
| Modal 關閉 | Scrim、返回鍵、下滑都會取消；Close 鈕保留 |
| 巢狀 Modal | 視窗堆疊，返回只關最上層 |
| 鍵盤 | 由 sheet insets 和 `imePadding` 處理 |
| FAB | 拖曳手勢移除，點擊切換選單，返回鍵收合 |
| More menu | 圓形展開和滑動改為 sheet，項目不變 |
| `EditBottomSheet` | 滑動移除，點金額開 sheet |
| 返回按鈕 | 從底部移到頂部 |
| Reorder | Sheet 不能滑掉，用按鈕、返回鍵或 scrim 關閉 |
| 主題設定 | 仍是 4 值循環，強調色改隨桌布 |
| 收支顏色 | 隨桌布變化，移除漸層 |
| 字體 | App 內改為 Roboto |
| 日期和時間 | Compose dialog 取代 View dialog。今天的 `minDate`、`maxDate` 從未限制可選範圍，新版同樣不加限制。取消時不再寫回日期，所以舊版在 `ChoosePeriodModal` 取消時悄悄改動另一端 ±1 天的問題會消失，記入 ledger |
| 帳戶和分類外觀 | 彩色卡片改為徽章，顏色資料不變 |
| 介面圖示 | 圖樣全面改變，功能不變 |
| Onboarding splash | 位置動畫改為一般版面 |
| Android 9 到 11 | 不再支援 |

每個階段把實際發生的改變寫入 `docs/m3-migration/LEDGER.md`，遇到表外的改變由實作 agent 自行選最接近原生的做法並記錄。

## 無硬編間距規則

原則：優先讓 M3 元件自己提供間距。元件之間確實需要間隔時，優先寫在父層，偶爾用 `Spacer` 也可以。不建命名的間距常數。

`scripts/m3-check.sh` 掃描該階段新增的非測試程式碼：
- **一律失敗：** `.offset(` 裡的數字 `.dp`；`.sp` literal；`fontSize =`；`RoundedCornerShape(N.dp)`。
- **合法的間隔寫法：** `Arrangement.spacedBy(...)`、`contentPadding`、`padding(...)`、偶爾的 `Spacer(...)`，數值限 M3 格線 4、8、12、16、24，同樣計入下方預算。
- **需列入白名單：** 其他數字 `.dp`，以及 `.size(`、`.height(`、`.width(`。白名單是 `docs/m3-migration/dp-allowlist.tsv`，理由限圖表幾何和圖示尺寸。
- **預算：** 每模組每 100 行新增最多 3 個數字 `.dp`（現有 M3 範本約 2.2 到 3.0）；全 repo `.dp` 總數（目前 1,781）每階段都要下降。
- 驗收 agent 退回元件已提供間距卻又多加 padding 的寫法，也退回只為了還原舊版面而存在的數值。

## 「已刪除」的定義

S6 結束時以下全部為空：
```
git grep -nE 'com\.ivy\.design|com\.ivy\.wallet\.ui\.theme|\bUI\.(colors|typo|shapes)\b|LocalIvy|Ivy(Modal|UI|Theme|Material3Theme|WalletPreview|WalletComponentPreview|Typography|Shapes)\b|IvyPreview|modalBackHandling|screen(Width|Height)|Old design system|old-design|oldDesign' -- '*.kt' '*.kts'
git grep -nE 'fun Box(WithConstraints)?Scope\.' -- 'feature/*.kt' 'temp/*.kt'
git grep -nE 'R\.font\.|\.style\(' -- '*.kt'
git grep -nE 'R\.drawable\.ic_' -- 'feature/*.kt' 'temp/*.kt'
```
最後一條的剩餘結果必須是非介面用途（例如 widget、通知、onboarding 插圖），每筆都要列入報告。另外重產 compose stability baseline，detekt baseline 只能縮小，刪除未使用的 drawable（保留 `ic_custom_*`，它們透過 `getIdentifier` 載入），字型只留 widget XML 用到的。

## 驗收流程

每個工作單元由實作 agent 完成，再由全新的驗收 agent 只看階段規格和 diff，回報通過或失敗清單，不代為修改。

**模型分配（節省 token）：** 實作和驗收 agent 預設用 `sonnet`，因為驗收要判讀 golden 截圖。純機械工作用 `haiku`，例如 token 批次替換、重錄 golden、跑測試回報結果。主 session 只負責派工、合併和處理卡關，不親自做大量改寫。

**連續 3 輪未過時（我的決定）：** 由主 session 親自診斷，換一個實作 agent 重做。仍有爭議的非測試項目（例如間距或視覺判斷）記入報告的「已知缺口」並繼續。測試失敗不能當缺口，必須修到綠才進下一階段。

驗收 agent 執行：
1. `scripts/m3-check.sh` 和該範圍的 grep。
2. 模組測試和 `verifyPaparazziDebug`；階段合併時跑全量 `testDebugUnitTest`、`verifyPaparazziDebug`、`detekt`；階段結束加 `lintRelease`。
3. 逐張檢視重錄的 golden，檢查裁切、對比、重疊。
4. 確認凍結的測試名稱沒變。
5. S3 起跑模擬器冒煙測試；S1、S7 跑兩個 `connectedDebugAndroidTest`。

**模擬器冒煙測試**（`android-35` image，用 `avdmanager` 建 AVD，`adb` 和 `uiautomator` 操作，logcat 出現 `FATAL EXCEPTION` 即失敗）涵蓋：冷啟動與切 tab；FAB 新增支出、鍵盤、儲存；Account 到 Amount 到 Calculator 再逐層返回；sheet 上的鍵盤；日期選擇；reorder；主題循環。S7 在 `assembleDemo`（R8）上重跑，並嘗試 android-31 image；裝不到就記為缺口。

## 報告（S7）

`scripts/m3-report.py`（Pillow）用 `m3-baseline` tag 和 HEAD 的 golden 依模組分組，產出 before/after 並排、差異 heatmap 和像素變動比例，圖片縮小以符合 artifact 大小上限。未配對的項目明確列出。

行為改變來自 `LEDGER.md`，S1 的渲染差異另列。收尾放「待 review 項目」和「已知缺口」，第一項是 `tertiary` 作為收入色。最後用 Artifact 工具發布私人頁面。

## Git 與停止條件

- 依階段 commit 到目前的 worktree 分支，本機打 tag `m3-baseline`。不 push、不開 PR。
- `scripts/m3-*` 和 `docs/m3-migration/` 留在分支上，等 review 後再決定去留。
- 計畫核准後，把這份文件存到 `docs/superpowers/specs/2026-11-07-m3-expressive-design.md`，再用 writing-plans 展開成逐階段任務，交由 subagent 執行。
- **只有這些情況才停下來問使用者：** Paparazzi 和 Roborazzi 都無法錄製；工具鏈組合走不通，需要改動已定案的版本；有測試必須改動非截圖的斷言才能通過。

## 主要風險

1. Paparazzi alpha 尚未驗證 Compose 1.12 和 compileSdk 37，由 S1 閘門先測。
2. Hilt 2.60.1 搭 Kotlin 2.4.21 未驗證。
3. material3 是 RC，版本鎖定，sheet 邏輯集中在 `ActionSheet`。
4. 巢狀 sheet 的焦點和鍵盤無法靠截圖抓，靠模擬器流程。
5. `tertiary` 在部分桌布下看起來不像收入，改動集中在一個檔案。

## 關鍵檔案

- `gradle/libs.versions.toml`、`buildSrc/src/main/kotlin/ivy.kotlin-android.gradle.kts`、`ivy.compose.gradle.kts`、`ivy.paparazzi.gradle.kts`
- `shared/ui/testing/src/main/java/com/ivy/ui/testing/PaparazziScreenshotTest.kt`
- `shared/ui/core/src/main/java/com/ivy/design/system/IvyMaterial3Theme.kt`
- `temp/old-design/src/main/java/com/ivy/design/api/IvyUI.kt`
- `temp/legacy-code/src/main/java/com/ivy/legacy/legacy/ui/theme/modal/IvyModal.kt`
- `app/src/main/java/com/ivy/wallet/RootActivity.kt`
- 範本：`feature/attributions`、`feature/disclaimer`、`feature/poll/impl`
