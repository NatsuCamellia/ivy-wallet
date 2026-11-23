package com.ivy.importdata.csvimport.flow.instructions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

@Composable
internal fun FinancistoSteps() {
    InstructionStep(
        number = 1,
        title = stringResource(R.string.export_csv_file_standard),
        description = stringResource(R.string.export_csv_file_standard_description),
    )
    UploadFileStep(stepNumber = 2)
}
