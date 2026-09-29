package com.example.outstandingmanager.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.outstandingmanager.export.PdfOrientation
import com.example.outstandingmanager.model.ConsumerRecord
import com.example.outstandingmanager.model.Dataset
import com.example.outstandingmanager.model.FilterCriteria
import com.example.outstandingmanager.model.SortOption
import com.example.outstandingmanager.ui.common.CurrencyFormatter
import com.example.outstandingmanager.ui.common.StatusBadge
import com.example.outstandingmanager.ui.export.PdfExportDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingListScreen(
    dataset: Dataset?,
    records: List<ConsumerRecord>,
    filterCriteria: FilterCriteria,
    sortOption: SortOption,
    availableFeeders: List<String>,
    availableTariffCodes: List<String>,
    availableAreaCodes: List<String>,
    onFilterChange: (FilterCriteria) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onConsumerClick: (ConsumerRecord) -> Unit,
    onExportPdf: (selectedColumns: List<String>, orientation: PdfOrientation) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(filterCriteria.query.isNotEmpty()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sortSheetState = rememberModalBottomSheetState()

    val activeFilterCount = filterCriteria.activeFilterCount()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Outstanding", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${records.size} consumers • Sort: ${sortOption.displayName.substringBefore(" (")}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isSearchActive) MaterialTheme.colorScheme.primary else Color(0xFF475569))
                    }

                    // Filter with badge
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("$activeFilterCount")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color(0xFF475569))
                        }
                    }

                    // Sort button
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = Color(0xFF475569))
                    }

                    // PDF export
                    IconButton(onClick = { showPdfDialog = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = Color(0xFFDC2626))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
        ) {
            // Search Input Bar
            if (isSearchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = filterCriteria.query,
                        onValueChange = { onFilterChange(filterCriteria.copy(query = it)) },
                        placeholder = { Text("Search KNO, Name, Mobile, Feeder...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (filterCriteria.query.isNotEmpty()) {
                                IconButton(onClick = { onFilterChange(filterCriteria.copy(query = "")) }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
            }

            // Active filters bar if any
            if (activeFilterCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeFilterCount filter(s) active • ${records.size} match",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1D4ED8)
                    )
                    Text(
                        text = "Reset",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.clickable { onFilterChange(FilterCriteria(query = filterCriteria.query)) }
                    )
                }
                HorizontalDivider(color = Color(0xFFDBEAFE))
            }

            // Records List
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No matching consumers found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing search or relaxing filters.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(records, key = { it.id }) { consumer ->
                        ConsumerListItem(
                            consumer = consumer,
                            onClick = { onConsumerClick(consumer) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            sheetState = filterSheetState,
            initialCriteria = filterCriteria,
            availableFeeders = availableFeeders,
            availableTariffCodes = availableTariffCodes,
            availableAreaCodes = availableAreaCodes,
            matchingCount = records.size,
            onApply = onFilterChange,
            onReset = { onFilterChange(FilterCriteria(query = filterCriteria.query)) },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        SortBottomSheet(
            sheetState = sortSheetState,
            currentSort = sortOption,
            onSelectSort = onSortChange,
            onDismiss = { showSortSheet = false }
        )
    }

    // PDF Export Dialog
    if (showPdfDialog) {
        PdfExportDialog(
            availableColumns = dataset?.retainedColumns ?: emptyList(),
            recordCount = records.size,
            onExport = onExportPdf,
            onDismiss = { showPdfDialog = false }
        )
    }
}

@Composable
fun ConsumerListItem(
    consumer: ConsumerRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Row 1: KNO and Outstanding Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = consumer.primaryIdentifier,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = CurrencyFormatter.formatIndianCurrency(consumer.outstandingAmount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB91C1C)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Row 2: Consumer Name and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = consumer.consumerName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                StatusBadge(status = consumer.displayStatus)
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Row 3: Feeder / Mobile & Remark indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val subtitle = buildString {
                    if (consumer.feederName.isNotBlank()) append(consumer.feederName)
                    if (consumer.mobileNo.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(consumer.mobileNo)
                    }
                }

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (consumer.remark.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = "Remark",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = consumer.remark,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }
            }
        }
    }
}
