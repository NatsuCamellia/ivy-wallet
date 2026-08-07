package com.ivy.ui.component.amount

import androidx.compose.runtime.Immutable
import com.notkamui.keval.Keval

private const val Operators = "+-*/"

enum class AmountKeypadKey {
    Digit0, Digit1, Digit2, Digit3, Digit4,
    Digit5, Digit6, Digit7, Digit8, Digit9,
    Decimal, Backspace, Plus, Minus, Times, Divide,
}

private val AmountKeypadKey.digit: Char?
    get() = when (this) {
        AmountKeypadKey.Digit0 -> '0'
        AmountKeypadKey.Digit1 -> '1'
        AmountKeypadKey.Digit2 -> '2'
        AmountKeypadKey.Digit3 -> '3'
        AmountKeypadKey.Digit4 -> '4'
        AmountKeypadKey.Digit5 -> '5'
        AmountKeypadKey.Digit6 -> '6'
        AmountKeypadKey.Digit7 -> '7'
        AmountKeypadKey.Digit8 -> '8'
        AmountKeypadKey.Digit9 -> '9'
        else -> null
    }

private val AmountKeypadKey.operator: Char?
    get() = when (this) {
        AmountKeypadKey.Plus -> '+'
        AmountKeypadKey.Minus -> '-'
        AmountKeypadKey.Times -> '*'
        AmountKeypadKey.Divide -> '/'
        else -> null
    }

/**
 * The raw text on the keypad's display. May be a plain amount ("42.50") or an
 * arithmetic expression ("12+3*2"); [evaluate] resolves both.
 */
@Immutable
data class AmountKeypadInput(val text: String) {
    val isExpression: Boolean
        get() = text.drop(1).any { it in Operators }
}

fun amountKeypadInputOf(amount: Double?, decimalSeparator: Char): AmountKeypadInput {
    if (amount == null || amount == 0.0) return AmountKeypadInput("")
    val plain = if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        amount.toString()
    }
    return AmountKeypadInput(plain.replace('.', decimalSeparator))
}

@Suppress("ReturnCount")
fun AmountKeypadInput.press(
    key: AmountKeypadKey,
    decimalCountMax: Int,
    decimalSeparator: Char,
): AmountKeypadInput {
    key.digit?.let { digit ->
        if (decimalsExceeded(decimalCountMax, decimalSeparator)) return this
        return AmountKeypadInput(text + digit)
    }
    key.operator?.let { operator ->
        if (text.isEmpty()) return this
        val withoutTrailingOperator = text.trimEnd { it in Operators }
        if (withoutTrailingOperator.isEmpty()) return this
        return AmountKeypadInput(withoutTrailingOperator + operator)
    }
    return when (key) {
        AmountKeypadKey.Backspace -> AmountKeypadInput(text.dropLast(1))
        AmountKeypadKey.Decimal -> appendDecimal(decimalSeparator)
        else -> this
    }
}

private fun AmountKeypadInput.appendDecimal(decimalSeparator: Char): AmountKeypadInput {
    val segment = currentSegment()
    return when {
        segment.contains(decimalSeparator) -> this
        segment.isEmpty() -> AmountKeypadInput("${text}0$decimalSeparator")
        else -> AmountKeypadInput(text + decimalSeparator)
    }
}

private fun AmountKeypadInput.decimalsExceeded(
    decimalCountMax: Int,
    decimalSeparator: Char,
): Boolean {
    val segment = currentSegment()
    val separatorIndex = segment.indexOf(decimalSeparator)
    return separatorIndex >= 0 && segment.length - separatorIndex - 1 >= decimalCountMax
}

/** The number currently being typed — everything after the last operator. */
private fun AmountKeypadInput.currentSegment(): String =
    text.takeLastWhile { it !in Operators }

fun AmountKeypadInput.evaluate(decimalSeparator: Char): Double? {
    val normalized = text.replace(decimalSeparator, '.')
    if (normalized.isBlank()) return null
    return runCatching { Keval.eval(normalized) }
        .getOrNull()
        ?.takeIf { it.isFinite() }
}
