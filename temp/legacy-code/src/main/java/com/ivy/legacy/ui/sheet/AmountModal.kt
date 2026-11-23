package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.domain.di.FeaturesEntryPoint
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.amountToDouble
import com.ivy.legacy.utils.amountToDoubleOrNull
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.formatInputAmount
import com.ivy.legacy.utils.formatInt
import com.ivy.legacy.utils.localDecimalSeparator
import com.ivy.ui.R
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID
import kotlin.math.truncate

@SuppressLint("ComposeModifierMissing")
@Suppress("ParameterNaming")
@Composable
fun AmountModal(
    id: UUID,
    visible: Boolean,
    currency: String,
    initialAmount: Double?,
    dismiss: () -> Unit,
    showPlusMinus: Boolean = false,
    decimalCountMax: Int = 2,
    Header: (@Composable () -> Unit)? = null,
    onAmountChanged: (Double) -> Unit,
) {
    // An id that changes while the sheet is open (callers key it on the amount) must not recreate the sheet
    val openedId = remember(visible) { id }

    var amount by remember(openedId) {
        mutableStateOf(
            if (currency.isNotEmpty()) {
                initialAmount?.takeIf { it != 0.0 }?.format(currency)
                    ?: ""
            } else {
                initialAmount?.takeIf { it != 0.0 }?.format(decimalCountMax)
                    ?: ""
            }
        )
    }

    var calculatorModalVisible by remember(openedId) {
        mutableStateOf(false)
    }

    if (visible) {
        key(openedId) {
            ActionSheet(
                onDismiss = dismiss,
                actions = { dismissSheet ->
                    if (showPlusMinus) {
                        OutlinedButton(
                            onClick = {
                                when {
                                    amount.firstOrNull() == '-' -> amount = amount.drop(1)
                                    amount.isNotEmpty() -> amount = "-$amount"
                                }
                            },
                            modifier = Modifier.testTag("plus_minus"),
                        ) {
                            Text("+/-")
                        }
                    }

                    IconButton(
                        onClick = { calculatorModalVisible = true },
                        modifier = Modifier.testTag("btn_calculator"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = stringResource(R.string.calculator),
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            try {
                                onAmountChanged(amount.amountToDouble())
                                dismissSheet()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.enter))
                    }
                }
            ) {
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(Unit) { keyboard?.hide() }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Header?.invoke()

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(amount.ifBlank { "0" })
                                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                    append(" $currency")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.displaySmallEmphasized,
                            textAlign = TextAlign.Center,
                        )

                        AmountInput(
                            currency = currency,
                            decimalCountMax = decimalCountMax,
                            amount = amount,
                        ) {
                            amount = it
                        }
                    }
                }
            }
        }
    }

    CalculatorModal(
        visible = calculatorModalVisible,
        initialAmount = amount.amountToDoubleOrNull(),
        currency = currency,
        dismiss = {
            calculatorModalVisible = false
        },
        onCalculation = {
            amount = if (currency.isNotEmpty()) it.format(currency) else it.format(decimalCountMax)
        }
    )
}

@Composable
private fun AmountInput(
    currency: String,
    amount: String,
    decimalCountMax: Int = 2,
    setAmount: (String) -> Unit,
) {
    var firstInput by remember { mutableStateOf(true) }

    AmountKeypad(
        forCalculator = false,
        onNumberPress = {
            if (firstInput) {
                setAmount(it)
                firstInput = false
            } else {
                val formattedAmount = formatInputAmount(
                    currency = currency,
                    amount = amount,
                    newSymbol = it,
                    decimalCountMax = decimalCountMax
                )
                if (formattedAmount != null) {
                    setAmount(formattedAmount)
                }
            }
        },
        onDecimalPoint = {
            if (firstInput) {
                setAmount("0${localDecimalSeparator()}")
                firstInput = false
            } else {
                val newlyEnteredString = if (amount.isEmpty()) {
                    "0${localDecimalSeparator()}"
                } else {
                    "$amount${localDecimalSeparator()}"
                }
                if (newlyEnteredString.amountToDoubleOrNull() != null) {
                    setAmount(newlyEnteredString)
                }
            }
        },
        onBackspace = {
            if (firstInput) {
                setAmount("")
                firstInput = false
            } else {
                if (amount.isNotEmpty()) {
                    val formattedNumber = formatNumber(amount.dropLast(1))
                    setAmount(formattedNumber ?: "")
                }
            }
        }
    )
}

private fun formatNumber(number: String): String? {
    val decimalPartString = number
        .split(localDecimalSeparator())
        .getOrNull(1)
    val newDecimalCount = decimalPartString?.length ?: 0

    val amountDouble = number.amountToDoubleOrNull()

    if (newDecimalCount <= 2 && amountDouble != null) {
        val intPart = truncate(amountDouble).toInt()
        val decimalFormatted = if (decimalPartString != null) {
            "${localDecimalSeparator()}$decimalPartString"
        } else {
            ""
        }

        return formatInt(intPart) + decimalFormatted
    }

    return null
}

enum class KeypadRole { Digit, Operator, Clear }

class KeypadKey(
    val tag: String,
    val onClick: () -> Unit,
    val label: String = "",
    val icon: ImageVector? = null,
    val description: String? = null,
    val role: KeypadRole = KeypadRole.Digit,
)

/**
 * Digit grid shared by the amount and calculator sheets. [topRow] is an optional extra first row;
 * [sideKeys] adds one key at the end of each of the three digit rows and the last row.
 */
@Suppress("UnstableCollections")
@Composable
fun AmountKeypad(
    forCalculator: Boolean,
    onNumberPress: (String) -> Unit,
    onDecimalPoint: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    topRow: List<KeypadKey> = emptyList(),
    sideKeys: List<KeypadKey> = emptyList(),
) {
    /**
     * Retrieve `features` via EntryPointAccessors. `isStandardLayout` is stable and
     * only changes via settings, so there isn't unnecessary recompositions.
     * `isStandardLayout` doesn't change while the keyboard is shown.
     * `AmountKeypad` is self-contained, making it easy to reuse without passing parameters.
     * Layout changes are rare, so recomposition has minimal impact on performance.
     **/
    val isStandardLayout = if (LocalInspectionMode.current) {
        false
    } else {
        val context = LocalContext.current
        val features = remember {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                FeaturesEntryPoint::class.java
            ).getFeatures()
        }
        features.standardKeypadLayout.asEnabledState()
    }

    // Decide the order of the numbers based on the keypad layout
    val countButtonValues = if (isStandardLayout) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )
    } else { // Calculator like numeric keypad layout
        listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
        )
    }

    val tagPrefix = if (forCalculator) "calc_key_" else "key_"
    val digit = { value: String ->
        KeypadKey(tag = tagPrefix + value, label = value, onClick = { onNumberPress(value) })
    }
    val rows = buildList {
        if (topRow.isNotEmpty()) add(topRow)
        countButtonValues.forEachIndexed { index, numbers ->
            add(numbers.map(digit) + listOfNotNull(sideKeys.getOrNull(index)))
        }
        add(
            listOf(
                KeypadKey(
                    tag = if (forCalculator) "calc_key_decimal_separator" else "key_decimal_separator",
                    label = localDecimalSeparator(),
                    onClick = onDecimalPoint,
                ),
                digit("0"),
                KeypadKey(
                    tag = "key_del",
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    description = stringResource(R.string.backspace),
                    role = KeypadRole.Clear,
                    onClick = onBackspace,
                ),
            ) + listOfNotNull(sideKeys.getOrNull(countButtonValues.size))
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    FilledTonalButton(
                        onClick = key.onClick,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ButtonDefaults.MediumContainerHeight)
                            .testTag(key.tag),
                        shapes = ButtonDefaults.shapes(),
                        colors = key.role.colors(),
                        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                    ) {
                        if (key.icon != null) {
                            Icon(imageVector = key.icon, contentDescription = key.description)
                        } else {
                            Text(text = key.label, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadRole.colors(): ButtonColors = when (this) {
    KeypadRole.Digit -> ButtonDefaults.filledTonalButtonColors()
    KeypadRole.Operator -> ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    KeypadRole.Clear -> ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        AmountModal(
            id = UUID.randomUUID(),
            visible = true,
            currency = "BGN",
            initialAmount = null,
            dismiss = { }
        ) {
        }
    }
}

/** For screenshot testing */
@Composable
fun AmountModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
