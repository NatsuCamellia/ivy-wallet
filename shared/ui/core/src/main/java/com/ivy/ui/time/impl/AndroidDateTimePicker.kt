package com.ivy.ui.time.impl

import android.text.format.DateFormat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private const val MILLISECONDS_IN_DAY = 24 * 60 * 60 * 1000L

@Stable
@Singleton
class AndroidDateTimePicker @Inject constructor(
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : DateTimePicker {
    private var datePickerViewState by mutableStateOf<DatePickerViewState?>(null)
    private var timePickerViewState by mutableStateOf<TimePickerViewState?>(null)

    @Composable
    override fun Content() {
        datePickerViewState?.let { DatePicker(it) }
        timePickerViewState?.let { TimePicker(it) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DatePicker(viewState: DatePickerViewState) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewState.initialDate.toEpochDay() * MILLISECONDS_IN_DAY,
        )
        val dismiss = { datePickerViewState = null }
        DatePickerDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        dismiss()
                        pickerState.selectedDateMillis?.let {
                            viewState.onDatePicked(LocalDate.ofEpochDay(it / MILLISECONDS_IN_DAY))
                        }
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TimePicker(viewState: TimePickerViewState) {
        val time = viewState.initialTime ?: timeProvider.localNow().toLocalTime()
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
        val dismiss = { timePickerViewState = null }
        TimePickerDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        dismiss()
                        viewState.onTimePicked(LocalTime.of(pickerState.hour, pickerState.minute))
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                            TimePickerDisplayMode.Input
                        } else {
                            TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = displayMode,
                )
            },
            title = { TimePickerDialogDefaults.Title(displayMode = displayMode) },
        ) {
            if (displayMode == TimePickerDisplayMode.Picker) {
                TimePicker(state = pickerState)
            } else {
                TimeInput(state = pickerState)
            }
        }
    }

    override fun pickDate(initialDate: Instant?, onDatePick: (LocalDate) -> Unit) {
        pickCalendarDate(initialDate?.let { with(timeConverter) { it.toLocalDate() } }, onDatePick)
    }

    override fun pickCalendarDate(initialDate: LocalDate?, onDatePick: (LocalDate) -> Unit) {
        datePickerViewState = DatePickerViewState(
            initialDate = initialDate ?: timeProvider.localDateNow(),
            onDatePicked = onDatePick
        )
    }

    override fun pickTime(initialTime: LocalTime?, onTimePick: (LocalTime) -> Unit) {
        timePickerViewState = TimePickerViewState(
            initialTime = initialTime,
            onTimePicked = onTimePick
        )
    }

    @Immutable
    private data class DatePickerViewState(
        val initialDate: LocalDate,
        val onDatePicked: (LocalDate) -> Unit
    )

    @Immutable
    private data class TimePickerViewState(
        val initialTime: LocalTime?,
        val onTimePicked: (LocalTime) -> Unit
    )
}
