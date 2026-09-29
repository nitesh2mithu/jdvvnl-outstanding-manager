package com.example.outstandingmanager.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.outstandingmanager.model.AppStatus
import com.example.outstandingmanager.model.ConsumerRecord
import com.example.outstandingmanager.model.DashboardSummary
import com.example.outstandingmanager.model.Dataset
import com.example.outstandingmanager.model.FilterCriteria
import com.example.outstandingmanager.model.RemarkFilter
import com.example.outstandingmanager.model.SortOption
import org.json.JSONArray
import org.json.JSONObject

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "outstanding_manager.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_DATASETS = "datasets"
        const val TABLE_CONSUMERS = "consumers"

        @Volatile
        private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_DATASETS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_file_name TEXT NOT NULL,
                imported_at INTEGER NOT NULL,
                original_count INTEGER NOT NULL,
                active_count INTEGER NOT NULL,
                removed_count INTEGER NOT NULL,
                columns_json TEXT NOT NULL,
                header_row_index INTEGER NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CONSUMERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dataset_id INTEGER NOT NULL,
                source_row_id INTEGER NOT NULL,
                kno TEXT NOT NULL,
                account_no TEXT NOT NULL,
                consumer_name TEXT NOT NULL,
                father_name TEXT,
                address TEXT,
                feeder_name TEXT,
                mobile_no TEXT,
                tariff_code TEXT,
                consumer_status TEXT,
                meter_no TEXT,
                meter_status TEXT,
                outstanding_amount REAL NOT NULL,
                last_received_amount REAL,
                last_paid_date TEXT,
                dues_since TEXT,
                billing_month TEXT,
                fi_code TEXT,
                area_code TEXT,
                phase TEXT,
                dept_name TEXT,
                govt_non_govt TEXT,
                smart_non_smart TEXT,
                dynamic_fields_json TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT '',
                status_other_text TEXT NOT NULL DEFAULT '',
                remark TEXT NOT NULL DEFAULT '',
                last_updated INTEGER NOT NULL DEFAULT 0,
                is_removed INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // Indexes for fast searching and filtering
        db.execSQL("CREATE INDEX idx_consumers_dataset_removed ON $TABLE_CONSUMERS(dataset_id, is_removed)")
        db.execSQL("CREATE INDEX idx_consumers_amount ON $TABLE_CONSUMERS(outstanding_amount)")
        db.execSQL("CREATE INDEX idx_consumers_kno ON $TABLE_CONSUMERS(kno)")
        db.execSQL("CREATE INDEX idx_consumers_account ON $TABLE_CONSUMERS(account_no)")
        db.execSQL("CREATE INDEX idx_consumers_name ON $TABLE_CONSUMERS(consumer_name)")
        db.execSQL("CREATE INDEX idx_consumers_feeder ON $TABLE_CONSUMERS(feeder_name)")
        db.execSQL("CREATE INDEX idx_consumers_status ON $TABLE_CONSUMERS(status)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONSUMERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DATASETS")
        onCreate(db)
    }

    /**
     * Inserts a new dataset and all its consumer records in an atomic transaction
     */
    fun insertDatasetWithRecords(
        dataset: Dataset,
        records: List<ConsumerRecord>
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Mark all older datasets as inactive
            db.execSQL("UPDATE $TABLE_DATASETS SET is_active = 0")

            val columnsArray = JSONArray()
            dataset.retainedColumns.forEach { columnsArray.put(it) }

            val cvDataset = ContentValues().apply {
                put("source_file_name", dataset.sourceFileName)
                put("imported_at", dataset.importedAt)
                put("original_count", dataset.originalRecordCount)
                put("active_count", dataset.activeRecordCount)
                put("removed_count", dataset.removedRecordCount)
                put("columns_json", columnsArray.toString())
                put("header_row_index", dataset.headerRowIndex)
                put("is_active", 1)
            }
            val datasetId = db.insert(TABLE_DATASETS, null, cvDataset)

            // Insert records
            for (r in records) {
                val cvRec = ContentValues().apply {
                    put("dataset_id", datasetId)
                    put("source_row_id", r.sourceRowId)
                    put("kno", r.kno)
                    put("account_no", r.accountNo)
                    put("consumer_name", r.consumerName)
                    put("father_name", r.fatherName)
                    put("address", r.address)
                    put("feeder_name", r.feederName)
                    put("mobile_no", r.mobileNo)
                    put("tariff_code", r.tariffCode)
                    put("consumer_status", r.consumerStatus)
                    put("meter_no", r.meterNo)
                    put("meter_status", r.meterStatus)
                    put("outstanding_amount", r.outstandingAmount)
                    if (r.lastReceivedAmount != null) {
                        put("last_received_amount", r.lastReceivedAmount)
                    } else {
                        putNull("last_received_amount")
                    }
                    put("last_paid_date", r.lastPaidDate)
                    put("dues_since", r.duesSince)
                    put("billing_month", r.billingMonth)
                    put("fi_code", r.fiCode)
                    put("area_code", r.areaCode)
                    put("phase", r.phase)
                    put("dept_name", r.deptName)
                    put("govt_non_govt", r.govtNonGovt)
                    put("smart_non_smart", r.smartNonSmart)

                    val dynObj = JSONObject()
                    r.dynamicFields.forEach { (k, v) -> dynObj.put(k, v) }
                    put("dynamic_fields_json", dynObj.toString())

                    put("status", r.status)
                    put("status_other_text", r.statusOtherText)
                    put("remark", r.remark)
                    put("last_updated", r.lastUpdated)
                    put("is_removed", if (r.isRemoved) 1 else 0)
                }
                db.insert(TABLE_CONSUMERS, null, cvRec)
            }

            db.setTransactionSuccessful()
            return datasetId
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Gets the currently active dataset metadata
     */
    fun getActiveDataset(): Dataset? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_DATASETS WHERE is_active = 1 ORDER BY id DESC LIMIT 1",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow("id"))
                val name = it.getString(it.getColumnIndexOrThrow("source_file_name"))
                val importedAt = it.getLong(it.getColumnIndexOrThrow("imported_at"))
                val originalCount = it.getInt(it.getColumnIndexOrThrow("original_count"))
                val activeCount = it.getInt(it.getColumnIndexOrThrow("active_count"))
                val removedCount = it.getInt(it.getColumnIndexOrThrow("removed_count"))
                val colJson = it.getString(it.getColumnIndexOrThrow("columns_json"))
                val headerRowIndex = it.getInt(it.getColumnIndexOrThrow("header_row_index"))

                val cols = mutableListOf<String>()
                val arr = JSONArray(colJson)
                for (i in 0 until arr.length()) {
                    cols.add(arr.getString(i))
                }

                return Dataset(
                    id = id,
                    sourceFileName = name,
                    importedAt = importedAt,
                    originalRecordCount = originalCount,
                    activeRecordCount = activeCount,
                    removedRecordCount = removedCount,
                    retainedColumns = cols,
                    headerRowIndex = headerRowIndex
                )
            }
        }
        return null
    }

    /**
     * Loads consumer record by ID
     */
    fun getConsumerById(id: Long): ConsumerRecord? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CONSUMERS WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return mapCursorToConsumer(it)
            }
        }
        return null
    }

    /**
     * Gets all records for a dataset
     */
    fun getAllRecords(datasetId: Long, isRemoved: Boolean): List<ConsumerRecord> {
        val db = readableDatabase
        val removedFlag = if (isRemoved) 1 else 0
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CONSUMERS WHERE dataset_id = ? AND is_removed = ? ORDER BY outstanding_amount DESC",
            arrayOf(datasetId.toString(), removedFlag.toString())
        )
        val list = mutableListOf<ConsumerRecord>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(mapCursorToConsumer(it))
            }
        }
        return list
    }

    /**
     * Updates status and remark for a consumer
     */
    fun updateStatusAndRemark(
        consumerId: Long,
        status: String,
        statusOtherText: String,
        remark: String,
        timestamp: Long
    ) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("status", status)
            put("status_other_text", statusOtherText)
            put("remark", remark)
            put("last_updated", timestamp)
        }
        db.update(TABLE_CONSUMERS, cv, "id = ?", arrayOf(consumerId.toString()))
    }

    /**
     * Marks consumers as removed or restored, and updates counts in dataset
     */
    fun setRecordsRemovedStatus(datasetId: Long, consumerIds: List<Long>, isRemoved: Boolean) {
        if (consumerIds.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val removedVal = if (isRemoved) 1 else 0
            val chunks = consumerIds.chunked(500)
            for (chunk in chunks) {
                val placeholders = chunk.joinToString(",") { "?" }
                val args = mutableListOf<String>()
                args.add(removedVal.toString())
                chunk.forEach { args.add(it.toString()) }
                db.execSQL("UPDATE $TABLE_CONSUMERS SET is_removed = ? WHERE id IN ($placeholders)", args.toTypedArray())
            }

            // Update dataset counts
            updateDatasetCounts(db, datasetId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Permanently deletes all removed records for dataset
     */
    fun clearPermanentlyRemovedRecords(datasetId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_CONSUMERS, "dataset_id = ? AND is_removed = 1", arrayOf(datasetId.toString()))
            updateDatasetCounts(db, datasetId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Completely resets all datasets and consumers
     */
    fun resetAllData() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_CONSUMERS, null, null)
            db.delete(TABLE_DATASETS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun updateDatasetCounts(db: SQLiteDatabase, datasetId: Long) {
        var activeCount = 0
        var removedCount = 0

        val cur = db.rawQuery(
            "SELECT is_removed, COUNT(*) FROM $TABLE_CONSUMERS WHERE dataset_id = ? GROUP BY is_removed",
            arrayOf(datasetId.toString())
        )
        cur.use {
            while (it.moveToNext()) {
                val flag = it.getInt(0)
                val count = it.getInt(1)
                if (flag == 0) activeCount = count
                else removedCount = count
            }
        }

        val cv = ContentValues().apply {
            put("active_count", activeCount)
            put("removed_count", removedCount)
        }
        db.update(TABLE_DATASETS, cv, "id = ?", arrayOf(datasetId.toString()))
    }

    /**
     * Computes dashboard summary directly using SQL aggregation
     */
    fun getDashboardSummary(datasetId: Long): DashboardSummary {
        val db = readableDatabase
        var totalConsumers = 0
        var totalOutstanding = 0.0
        var avgOutstanding = 0.0
        var highestOutstanding = 0.0
        var b5k = 0
        var b10k = 0
        var b25k = 0
        var b50k = 0
        var b1L = 0
        val statusCounts = mutableMapOf<String, Int>()

        // Initialize status counts with 0
        for (st in AppStatus.entries) {
            statusCounts[st.displayName] = 0
        }

        // Aggregate KPIs
        val cursor = db.rawQuery(
            """
            SELECT 
                COUNT(*),
                COALESCE(SUM(outstanding_amount), 0),
                COALESCE(AVG(outstanding_amount), 0),
                COALESCE(MAX(outstanding_amount), 0)
            FROM $TABLE_CONSUMERS
            WHERE dataset_id = ? AND is_removed = 0
            """.trimIndent(),
            arrayOf(datasetId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                totalConsumers = it.getInt(0)
                totalOutstanding = it.getDouble(1)
                avgOutstanding = it.getDouble(2)
                highestOutstanding = it.getDouble(3)
            }
        }

        if (totalConsumers > 0) {
            // Bucket counts
            val bucketCur = db.rawQuery(
                """
                SELECT
                    SUM(CASE WHEN outstanding_amount >= 5000 THEN 1 ELSE 0 END),
                    SUM(CASE WHEN outstanding_amount >= 10000 THEN 1 ELSE 0 END),
                    SUM(CASE WHEN outstanding_amount >= 25000 THEN 1 ELSE 0 END),
                    SUM(CASE WHEN outstanding_amount >= 50000 THEN 1 ELSE 0 END),
                    SUM(CASE WHEN outstanding_amount >= 100000 THEN 1 ELSE 0 END)
                FROM $TABLE_CONSUMERS
                WHERE dataset_id = ? AND is_removed = 0
                """.trimIndent(),
                arrayOf(datasetId.toString())
            )
            bucketCur.use {
                if (it.moveToFirst()) {
                    b5k = it.getInt(0)
                    b10k = it.getInt(1)
                    b25k = it.getInt(2)
                    b50k = it.getInt(3)
                    b1L = it.getInt(4)
                }
            }

            // Status counts
            val statusCur = db.rawQuery(
                """
                SELECT status, COUNT(*)
                FROM $TABLE_CONSUMERS
                WHERE dataset_id = ? AND is_removed = 0
                GROUP BY status
                """.trimIndent(),
                arrayOf(datasetId.toString())
            )
            statusCur.use {
                while (it.moveToNext()) {
                    val rawStatus = it.getString(0) ?: ""
                    val count = it.getInt(1)
                    val appStatus = AppStatus.fromString(rawStatus)
                    statusCounts[appStatus.displayName] = (statusCounts[appStatus.displayName] ?: 0) + count
                }
            }
        }

        return DashboardSummary(
            totalConsumers = totalConsumers,
            totalOutstanding = totalOutstanding,
            averageOutstanding = avgOutstanding,
            highestOutstanding = highestOutstanding,
            bucket5kCount = b5k,
            bucket10kCount = b10k,
            bucket25kCount = b25k,
            bucket50kCount = b50k,
            bucket1LakhCount = b1L,
            statusCounts = statusCounts
        )
    }

    private fun mapCursorToConsumer(it: Cursor): ConsumerRecord {
        val dynJson = it.getString(it.getColumnIndexOrThrow("dynamic_fields_json")) ?: "{}"
        val dynMap = mutableMapOf<String, String>()
        try {
            val json = JSONObject(dynJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                dynMap[k] = json.optString(k, "")
            }
        } catch (_: Exception) {}

        return ConsumerRecord(
            id = it.getLong(it.getColumnIndexOrThrow("id")),
            datasetId = it.getLong(it.getColumnIndexOrThrow("dataset_id")),
            sourceRowId = it.getInt(it.getColumnIndexOrThrow("source_row_id")),
            kno = it.getString(it.getColumnIndexOrThrow("kno")) ?: "",
            accountNo = it.getString(it.getColumnIndexOrThrow("account_no")) ?: "",
            consumerName = it.getString(it.getColumnIndexOrThrow("consumer_name")) ?: "",
            fatherName = it.getString(it.getColumnIndexOrThrow("father_name")) ?: "",
            address = it.getString(it.getColumnIndexOrThrow("address")) ?: "",
            feederName = it.getString(it.getColumnIndexOrThrow("feeder_name")) ?: "",
            mobileNo = it.getString(it.getColumnIndexOrThrow("mobile_no")) ?: "",
            tariffCode = it.getString(it.getColumnIndexOrThrow("tariff_code")) ?: "",
            consumerStatus = it.getString(it.getColumnIndexOrThrow("consumer_status")) ?: "",
            meterNo = it.getString(it.getColumnIndexOrThrow("meter_no")) ?: "",
            meterStatus = it.getString(it.getColumnIndexOrThrow("meter_status")) ?: "",
            outstandingAmount = it.getDouble(it.getColumnIndexOrThrow("outstanding_amount")),
            lastReceivedAmount = if (it.isNull(it.getColumnIndexOrThrow("last_received_amount"))) null else it.getDouble(it.getColumnIndexOrThrow("last_received_amount")),
            lastPaidDate = it.getString(it.getColumnIndexOrThrow("last_paid_date")) ?: "",
            duesSince = it.getString(it.getColumnIndexOrThrow("dues_since")) ?: "",
            billingMonth = it.getString(it.getColumnIndexOrThrow("billing_month")) ?: "",
            fiCode = it.getString(it.getColumnIndexOrThrow("fi_code")) ?: "",
            areaCode = it.getString(it.getColumnIndexOrThrow("area_code")) ?: "",
            phase = it.getString(it.getColumnIndexOrThrow("phase")) ?: "",
            deptName = it.getString(it.getColumnIndexOrThrow("dept_name")) ?: "",
            govtNonGovt = it.getString(it.getColumnIndexOrThrow("govt_non_govt")) ?: "",
            smartNonSmart = it.getString(it.getColumnIndexOrThrow("smart_non_smart")) ?: "",
            dynamicFields = dynMap,
            status = it.getString(it.getColumnIndexOrThrow("status")) ?: "",
            statusOtherText = it.getString(it.getColumnIndexOrThrow("status_other_text")) ?: "",
            remark = it.getString(it.getColumnIndexOrThrow("remark")) ?: "",
            lastUpdated = it.getLong(it.getColumnIndexOrThrow("last_updated")),
            isRemoved = it.getInt(it.getColumnIndexOrThrow("is_removed")) == 1
        )
    }
}
