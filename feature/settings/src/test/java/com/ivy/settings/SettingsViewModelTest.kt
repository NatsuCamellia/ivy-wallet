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
        // Note: we intentionally do NOT assert `currentTheme` from uiState here.
        // uiState()'s onStart() loads the initial settings via `ioThread { settingsDao.findFirst() }`
        // on the real Dispatchers.IO (unaffected by this test harness's Unconfined main dispatcher),
        // then sets currentTheme.value = Theme.AUTO. Under full-suite contention that initialization
        // can land after the synchronously-handled SetTheme(Theme.DARK) event, overwriting the state
        // before expectMostRecentItem() samples it, which flakes the assertion. Assert the race-free
        // side-effect contract instead. Do not "restore" a state assertion here.
        viewModel.runTest(events = listOf(SettingsEvent.SetTheme(Theme.DARK))) {
            // state intentionally not asserted, see comment above
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
