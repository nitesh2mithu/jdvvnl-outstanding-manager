package com.example.outstandingmanager

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.outstandingmanager.data.parser.ParsedXlsResult
import com.example.outstandingmanager.data.parser.XlsParser
import com.example.outstandingmanager.data.repository.OutstandingRepository
import com.example.outstandingmanager.export.PdfExporter
import com.example.outstandingmanager.export.PdfOrientation
import com.example.outstandingmanager.model.ConsumerRecord
import com.example.outstandingmanager.model.FilterCriteria
import com.example.outstandingmanager.model.SortOption
import com.example.outstandingmanager.ui.common.ConfirmationDialog
import com.example.outstandingmanager.ui.dashboard.DashboardScreen
import com.example.outstandingmanager.ui.details.ConsumerDetailScreen
import com.example.outstandingmanager.ui.empty.EmptyStateScreen
import com.example.outstandingmanager.ui.importflow.ColumnCleaningScreen
import com.example.outstandingmanager.ui.importflow.ImportPreviewScreen
import com.example.outstandingmanager.ui.importflow.RowCleaningScreen
import com.example.outstandingmanager.ui.importflow.SaveCleanedScreen
import com.example.outstandingmanager.ui.list.OutstandingListScreen
import com.example.outstandingmanager.ui.removed.RemovedRecordsScreen
import com.example.outstandingmanager.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

enum class AppTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    OUTSTANDING("Outstanding", Icons.AutoMirrored.Filled.FormatListBulleted),
    REMOVED("Removed", Icons.Default.Delete),
    SETTINGS("Settings", Icons.Default.Settings)
}

sealed interface AppScreenState {
    data object MainTabs : AppScreenState
    data class ConsumerDetail(val consumer: ConsumerRecord) : AppScreenState

    // Import Flow States
    data class ImportPreview(val parsedResult: ParsedXlsResult) : AppScreenState
    data class ColumnCleaning(
        val parsedResult: ParsedXlsResult,
        val selectedColumns: Set<String>
    ) : AppScreenState
    data class RowCleaning(
        val parsedResult: ParsedXlsResult,
        val retainedColumns: List<String>
    ) : AppScreenState
    data class SaveCleaned(
        val fileName: String,
        val headerRowIndex: Int,
        val retainedColumns: List<String>,
        val activeRows: List<Map<String, String>>,
        val removedRows: List<Map<String, String>>
    ) : AppScreenState
}

@Composable
fun OutstandingApp(
    repository: OutstandingRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeDataset by repository.activeDataset.collectAsStateWithLifecycle()
    val activeRecords by repository.activeRecords.collectAsStateWithLifecycle()
    val removedRecords by repository.removedRecords.collectAsStateWithLifecycle()
    val dashboardSummary by repository.dashboardSummary.collectAsStateWithLifecycle()
    val distinctFeeders by repository.distinctFeeders.collectAsStateWithLifecycle()
    val distinctTariffCodes by repository.distinctTariffCodes.collectAsStateWithLifecycle()
    val distinctAreaCodes by repository.distinctAreaCodes.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppTab.OUTSTANDING) }
    var screenState by remember { mutableStateOf<AppScreenState>(AppScreenState.MainTabs) }
    var filterCriteria by remember { mutableStateOf(FilterCriteria()) }
    var sortOption by remember { mutableStateOf(SortOption.DEFAULT) }

    var isLoading by remember { mutableStateOf(false) }
    var isSavingDataset by remember { mutableStateOf(false) }
    var showNewDatasetConfirmDialog by remember { mutableStateOf(false) }

    // Filter & Sort records
    val filteredRecords = remember(activeRecords, filterCriteria, sortOption) {
        repository.filterAndSort(activeRecords, filterCriteria, sortOption)
    }

    // Function to parse input stream into Preview
    fun parseStream(stream: InputStream, fileName: String) {
        scope.launch {
            isLoading = true
            try {
                val result = withContext(Dispatchers.IO) {
                    XlsParser.parse(stream, fileName)
                }
                screenState = AppScreenState.ImportPreview(result)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to read XLS: ${e.message ?: "Invalid file"}")
            } finally {
                isLoading = false
            }
        }
    }

    // SAF file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryFileName(context, uri) ?: "Outstanding_Import.xls"
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    parseStream(inputStream, fileName)
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Could not open selected file.") }
                }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Error opening file: ${e.message}") }
            }
        }
    }

    fun initiateImport() {
        if (activeDataset != null) {
            showNewDatasetConfirmDialog = true
        } else {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    fun loadSampleFile() {
        try {
            val assetStream = context.assets.open("sample.xls")
            parseStream(assetStream, "KNO Wise Oustanding Dues(web)_Sample.xls")
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar("Error opening sample: ${e.message}") }
        }
    }

    // New Dataset confirmation dialog when active dataset exists
    if (showNewDatasetConfirmDialog) {
        ConfirmationDialog(
            title = "CREATE NEW DATASET?",
            message = "Current dataset exists with ${activeDataset?.activeRecordCount ?: 0} active records. Creating a new dataset will replace the current active dataset once saved.",
            confirmButtonText = "PROCEED WITH IMPORT",
            cancelButtonText = "CANCEL",
            onConfirm = {
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = { showNewDatasetConfirmDialog = false }
        )
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // First launch empty state if no dataset loaded and not in import flow
    if (activeDataset == null && screenState is AppScreenState.MainTabs) {
        EmptyStateScreen(
            onImportClick = { initiateImport() },
            onLoadSampleClick = { loadSampleFile() },
            modifier = modifier
        )
        return
    }

    // Navigation and screen rendering
    AnimatedContent(targetState = screenState, label = "screen_transition") { state ->
        when (state) {
            // STEP 1: Import Preview
            is AppScreenState.ImportPreview -> {
                ImportPreviewScreen(
                    parsedResult = state.parsedResult,
                    onContinue = {
                        screenState = AppScreenState.ColumnCleaning(
                            parsedResult = state.parsedResult,
                            selectedColumns = state.parsedResult.headers.toSet()
                        )
                    },
                    onCancel = { screenState = AppScreenState.MainTabs }
                )
            }

            // STEP 2: Column Cleaning
            is AppScreenState.ColumnCleaning -> {
                ColumnCleaningScreen(
                    availableColumns = state.parsedResult.headers,
                    initialSelectedColumns = state.selectedColumns,
                    onContinue = { retained ->
                        screenState = AppScreenState.RowCleaning(
                            parsedResult = state.parsedResult,
                            retainedColumns = retained
                        )
                    },
                    onBack = {
                        screenState = AppScreenState.ImportPreview(state.parsedResult)
                    }
                )
            }

            // STEP 3: Row Cleaning
            is AppScreenState.RowCleaning -> {
                RowCleaningScreen(
                    headers = state.retainedColumns,
                    rawRows = state.parsedResult.rawRows,
                    onProceedToSave = { active, removed ->
                        screenState = AppScreenState.SaveCleaned(
                            fileName = state.parsedResult.fileName,
                            headerRowIndex = state.parsedResult.headerRowIndex,
                            retainedColumns = state.retainedColumns,
                            activeRows = active,
                            removedRows = removed
                        )
                    },
                    onBack = {
                        screenState = AppScreenState.ColumnCleaning(
                            parsedResult = state.parsedResult,
                            selectedColumns = state.retainedColumns.toSet()
                        )
                    }
                )
            }

            // STEP 4: Save Cleaned Dataset
            is AppScreenState.SaveCleaned -> {
                SaveCleanedScreen(
                    fileName = state.fileName,
                    originalCount = state.activeRows.size + state.removedRows.size,
                    removedCount = state.removedRows.size,
                    activeCount = state.activeRows.size,
                    columnsCount = state.retainedColumns.size,
                    isSaving = isSavingDataset,
                    onSaveDataset = {
                        scope.launch {
                            isSavingDataset = true
                            try {
                                repository.saveCleanedDataset(
                                    sourceFileName = state.fileName,
                                    headerRowIndex = state.headerRowIndex,
                                    retainedColumns = state.retainedColumns,
                                    activeRows = state.activeRows,
                                    removedRows = state.removedRows
                                )
                                screenState = AppScreenState.MainTabs
                                currentTab = AppTab.OUTSTANDING
                                Toast.makeText(context, "Dataset saved successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error saving dataset: ${e.message}")
                            } finally {
                                isSavingDataset = false
                            }
                        }
                    },
                    onBack = {
                        // Return to row cleaning
                        screenState = AppScreenState.MainTabs
                    }
                )
            }

            // Consumer Details Screen
            is AppScreenState.ConsumerDetail -> {
                ConsumerDetailScreen(
                    consumer = state.consumer,
                    retainedColumns = activeDataset?.retainedColumns ?: emptyList(),
                    onSaveStatusAndRemark = { status, otherText, remark ->
                        scope.launch {
                            repository.updateConsumerStatusAndRemark(
                                consumerId = state.consumer.id,
                                status = status,
                                statusOtherText = otherText,
                                remark = remark
                            )
                        }
                    },
                    onBack = { screenState = AppScreenState.MainTabs }
                )
            }

            // Main Application Screen with Bottom Navigation
            is AppScreenState.MainTabs -> {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White
                        ) {
                            AppTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title) }
                                )
                            }
                        }
                    },
                    modifier = modifier
                ) { innerPadding ->
                    when (currentTab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                dataset = activeDataset,
                                summary = dashboardSummary,
                                onNavigateToOutstandingWithFilter = { filter ->
                                    filterCriteria = filter
                                    currentTab = AppTab.OUTSTANDING
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        AppTab.OUTSTANDING -> {
                            OutstandingListScreen(
                                dataset = activeDataset,
                                records = filteredRecords,
                                filterCriteria = filterCriteria,
                                sortOption = sortOption,
                                availableFeeders = distinctFeeders,
                                availableTariffCodes = distinctTariffCodes,
                                availableAreaCodes = distinctAreaCodes,
                                onFilterChange = { filterCriteria = it },
                                onSortChange = { sortOption = it },
                                onConsumerClick = { consumer ->
                                    screenState = AppScreenState.ConsumerDetail(consumer)
                                },
                                onExportPdf = { cols, orientation ->
                                    scope.launch {
                                        try {
                                            val pdfFile = withContext(Dispatchers.IO) {
                                                PdfExporter.exportPdf(
                                                    context = context,
                                                    datasetName = activeDataset?.sourceFileName ?: "Outstanding",
                                                    records = filteredRecords,
                                                    selectedColumns = cols,
                                                    filterCriteria = filterCriteria,
                                                    orientation = orientation
                                                )
                                            }
                                            PdfExporter.sharePdf(context, pdfFile)
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("PDF Export failed: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        AppTab.REMOVED -> {
                            RemovedRecordsScreen(
                                removedRecords = removedRecords,
                                onRestoreRecords = { ids ->
                                    scope.launch {
                                        repository.restoreRecords(ids)
                                        Toast.makeText(context, "${ids.size} record(s) restored", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPermanentlyClear = {
                                    scope.launch {
                                        repository.permanentlyClearRemoved()
                                        Toast.makeText(context, "Removed records cleared permanently", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                dataset = activeDataset,
                                onStartNewDatasetImport = { initiateImport() },
                                onResetCurrentData = {
                                    scope.launch {
                                        repository.resetAllData()
                                        Toast.makeText(context, "All data reset successfully", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onViewRemovedRecords = { currentTab = AppTab.REMOVED },
                                onPermanentlyClearRemoved = {
                                    scope.launch {
                                        repository.permanentlyClearRemoved()
                                        Toast.makeText(context, "Removed records cleared", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0) {
                    return cursor.getString(nameIdx)
                }
            }
        }
    }
    return uri.path?.substringAfterLast('/')
}
