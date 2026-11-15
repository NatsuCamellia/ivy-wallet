package com.ivy.importdata.csvimport.flow.instructions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.domain.RootScreen
import com.ivy.importdata.csvimport.flow.ImportSteps
import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import com.ivy.legacy.ui.WalletPreview
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.ui.R

@Composable
fun ImportInstructions(
    hasSkip: Boolean,
    importType: ImportType,
    onSkip: () -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingToolbar(
                hasSkip = hasSkip,
                onBack = { nav.onBackPressed() },
                onSkip = onSkip,
                title = stringResource(R.string.how_to_import),
            )
        },
        bottomBar = {
            Surface {
                Button(
                    onClick = onUploadClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = importType.uploadButtonTitle())
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            App(importType = importType)

            Text(
                text = stringResource(R.string.steps),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )

            importType.ImportSteps()
        }
    }
}

@Composable
private fun App(
    importType: ImportType,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        onClick = { (context as RootScreen).openGooglePlayAppPage(appId = importType.appId()) },
        modifier = modifier.fillMaxWidth(),
    ) {
        ListItem(
            overlineContent = { Text(text = stringResource(R.string.open)) },
            leadingContent = {
                Image(
                    painter = painterResource(importType.logo()),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(text = importType.appName())
        }
    }
}

@Composable
private fun VideoArticleRow(
    videoUrl: String?,
    articleUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (videoUrl != null) {
            InstructionButton(
                icon = Icons.Filled.PlayCircle,
                caption = stringResource(R.string.how_to),
                text = stringResource(R.string.video),
                onClick = { (context as RootScreen).openUrlInBrowser(videoUrl) },
                modifier = Modifier.weight(1f),
            )
        }

        if (articleUrl != null) {
            InstructionButton(
                icon = Icons.Filled.Language,
                caption = stringResource(R.string.how_to),
                text = stringResource(R.string.article),
                onClick = { (context as RootScreen).openUrlInBrowser(articleUrl) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun InstructionButton(
    caption: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Column {
            Text(text = caption, style = MaterialTheme.typography.labelSmall)
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun UploadFileStep(
    stepNumber: Int,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.upload_csv_file),
) {
    InstructionStep(number = stepNumber, title = text, modifier = modifier)
}

@Composable
fun InstructionStep(
    number: Int,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    videoUrl: String? = null,
    articleUrl: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val hasLinks = videoUrl != null || articleUrl != null
    Card(modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { Text(text = number.toString(), style = MaterialTheme.typography.labelLarge) },
            supportingContent = if (description != null || hasLinks || content != null) {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (description != null) Text(text = description)
                        if (hasLinks) VideoArticleRow(videoUrl = videoUrl, articleUrl = articleUrl)
                        content?.invoke(this)
                    }
                }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ImportType.uploadButtonTitle(): String = when (this) {
    ImportType.IVY -> "Upload Backup file"
    else -> stringResource(R.string.upload_csv_file)
}

@Preview
@Composable
private fun Preview() {
    WalletPreview {
        NavigationRoot(navigation = Navigation()) {
            ImportInstructions(
                hasSkip = true,
                importType = ImportType.MONEY_MANAGER,
                onSkip = {},
                onUploadClick = {},
            )
        }
    }
}
