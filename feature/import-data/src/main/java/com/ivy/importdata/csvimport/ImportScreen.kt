package com.ivy.importdata.csvimport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.importdata.csvimport.flow.ImportFrom
import com.ivy.importdata.csvimport.flow.ImportProcessing
import com.ivy.importdata.csvimport.flow.ImportResultUI
import com.ivy.importdata.csvimport.flow.instructions.ImportInstructions
import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import com.ivy.legacy.ui.WalletPreview
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import com.ivy.data.backup.ImportResult
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun ImportCSVScreen(screen: ImportScreen) {
    val viewModel: ImportViewModel = viewModel()

    val importStep by viewModel.importStep.observeAsState(ImportStep.IMPORT_FROM)
    val importType by viewModel.importType.observeAsState()
    val importProgressPercent by viewModel.importProgressPercent.observeAsState(0)
    val importResult by viewModel.importResult.observeAsState()

    val onboardingViewModel: OnboardingViewModel = viewModel()

    com.ivy.legacy.utils.onScreenStart {
        viewModel.start(screen)
    }
    val context = LocalContext.current

    UI(
        screen = screen,
        importStep = importStep,
        importType = importType,
        importProgressPercent = importProgressPercent,
        importResult = importResult,

        onChooseImportType = viewModel::setImportType,
        onUploadCSVFile = { viewModel.uploadFile(context) },
        onSkip = {
            viewModel.skip(
                screen = screen,
                onboardingViewModel = onboardingViewModel
            )
        },
        onFinish = {
            viewModel.finish(
                screen = screen,
                onboardingViewModel = onboardingViewModel
            )
        }
    )
}

@Composable
private fun UI(
    screen: ImportScreen,

    importStep: ImportStep,
    importType: ImportType?,
    importProgressPercent: Int,
    importResult: ImportResult?,

    onChooseImportType: (ImportType) -> Unit = {},
    onUploadCSVFile: () -> Unit = {},
    onSkip: () -> Unit = {},
    onFinish: () -> Unit = {},
) {
    when (importStep) {
        ImportStep.IMPORT_FROM -> {
            ImportFrom(
                hasSkip = screen.launchedFromOnboarding,
                launchedFromOnboarding = screen.launchedFromOnboarding,
                onSkip = onSkip,
                onImportFrom = onChooseImportType
            )
        }

        ImportStep.INSTRUCTIONS -> {
            ImportInstructions(
                hasSkip = screen.launchedFromOnboarding,
                importType = importType!!,
                onSkip = onSkip,
                onUploadClick = onUploadCSVFile
            )
        }

        ImportStep.LOADING -> {
            ImportProcessing(
                progressPercent = importProgressPercent
            )
        }

        ImportStep.RESULT -> {
            ImportResultUI(
                result = importResult!!,
                launchedFromOnboarding = screen.launchedFromOnboarding,
            ) {
                onFinish()
            }
        }
    }
}

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    importStep: ImportStep = ImportStep.IMPORT_FROM,
    importResult: ImportResult? = null,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            UI(
                screen = ImportScreen(launchedFromOnboarding = true),
                importStep = importStep,
                importType = ImportType.MONEY_MANAGER,
                importProgressPercent = 49,
                importResult = importResult
            )
        }
    }
}

private val previewSuccess = ImportResult(
    rowsFound = 356,
    transactionsImported = 320,
    accountsImported = 4,
    categoriesImported = 13,
    failedRows = persistentListOf()
)

private val previewFailure = ImportResult(
    rowsFound = 100,
    transactionsImported = 12,
    accountsImported = 1,
    categoriesImported = 2,
    failedRows = persistentListOf()
)

private fun Boolean.theme(): Theme = if (this) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun ImportScreenUiTest(isDark: Boolean) {
    Preview(isDark.theme())
}

/** For screenshot testing */
@Composable
fun ImportInstructionsUiTest(isDark: Boolean) {
    Preview(isDark.theme(), ImportStep.INSTRUCTIONS)
}

/** For screenshot testing */
@Composable
fun ImportProcessingUiTest(isDark: Boolean) {
    Preview(isDark.theme(), ImportStep.LOADING)
}

/** For screenshot testing */
@Composable
fun ImportResultUiTest(isDark: Boolean) {
    Preview(isDark.theme(), ImportStep.RESULT, previewSuccess)
}

/** For screenshot testing */
@Composable
fun ImportResultFailureUiTest(isDark: Boolean) {
    Preview(isDark.theme(), ImportStep.RESULT, previewFailure)
}
