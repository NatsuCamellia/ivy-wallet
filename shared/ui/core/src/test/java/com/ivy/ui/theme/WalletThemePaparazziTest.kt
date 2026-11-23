package com.ivy.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.ui.PaparazziScreenshotTest
import org.junit.Test

class WalletThemePaparazziTest : PaparazziScreenshotTest() {

    @Test
    fun snapshotAmoledSurfaces() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WalletTheme(Theme.AMOLED_DARK) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Card on true black",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = BottomSheetDefaults.ExpandedShape,
                                color = BottomSheetDefaults.ContainerColor,
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Sheet content",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Button(onClick = {}) { Text("Action") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
