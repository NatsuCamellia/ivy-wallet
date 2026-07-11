package com.ivy.design.system

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ivy.ui.R

val OpenSans: FontFamily = FontFamily(
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_light, FontWeight.Light),
    Font(R.font.opensans_semibold, FontWeight.SemiBold),
    Font(R.font.opensans_bold, FontWeight.Bold),
    Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ivyExpressiveTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = OpenSans),
        displayMedium = base.displayMedium.copy(fontFamily = OpenSans),
        displaySmall = base.displaySmall.copy(fontFamily = OpenSans),
        displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = OpenSans),
        displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = OpenSans),
        displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = OpenSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = OpenSans),
        headlineMedium = base.headlineMedium.copy(fontFamily = OpenSans),
        headlineSmall = base.headlineSmall.copy(fontFamily = OpenSans),
        headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = OpenSans),
        headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = OpenSans),
        headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = OpenSans),
        titleLarge = base.titleLarge.copy(fontFamily = OpenSans),
        titleMedium = base.titleMedium.copy(fontFamily = OpenSans),
        titleSmall = base.titleSmall.copy(fontFamily = OpenSans),
        titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = OpenSans),
        titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = OpenSans),
        titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = OpenSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = OpenSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = OpenSans),
        bodySmall = base.bodySmall.copy(fontFamily = OpenSans),
        bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = OpenSans),
        bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = OpenSans),
        bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = OpenSans),
        labelLarge = base.labelLarge.copy(fontFamily = OpenSans),
        labelMedium = base.labelMedium.copy(fontFamily = OpenSans),
        labelSmall = base.labelSmall.copy(fontFamily = OpenSans),
        labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = OpenSans),
        labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = OpenSans),
        labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = OpenSans),
    )
}
