package com.ivy.importdata.csvimport.flow.instructions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

@Composable
fun DefaultImportSteps(
    videoUrl: String? = null,
    articleUrl: String? = null,
) {
    InstructionStep(
        number = 1,
        title = stringResource(R.string.export_csv_file),
        videoUrl = videoUrl,
        articleUrl = articleUrl,
    )
    UploadFileStep(stepNumber = 2)
}
