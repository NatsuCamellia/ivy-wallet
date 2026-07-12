package com.ivy.design.system

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.Test

class TrueBlackTest {

    private val scheme = darkColorScheme(
        background = Color(0xFF1C1C1F),
        surface = Color(0xFF1C1C1F),
        surfaceContainer = Color(0xFF2A2A2E),
    )

    @Test
    fun `leaves background, surface and surfaceContainer untouched when isTrueBlack is false`() {
        val result = scheme.applyTrueBlack(isTrueBlack = false)

        result.background shouldBe scheme.background
        result.surface shouldBe scheme.surface
        result.surfaceContainer shouldBe scheme.surfaceContainer
    }

    @Test
    fun `forces background, surface and surfaceContainer to pure black when isTrueBlack is true`() {
        val result = scheme.applyTrueBlack(isTrueBlack = true)

        result.background shouldBe Color(0xFF000000)
        result.surface shouldBe Color(0xFF000000)
        result.surfaceContainer shouldBe Color(0xFF000000)
    }
}
