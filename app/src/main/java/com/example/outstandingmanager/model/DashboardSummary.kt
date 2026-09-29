package com.example.outstandingmanager.model

data class DashboardSummary(
    val totalConsumers: Int = 0,
    val totalOutstanding: Double = 0.0,
    val averageOutstanding: Double = 0.0,
    val highestOutstanding: Double = 0.0,
    val bucket5kCount: Int = 0,
    val bucket10kCount: Int = 0,
    val bucket25kCount: Int = 0,
    val bucket50kCount: Int = 0,
    val bucket1LakhCount: Int = 0,
    val statusCounts: Map<String, Int> = emptyMap()
)
