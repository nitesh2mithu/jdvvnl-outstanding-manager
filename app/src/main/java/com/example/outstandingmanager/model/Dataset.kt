package com.example.outstandingmanager.model

data class Dataset(
    val id: Long = 0,
    val sourceFileName: String,
    val importedAt: Long,
    val originalRecordCount: Int,
    val activeRecordCount: Int,
    val removedRecordCount: Int,
    val retainedColumns: List<String>,
    val headerRowIndex: Int = 0
)
