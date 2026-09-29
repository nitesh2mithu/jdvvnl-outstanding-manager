package com.example.outstandingmanager.data.repository

import com.example.outstandingmanager.model.AppStatus
import com.example.outstandingmanager.model.ConsumerRecord
import com.example.outstandingmanager.model.FilterCriteria
import com.example.outstandingmanager.model.RemarkFilter
import com.example.outstandingmanager.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterAndSortTest {

    private val sampleRecords = listOf(
        ConsumerRecord(
            id = 1,
            datasetId = 1,
            sourceRowId = 1,
            kno = "320424032284",
            accountNo = "90280001",
            consumerName = "MS Indian Oil Corporation",
            address = "Village Bandi Rohat, Pali",
            feederName = "IOC MORIYA FEEDER",
            mobileNo = "9617520509",
            outstandingAmount = 1765872.0,
            status = AppStatus.PAYMENT_PROMISED.displayName,
            remark = "Promised by 5th"
        ),
        ConsumerRecord(
            id = 2,
            datasetId = 1,
            sourceRowId = 2,
            kno = "320424028981",
            accountNo = "90240001",
            consumerName = "MS Shree Industries",
            address = "Kharda, Rohat",
            feederName = "CHOTA RUNECHA",
            mobileNo = "9509457639",
            outstandingAmount = 332990.0,
            status = AppStatus.VISITED.displayName,
            remark = ""
        ),
        ConsumerRecord(
            id = 3,
            datasetId = 1,
            sourceRowId = 3,
            kno = "320424027666",
            accountNo = "90200001",
            consumerName = "MS Abhinav Stones",
            address = "Gajangarh",
            feederName = "CHOTA RUNECHA",
            mobileNo = "9784454053",
            outstandingAmount = 31365.0,
            status = AppStatus.PAID.displayName,
            remark = "Paid via portal"
        ),
        ConsumerRecord(
            id = 4,
            datasetId = 1,
            sourceRowId = 4,
            kno = "320424011111",
            accountNo = "90110001",
            consumerName = "Bhima Ram",
            address = "Rohat",
            feederName = "SAWAIPURA FEEDER",
            mobileNo = "9829000000",
            outstandingAmount = 7500.0,
            status = "",
            remark = ""
        )
    )

    // Standalone filter/sort helper function matching repository logic
    private fun filterAndSort(
        records: List<ConsumerRecord>,
        criteria: FilterCriteria,
        sortOption: SortOption
    ): List<ConsumerRecord> {
        val q = criteria.query.trim().lowercase()
        val effMin = criteria.effectiveMinAmount

        val filtered = records.filter { r ->
            if (q.isNotEmpty()) {
                val matches = r.kno.lowercase().contains(q) ||
                        r.accountNo.lowercase().contains(q) ||
                        r.consumerName.lowercase().contains(q) ||
                        r.address.lowercase().contains(q) ||
                        r.mobileNo.lowercase().contains(q) ||
                        r.feederName.lowercase().contains(q)
                if (!matches) return@filter false
            }

            if (effMin != null && r.outstandingAmount < effMin) return@filter false
            if (criteria.maxAmount != null && r.outstandingAmount > criteria.maxAmount) return@filter false

            if (!criteria.feeder.isNullOrBlank() && !r.feederName.equals(criteria.feeder, ignoreCase = true)) return@filter false
            if (!criteria.appStatus.isNullOrBlank()) {
                val currentStatus = if (r.status.isBlank()) AppStatus.NO_STATUS.displayName else r.status
                if (!currentStatus.equals(criteria.appStatus, ignoreCase = true)) return@filter false
            }

            when (criteria.remarkFilter) {
                RemarkFilter.ALL -> true
                RemarkFilter.WITH_REMARK -> r.remark.isNotBlank()
                RemarkFilter.WITHOUT_REMARK -> r.remark.isBlank()
            }
        }

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

    @Test
    fun testAmountPresetFiltering() {
        val res50k = filterAndSort(sampleRecords, FilterCriteria(presetAmount = 50000.0), SortOption.AMOUNT_DESC)
        assertEquals(2, res50k.size) // 1765872, 332990

        val res10k = filterAndSort(sampleRecords, FilterCriteria(presetAmount = 10000.0), SortOption.AMOUNT_DESC)
        assertEquals(3, res10k.size) // 1765872, 332990, 31365
    }

    @Test
    fun testCombinedFilter() {
        // Amount > 10,000 AND Status = Payment Promised
        val criteria = FilterCriteria(
            presetAmount = 10000.0,
            appStatus = AppStatus.PAYMENT_PROMISED.displayName
        )
        val result = filterAndSort(sampleRecords, criteria, SortOption.AMOUNT_DESC)
        assertEquals(1, result.size)
        assertEquals("MS Indian Oil Corporation", result[0].consumerName)
    }

    @Test
    fun testSearchQuery() {
        val byKno = filterAndSort(sampleRecords, FilterCriteria(query = "028981"), SortOption.DEFAULT)
        assertEquals(1, byKno.size)
        assertEquals("MS Shree Industries", byKno[0].consumerName)

        val byFeeder = filterAndSort(sampleRecords, FilterCriteria(query = "RUNECHA"), SortOption.DEFAULT)
        assertEquals(2, byFeeder.size)
    }

    @Test
    fun testSorting() {
        val byNameAsc = filterAndSort(sampleRecords, FilterCriteria(), SortOption.NAME_ASC)
        assertEquals("Bhima Ram", byNameAsc[0].consumerName)
        assertEquals("MS Shree Industries", byNameAsc.last().consumerName)

        val byAmountAsc = filterAndSort(sampleRecords, FilterCriteria(), SortOption.AMOUNT_ASC)
        assertEquals(7500.0, byAmountAsc[0].outstandingAmount, 0.001)
        assertEquals(1765872.0, byAmountAsc.last().outstandingAmount, 0.001)
    }

    @Test
    fun testRemarkFilter() {
        val withRemark = filterAndSort(sampleRecords, FilterCriteria(remarkFilter = RemarkFilter.WITH_REMARK), SortOption.DEFAULT)
        assertEquals(2, withRemark.size)

        val withoutRemark = filterAndSort(sampleRecords, FilterCriteria(remarkFilter = RemarkFilter.WITHOUT_REMARK), SortOption.DEFAULT)
        assertEquals(2, withoutRemark.size)
    }

    @Test
    fun testStatusOthersWithCustomReason() {
        val customRecord = ConsumerRecord(
            id = 5,
            datasetId = 1,
            sourceRowId = 5,
            kno = "320424099999",
            accountNo = "90990001",
            consumerName = "Prem Singh",
            outstandingAmount = 45000.0,
            status = AppStatus.OTHERS.displayName,
            statusOtherText = "Meter Burned"
        )
        val allWithCustom = sampleRecords + customRecord

        val othersFiltered = filterAndSort(allWithCustom, FilterCriteria(appStatus = AppStatus.OTHERS.displayName), SortOption.DEFAULT)
        assertEquals(1, othersFiltered.size)
        assertEquals("Meter Burned", othersFiltered[0].displayStatus)
    }

    @Test
    fun testMinMaxRangeFilter() {
        // Between 10,000 and 500,000
        val rangeCriteria = FilterCriteria(
            minAmount = 10000.0,
            maxAmount = 500000.0
        )
        val result = filterAndSort(sampleRecords, rangeCriteria, SortOption.AMOUNT_DESC)
        assertEquals(2, result.size) // 332990 and 31365
        assertTrue(result.all { it.outstandingAmount in 10000.0..500000.0 })
    }

    @Test
    fun testFeederFiltering() {
        val feederCriteria = FilterCriteria(feeder = "CHOTA RUNECHA")
        val result = filterAndSort(sampleRecords, feederCriteria, SortOption.DEFAULT)
        assertEquals(2, result.size)
        assertTrue(result.all { it.feederName == "CHOTA RUNECHA" })
    }

    @Test
    fun testExtremeAmountsAndBlankFields() {
        val extremeRecord = ConsumerRecord(
            id = 10,
            datasetId = 1,
            sourceRowId = 10,
            kno = "",
            accountNo = "",
            consumerName = "",
            outstandingAmount = 99999999.0
        )
        val list = sampleRecords + extremeRecord
        val result = filterAndSort(list, FilterCriteria(presetAmount = 100000.0), SortOption.AMOUNT_DESC)
        assertEquals(3, result.size)
        assertEquals(99999999.0, result[0].outstandingAmount, 0.001)
    }

    @Test
    fun testAllSortOptions() {
        for (option in SortOption.entries) {
            val sorted = filterAndSort(sampleRecords, FilterCriteria(), option)
            assertEquals(sampleRecords.size, sorted.size)
        }
    }
}
