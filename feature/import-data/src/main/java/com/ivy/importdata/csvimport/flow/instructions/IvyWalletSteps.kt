package com.ivy.importdata.csvimport.flow.instructions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

@Composable
internal fun IvyWalletSteps() {
    InstructionStep(
        number = 1,
        title = stringResource(R.string.export_data),
        description = """
            Settings > "Backup Data" (do NOT use "Export to CSV"!)
        """.trimIndent(),
    )
    UploadFileStep(
        stepNumber = 2,
        text = "Upload Backup (ZIP/CSV/JSON) file",
    )
}
