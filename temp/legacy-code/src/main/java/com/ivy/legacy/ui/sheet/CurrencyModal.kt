package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
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
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.CurrencyPicker
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Composable
fun CurrencyModal(
    title: String,
    initialCurrency: IvyCurrency?,
    visible: Boolean,
    dismiss: () -> Unit,
    id: UUID = remember { UUID.randomUUID() },

    onSetCurrency: (String) -> Unit
) {
    if (visible) {
        key(id) {
            var currency by remember { mutableStateOf(initialCurrency ?: IvyCurrency.getDefault()) }

            ActionSheet(
                onDismiss = dismiss,
                title = title,
                contentScrollable = false,
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            onSetCurrency(currency.code)
                            dismissSheet()
                        },
                        modifier = Modifier.testTag("set_currency_save"),
                    ) {
                        Text(stringResource(R.string.set))
                    }
                }
            ) {
                CurrencyPicker(
                    modifier = Modifier.weight(1f),
                    initialSelectedCurrency = currency,

                    includeKeyboardShownInsetSpacer = false,
                ) {
                    currency = it
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        CurrencyModal(
            title = "Set currency",
            initialCurrency = null,
            visible = true,
            dismiss = {}
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun CurrencyModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
