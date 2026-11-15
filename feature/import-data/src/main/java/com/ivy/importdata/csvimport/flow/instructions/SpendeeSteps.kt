package com.ivy.importdata.csvimport.flow.instructions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

@Composable
fun SpendeeSteps() {
    InstructionStep(
        number = 1,
        title = stringResource(R.string.export_csv_file),
        articleUrl = "https://help.spendee.com/article/137-export-transactions",
    )
    InstructionStep(
        number = 2,
        title = stringResource(R.string.check_email_spam),
    )
    InstructionStep(
        number = 3,
        title = stringResource(R.string.download_email_file),
        description = stringResource(R.string.download_email_file_description),
    )
    UploadFileStep(stepNumber = 4)
}
