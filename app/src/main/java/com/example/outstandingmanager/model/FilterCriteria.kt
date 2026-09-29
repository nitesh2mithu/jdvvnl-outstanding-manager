package com.example.outstandingmanager.model

enum class RemarkFilter(val displayName: String) {
    ALL("All Records"),
    WITH_REMARK("With Remark"),
    WITHOUT_REMARK("Without Remark")
}

data class FilterCriteria(
    val query: String = "",
    val presetAmount: Double? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val feeder: String? = null,
    val consumerStatus: String? = null,
    val meterStatus: String? = null,
    val tariffCode: String? = null,
    val fiCode: String? = null,
    val areaCode: String? = null,
    val phase: String? = null,
    val govtNonGovt: String? = null,
    val smartNonSmart: String? = null,
    val appStatus: String? = null,
    val remarkFilter: RemarkFilter = RemarkFilter.ALL
) {
    val effectiveMinAmount: Double?
        get() {
            if (minAmount != null) return minAmount
            return presetAmount
        }

    val isActive: Boolean
        get() = query.isNotBlank() ||
                presetAmount != null ||
                minAmount != null ||
                maxAmount != null ||
                !feeder.isNullOrBlank() ||
                !consumerStatus.isNullOrBlank() ||
                !meterStatus.isNullOrBlank() ||
                !tariffCode.isNullOrBlank() ||
                !fiCode.isNullOrBlank() ||
                !areaCode.isNullOrBlank() ||
                !phase.isNullOrBlank() ||
                !govtNonGovt.isNullOrBlank() ||
                !smartNonSmart.isNullOrBlank() ||
                !appStatus.isNullOrBlank() ||
                remarkFilter != RemarkFilter.ALL

    fun activeFilterCount(): Int {
        var count = 0
        if (effectiveMinAmount != null) count++
        if (maxAmount != null) count++
        if (!feeder.isNullOrBlank()) count++
        if (!consumerStatus.isNullOrBlank()) count++
        if (!meterStatus.isNullOrBlank()) count++
        if (!tariffCode.isNullOrBlank()) count++
        if (!fiCode.isNullOrBlank()) count++
        if (!areaCode.isNullOrBlank()) count++
        if (!phase.isNullOrBlank()) count++
        if (!govtNonGovt.isNullOrBlank()) count++
        if (!smartNonSmart.isNullOrBlank()) count++
        if (!appStatus.isNullOrBlank()) count++
        if (remarkFilter != RemarkFilter.ALL) count++
        return count
    }
}
