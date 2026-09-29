package com.example.outstandingmanager.ui.export

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.outstandingmanager.export.PdfOrientation

@Composable
fun PdfExportDialog(
    availableColumns: List<String>,
    recordCount: Int,
    onExport: (selectedColumns: List<String>, orientation: PdfOrientation) -> Unit,
    onDismiss: () -> Unit
) {
    // Default columns for PDF: KNO, Consumer Name, Address, Outstanding Amount, Status, Remark
    val allCandidateCols = remember(availableColumns) {
        val list = availableColumns.toMutableList()
        if (!list.any { it.equals("Status", ignoreCase = true) }) list.add("Status")
        if (!list.any { it.equals("Remark", ignoreCase = true) }) list.add("Remark")
        list
    }

    val defaultSelected = remember(allCandidateCols) {
        val preferred = listOf("kno", "consumer name", "address", "outstanding amount", "status", "remark")
        allCandidateCols.filter { col ->
            preferred.any { p -> col.lowercase().contains(p) }
        }.toSet()
    }

    var selectedCols by remember { mutableStateOf(if (defaultSelected.isNotEmpty()) defaultSelected else allCandidateCols.take(6).toSet()) }
    var orientation by remember { mutableStateOf(PdfOrientation.AUTO) }

    val autoRecommendedLandscape = selectedCols.size >= 5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Export A4 PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Exporting $recordCount filtered records",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SELECT COLUMNS FOR PDF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedCols = allCandidateCols.toSet() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("SELECT ALL", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { selectedCols = emptySet() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("CLEAR ALL", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    items(allCandidateCols) { col ->
                        val isChecked = selectedCols.contains(col)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCols = if (isChecked) selectedCols - col else selectedCols + col
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedCols = if (checked) selectedCols + col else selectedCols - col
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = col,
                                fontSize = 13.sp,
                                color = if (isChecked) Color(0xFF0F172A) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ORIENTATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = orientation == PdfOrientation.AUTO,
                        onClick = { orientation = PdfOrientation.AUTO },
                        label = { Text(if (autoRecommendedLandscape) "Auto (Landscape)" else "Auto (Portrait)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = orientation == PdfOrientation.PORTRAIT,
                        onClick = { orientation = PdfOrientation.PORTRAIT },
                        label = { Text("Portrait", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = orientation == PdfOrientation.LANDSCAPE,
                        onClick = { orientation = PdfOrientation.LANDSCAPE },
                        label = { Text("Landscape", fontSize = 11.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ordered = allCandidateCols.filter { selectedCols.contains(it) }
                    onExport(ordered, orientation)
                    onDismiss()
                },
                enabled = selectedCols.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("EXPORT PDF", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
