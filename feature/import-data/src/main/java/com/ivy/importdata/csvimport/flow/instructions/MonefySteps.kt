package com.ivy.importdata.csvimport.flow.instructions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

@Composable
fun MonefySteps() {
    InstructionStep(
        number = 1,
        title = stringResource(R.string.export_to_file),
        description = stringResource(R.string.export_to_file_description),
    )
    UploadFileStep(stepNumber = 2)
}
