package com.ivy.legacy.ui.component

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val UnicodeMinus = '\u2212'

class AmountLabelTest {
    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Before
    fun useUs() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun zeroIsNeverSignedInTheUsLocale() {
        assertEquals("0.00 USD", amountLabel(-0.0, "USD"))
        assertEquals("0.00 USD", amountLabel(-0.001, "USD"))
        assertEquals("0.00 USD", amountLabel(0.001, "USD", showPlusSign = true))
        assertEquals("0.00 USD", amountLabel(-0.0, "USD", showPlusSign = true))
    }

    @Test
    fun nonZeroKeepsItsSignInTheUsLocale() {
        assertEquals("-12.50 USD", amountLabel(-12.5, "USD"))
        assertEquals("12.50 USD", amountLabel(12.5, "USD"))
        assertEquals("+12.50 USD", amountLabel(12.5, "USD", showPlusSign = true))
    }

    @Test
    fun zeroIsNeverSignedWithTheUnicodeMinus() {
        Locale.setDefault(Locale.forLanguageTag("sv-SE"))
        assertEquals(UnicodeMinus, DecimalFormatSymbols.getInstance(Locale.getDefault()).minusSign)
        assertEquals("0,00 SEK", amountLabel(-0.0, "SEK"))
        assertEquals("0,00 SEK", amountLabel(-0.001, "SEK"))
        assertEquals("0,00 SEK", amountLabel(0.001, "SEK", showPlusSign = true))
    }

    @Test
    fun nonZeroKeepsItsSignWithTheUnicodeMinus() {
        Locale.setDefault(Locale.forLanguageTag("nb-NO"))
        assertEquals(UnicodeMinus, DecimalFormatSymbols.getInstance(Locale.getDefault()).minusSign)
        assertEquals("${UnicodeMinus}12,50 NOK", amountLabel(-12.5, "NOK"))
        assertEquals("+12,50 NOK", amountLabel(12.5, "NOK", showPlusSign = true))
    }
}
