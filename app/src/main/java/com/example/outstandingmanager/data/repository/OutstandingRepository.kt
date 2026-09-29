package com.example.outstandingmanager.data.repository

import android.content.Context
import com.example.outstandingmanager.data.db.AppDatabaseHelper
import com.example.outstandingmanager.model.AppStatus
import com.example.outstandingmanager.model.ConsumerRecord
import com.example.outstandingmanager.model.DashboardSummary
import com.example.outstandingmanager.model.Dataset
import com.example.outstandingmanager.model.FilterCriteria
import com.example.outstandingmanager.model.RemarkFilter
import com.example.outstandingmanager.model.SortOption
import com.example.outstandingmanager.ui.common.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutstandingRepository(context: Context) {

    private val dbHelper = AppDatabaseHelper.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _activeDataset = MutableStateFlow<Dataset?>(null)
    val activeDataset: StateFlow<Dataset?> = _activeDataset.asStateFlow()

    private val _activeRecords = MutableStateFlow<List<ConsumerRecord>>(emptyList())
    val activeRecords: StateFlow<List<ConsumerRecord>> = _activeRecords.asStateFlow()

    private val _removedRecords = MutableStateFlow<List<ConsumerRecord>>(emptyList())
    val removedRecords: StateFlow<List<ConsumerRecord>> = _removedRecords.asStateFlow()

    private val _dashboardSummary = MutableStateFlow(DashboardSummary())
    val dashboardSummary: StateFlow<DashboardSummary> = _dashboardSummary.asStateFlow()

    private val _distinctFeeders = MutableStateFlow<List<String>>(emptyList())
    val distinctFeeders: StateFlow<List<String>> = _distinctFeeders.asStateFlow()

    private val _distinctTariffCodes = MutableStateFlow<List<String>>(emptyList())
    val distinctTariffCodes: StateFlow<List<String>> = _distinctTariffCodes.asStateFlow()

    private val _distinctAreaCodes = MutableStateFlow<List<String>>(emptyList())
    val distinctAreaCodes: StateFlow<List<String>> = _distinctAreaCodes.asStateFlow()

    init {
        scope.launch {
            refresh()
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val dataset = dbHelper.getActiveDataset()
        _activeDataset.value = dataset

        if (dataset != null) {
            val active = dbHelper.getAllRecords(dataset.id, isRemoved = false)
            val removed = dbHelper.getAllRecords(dataset.id, isRemoved = true)
            _activeRecords.value = active
            _removedRecords.value = removed

            _dashboardSummary.value = dbHelper.getDashboardSummary(dataset.id)

            _distinctFeeders.value = active.map { it.feederName }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            _distinctTariffCodes.value = active.map { it.tariffCode }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            _distinctAreaCodes.value = active.map { it.areaCode }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        } else {
            _activeRecords.value = emptyList()
            _removedRecords.value = emptyList()
            _dashboardSummary.value = DashboardSummary()
            _distinctFeeders.value = emptyList()
            _distinctTariffCodes.value = emptyList()
            _distinctAreaCodes.value = emptyList()
        }
    }

    suspend fun getConsumerById(id: Long): ConsumerRecord? = withContext(Dispatchers.IO) {
        dbHelper.getConsumerById(id)
    }

    suspend fun updateConsumerStatusAndRemark(
        consumerId: Long,
        status: String,
        statusOtherText: String,
        remark: String
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dbHelper.updateStatusAndRemark(consumerId, status, statusOtherText, remark, now)
        refresh()
    }

    suspend fun moveRecordsToRemoved(consumerIds: List<Long>) = withContext(Dispatchers.IO) {
        val dataset = _activeDataset.value ?: return@withContext
        dbHelper.setRecordsRemovedStatus(dataset.id, consumerIds, isRemoved = true)
        refresh()
    }

    suspend fun restoreRecords(consumerIds: List<Long>) = withContext(Dispatchers.IO) {
        val dataset = _activeDataset.value ?: return@withContext
        dbHelper.setRecordsRemovedStatus(dataset.id, consumerIds, isRemoved = false)
        refresh()
    }

    suspend fun permanentlyClearRemoved() = withContext(Dispatchers.IO) {
        val dataset = _activeDataset.value ?: return@withContext
        dbHelper.clearPermanentlyRemovedRecords(dataset.id)
        refresh()
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        dbHelper.resetAllData()
        refresh()
    }

    /**
     * Converts cleaned preview data rows into ConsumerRecord objects and saves to DB
     */
    suspend fun saveCleanedDataset(
        sourceFileName: String,
        headerRowIndex: Int,
        retainedColumns: List<String>,
        activeRows: List<Map<String, String>>,
        removedRows: List<Map<String, String>>
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val totalOriginal = activeRows.size + removedRows.size
        val dataset = Dataset(
            sourceFileName = sourceFileName,
            importedAt = now,
            originalRecordCount = totalOriginal,
            activeRecordCount = activeRows.size,
            removedRecordCount = removedRows.size,
            retainedColumns = retainedColumns,
            headerRowIndex = headerRowIndex
        )

        val consumerRecords = mutableListOf<ConsumerRecord>()
        var rowId = 1

        fun processRow(row: Map<String, String>, isRemoved: Boolean) {
            val kno = getFieldValue(row, "kno", "consumer id", "consumerno")
            val accNo = getFieldValue(row, "accno", "account no", "account", "acno")
            val name = getFieldValue(row, "consumer name", "name")
            val fatherName = getFieldValue(row, "father's name", "father name", "husband name")
            val address = getFieldValue(row, "address")
            val feeder = getFieldValue(row, "feeder name", "feeder")
            val mobile = getFieldValue(row, "mobile no", "mobile")
            val tariff = getFieldValue(row, "tariffcode", "tariff")
            val cStatus = getFieldValue(row, "consumer status")
            val meterNo = getFieldValue(row, "meter no", "meterno")
            val meterStatus = getFieldValue(row, "meter status")
            val outstandingStr = getFieldValue(row, "outstanding amount", "outstanding", "total dues")
            val outstandingAmount = CurrencyFormatter.parseAmount(outstandingStr)
            val lastReceivedStr = getFieldValue(row, "last received amount", "last received")
            val lastReceived = if (lastReceivedStr.isNotBlank()) CurrencyFormatter.parseAmount(lastReceivedStr) else null
            val lastPaid = getFieldValue(row, "last_paid", "last paid")
            val duesSince = getFieldValue(row, "dues since", "dues_since")
            val billingMonth = getFieldValue(row, "billing month")
            val fiCode = getFieldValue(row, "fi_code", "fi code")
            val areaCode = getFieldValue(row, "areacode", "area code")
            val phase = getFieldValue(row, "phase")
            val deptName = getFieldValue(row, "dept name", "department")
            val govtNonGovt = getFieldValue(row, "govt/non govt", "govt")
            val smartNonSmart = getFieldValue(row, "smart/non smart", "smart")

            consumerRecords.add(
                ConsumerRecord(
                    datasetId = 0,
                    sourceRowId = rowId++,
                    kno = kno,
                    accountNo = accNo,
                    consumerName = name,
                    fatherName = fatherName,
                    address = address,
                    feederName = feeder,
                    mobileNo = mobile,
                    tariffCode = tariff,
                    consumerStatus = cStatus,
                    meterNo = meterNo,
                    meterStatus = meterStatus,
                    outstandingAmount = outstandingAmount,
                    lastReceivedAmount = lastReceived,
                    lastPaidDate = lastPaid,
                    duesSince = duesSince,
                    billingMonth = billingMonth,
                    fiCode = fiCode,
                    areaCode = areaCode,
                    phase = phase,
                    deptName = deptName,
                    govtNonGovt = govtNonGovt,
                    smartNonSmart = smartNonSmart,
                    dynamicFields = row,
                    status = "",
                    statusOtherText = "",
                    remark = "",
                    lastUpdated = 0L,
                    isRemoved = isRemoved
                )
            )
        }

        for (row in activeRows) {
            processRow(row, isRemoved = false)
        }
        for (row in removedRows) {
            processRow(row, isRemoved = true)
        }

        val id = dbHelper.insertDatasetWithRecords(dataset, consumerRecords)
        refresh()
        id
    }

    private fun getFieldValue(row: Map<String, String>, vararg possibleNames: String): String {
        for (p in possibleNames) {
            val key = row.keys.firstOrNull { it.trim().equals(p, ignoreCase = true) }
            if (key != null) {
                val v = row[key]
                if (!v.isNullOrBlank()) return v.trim()
            }
        }
        return ""
    }

    /**
     * Filters and sorts consumer records in-memory with sub-millisecond latency
     */
    fun filterAndSort(
        records: List<ConsumerRecord>,
        criteria: FilterCriteria,
        sortOption: SortOption
    ): List<ConsumerRecord> {
        val q = criteria.query.trim().lowercase()
        val effMin = criteria.effectiveMinAmount

        val filtered = records.filter { r ->
            // Search query across KNO, Accno, Name, Address, Mobile, Feeder
            if (q.isNotEmpty()) {
                val matches = r.kno.lowercase().contains(q) ||
                        r.accountNo.lowercase().contains(q) ||
                        r.consumerName.lowercase().contains(q) ||
                        r.address.lowercase().contains(q) ||
                        r.mobileNo.lowercase().contains(q) ||
                        r.feederName.lowercase().contains(q)
                if (!matches) return@filter false
            }

            // Amount filtering
            if (effMin != null && r.outstandingAmount < effMin) {
                return@filter false
            }
            if (criteria.maxAmount != null && r.outstandingAmount > criteria.maxAmount) {
                return@filter false
            }

            // Feeder
            if (!criteria.feeder.isNullOrBlank() && !r.feederName.equals(criteria.feeder, ignoreCase = true)) {
                return@filter false
            }

            // Consumer Status
            if (!criteria.consumerStatus.isNullOrBlank() && !r.consumerStatus.equals(criteria.consumerStatus, ignoreCase = true)) {
                return@filter false
            }

            // Meter Status
            if (!criteria.meterStatus.isNullOrBlank() && !r.meterStatus.equals(criteria.meterStatus, ignoreCase = true)) {
                return@filter false
            }

            // Tariff Code
            if (!criteria.tariffCode.isNullOrBlank() && !r.tariffCode.equals(criteria.tariffCode, ignoreCase = true)) {
                return@filter false
            }

            // Area Code
            if (!criteria.areaCode.isNullOrBlank() && !r.areaCode.equals(criteria.areaCode, ignoreCase = true)) {
                return@filter false
            }

            // FI Code
            if (!criteria.fiCode.isNullOrBlank() && !r.fiCode.equals(criteria.fiCode, ignoreCase = true)) {
                return@filter false
            }

            // Phase
            if (!criteria.phase.isNullOrBlank() && !r.phase.equals(criteria.phase, ignoreCase = true)) {
                return@filter false
            }

            // Govt / Non-Govt
            if (!criteria.govtNonGovt.isNullOrBlank() && !r.govtNonGovt.equals(criteria.govtNonGovt, ignoreCase = true)) {
                return@filter false
            }

            // Smart / Non-Smart
            if (!criteria.smartNonSmart.isNullOrBlank() && !r.smartNonSmart.equals(criteria.smartNonSmart, ignoreCase = true)) {
                return@filter false
            }

            // App Status
            if (!criteria.appStatus.isNullOrBlank()) {
                val currentStatus = if (r.status.isBlank()) AppStatus.NO_STATUS.displayName else r.status
                if (!currentStatus.equals(criteria.appStatus, ignoreCase = true)) {
                    return@filter false
                }
            }

            // Remark filter
            when (criteria.remarkFilter) {
                RemarkFilter.ALL -> true
                RemarkFilter.WITH_REMARK -> r.remark.isNotBlank()
                RemarkFilter.WITHOUT_REMARK -> r.remark.isBlank()
            }
        }

        // Sorting
        return when (sortOption) {
            SortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.outstandingAmount }
            SortOption.AMOUNT_ASC -> filtered.sortedBy { it.outstandingAmount }
            SortOption.NAME_ASC -> filtered.sortedBy { it.consumerName.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.consumerName.lowercase() }
            SortOption.KNO -> filtered.sortedBy { it.kno }
            SortOption.LAST_PAID_DATE -> filtered.sortedByDescending { it.lastPaidDate }
            SortOption.DUES_SINCE -> filtered.sortedByDescending { it.duesSince }
            SortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.lastUpdated }
        }
    }
}
