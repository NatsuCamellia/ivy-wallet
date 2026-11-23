package com.ivy.ui.theme

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
    fun forcesEverySurfaceRoleToPureBlack() {
        val result = scheme.trueBlack()

        result.background shouldBe Color.Black
        result.surface shouldBe Color.Black
        result.surfaceDim shouldBe Color.Black
        result.surfaceBright shouldBe Color.Black
        result.surfaceContainerLowest shouldBe Color.Black
        result.surfaceContainerLow shouldBe Color.Black
        result.surfaceContainer shouldBe Color.Black
        result.surfaceContainerHigh shouldBe Color.Black
        result.surfaceContainerHighest shouldBe Color.Black
    }

    @Test
    fun keepsTheAccentRolesUntouched() {
        val result = scheme.trueBlack()

        result.primary shouldBe scheme.primary
        result.onSurface shouldBe scheme.onSurface
    }
}
