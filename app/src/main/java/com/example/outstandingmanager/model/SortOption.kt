package com.example.outstandingmanager.model

enum class SortOption(val displayName: String) {
    AMOUNT_DESC("Outstanding Amount (Highest first)"),
    AMOUNT_ASC("Outstanding Amount (Lowest first)"),
    NAME_ASC("Consumer Name (A - Z)"),
    NAME_DESC("Consumer Name (Z - A)"),
    KNO("KNO"),
    LAST_PAID_DATE("Last Payment Date"),
    DUES_SINCE("Dues Since"),
    RECENTLY_UPDATED("Recently Updated");

    companion object {
        val DEFAULT = AMOUNT_DESC
    }
}
