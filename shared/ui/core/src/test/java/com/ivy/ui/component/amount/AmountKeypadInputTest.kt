package com.ivy.ui.component.amount

import io.kotest.matchers.shouldBe
import org.junit.Test

class AmountKeypadInputTest {

    private val sep = '.'

    private fun input(text: String) = AmountKeypadInput(text)

    private fun AmountKeypadInput.press(vararg keys: AmountKeypadKey): AmountKeypadInput =
        keys.fold(this) { acc, key -> acc.press(key, decimalCountMax = 2, decimalSeparator = sep) }

    @Test
    fun `appends digits`() {
        input("").press(AmountKeypadKey.Digit4, AmountKeypadKey.Digit2).text shouldBe "42"
    }

    @Test
    fun `appends the decimal separator once per number`() {
        input("42").press(AmountKeypadKey.Decimal, AmountKeypadKey.Decimal).text shouldBe "42."
    }

    @Test
    fun `starts a decimal with a leading zero when the input is empty`() {
        input("").press(AmountKeypadKey.Decimal).text shouldBe "0."
    }

    @Test
    fun `rejects digits beyond decimalCountMax`() {
        input("42.50").press(AmountKeypadKey.Digit9).text shouldBe "42.50"
    }

    @Test
    fun `allows a second decimal separator after an operator`() {
        input("1.5+2").press(AmountKeypadKey.Decimal, AmountKeypadKey.Digit5).text shouldBe "1.5+2.5"
    }

    @Test
    fun `backspace removes the last character`() {
        input("42").press(AmountKeypadKey.Backspace).text shouldBe "4"
    }

    @Test
    fun `backspace on empty input is a no-op`() {
        input("").press(AmountKeypadKey.Backspace).text shouldBe ""
    }

    @Test
    fun `ignores a leading operator`() {
        input("").press(AmountKeypadKey.Times).text shouldBe ""
    }

    @Test
    fun `replaces a trailing operator instead of stacking operators`() {
        input("42").press(AmountKeypadKey.Plus, AmountKeypadKey.Times).text shouldBe "42*"
    }

    @Test
    fun `isExpression is false for a plain number`() {
        input("42.50").isExpression shouldBe false
    }

    @Test
    fun `isExpression is true once an operator is present`() {
        input("42+8").isExpression shouldBe true
    }

    @Test
    fun `evaluates a plain decimal`() {
        input("42.50").evaluate(sep) shouldBe 42.5
    }

    @Test
    fun `evaluates a comma decimal separator`() {
        AmountKeypadInput("42,50").evaluate(',') shouldBe 42.5
    }

    @Test
    fun `evaluates an expression honouring precedence`() {
        input("12+3*2").evaluate(sep) shouldBe 18.0
    }

    @Test
    fun `returns null for an incomplete expression`() {
        input("12+").evaluate(sep) shouldBe null
    }

    @Test
    fun `returns null for empty input`() {
        input("").evaluate(sep) shouldBe null
    }

    @Test
    fun `returns null for division by zero`() {
        input("12/0").evaluate(sep) shouldBe null
    }

    @Test
    fun `amountKeypadInputOf renders an amount with the given separator`() {
        amountKeypadInputOf(42.5, ',').text shouldBe "42,5"
    }

    @Test
    fun `amountKeypadInputOf maps null and zero to empty input`() {
        amountKeypadInputOf(null, '.').text shouldBe ""
        amountKeypadInputOf(0.0, '.').text shouldBe ""
    }
}
