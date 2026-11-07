package com.ivy.ui.theme

import androidx.compose.ui.graphics.Color
import com.ivy.base.legacy.Theme
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class WalletThemeTest {

    @Test
    fun `explicit themes ignore the system`() {
        for (systemDark in listOf(true, false)) {
            Theme.LIGHT.isDark(systemDark) shouldBe false
            Theme.DARK.isDark(systemDark) shouldBe true
            Theme.AMOLED_DARK.isDark(systemDark) shouldBe true
        }
    }

    @Test
    fun `auto follows the system`() {
        Theme.AUTO.isDark(systemDark = true) shouldBe true
        Theme.AUTO.isDark(systemDark = false) shouldBe false
    }

    @Test
    fun `trueBlack blacks out the base surfaces only`() {
        val scheme = SeedDarkColorScheme.trueBlack()

        scheme.background shouldBe Color.Black
        scheme.surface shouldBe Color.Black
        scheme.surfaceDim shouldBe Color.Black
        scheme.surfaceContainerLowest shouldBe Color.Black
        scheme.surfaceContainerHigh shouldNotBe Color.Black
        scheme.primary shouldBe SeedDarkColorScheme.primary
    }
}
