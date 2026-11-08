package com.ivy.planned.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.base.legacy.Theme
import com.ivy.data.model.IntervalType
import com.ivy.legacy.forDisplay
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.TimeFormatter
import java.time.LocalDateTime

@Composable
fun RecurringRule(
    startDate: LocalDateTime?,
    intervalN: Int?,
    intervalType: IntervalType?,
    oneTime: Boolean,
    onShowRecurringRuleModal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasRule = hasRecurringRule(
        startDate = startDate,
        intervalN = intervalN,
        intervalType = intervalType,
        oneTime = oneTime
    )
    val repeats = hasRule && !oneTime
    Card(onClick = onShowRecurringRuleModal, modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
            overlineContent = if (hasRule) {
                {
                    Text(
                        stringResource(if (oneTime) R.string.planned_for else R.string.planned_start_at)
                    )
                }
            } else {
                null
            },
            supportingContent = if (repeats && intervalType != null && intervalN != null) {
                {
                    Text(
                        stringResource(
                            R.string.planned_repeats_every,
                            intervalN,
                            intervalType.forDisplay(intervalN)
                        )
                    )
                }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            if (hasRule && startDate != null) {
                Text(with(LocalTimeFormatter.current) { startDate.format(TimeFormatter.Style.DateOnly(false)) })
            } else {
                Text(stringResource(R.string.add_planned_date_payment))
            }
        }
    }
}

fun hasRecurringRule(
    startDate: LocalDateTime?,
    intervalN: Int?,
    intervalType: IntervalType?,
    oneTime: Boolean,
): Boolean {
    return startDate != null &&
        ((intervalN != null && intervalType != null) || oneTime)
}

@Composable
private fun PreviewRule(
    startDate: LocalDateTime?,
    intervalN: Int?,
    intervalType: IntervalType?,
    oneTime: Boolean,
) {
    WalletPreview(Theme.LIGHT) {
        RecurringRule(
            startDate = startDate,
            intervalN = intervalN,
            intervalType = intervalType,
            oneTime = oneTime,
            onShowRecurringRuleModal = {}
        )
    }
}

@Preview
@Composable
private fun Preview_Empty() {
    PreviewRule(startDate = null, intervalN = null, intervalType = null, oneTime = true)
}

@Preview
@Composable
private fun Preview_Repeat() {
    PreviewRule(startDate = timeNowUTC(), intervalN = 1, intervalType = IntervalType.MONTH, oneTime = false)
}

@Preview
@Composable
private fun Preview_OneTime() {
    PreviewRule(startDate = timeNowUTC().plusDays(5), intervalN = null, intervalType = null, oneTime = true)
}
