package com.example.outstandingmanager.ui.importflow

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.outstandingmanager.ui.common.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowCleaningScreen(
    headers: List<String>,
    rawRows: List<List<String>>,
    onProceedToSave: (activeRows: List<Map<String, String>>, removedRows: List<Map<String, String>>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Convert raw rows to List<Map<String, String>>
    val allRowMaps = remember(rawRows, headers) {
        rawRows.mapIndexed { idx, row ->
            val map = mutableMapOf<String, String>()
            map["_row_id"] = idx.toString()
            headers.forEachIndexed { colIdx, header ->
                map[header] = if (colIdx in row.indices) row[colIdx] else ""
            }
            map
        }
    }

    val removedRowIds = remember { mutableStateListOf<String>() }
    val selectedRowIds = remember { mutableStateListOf<String>() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFeederFilter by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Distinct feeders in data for quick filtering
    val feeders = remember(allRowMaps) {
        val feederCol = headers.firstOrNull { it.contains("feeder", ignoreCase = true) }
        if (feederCol != null) {
            allRowMaps.map { it[feederCol] ?: "" }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        } else emptyList()
    }

    // Active records
    val activeRowMaps by remember(allRowMaps, removedRowIds.size) {
        derivedStateOf {
            allRowMaps.filter { !removedRowIds.contains(it["_row_id"]) }
        }
    }

    // Filtered records
    val filteredActiveRows by remember(activeRowMaps, searchQuery, selectedFeederFilter) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            val feederCol = headers.firstOrNull { it.contains("feeder", ignoreCase = true) }

            activeRowMaps.filter { row ->
                if (selectedFeederFilter != null && feederCol != null) {
                    if (row[feederCol] != selectedFeederFilter) return@filter false
                }
                if (q.isNotEmpty()) {
                    val matches = row.values.any { it.lowercase().contains(q) }
                    if (!matches) return@filter false
                }
                true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Clean Records", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Active: ${activeRowMaps.size} | Removed: ${removedRowIds.size}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (selectedRowIds.isNotEmpty()) {
                        Button(
                            onClick = {
                                val count = selectedRowIds.size
                                removedRowIds.addAll(selectedRowIds)
                                selectedRowIds.clear()
                                statusMessage = "$count records moved to Removed Records"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DELETE SELECTED (${selectedRowIds.size})", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                val active = allRowMaps.filter { !removedRowIds.contains(it["_row_id"]) }
                                val removed = allRowMaps.filter { removedRowIds.contains(it["_row_id"]) }
                                onProceedToSave(active, removed)
                            },
                            enabled = activeRowMaps.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Proceed to Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by KNO, Name, Address...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Feeder chips if feeders exist
            if (feeders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFeederFilter == null,
                            onClick = { selectedFeederFilter = null },
                            label = { Text("All Feeders", fontSize = 12.sp) }
                        )
                    }
                    items(feeders) { feeder ->
                        FilterChip(
                            selected = selectedFeederFilter == feeder,
                            onClick = {
                                selectedFeederFilter = if (selectedFeederFilter == feeder) null else feeder
                            },
                            label = { Text(feeder, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selection Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${filteredActiveRows.size} records",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filteredIds = filteredActiveRows.mapNotNull { it["_row_id"] }
                    val areAllFilteredSelected = filteredIds.isNotEmpty() && filteredIds.all { selectedRowIds.contains(it) }

                    OutlinedButton(
                        onClick = {
                            if (areAllFilteredSelected) {
                                selectedRowIds.removeAll(filteredIds)
                            } else {
                                filteredIds.forEach { id ->
                                    if (!selectedRowIds.contains(id)) selectedRowIds.add(id)
                                }
                            }
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (areAllFilteredSelected) "DESELECT FILTERED" else "SELECT ALL FILTERED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Status message banner
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusMessage!!,
                            fontSize = 12.sp,
                            color = Color(0xFF1D4ED8),
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { statusMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Record list with checkboxes
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val knoKey = headers.firstOrNull { it.contains("kno", ignoreCase = true) } ?: headers.getOrNull(2) ?: ""
                val nameKey = headers.firstOrNull { it.contains("consumer name", ignoreCase = true) || it.contains("name", ignoreCase = true) } ?: headers.getOrNull(4) ?: ""
                val amountKey = headers.firstOrNull { it.contains("outstanding", ignoreCase = true) || it.contains("amount", ignoreCase = true) } ?: ""
                val feederKey = headers.firstOrNull { it.contains("feeder", ignoreCase = true) } ?: ""

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredActiveRows, key = { it["_row_id"] ?: "" }) { row ->
                        val rowId = row["_row_id"] ?: ""
                        val isSelected = selectedRowIds.contains(rowId)
                        val knoVal = row[knoKey] ?: ""
                        val nameVal = row[nameKey] ?: ""
                        val amountVal = row[amountKey] ?: ""
                        val feederVal = if (feederKey.isNotBlank()) row[feederKey] ?: "" else ""

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedRowIds.remove(rowId) else selectedRowIds.add(rowId)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedRowIds.add(rowId) else selectedRowIds.remove(rowId)
                                }
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = knoVal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (amountVal.isNotBlank()) {
                                        val numAmount = CurrencyFormatter.parseAmount(amountVal)
                                        Text(
                                            text = CurrencyFormatter.formatIndianCurrency(numAmount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = nameVal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (feederVal.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = feederVal,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
