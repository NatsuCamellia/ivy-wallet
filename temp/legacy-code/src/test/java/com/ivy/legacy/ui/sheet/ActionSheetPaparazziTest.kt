package com.ivy.legacy.ui.sheet

import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.cash.paparazzi.DeviceConfig
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test

class ActionSheetPaparazziTest : PaparazziScreenshotTest() {

    @Test
    fun snapshotShortContent() {
        snapshot(PaparazziTheme.Light) {
            TestSheet(title = "Short sheet") {
                Text("A few words of content")
            }
        }
    }

    @Test
    fun snapshotLongContentKeepsActions() {
        snapshot(PaparazziTheme.Light) {
            TestSheet(title = "Long sheet") {
                repeat(50) { ListItem(headlineContent = { Text("Row $it") }) }
            }
        }
    }

    @Test
    fun snapshotLargeFontScale() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_6_PRO.copy(fontScale = 1.5f))
        snapshot(PaparazziTheme.Light) {
            TestSheet(title = "Large font sheet") {
                repeat(5) { ListItem(headlineContent = { Text("Row $it") }) }
            }
        }
    }
}

@Composable
private fun TestSheet(title: String, content: @Composable () -> Unit) {
    ActionSheet(
        onDismiss = {},
        title = title,
        actions = { Button(onClick = it) { Text("Save") } },
    ) {
        NavigationBarInset()
        content()
    }
}

// Paparazzi does not deliver system insets to dialog windows, so give the sheet window the nav bar of PIXEL_6_PRO.
@Composable
private fun NavigationBarInset() {
    val view = LocalView.current
    SideEffect {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, NavBarHeightPx))
            .setVisible(WindowInsetsCompat.Type.navigationBars(), true)
            .build()
        ViewCompat.dispatchApplyWindowInsets(view, insets)
    }
}

private const val NavBarHeightPx = 168
