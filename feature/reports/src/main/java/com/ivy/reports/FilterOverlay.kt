package com.ivy.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.IconBadge
import com.ivy.legacy.ui.component.tags.ShowTagModal
import com.ivy.legacy.ui.sheet.ActionSheet
import com.ivy.legacy.ui.sheet.AddKeywordModal
import com.ivy.legacy.ui.sheet.AmountModal
import com.ivy.legacy.ui.sheet.ChoosePeriodModal
import com.ivy.legacy.ui.sheet.ChoosePeriodModalData
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.GreenDark
import com.ivy.ui.color.GreenLight
import com.ivy.ui.color.Ivy
import com.ivy.ui.color.IvyDark
import com.ivy.ui.color.Purple1Dark
import com.ivy.ui.color.Red3Light
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.util.UUID

@Suppress("LongMethod", "LongParameterList")
@Composable
fun FilterOverlay(
    visible: Boolean,
    baseCurrency: String,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: ImmutableList<Tag>,
    filter: ReportFilter?,
    onClose: () -> Unit,
    onSetFilter: (ReportFilter?) -> Unit,
    onTagSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var localFilter by remember(filter) { mutableStateOf(filter) }
    val baseFilter = remember(baseCurrency) { ReportFilter.emptyFilter(baseCurrency = baseCurrency) }
    val current = localFilter ?: baseFilter
    val update = { transform: (ReportFilter) -> ReportFilter -> localFilter = transform(localFilter ?: baseFilter) }

    var choosePeriodModal: ChoosePeriodModalData? by remember { mutableStateOf(null) }
    var minAmountModalShown by remember { mutableStateOf(false) }
    var maxAmountModalShown by remember { mutableStateOf(false) }
    var includeKeywordModalShown by remember { mutableStateOf(false) }
    var excludeKeywordModalShown by remember { mutableStateOf(false) }
    var includedTagModalVisible by remember { mutableStateOf(false) }
    var excludedTagModalVisible by remember { mutableStateOf(false) }
    val ivyContext = ivyWalletCtx()

    if (visible) {
        ActionSheet(
            onDismiss = onClose,
            modifier = modifier,
            title = stringResource(R.string.filter),
            actions = { dismiss ->
                TextButton(
                    onClick = {
                        localFilter = null
                        onSetFilter(null)
                    },
                    modifier = Modifier.testTag("filter_clear"),
                ) {
                    Text(stringResource(R.string.clean_filter))
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        localFilter?.let(onSetFilter)
                        dismiss()
                    },
                    modifier = Modifier.testTag("filter_apply"),
                    enabled = localFilter?.validate() == true,
                ) {
                    Text(stringResource(R.string.apply_filter))
                }
            }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TypeFilter(filter = localFilter, onChange = update)

                PeriodFilter(
                    filter = localFilter,
                    onShowPeriodChooserModal = {
                        choosePeriodModal = ChoosePeriodModalData(
                            period = localFilter?.period ?: ivyContext.selectedPeriod
                        )
                    }
                )

                AccountsFilter(allAccounts = accounts, filter = localFilter, onChange = update)

                CategoriesFilter(allCategories = categories, filter = localFilter, onChange = update)

                AmountFilter(
                    baseCurrency = baseCurrency,
                    filter = localFilter,
                    onShowMinAmountModal = { minAmountModalShown = true },
                    onShowMaxAmountModal = { maxAmountModalShown = true },
                )

                KeywordsFilter(
                    filter = localFilter,
                    onChange = update,
                    onShowIncludeKeywordModal = { includeKeywordModalShown = true },
                    onShowExcludeKeywordModal = { excludeKeywordModalShown = true },
                )

                TagsFilter(
                    filter = localFilter,
                    allTags = allTags,
                    onChange = update,
                    onShowIncludedTagModal = { includedTagModalVisible = true },
                    onShowExcludedTagModal = { excludedTagModalVisible = true },
                )
            }
        }
    }

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = { choosePeriodModal = null },
    ) { selectedPeriod ->
        update { it.copy(period = selectedPeriod) }
    }

    AmountFilterModal(
        visible = minAmountModalShown,
        currency = baseCurrency,
        filterId = current.id,
        amount = current.minAmount,
        dismiss = { minAmountModalShown = false },
    ) { amount ->
        update { it.copy(minAmount = amount) }
    }

    AmountFilterModal(
        visible = maxAmountModalShown,
        currency = baseCurrency,
        filterId = current.id,
        amount = current.maxAmount,
        dismiss = { maxAmountModalShown = false },
    ) { amount ->
        update { it.copy(maxAmount = amount) }
    }

    AddKeywordModal(
        keyword = "",
        visible = includeKeywordModalShown,
        dismiss = { includeKeywordModalShown = false }
    ) { keyword ->
        update { it.copy(includeKeywords = it.includeKeywords.plus(keyword).distinct()) }
    }

    AddKeywordModal(
        keyword = "",
        visible = excludeKeywordModalShown,
        dismiss = { excludeKeywordModalShown = false }
    ) { keyword ->
        update { it.copy(excludeKeywords = it.excludeKeywords.plus(keyword).distinct()) }
    }

    TagSelectModal(
        visible = includedTagModalVisible,
        allTags = allTags,
        selected = current.includedTags,
        onDismiss = { includedTagModalVisible = false },
        onTagSelect = { tag -> update { it.copy(includedTags = it.includedTags.plus(tag.id)) } },
        onTagDeselect = { tag -> update { it.copy(includedTags = it.includedTags.minus(tag.id)) } },
        onTagSearch = onTagSearch,
    )

    TagSelectModal(
        visible = excludedTagModalVisible,
        allTags = allTags,
        selected = current.excludedTags,
        onDismiss = { excludedTagModalVisible = false },
        onTagSelect = { tag -> update { it.copy(excludedTags = it.excludedTags.plus(tag.id)) } },
        onTagDeselect = { tag -> update { it.copy(excludedTags = it.excludedTags.minus(tag.id)) } },
        onTagSearch = onTagSearch,
    )
}

@Composable
private fun AmountFilterModal(
    visible: Boolean,
    currency: String,
    filterId: UUID,
    amount: Double?,
    dismiss: () -> Unit,
    onAmountChange: (Double?) -> Unit,
) {
    val id = remember(filterId, amount) { UUID.randomUUID() }
    AmountModal(
        id = id,
        visible = visible,
        currency = currency,
        initialAmount = amount?.takeIf { it > 0 },
        dismiss = dismiss,
    ) {
        onAmountChange(it.takeIf { value -> value > 0 })
    }
}

@Composable
private fun TagSelectModal(
    visible: Boolean,
    allTags: ImmutableList<Tag>,
    selected: List<TagId>,
    onDismiss: () -> Unit,
    onTagSelect: (Tag) -> Unit,
    onTagDeselect: (Tag) -> Unit,
    onTagSearch: (String) -> Unit,
) {
    val selectedTags by remember(selected) { derivedStateOf { selected.toImmutableList() } }
    ShowTagModal(
        visible = visible,
        selectOnlyMode = true,
        onDismiss = {
            onDismiss()
            // Reset TagList, avoids showing incorrect tag list if user had searched for a tag previously
            onTagSearch("")
        },
        allTagList = allTags,
        selectedTagList = selectedTags,
        onTagAdd = {},
        onTagEdit = { _, _ -> },
        onTagDelete = {},
        onTagSelected = onTagSelect,
        onTagDeSelected = onTagDeselect,
        onTagSearch = onTagSearch,
    )
}

@Composable
private fun FilterSection(
    title: String,
    modifier: Modifier = Modifier,
    invalid: Boolean = false,
    action: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            action()
        }
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun TypeFilter(
    filter: ReportFilter?,
    onChange: ((ReportFilter) -> ReportFilter) -> Unit,
) {
    val types = filter?.trnTypes.orEmpty()
    FilterSection(title = stringResource(R.string.by_type), invalid = types.isEmpty()) {
        ChipRow {
            TransactionType.entries.forEach { type ->
                val selected = types.contains(type)
                FilterChip(
                    selected = selected,
                    onClick = {
                        onChange {
                            if (selected) {
                                it.copy(trnTypes = it.trnTypes.filter { t -> t != type })
                            } else {
                                it.copy(trnTypes = it.trnTypes.plus(type))
                            }
                        }
                    },
                    label = {
                        Text(
                            when (type) {
                                TransactionType.INCOME -> stringResource(R.string.incomes)
                                TransactionType.EXPENSE -> stringResource(R.string.expenses)
                                TransactionType.TRANSFER -> stringResource(R.string.account_transfers)
                            }
                        )
                    },
                    modifier = Modifier.testTag("filter_type_${type.name.lowercase()}"),
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
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
}

@Composable
private fun PeriodFilter(
    filter: ReportFilter?,
    onShowPeriodChooserModal: () -> Unit,
) {
    val startDayOfMonth = ivyWalletCtx().startDayOfMonth
    val timeProvider = LocalTimeProvider.current
    val timeConverter = LocalTimeConverter.current
    val timeFormatter = LocalTimeFormatter.current
    FilterSection(title = stringResource(R.string.time_period), invalid = filter?.period == null) {
        AssistChip(
            onClick = onShowPeriodChooserModal,
            label = {
                Text(
                    filter?.period?.toDisplayLong(
                        startDateOfMonth = startDayOfMonth,
                        timeProvider = timeProvider,
                        timeConverter = timeConverter,
                        timeFormatter = timeFormatter,
                    )?.capitalizeLocal() ?: stringResource(R.string.select_time_range)
                )
            },
            modifier = Modifier.testTag("filter_period"),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
    }
}

@Composable
private fun AccountsFilter(
    allAccounts: List<Account>,
    filter: ReportFilter?,
    onChange: ((ReportFilter) -> ReportFilter) -> Unit,
) {
    val selected = filter?.accounts.orEmpty()
    FilterSection(
        title = stringResource(R.string.accounts_number, selected.size),
        invalid = selected.isEmpty(),
        action = {
            SelectAllButton(
                anySelected = selected.isNotEmpty(),
                onClearAll = { onChange { it.copy(accounts = emptyList()) } },
                onSelectAll = { onChange { it.copy(accounts = allAccounts) } },
            )
        }
    ) {
        ChipRow {
            allAccounts.forEach { account ->
                FilterChip(
                    selected = selected.contains(account),
                    onClick = {
                        onChange {
                            if (it.accounts.contains(account)) {
                                it.copy(accounts = it.accounts.filter { a -> a != account })
                            } else {
                                it.copy(accounts = it.accounts.plus(account).sortedBy { a -> a.orderNum })
                            }
                        }
                    },
                    label = { Text(account.name) },
                    modifier = Modifier.testTag("filter_account"),
                    leadingIcon = {
                        IconBadge(
                            color = Color(account.color),
                            iconName = account.icon,
                            defaultIcon = R.drawable.ic_custom_account_m,
                            size = FilterChipDefaults.IconSize,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoriesFilter(
    allCategories: List<Category>,
    filter: ReportFilter?,
    onChange: ((ReportFilter) -> ReportFilter) -> Unit,
) {
    val selected = filter?.categories.orEmpty()
    FilterSection(
        title = stringResource(R.string.categories_number, selected.size),
        invalid = selected.isEmpty(),
        action = {
            SelectAllButton(
                anySelected = selected.isNotEmpty(),
                onClearAll = { onChange { it.copy(categories = emptyList()) } },
                onSelectAll = { onChange { it.copy(categories = allCategories) } },
            )
        }
    ) {
        ChipRow {
            allCategories.forEach { category ->
                FilterChip(
                    selected = selected.contains(category),
                    onClick = {
                        onChange {
                            if (it.categories.contains(category)) {
                                it.copy(categories = it.categories.filter { c -> c != category })
                            } else {
                                it.copy(categories = it.categories.plus(category).sortedBy { c -> c.orderNum })
                            }
                        }
                    },
                    label = { Text(category.name.value) },
                    modifier = Modifier.testTag("filter_category"),
                    leadingIcon = {
                        IconBadge(
                            color = Color(category.color.value),
                            iconName = category.icon?.id,
                            defaultIcon = R.drawable.ic_custom_category_m,
                            size = FilterChipDefaults.IconSize,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectAllButton(
    anySelected: Boolean,
    onClearAll: () -> Unit,
    onSelectAll: () -> Unit,
) {
    TextButton(onClick = if (anySelected) onClearAll else onSelectAll) {
        Text(stringResource(if (anySelected) R.string.clear_all else R.string.select_all))
    }
}

@Composable
private fun AmountFilter(
    baseCurrency: String,
    filter: ReportFilter?,
    onShowMinAmountModal: () -> Unit,
    onShowMaxAmountModal: () -> Unit,
) {
    FilterSection(title = stringResource(R.string.amount_optional)) {
        ChipRow {
            AssistChip(
                onClick = onShowMinAmountModal,
                label = {
                    Text(
                        stringResource(
                            R.string.filter_amount_from,
                            "${(filter?.minAmount ?: 0.0).format(baseCurrency)} $baseCurrency"
                        )
                    )
                },
                modifier = Modifier.testTag("filter_min_amount"),
            )
            AssistChip(
                onClick = onShowMaxAmountModal,
                label = {
                    Text(
                        stringResource(
                            R.string.filter_amount_to,
                            "${(filter?.maxAmount ?: 0.0).format(baseCurrency)} $baseCurrency"
                        )
                    )
                },
                modifier = Modifier.testTag("filter_max_amount"),
            )
        }
    }
}

@Composable
private fun KeywordsFilter(
    filter: ReportFilter?,
    onChange: ((ReportFilter) -> ReportFilter) -> Unit,
    onShowIncludeKeywordModal: () -> Unit,
    onShowExcludeKeywordModal: () -> Unit,
) {
    FilterSection(title = stringResource(R.string.keywords_optional)) {
        KeywordGroup(
            label = stringResource(R.string.filter_includes),
            keywords = filter?.includeKeywords.orEmpty(),
            onRemove = { keyword ->
                onChange { it.copy(includeKeywords = it.includeKeywords.filter { k -> k != keyword }) }
            },
            onAdd = onShowIncludeKeywordModal,
        )
        KeywordGroup(
            label = stringResource(R.string.filter_excludes),
            keywords = filter?.excludeKeywords.orEmpty(),
            onRemove = { keyword ->
                onChange { it.copy(excludeKeywords = it.excludeKeywords.filter { k -> k != keyword }) }
            },
            onAdd = onShowExcludeKeywordModal,
        )
    }
}

@Composable
private fun KeywordGroup(
    label: String,
    keywords: List<String>,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        ChipRow {
            keywords.forEach { keyword ->
                RemovableChip(text = keyword, onRemove = { onRemove(keyword) })
            }
            AddChip(text = stringResource(R.string.add_keyword), onClick = onAdd)
        }
    }
}

@Composable
private fun TagsFilter(
    filter: ReportFilter?,
    allTags: ImmutableList<Tag>,
    onChange: ((ReportFilter) -> ReportFilter) -> Unit,
    onShowIncludedTagModal: () -> Unit,
    onShowExcludedTagModal: () -> Unit,
) {
    FilterSection(title = stringResource(R.string.tags_optional)) {
        TagGroup(
            label = stringResource(R.string.filter_includes),
            selected = filter?.includedTags.orEmpty(),
            allTags = allTags,
            onRemove = { id -> onChange { it.copy(includedTags = it.includedTags.minus(id)) } },
            onSelect = onShowIncludedTagModal,
        )
        TagGroup(
            label = stringResource(R.string.filter_excludes),
            selected = filter?.excludedTags.orEmpty(),
            allTags = allTags,
            onRemove = { id -> onChange { it.copy(excludedTags = it.excludedTags.minus(id)) } },
            onSelect = onShowExcludedTagModal,
        )
    }
}

@Composable
private fun TagGroup(
    label: String,
    selected: List<TagId>,
    allTags: ImmutableList<Tag>,
    onRemove: (TagId) -> Unit,
    onSelect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        ChipRow {
            selected.forEach { id ->
                allTags.firstOrNull { it.id == id }?.let { tag ->
                    RemovableChip(text = "#${tag.name.value}", onRemove = { onRemove(id) })
                }
            }
            AddChip(text = stringResource(R.string.select_tags), onClick = onSelect)
        }
    }
}

@Composable
private fun RemovableChip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InputChip(
        selected = false,
        onClick = onRemove,
        label = { Text(text) },
        modifier = modifier,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.delete),
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun AddChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}

@Suppress("MagicNumber")
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    val accounts = listOf(
        Account("Cash", color = Green.toArgb()),
        Account("DSK", color = GreenDark.toArgb()),
        Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
        Account("Revolut", color = IvyDark.toArgb()),
    )
    val categories = listOf(
        Category(
            name = NotBlankTrimmedString.unsafe("Science"),
            color = ColorInt(Purple1Dark.toArgb()),
            icon = IconAsset.unsafe("atom"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        ),
        Category(
            name = NotBlankTrimmedString.unsafe("Pet"),
            color = ColorInt(Red3Light.toArgb()),
            icon = IconAsset.unsafe("pet"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 1.0,
        ),
        Category(
            name = NotBlankTrimmedString.unsafe("Home"),
            color = ColorInt(Green.toArgb()),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 2.0,
        ),
    )
    val tags = listOf("Groceries", "Travel").mapIndexed { index, name ->
        Tag(
            id = TagId(UUID.fromString("00000000-0000-0000-0000-00000000000${index + 1}")),
            name = NotBlankTrimmedString.unsafe(name),
            description = null,
            color = ColorInt(Ivy.toArgb()),
            icon = null,
            orderNum = index.toDouble(),
            creationTimestamp = Instant.parse("2026-01-15T12:00:00Z"),
        )
    }

    WalletPreview(theme) {
        FilterOverlay(
            visible = true,
            baseCurrency = "BGN",
            accounts = accounts,
            categories = categories,
            allTags = tags.toImmutableList(),
            filter = ReportFilter.emptyFilter("BGN").copy(
                trnTypes = listOf(TransactionType.INCOME, TransactionType.EXPENSE),
                period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                accounts = accounts.take(2),
                categories = categories.take(1),
                maxAmount = 13256.27,
                includeKeywords = listOf("salary"),
                excludeKeywords = listOf("refund"),
                includedTags = listOf(tags[0].id),
            ),
            onClose = {},
            onSetFilter = {},
            onTagSearch = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLight() {
    Preview(Theme.LIGHT)
}

/** For screenshot testing */
@Composable
fun ReportFilterUiTest(isDark: Boolean) {
    Preview(if (isDark) Theme.DARK else Theme.LIGHT)
}
