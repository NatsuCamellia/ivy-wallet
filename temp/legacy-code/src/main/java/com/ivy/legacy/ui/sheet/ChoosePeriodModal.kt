package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.IntervalType
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.LastNTimeRange
import com.ivy.legacy.data.model.Month.Companion.fromMonthValue
import com.ivy.legacy.data.model.Month.Companion.monthsList
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.formatDateOnlyWithYear
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.wallet.ui.theme.components.IntervalPickerRow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChoosePeriodModalData(
    val id: UUID = UUID.randomUUID(),
    val period: TimePeriod
)

@Suppress("ParameterNaming")
@SuppressLint("ComposeModifierMissing")
@Composable
fun ChoosePeriodModal(
    modal: ChoosePeriodModalData?,

    dismiss: () -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    if (modal == null) return

    key(modal.id) {
        var period by remember { mutableStateOf(modal.period) }
        val ivyContext = ivyWalletCtx()

        ActionSheet(
            onDismiss = dismiss,
            actions = { dismissSheet ->
                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        ivyContext.updateSelectedPeriodInMemory(period)
                        onPeriodSelected(period)
                        dismissSheet()
                    },
                    enabled = period.isValid(),
                ) {
                    Text(stringResource(R.string.set))
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ChooseMonth(
                    selectedMonthYear = period.month?.let {
                        MonthYear(month = it, year = period.year ?: dateNowUTC().year)
                    }
                ) {
                    period = TimePeriod(
                        month = it.month,
                        year = it.year
                    )
                }

                FromToRange(
                    timeRange = period.fromToRange
                ) {
                    period = TimePeriod(
                        fromToRange = it
                    )
                }

                LastNPeriod(
                    lastNTimeRange = period.lastNRange,
                ) {
                    period = TimePeriod(
                        lastNRange = it
                    )
                }

                AllTime(
                    timeRange = period.fromToRange
                ) {
                    period = TimePeriod(
                        fromToRange = it
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSection(
    title: String,
    active: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        content()
    }
}

@Composable
@Suppress("MagicNumber")
private fun ChooseMonth(
    selectedMonthYear: MonthYear?,
    onSelect: (MonthYear) -> Unit,
) {
    val today = if (LocalInspectionMode.current) LocalDate.of(2026, 1, 15) else dateNowUTC()
    val currentYear = today.year
    val months = remember(currentYear) {
        monthsList()
            .map {
                MonthYear(month = it, year = currentYear - 1)
            }
            .plus(
                monthsList().map { MonthYear(month = it, year = currentYear) }
            )
            .plus(
                monthsList().map { MonthYear(month = it, year = currentYear + 1) }
            )
    }

    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = months.indexOf(
            selectedMonthYear ?: MonthYear(month = fromMonthValue(today.monthValue), year = currentYear)
        ).coerceAtLeast(0)
    )

    PeriodSection(
        title = stringResource(R.string.choose_month),
        active = selectedMonthYear != null,
    ) {
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = months) { monthYear ->
                FilterChip(
                    selected = monthYear == selectedMonthYear,
                    onClick = { onSelect(monthYear) },
                    label = { Text(monthYear.forDisplay(currentYear = currentYear)) },
                )
            }
        }
    }
}

data class MonthYear(
    val month: com.ivy.legacy.data.model.Month,
    val year: Int
) {
    fun forDisplay(
        currentYear: Int
    ): String {
        return if (year != currentYear) {
            // not current year
            "${month.name}, $year"
        } else {
            // current year
            month.name
        }
    }
}

@Composable
private fun FromToRange(
    timeRange: FromToTimeRange?,
    onSelect: (FromToTimeRange?) -> Unit,
) {
    val converter = LocalTimeConverter.current

    PeriodSection(
        title = stringResource(R.string.or_custom_range),
        active = timeRange != null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            IntervalFromToDate(
                border = IntervalBorder.FROM,
                dateTime = with(converter) { timeRange?.from?.toLocalDateTime() },
                otherEndDateTime = with(converter) { timeRange?.to?.toLocalDateTime() }
            ) { from ->
                onSelect(
                    if (from == null && timeRange?.to == null) {
                        null
                    } else {
                        timeRange?.copy(
                            from = with(converter) { from?.toUTC() }
                        ) ?: FromToTimeRange(
                            from = with(converter) { from?.toUTC() },
                            to = null
                        )
                    }
                )
            }

            IntervalFromToDate(
                border = IntervalBorder.TO,
                dateTime = with(converter) { timeRange?.to?.toLocalDateTime() },
                otherEndDateTime = with(converter) { timeRange?.from?.toLocalDateTime() },
            ) { to ->
                onSelect(
                    if (timeRange?.from == null && to == null) {
                        null
                    } else {
                        timeRange?.copy(
                            to = with(converter) { to?.plusDays(1)?.minusNanos(1)?.toUTC() }
                        ) ?: FromToTimeRange(
                            from = null,
                            to = with(converter) { to?.toUTC() }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun IntervalFromToDate(
    border: IntervalBorder,
    dateTime: LocalDateTime?,
    otherEndDateTime: LocalDateTime?,
    onSelect: (LocalDateTime?) -> Unit
) {
    val ivyContext = ivyWalletCtx()

    SegmentedListItem(
        onClick = {
            ivyContext.datePicker(
                minDate = if (border == IntervalBorder.TO) {
                    otherEndDateTime
                        ?.toLocalDate()
                        ?.plusDays(1)
                } else {
                    null
                },
                maxDate = if (border == IntervalBorder.FROM) {
                    otherEndDateTime
                        ?.toLocalDate()
                        ?.minusDays(1)
                } else {
                    null
                },
                initialDate = dateTime?.toLocalDate()
            ) {
                onSelect(it.atStartOfDay())
            }
        },
        shapes = ListItemDefaults.segmentedShapes(
            index = if (border == IntervalBorder.FROM) 0 else 1,
            count = 2,
        ),
        overlineContent = {
            Text(stringResource(if (border == IntervalBorder.FROM) R.string.from else R.string.to))
        },
        trailingContent = if (dateTime != null) {
            {
                IconButton(onClick = { onSelect(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.deselect))
                }
            }
        } else {
            null
        },
    ) {
        Text(dateTime?.toLocalDate()?.formatDateOnlyWithYear() ?: stringResource(R.string.add_date))
    }
}

private enum class IntervalBorder {
    FROM, TO
}

@Composable
private fun LastNPeriod(
    lastNTimeRange: LastNTimeRange?,

    onSelect: (LastNTimeRange) -> Unit
) {
    PeriodSection(
        title = stringResource(R.string.or_in_the_last),
        active = lastNTimeRange != null,
    ) {
        IntervalPickerRow(
            intervalN = lastNTimeRange?.periodN ?: 0,
            intervalType = lastNTimeRange?.periodType ?: IntervalType.WEEK,
            onSetIntervalN = {
                onSelect(
                    lastNTimeRange?.copy(
                        periodN = it
                    ) ?: LastNTimeRange(
                        periodN = it,
                        periodType = IntervalType.WEEK
                    )
                )
            },
            onSetIntervalType = {
                onSelect(
                    lastNTimeRange?.copy(
                        periodType = it
                    ) ?: LastNTimeRange(
                        periodN = 1,
                        periodType = it
                    )
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
@Suppress("MagicNumber")
private fun AllTime(
    timeRange: FromToTimeRange?,
    onSelect: (FromToTimeRange?) -> Unit,
) {
    val timeProvider = LocalTimeProvider.current
    val active = timeRange != null && timeRange.from == null &&
        timeRange.to != null && timeRange.to!!.isAfter(timeProvider.utcNow())

    PeriodSection(
        title = stringResource(R.string.or_all_time),
        active = active,
    ) {
        val onClick = {
            onSelect(
                if (active) {
                    null
                } else {
                    FromToTimeRange(
                        from = null,
                        to = timeProvider.utcNow().plusSeconds(TimeUnit.HOURS.toSeconds(24L))
                    )
                }
            )
        }
        val label = stringResource(if (active) R.string.unselect_all_time else R.string.select_all_time)
        val buttonModifier = Modifier.padding(horizontal = 16.dp)

        if (active) {
            FilledTonalButton(onClick = onClick, modifier = buttonModifier) { Text(label) }
        } else {
            OutlinedButton(onClick = onClick, modifier = buttonModifier) { Text(label) }
        }
    }
}

@Preview
@Composable
private fun Preview_MonthSelected(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        ChoosePeriodModal(
            modal = ChoosePeriodModalData(
                period = TimePeriod(
                    month = fromMonthValue(3),
                    year = 2026
                )
            ),
            dismiss = {}
        ) {
        }
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_FromTo(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        ChoosePeriodModal(
            modal = ChoosePeriodModalData(
                period = TimePeriod(
                    fromToRange = FromToTimeRange(
                        from = Instant.parse("2026-01-15T12:00:00Z"),
                        to = Instant.parse("2026-02-20T12:00:00Z")
                    )
                )
            ),
            dismiss = {}
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_LastN(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        ChoosePeriodModal(
            modal = ChoosePeriodModalData(
                period = TimePeriod(
                    lastNRange = LastNTimeRange(
                        periodN = 1,
                        periodType = IntervalType.WEEK
                    )
                )
            ),
            dismiss = {}
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun ChoosePeriodModalMonthUiTest(isDark: Boolean) {
    Preview_MonthSelected(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun ChoosePeriodModalFromToUiTest(isDark: Boolean) {
    Preview_FromTo(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun ChoosePeriodModalLastNUiTest(isDark: Boolean) {
    Preview_LastN(if (isDark) Theme.DARK else Theme.LIGHT)
}
