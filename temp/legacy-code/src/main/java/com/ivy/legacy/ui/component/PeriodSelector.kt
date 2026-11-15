package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider

@Composable
fun PeriodSelector(
    period: TimePeriod,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowChoosePeriodModal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = period.toDisplayShort(
        startDateOfMonth = ivyWalletCtx().startDayOfMonth,
        timeConverter = LocalTimeConverter.current,
        timeProvider = LocalTimeProvider.current,
        timeFormatter = LocalTimeFormatter.current,
    )

    ButtonGroup(
        overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (period.month != null) {
            customItem(
                buttonGroupContent = {
                    FilledTonalIconButton(onClick = onPreviousMonth) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.previous),
                        )
                    }
                },
                menuContent = {},
            )
        }

        customItem(
            buttonGroupContent = {
                FilledTonalButton(onClick = onShowChoosePeriodModal, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(label)
                }
            },
            menuContent = {},
        )

        if (period.month != null) {
            customItem(
                buttonGroupContent = {
                    FilledTonalIconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next),
                        )
                    }
                },
                menuContent = {},
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        PeriodSelector(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1
            ), // preview
            onPreviousMonth = { },
            onNextMonth = { },
            onShowChoosePeriodModal = {}
        )
    }
}
