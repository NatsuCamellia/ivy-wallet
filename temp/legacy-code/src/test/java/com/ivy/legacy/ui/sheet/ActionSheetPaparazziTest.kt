package com.ivy.legacy.ui.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test

class ActionSheetPaparazziTest : PaparazziScreenshotTest() {

    @Test
    fun snapshotShortContent() {
        snapshot(PaparazziTheme.Light) {
            TestSheet(title = "Short sheet") {
                Text("A few words of content", Modifier.padding(horizontal = 16.dp))
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
        content()
    }
}
