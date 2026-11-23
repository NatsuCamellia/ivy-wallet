package com.ivy.importdata

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.importdata.csv.CSVScreenInvalidMappingUiTest
import com.ivy.importdata.csv.CSVScreenManyColumnsUiTest
import com.ivy.importdata.csv.CSVScreenMappedUiTest
import com.ivy.importdata.csv.CSVScreenTransferFieldsUiTest
import com.ivy.importdata.csv.CSVScreenTypeFieldsUiTest
import com.ivy.importdata.csv.CSVScreenUiTest
import com.ivy.importdata.csvimport.ImportInstructionsUiTest
import com.ivy.importdata.csvimport.ImportProcessingUiTest
import com.ivy.importdata.csvimport.ImportResultFailureUiTest
import com.ivy.importdata.csvimport.ImportResultUiTest
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

    @Test
    fun snapshotImportInstructionsScreen() {
        snapshot(theme) {
            ImportInstructionsUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotImportProcessingScreen() {
        snapshot(theme) {
            ImportProcessingUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotImportResultScreen() {
        snapshot(theme) {
            ImportResultUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotImportResultFailureScreen() {
        snapshot(theme) {
            ImportResultFailureUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotCsvMappedScreen() {
        snapshot(theme) {
            CSVScreenMappedUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotCsvInvalidMappingScreen() {
        snapshot(theme) {
            CSVScreenInvalidMappingUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotCsvTransferFieldsScreen() {
        snapshot(theme) {
            CSVScreenTransferFieldsUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotCsvTypeFieldsScreen() {
        snapshot(theme) {
            CSVScreenTypeFieldsUiTest(theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun snapshotCsvManyColumnsScreen() {
        snapshot(theme) {
            CSVScreenManyColumnsUiTest(theme == PaparazziTheme.Dark)
        }
    }
}
