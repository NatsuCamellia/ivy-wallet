package com.ivy.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ivy.ui.R

private val FontWeights = listOf(
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
)

@OptIn(ExperimentalTextApi::class)
private fun googleSansFlex(rond: Float?): FontFamily = FontFamily(
    FontWeights.map { weight ->
        Font(
            resId = R.font.google_sans_flex,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                *listOfNotNull(rond?.let { FontVariation.Setting("ROND", it) }).toTypedArray(),
            ),
        )
    }
)

private val GoogleSansFlex = googleSansFlex(rond = null)

/** Google Sans Flex with the ROND (roundness) axis fully on. */
private val GoogleSansFlexRounded = googleSansFlex(rond = 100f)

/**
 * Same as this style but with the rounded (ROND = 100) variant of Google Sans Flex.
 * Meant for large display numbers such as the Balance banner.
 */
fun TextStyle.rounded(): TextStyle = copy(fontFamily = GoogleSansFlexRounded)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val WalletTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = GoogleSansFlex),
        displayMedium = base.displayMedium.copy(fontFamily = GoogleSansFlex),
        displaySmall = base.displaySmall.copy(fontFamily = GoogleSansFlex),
        displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = GoogleSansFlex),
        headlineLarge = base.headlineLarge.copy(fontFamily = GoogleSansFlex),
        headlineMedium = base.headlineMedium.copy(fontFamily = GoogleSansFlex),
        headlineSmall = base.headlineSmall.copy(fontFamily = GoogleSansFlex),
        headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = GoogleSansFlex),
        titleLarge = base.titleLarge.copy(fontFamily = GoogleSansFlex),
        titleMedium = base.titleMedium.copy(fontFamily = GoogleSansFlex),
        titleSmall = base.titleSmall.copy(fontFamily = GoogleSansFlex),
        titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = GoogleSansFlex),
        bodyLarge = base.bodyLarge.copy(fontFamily = GoogleSansFlex),
        bodyMedium = base.bodyMedium.copy(fontFamily = GoogleSansFlex),
        bodySmall = base.bodySmall.copy(fontFamily = GoogleSansFlex),
        bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = GoogleSansFlex),
        labelLarge = base.labelLarge.copy(fontFamily = GoogleSansFlex),
        labelMedium = base.labelMedium.copy(fontFamily = GoogleSansFlex),
        labelSmall = base.labelSmall.copy(fontFamily = GoogleSansFlex),
        labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = GoogleSansFlex),
    )
}
