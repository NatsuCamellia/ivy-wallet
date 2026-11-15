package com.ivy.importdata.csvimport.flow.instructions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ivy.domain.RootScreen
import com.ivy.ui.R

private const val ConverterUrl = "https://www.zamzar.com/converters/document/xls-to-csv/"

@Composable
internal fun MoneyManagerPraseSteps() {
    val context = LocalContext.current

    InstructionStep(
        number = 1,
        title = stringResource(R.string.export_excel_file),
    )
    InstructionStep(
        number = 2,
        title = stringResource(R.string.convert_xls_to_csv),
        description = stringResource(R.string.convert_xls_to_csv_description),
    ) {
        InstructionButton(
            caption = stringResource(R.string.online_csv_converter_free),
            text = ConverterUrl,
            onClick = { (context as RootScreen).openUrlInBrowser(ConverterUrl) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    UploadFileStep(stepNumber = 3)
}
