package com.ivy.design.system

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.Test

class TrueBlackTest {

    private val scheme = darkColorScheme(
        background = Color(0xFF1C1C1F),
        surface = Color(0xFF1C1C1F),
    )

    @Test
    fun `leaves background and surface untouched when isTrueBlack is false`() {
        val result = scheme.applyTrueBlack(isTrueBlack = false)

        result.background shouldBe scheme.background
        result.surface shouldBe scheme.surface
    }

    @Test
    fun `forces background and surface to pure black when isTrueBlack is true`() {
        val result = scheme.applyTrueBlack(isTrueBlack = true)

        result.background shouldBe Color(0xFF000000)
        result.surface shouldBe Color(0xFF000000)
    }
}
