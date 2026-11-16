package com.ivy.importdata.csv

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.importdata.csvimport.flow.ImportProcessing
import com.ivy.importdata.csvimport.flow.ImportResultUI
import com.ivy.legacy.ui.WalletPreview
import com.ivy.navigation.CSVScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import com.ivy.ui.R
import com.ivy.ui.util.thenIf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.abs

@Composable
fun CSVScreen(
    screen: CSVScreen,
) {
    val viewModel: CSVViewModel = viewModel()
    val state = viewModel.uiState()
    val onboardingViewModel: OnboardingViewModel = viewModel()

    when (val ui = state.uiState) {
        UIState.Idle -> ImportUI(
            state = state,
            launchedFromOnboarding = screen.launchedFromOnboarding,
            onEvent = viewModel::onEvent
        )

        is UIState.Processing -> ImportProcessing(progressPercent = ui.percent)
        is UIState.Result -> ImportResultUI(
            result = ui.importResult,
            isManualCsvImport = true,
            launchedFromOnboarding = screen.launchedFromOnboarding,
            onTryAgain = null,
            onFinish = {
                viewModel.onEvent(
                    CSVEvent.FinishImport(
                        launchedFromOnboarding = screen.launchedFromOnboarding,
                        onboardingViewModel = onboardingViewModel,
                    )
                )
            }
        )
    }
}

@Composable
private fun ImportUI(
    state: CSVState,
    launchedFromOnboarding: Boolean,
    onEvent: (CSVEvent) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val nav = navigation()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            OnboardingToolbar(
                hasSkip = false,
                onBack = { nav.onBackPressed() },
                onSkip = {},
                title = stringResource(R.string.manual_csv_import),
            )
        },
        bottomBar = {
            Surface {
                Button(
                    onClick = { onEvent(CSVEvent.Continue) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    enabled = state.continueEnabled,
                ) {
                    Text(text = stringResource(R.string.import_csv_continue))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            state = listState,
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "import_btn") {
                ImportButton(
                    onFilePick = {
                        onEvent(CSVEvent.FilePicked(it))
                    }
                )
            }
            item(key = "import_hint") {
                if (!launchedFromOnboarding) {
                    Text(
                        text = stringResource(R.string.warning_import_csv_file).trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.import_a_csv_file_to_continue),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (state.csv != null) {
                csvTable(state.csv)
            }
            if (state.columns != null && state.important != null) {
                importantFields(state.columns, importantFields = state.important, onEvent = onEvent)
            }
            if (state.columns != null && state.transfer != null) {
                transferFields(state.columns, state.transfer, onEvent = onEvent)
            }
            if (state.columns != null && state.optional != null) {
                optionalFields(state.columns, state.optional, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ImportButton(
    onFilePick: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ivyContext = com.ivy.legacy.ivyWalletCtx()
    FilledTonalButton(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            ivyContext.openFile {
                onFilePick(it)
            }
        }
    ) {
        Icon(
            imageVector = Icons.Filled.FileUpload,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.import_csv_file))
    }
}

private fun LazyListScope.csvTable(
    csv: List<CSVRow>,
) {
    item {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                val visibleRows = remember(csv) { csv.take(10) }
                visibleRows.forEachIndexed { index, row ->
                    CSVRow(row = row, header = index == 0, even = index % 2 == 0)
                }
            }
        }
    }
}

@Composable
private fun CSVRow(
    row: CSVRow,
    header: Boolean,
    even: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        row.values.forEach { value ->
            CSVCell(text = value, header = header, even = even)
        }
    }
}

@Composable
private fun CSVCell(
    text: String,
    header: Boolean,
    even: Boolean,
    modifier: Modifier = Modifier,
) {
    val zebra = MaterialTheme.colorScheme.surfaceContainerHigh
    Text(
        modifier = modifier
            .width(140.dp)
            .thenIf(even) {
                this.background(zebra)
            }
            .padding(8.dp),
        text = text.ifEmpty { " " },
        style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

@Composable
private fun <M> MappingCard(
    columns: CSVRow,
    mapping: ColumnMapping<M>,
    status: MappingStatus,
    onMapTo: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    metadataContent: (@Composable (M) -> Unit)? = null,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = mapping.ivyColumn,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = mapping.helpInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Choose a matching CSV column:",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(columns.values) { index, column ->
                    val selected = column == mapping.name
                    FilterChip(
                        selected = selected,
                        onClick = { onMapTo(index, column) },
                        label = { Text(text = column) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (metadataContent != null) {
                    metadataContent(mapping.metadata)
                }

                MappingStatusLine(mapping = mapping, status = status)

                if (status.sampleValues.isNotEmpty()) {
                    CSVRow(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        row = CSVRow(status.sampleValues),
                        header = false,
                        even = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MappingStatusLine(
    mapping: ColumnMapping<*>,
    status: MappingStatus,
    modifier: Modifier = Modifier,
) {
    val (icon, text, color) = when {
        status.success -> Triple(
            Icons.Filled.CheckCircle,
            "Parsed successfully",
            MaterialTheme.colorScheme.tertiary
        )

        mapping.required -> Triple(
            Icons.Filled.Error,
            "Required: choose a column that can be parsed",
            MaterialTheme.colorScheme.error
        )

        else -> return
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

private fun <M> LazyListScope.mappingRow(
    columns: CSVRow,
    mapping: ColumnMapping<M>,
    status: MappingStatus,
    onMapTo: (Int, String) -> Unit,
    metadataContent: (@Composable (M) -> Unit)? = null,
) {
    item {
        MappingCard(
            columns = columns,
            mapping = mapping,
            status = status,
            onMapTo = onMapTo,
            metadataContent = metadataContent,
        )
    }
}

fun LazyListScope.sectionDivider(text: String) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = text,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = """
                    Match the CSV column with the appropriate Ivy type.
                    If the parsing is successful the field is marked as parsed.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// region Important
fun LazyListScope.importantFields(
    columns: CSVRow,
    importantFields: ImportantFields,
    onEvent: (CSVEvent) -> Unit,
) {
    sectionDivider("Important")
    mappingRow(
        columns = columns,
        mapping = importantFields.amount,
        status = importantFields.amountStatus,
        onMapTo = { index, name ->
            onEvent(
                CSVEvent.MapAmount(
                    index,
                    name
                )
            )
        },
        metadataContent = { multiplier ->
            AmountMetadata(multiplier = multiplier, onMetaChange = {
                onEvent(CSVEvent.AmountMultiplier(it))
            })
        }
    )
    mappingRow(
        columns = columns,
        mapping = importantFields.type,
        status = importantFields.typeStatus,
        onMapTo = { index, name -> onEvent(CSVEvent.MapType(index, name)) },
        metadataContent = {
            TypeMetadata(metadata = it, onEvent = onEvent)
        }
    )
    mappingRow(
        columns = columns,
        mapping = importantFields.date,
        status = importantFields.dateStatus,
        onMapTo = { index, name -> onEvent(CSVEvent.MapDate(index, name)) },
        metadataContent = {
            DateMetadataUI(metadata = it, onEvent = onEvent)
        }
    )
    mappingRow(
        columns = columns,
        mapping = importantFields.account,
        status = importantFields.accountStatus,
        onMapTo = { index, name ->
            onEvent(
                CSVEvent.MapAccount(
                    index,
                    name
                )
            )
        },
    )
    mappingRow(
        columns = columns,
        mapping = importantFields.accountCurrency,
        status = importantFields.accountCurrencyStatus,
        onMapTo = { index, name ->
            onEvent(
                CSVEvent.MapAccountCurrency(
                    index,
                    name
                )
            )
        },
    )
}

@Composable
private fun AmountMetadata(
    multiplier: Int,
    onMetaChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Multiplier", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = {
                onMetaChange(
                    when {
                        multiplier < 0 -> multiplier * 10
                        multiplier == 1 -> -10
                        else -> multiplier / 10
                    }
                )
            }) {
                Text(text = "/10")
            }
            Text(
                text = when {
                    multiplier < 0 -> "/${abs(multiplier)}"
                    multiplier > 1 -> "*${abs(multiplier)}"
                    else -> "None"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            FilledTonalButton(onClick = {
                onMetaChange(
                    when {
                        multiplier == -10 -> 1
                        multiplier == -1 -> 10
                        multiplier > 0 -> multiplier * 10
                        else -> multiplier / 10
                    }
                )
            }) {
                Text(text = "*10")
            }
        }
    }
}

// region Type Metadata
@Composable
private fun TypeMetadata(
    metadata: TrnTypeMetadata,
    onEvent: (CSVEvent) -> Unit,
) {
    val onTypeMetaEvent = { newMeta: TrnTypeMetadata ->
        onEvent(CSVEvent.TypeMetaChange(newMeta))
    }

    LabelContainsField(
        label = "Income",
        value = metadata.income,
        onValueChange = {
            onTypeMetaEvent(metadata.copy(income = it))
        }
    )
    LabelContainsField(
        label = "Expense",
        value = metadata.expense,
        onValueChange = {
            onTypeMetaEvent(metadata.copy(expense = it))
        }
    )
    LabelContainsField(
        label = "Transfer",
        value = metadata.transfer ?: "",
        supportingText = "(optional)",
        onValueChange = {
            onTypeMetaEvent(metadata.copy(transfer = it))
        }
    )
}

@Composable
fun LabelContainsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var textField by remember {
        // move the cursor at the end of the text
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    OutlinedTextField(
        value = textField,
        onValueChange = {
            textField = it
            onValueChange(it.text)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = "$label contains") },
        supportingText = supportingText?.let { { Text(text = it) } },
        singleLine = true
    )
}
// endregion

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateMetadataUI(
    metadata: DateMetadata,
    onEvent: (CSVEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Which is first in the format?", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = metadata == DateMetadata.DateFirst,
                onClick = {
                    onEvent(CSVEvent.DataMetaChange(DateMetadata.DateFirst))
                },
                label = { Text(text = "Date/Day (1,2,3...31)") },
                leadingIcon = if (metadata == DateMetadata.DateFirst) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
            FilterChip(
                selected = metadata == DateMetadata.MonthFirst,
                onClick = {
                    onEvent(CSVEvent.DataMetaChange(DateMetadata.MonthFirst))
                },
                label = { Text(text = "Month (1,2..12 / Jan, Feb..Dec)") },
                leadingIcon = if (metadata == DateMetadata.MonthFirst) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

fun LazyListScope.transferFields(
    columns: CSVRow,
    transferFields: TransferFields,
    onEvent: (CSVEvent) -> Unit,
) {
    sectionDivider("Transfer fields")
    mappingRow(
        columns = columns,
        mapping = transferFields.toAccount,
        status = transferFields.toAccountStatus,
        onMapTo = { index, name ->
            onEvent(
                CSVEvent.MapToAccount(
                    index,
                    name
                )
            )
        },
    )
    mappingRow(
        columns = columns,
        mapping = transferFields.toAccountCurrency,
        status = transferFields.toAccountCurrencyStatus,
        onMapTo = { index, name ->
            onEvent(
                CSVEvent.MapToAccountCurrency(
                    index,
                    name
                )
            )
        },
    )
    mappingRow(
        columns = columns,
        mapping = transferFields.toAmount,
        status = transferFields.toAmountStatus,
        onMapTo = { index, name ->
            onEvent(
                CSVEvent.MapToAmount(
                    index,
                    name
                )
            )
        },
        metadataContent = { multiplier ->
            AmountMetadata(
                multiplier = multiplier,
                onMetaChange = {
                    onEvent(CSVEvent.ToAmountMetaChange(it))
                }
            )
        }
    )
}

fun LazyListScope.optionalFields(
    columns: CSVRow,
    optionalFields: OptionalFields,
    onEvent: (CSVEvent) -> Unit,
) {
    sectionDivider("Optional fields")
    mappingRow(
        columns = columns,
        mapping = optionalFields.category,
        status = optionalFields.categoryStatus,
        onMapTo = { index, name ->
            onEvent(
                CSVEvent.MapCategory(
                    index,
                    name
                )
            )
        },
    )
    mappingRow(
        columns = columns,
        mapping = optionalFields.title,
        status = optionalFields.titleStatus,
        onMapTo = { index, name -> onEvent(CSVEvent.MapTitle(index, name)) },
    )
    mappingRow(
        columns = columns,
        mapping = optionalFields.description,
        status = optionalFields.descriptionStatus,
        onMapTo = { index, name ->
            onEvent(
                CSVEvent.MapDescription(
                    index,
                    name
                )
            )
        },
    )
}

private fun previewColumns(extraColumns: Int = 0): CSVRow = CSVRow(
    listOf("Date", "Account", "Amount", "Category") +
        List(extraColumns) { "Extra column ${it + 1}" }
)

private fun previewCsv(extraColumns: Int): ImmutableList<CSVRow> = persistentListOf(
    previewColumns(extraColumns),
    CSVRow(listOf("2023-04-27", "Cash", "-12.50", "Food")),
    CSVRow(listOf("2023-04-28", "Bank", "1500.00", "Salary")),
)

private fun previewColumn(name: String): Int = previewColumns().values.indexOf(name)

private fun previewState(mapped: Boolean, extraColumns: Int): CSVState = CSVState(
    uiState = UIState.Idle,
    columns = if (mapped) previewColumns(extraColumns) else null,
    csv = previewCsv(extraColumns),
    important = if (mapped) {
        ImportantFields(
            amount = ColumnMapping("Amount", "The transaction amount", "Amount", 2, 1, true),
            amountStatus = MappingStatus(listOf("-12.5", "1500.0"), true),
            type = ColumnMapping(
                "Type",
                "Income or expense",
                "Amount",
                2,
                TrnTypeMetadata(income = "Income", expense = "Expense", transfer = null),
                true,
            ),
            typeStatus = MappingStatus(listOf("EXPENSE", "INCOME"), true),
            date = ColumnMapping("Date", "When it happened", "Date", 0, DateMetadata.DateFirst, true),
            dateStatus = MappingStatus(listOf("2023-04-27T00:00", "2023-04-28T00:00"), true),
            account = ColumnMapping(
                ivyColumn = "Account",
                helpInfo = "The account name",
                name = "Category",
                index = previewColumn("Category"),
                metadata = Unit,
                required = true,
            ),
            accountStatus = MappingStatus(emptyList(), false),
            accountCurrency = ColumnMapping("Account currency", "Optional currency", "", -1, Unit, false),
            accountCurrencyStatus = MappingStatus(emptyList(), false),
        )
    } else {
        null
    },
    transfer = if (mapped) {
        TransferFields(
            toAccount = ColumnMapping("To account", "The receiving account", "Account", 1, Unit, true),
            toAccountStatus = MappingStatus(listOf("Cash", "Bank"), true),
            toAccountCurrency = ColumnMapping("To account currency", "Optional currency", "", -1, Unit, false),
            toAccountCurrencyStatus = MappingStatus(emptyList(), false),
            toAmount = ColumnMapping("To amount", "The received amount", "Amount", 2, 10, true),
            toAmountStatus = MappingStatus(listOf("-125.0", "15000.0"), true),
        )
    } else {
        null
    },
    optional = if (mapped) {
        OptionalFields(
            category = ColumnMapping(
                ivyColumn = "Category",
                helpInfo = "The category name",
                name = "Category",
                index = previewColumn("Category"),
                metadata = Unit,
                required = false,
            ),
            categoryStatus = MappingStatus(listOf("Food", "Salary"), true),
            title = ColumnMapping("Title", "A short title", "", -1, Unit, false),
            titleStatus = MappingStatus(emptyList(), false),
            description = ColumnMapping("Description", "A longer note", "", -1, Unit, false),
            descriptionStatus = MappingStatus(emptyList(), false),
        )
    } else {
        null
    },
    continueEnabled = false,
)

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    mapped: Boolean = false,
    firstVisibleItem: Int = 0,
    extraColumns: Int = 0,
) {
    WalletPreview(theme) {
        NavigationRoot(navigation = Navigation()) {
            ImportUI(
                state = previewState(mapped, extraColumns),
                launchedFromOnboarding = false,
                onEvent = {},
                listState = rememberLazyListState(initialFirstVisibleItemIndex = firstVisibleItem),
            )
        }
    }
}

/** For screenshot testing */
@Composable
fun CSVScreenUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun CSVScreenMappedUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, mapped = true)
}

/** For screenshot testing */
@Composable
fun CSVScreenTypeFieldsUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, mapped = true, firstVisibleItem = 5)
}

/** For screenshot testing */
@Composable
fun CSVScreenInvalidMappingUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, mapped = true, firstVisibleItem = 6)
}

/** For screenshot testing */
@Composable
fun CSVScreenTransferFieldsUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, mapped = true, firstVisibleItem = 9)
}

/** For screenshot testing */
@Composable
fun CSVScreenManyColumnsUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT, mapped = true, firstVisibleItem = 2, extraColumns = 16)
}
