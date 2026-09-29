package com.example.outstandingmanager.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.outstandingmanager.model.Dataset
import com.example.outstandingmanager.ui.common.ConfirmationDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dataset: Dataset?,
    onStartNewDatasetImport: () -> Unit,
    onResetCurrentData: () -> Unit,
    onViewRemovedRecords: () -> Unit,
    onPermanentlyClearRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearRemovedDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Current Dataset Info Card
            if (dataset != null) {
                Text("CURRENT DATASET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dataset.sourceFileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val df = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
                        Text("Imported: ${df.format(Date(dataset.importedAt))}", fontSize = 12.sp, color = Color(0xFF64748B))

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Original", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("${dataset.originalRecordCount}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            }
                            Column {
                                Text("Active", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("${dataset.activeRecordCount}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                            Column {
                                Text("Removed", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("${dataset.removedRecordCount}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                            Column {
                                Text("Columns", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("${dataset.retainedColumns.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Dataset Management Section
            Text("DATASET MANAGEMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsActionRow(
                        title = "Create New Dataset",
                        subtitle = "Import new monthly XLS and replace current dataset",
                        icon = Icons.Default.Add,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onStartNewDatasetImport
                    )

                    if (dataset != null) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsActionRow(
                            title = "Reset Current Data",
                            subtitle = "Permanently delete current dataset, removed records, and remarks",
                            icon = Icons.Default.RestartAlt,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showResetDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Removed Records Management Section
            Text("REMOVED RECORDS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsActionRow(
                        title = "View Removed Records",
                        subtitle = "Inspect and restore previously removed consumers",
                        icon = Icons.Default.Delete,
                        iconTint = Color(0xFF64748B),
                        onClick = onViewRemovedRecords
                    )

                    if (dataset != null && dataset.removedRecordCount > 0) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsActionRow(
                            title = "Permanently Clear Removed Records",
                            subtitle = "Clear all ${dataset.removedRecordCount} removed records permanently",
                            icon = Icons.Default.DeleteForever,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showClearRemovedDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About Section
            Text("ABOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Outstanding Manager", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 1.0 (Build 1)", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Designed for personal field use by Junior Engineers of JdVVNL.\nOffline-first, secure local storage with Room SQLite.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Credits",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "build by nitesh2mithu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "build by nitesh2mithu",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Reset confirmation dialog
    if (showResetDialog && dataset != null) {
        ConfirmationDialog(
            title = "RESET DATA?",
            message = "This will permanently remove:\n• ${dataset.activeRecordCount} active records\n• ${dataset.removedRecordCount} removed records\n• All statuses and remarks\n\nThis cannot be undone.",
            confirmButtonText = "RESET DATA",
            cancelButtonText = "CANCEL",
            isDestructive = true,
            onConfirm = onResetCurrentData,
            onDismiss = { showResetDialog = false }
        )
    }

    // Clear removed confirmation dialog
    if (showClearRemovedDialog && dataset != null) {
        ConfirmationDialog(
            title = "CLEAR REMOVED RECORDS?",
            message = "This will permanently remove all ${dataset.removedRecordCount} removed records. This cannot be undone.",
            confirmButtonText = "CLEAR PERMANENTLY",
            cancelButtonText = "CANCEL",
            isDestructive = true,
            onConfirm = onPermanentlyClearRemoved,
            onDismiss = { showClearRemovedDialog = false }
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF64748B), lineHeight = 16.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
    }
}
