package com.example.outstandingmanager.model

data class ConsumerRecord(
    val id: Long = 0,
    val datasetId: Long,
    val sourceRowId: Int,
    val kno: String,
    val accountNo: String,
    val consumerName: String,
    val fatherName: String = "",
    val address: String = "",
    val feederName: String = "",
    val mobileNo: String = "",
    val tariffCode: String = "",
    val consumerStatus: String = "",
    val meterNo: String = "",
    val meterStatus: String = "",
    val outstandingAmount: Double = 0.0,
    val lastReceivedAmount: Double? = null,
    val lastPaidDate: String = "",
    val duesSince: String = "",
    val billingMonth: String = "",
    val fiCode: String = "",
    val areaCode: String = "",
    val phase: String = "",
    val deptName: String = "",
    val govtNonGovt: String = "",
    val smartNonSmart: String = "",
    val dynamicFields: Map<String, String> = emptyMap(),
    val status: String = "",
    val statusOtherText: String = "",
    val remark: String = "",
    val lastUpdated: Long = 0L,
    val isRemoved: Boolean = false
) {
    val displayStatus: String
        get() {
            if (status.equals(AppStatus.OTHERS.displayName, ignoreCase = true) && statusOtherText.isNotBlank()) {
                return statusOtherText
            }
            return if (status.isNotBlank()) status else AppStatus.NO_STATUS.displayName
        }

    val primaryIdentifier: String
        get() = kno.ifBlank { accountNo }
}
