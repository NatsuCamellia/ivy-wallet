package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.IntervalType
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.closeDay
import com.ivy.legacy.utils.formatDateWeekDayLong
import com.ivy.legacy.utils.formatNicely
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.wallet.ui.theme.components.IntervalPickerRow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RecurringRuleModalData(
    val initialStartDate: LocalDateTime?,
    val initialIntervalN: Int?,
    val initialIntervalType: IntervalType?,
    val initialOneTime: Boolean = false,
    val id: UUID = UUID.randomUUID()
)

@Suppress("ParameterNaming")
@SuppressLint("ComposeModifierMissing")
@Composable
fun RecurringRuleModal(
    modal: RecurringRuleModalData?,

    dismiss: () -> Unit,
    onRuleChanged: (LocalDateTime, oneTime: Boolean, Int?, IntervalType?) -> Unit,
) {
    if (modal == null) return

    key(modal.id) {
        val timeProvider = LocalTimeProvider.current
        var startDate by remember { mutableStateOf(modal.initialStartDate ?: timeProvider.localNow()) }
        var oneTime by remember { mutableStateOf(modal.initialOneTime) }
        var intervalN by remember { mutableStateOf(modal.initialIntervalN ?: 1) }
        var intervalType by remember { mutableStateOf(modal.initialIntervalType ?: IntervalType.MONTH) }

        ActionSheet(
            onDismiss = dismiss,
            title = stringResource(R.string.plan_for),
            actions = { dismissSheet ->
                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        onRuleChanged(
                            startDate,
                            oneTime,
                            intervalN,
                            intervalType
                        )
                        dismissSheet()
                    },
                    enabled = validate(oneTime, intervalN, intervalType),
                    modifier = Modifier.testTag("recurringModalSet"),
                ) {
                    Text(stringResource(R.string.set))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = oneTime,
                        onClick = { oneTime = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text(stringResource(R.string.one_time))
                    }

                    SegmentedButton(
                        selected = !oneTime,
                        onClick = { oneTime = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text(stringResource(R.string.multiple_times))
                    }
                }

                if (oneTime) {
                    DateRow(dateTime = startDate, onDatePick = { startDate = it })
                } else {
                    Text(
                        text = stringResource(R.string.starts_on),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    DateRow(dateTime = startDate, onDatePick = { startDate = it })

                    Text(
                        text = stringResource(R.string.repeats_every_text),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    IntervalPickerRow(
                        intervalN = intervalN,
                        intervalType = intervalType,
                        onSetIntervalN = { intervalN = it },
                        onSetIntervalType = { intervalType = it }
                    )
                }
            }
        }
    }
}

private fun validate(
    oneTime: Boolean,
    intervalN: Int?,
    intervalType: IntervalType?
): Boolean {
    return oneTime || intervalN != null && intervalN > 0 && intervalType != null
}

@Composable
private fun DateRow(
    dateTime: LocalDateTime,
    onDatePick: (LocalDateTime) -> Unit
) {
    val ivyContext = ivyWalletCtx()
    val date = dateTime.toLocalDate()
    val closeDay = date.closeDay()

    SegmentedListItem(
        onClick = { ivyContext.pickDate(date, onDatePick) },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        modifier = Modifier.testTag("recurring_modal_pick_date"),
        leadingContent = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
        supportingContent = if (closeDay != null) {
            { Text(date.formatDateWeekDayLong()) }
        } else {
            null
        },
    ) {
        Text(closeDay ?: date.formatNicely(pattern = "EEEE, dd MMM"))
    }
}

private fun IvyWalletCtx.pickDate(
    initialDate: LocalDate,
    onDatePick: (
        LocalDateTime
    ) -> Unit
) {
    datePicker(
        initialDate = initialDate
    ) {
        onDatePick(it.atTime(12, 0))
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_oneTime(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        RecurringRuleModal(
            modal = RecurringRuleModalData(
                initialStartDate = LocalDateTime.of(2026, 1, 15, 12, 0),
                initialIntervalN = null,
                initialIntervalType = null,
                initialOneTime = true
            ),
            dismiss = {},
            onRuleChanged = { _, _, _, _ -> }
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_multipleTimes(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        RecurringRuleModal(
            modal = RecurringRuleModalData(
                initialStartDate = LocalDateTime.of(2026, 1, 15, 12, 0),
                initialIntervalN = null,
                initialIntervalType = null,
                initialOneTime = false
            ),
            dismiss = {},
            onRuleChanged = { _, _, _, _ -> }
        )
    }
}

/** For screenshot testing */
@Composable
fun RecurringRuleModalOneTimeUiTest(isDark: Boolean) {
    Preview_oneTime(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun RecurringRuleModalMultipleTimesUiTest(isDark: Boolean) {
    Preview_multipleTimes(if (isDark) Theme.DARK else Theme.LIGHT)
}
