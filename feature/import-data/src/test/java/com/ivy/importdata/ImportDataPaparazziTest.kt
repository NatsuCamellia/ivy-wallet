package com.ivy.importdata

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.importdata.csv.CSVScreenUiTest
import com.ivy.importdata.csvimport.ImportScreenUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class ImportDataPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun snapshotImportScreen() {
        snapshot(theme) {
            ImportScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotCsvScreen() {
        snapshot(theme) {
            CSVScreenUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
