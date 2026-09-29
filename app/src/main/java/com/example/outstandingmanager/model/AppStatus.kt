package com.example.outstandingmanager.model

enum class AppStatus(val displayName: String) {
    NO_STATUS("No Status"),
    VISITED("Visited"),
    PAYMENT_PROMISED("Payment Promised"),
    PART_PAYMENT("Part Payment"),
    PAID("Paid"),
    DISCONNECTED("Disconnected"),
    DISPUTE("Dispute"),
    OTHERS("Others");

    companion object {
        val ALL_OPTIONS = listOf(
            VISITED,
            PAYMENT_PROMISED,
            PART_PAYMENT,
            PAID,
            DISCONNECTED,
            DISPUTE,
            OTHERS
        )

        fun fromString(value: String?): AppStatus {
            if (value.isNullOrBlank()) return NO_STATUS
            return entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) } ?: OTHERS
        }
    }
}
