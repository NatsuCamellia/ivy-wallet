package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.ui.R
import java.util.UUID

private const val DaysPerRow = 7
private const val DaysInMonth = 31

@Suppress("ParameterNaming")
@SuppressLint("ComposeModifierMissing")
@Composable
fun ChooseStartDateOfMonthModal(
    visible: Boolean,
    selectedStartDateOfMonth: Int,
    dismiss: () -> Unit,
    id: UUID = remember { UUID.randomUUID() },
    onStartDateOfMonthSelected: (Int) -> Unit,
) {
    if (visible) {
        key(id) {
            ActionSheet(
                onDismiss = dismiss,
                title = stringResource(R.string.choose_start_date_of_month),
                contentScrollable = false,
            ) { dismissSheet ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(DaysPerRow),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items((1..DaysInMonth).toList()) { number ->
                        FilledIconToggleButton(
                            checked = number == selectedStartDateOfMonth,
                            onCheckedChange = {
                                dismissSheet()
                                onStartDateOfMonthSelected(number)
                            },
                            modifier = Modifier.wrapContentWidth(),
                            shapes = IconButtonDefaults.toggleableShapes(),
                        ) {
                            Text(number.toString())
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        ChooseStartDateOfMonthModal(
            visible = true,
            selectedStartDateOfMonth = 1,
            dismiss = {}
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun ChooseStartDateOfMonthModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
