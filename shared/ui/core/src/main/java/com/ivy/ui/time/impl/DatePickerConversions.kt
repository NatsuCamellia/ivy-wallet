package com.ivy.ui.time.impl

import com.ivy.base.time.TimeConverter
import java.time.Instant
import java.time.LocalDate

private const val MillisecondsInDay = 24 * 60 * 60 * 1000L

internal fun LocalDate.toDatePickerMillis(): Long = toEpochDay() * MillisecondsInDay

internal fun Long.toDatePickerDate(): LocalDate = LocalDate.ofEpochDay(this / MillisecondsInDay)

internal fun TimeConverter.toPickerDate(instant: Instant): LocalDate = instant.toLocalDate()
