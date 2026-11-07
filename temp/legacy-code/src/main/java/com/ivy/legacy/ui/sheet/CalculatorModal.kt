package com.ivy.legacy.ui.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.utils.amountToDoubleOrNull
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.formatInputAmount
import com.ivy.legacy.utils.localDecimalSeparator
import com.ivy.legacy.utils.normalizeExpression
import com.ivy.ui.R
import com.notkamui.keval.Keval
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Composable
fun CalculatorModal(
    initialAmount: Double?,
    visible: Boolean,
    currency: String,
    dismiss: () -> Unit,
    id: UUID = remember { UUID.randomUUID() },
    onCalculation: (Double) -> Unit
) {
    var expression by remember(id, initialAmount) {
        mutableStateOf(initialAmount?.format(currency) ?: "")
    }

    if (visible) {
        key(id) {
            ActionSheet(
                onDismiss = dismiss,
                title = stringResource(R.string.calculator),
                actions = { dismissSheet ->
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            val result = calculate(expression)
                            if (result != null) {
                                onCalculation(result)
                                dismissSheet()
                            }
                        },
                        modifier = Modifier.testTag("calc_set"),
                    ) {
                        Text(stringResource(R.string.set))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val isEmpty = expression.isBlank()
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        text = if (isEmpty) stringResource(R.string.calculator_empty_expression) else expression,
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        textAlign = TextAlign.Center,
                        color = if (isEmpty) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )

                    val operator = { tag: String, symbol: String ->
                        KeypadKey(
                            tag = tag,
                            onClick = { expression = handleOperator(expression, symbol) },
                            label = symbol,
                            role = KeypadRole.Operator,
                        )
                    }

                    AmountKeypad(
                        forCalculator = true,
                        topRow = listOf(
                            KeypadKey("key_C", { expression = "" }, "C", role = KeypadRole.Clear),
                            KeypadKey("key_(", { expression += "(" }, "(", role = KeypadRole.Operator),
                            KeypadKey("key_)", { expression += ")" }, ")", role = KeypadRole.Operator),
                            operator("key_/", "÷"),
                        ),
                        sideKeys = listOf(
                            operator("key_*", "×"),
                            operator("key_-", "−"),
                            operator("key_+", "+"),
                            KeypadKey("key_=", {
                                val result = calculate(expression)
                                if (result != null) {
                                    expression = result.format(currency)
                                }
                            }, "=", role = KeypadRole.Operator),
                        ),
                        onNumberPress = {
                            expression = formatExpression(
                                expression = expression + it,
                                currency = currency
                            )
                        },
                        onDecimalPoint = {
                            expression = formatExpression(
                                expression = expression + localDecimalSeparator(),
                                currency = currency
                            )
                        },
                        onBackspace = {
                            if (expression.isNotEmpty()) {
                                expression = expression.dropLast(1)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun handleOperator(expression: String, operator: String): String {
    return if (expression.isNotEmpty() && expression.last().isOperator()) {
        expression.dropLast(1) + operator
    } else {
        expression + operator
    }
}

fun Char.isOperator(): Boolean = when (this) {
    '+', '−', '×', '÷' -> true
    else -> false
}

private fun formatExpression(expression: String, currency: String): String {
    var formattedExpression = expression

    expression
        .split("(", ")", "÷", "×", "−", "+")
        .ifEmpty {
            // handle only number expression formatting
            listOf(expression)
        }
        .forEach { part ->
            val numberPart = part.amountToDoubleOrNull()
            if (numberPart != null) {
                val formattedPart = formatInputAmount(
                    currency = currency,
                    amount = part,
                    newSymbol = ""
                )

                if (formattedPart != null) {
                    formattedExpression = formattedExpression.replace(part, formattedPart)
                }
            }
        }

    return formattedExpression
}

private fun calculate(expression: String): Double? {
    return try {
        // Keval doesn't support negative numbers, so we add a zero in front of the expression
        val expression = buildString {
            for (char in expression) {
                when (char) {
                    '÷' -> this.append('/')
                    '×' -> this.append('*')
                    '−' -> this.append('-')
                    else -> this.append(char)
                }
            }
        }
        val modifiedExpression = if (expression.startsWith("-")) "0$expression" else expression
        Keval.eval(modifiedExpression.normalizeExpression())
    } catch (e: Exception) {
        null
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    WalletPreview(theme) {
        CalculatorModal(
            visible = true,
            initialAmount = 50.23,
            currency = "BGN",
            dismiss = { },
            onCalculation = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun CalculatorModalUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
