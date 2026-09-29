package com.example.outstandingmanager.ui.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.outstandingmanager.model.AppStatus
import com.example.outstandingmanager.model.ConsumerRecord
import com.example.outstandingmanager.ui.common.CurrencyFormatter
import com.example.outstandingmanager.ui.common.StatusBadge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerDetailScreen(
    consumer: ConsumerRecord,
    retainedColumns: List<String>,
    onSaveStatusAndRemark: (status: String, otherText: String, remark: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedStatus by remember { mutableStateOf(consumer.status) }
    var statusOtherText by remember { mutableStateOf(consumer.statusOtherText) }
    var remarkText by remember { mutableStateOf(consumer.remark) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val isOtherSelected = selectedStatus.equals(AppStatus.OTHERS.displayName, ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(consumer.consumerName, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Text("KNO: ${consumer.primaryIdentifier}", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Outstanding Amount Highlight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("OUTSTANDING AMOUNT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text(
                            text = CurrencyFormatter.formatIndianCurrency(consumer.outstandingAmount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C)
                        )
                    }

                    StatusBadge(status = consumer.displayStatus)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action / Status Section Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FIELD UPDATE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (selectedStatus.isBlank()) AppStatus.NO_STATUS.displayName else selectedStatus,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            AppStatus.ALL_OPTIONS.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.displayName) },
                                    onClick = {
                                        selectedStatus = st.displayName
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // "Others" free text specify
                    if (isOtherSelected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = statusOtherText,
                            onValueChange = { statusOtherText = it },
                            label = { Text("Specify Status", fontSize = 12.sp) },
                            placeholder = { Text("Enter custom status details...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Remark Field
                    Text("Remark", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        placeholder = { Text("Enter field notes, consumer promise date, etc...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (consumer.lastUpdated > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val df = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
                        Text(
                            text = "Last updated: ${df.format(Date(consumer.lastUpdated))}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = {
                            onSaveStatusAndRemark(selectedStatus, statusOtherText, remarkText)
                            scope.launch {
                                snackbarHostState.showSnackbar("Status and remark saved successfully.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE STATUS & REMARK", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Consumer Details Section Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CONSUMER INFORMATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailItem("KNO", consumer.kno)
                    DetailItem("Account No.", consumer.accountNo)
                    DetailItem("Consumer Name", consumer.consumerName)
                    if (consumer.fatherName.isNotBlank()) DetailItem("Father's Name", consumer.fatherName)
                    if (consumer.address.isNotBlank()) DetailItem("Address", consumer.address)
                    if (consumer.feederName.isNotBlank()) DetailItem("Feeder", consumer.feederName)

                    // Mobile No with Call action
                    if (consumer.mobileNo.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mobile No.", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(consumer.mobileNo, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                            IconButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${consumer.mobileNo}"))
                                    context.startActivity(dialIntent)
                                }
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF16A34A))
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }

                    if (consumer.tariffCode.isNotBlank()) DetailItem("Tariff Code", consumer.tariffCode)
                    if (consumer.consumerStatus.isNotBlank()) DetailItem("Consumer Status", consumer.consumerStatus)
                    if (consumer.meterNo.isNotBlank()) DetailItem("Meter No.", consumer.meterNo)
                    if (consumer.meterStatus.isNotBlank()) DetailItem("Meter Status", consumer.meterStatus)
                    if (consumer.lastReceivedAmount != null) DetailItem("Last Received Amount", CurrencyFormatter.formatIndianCurrency(consumer.lastReceivedAmount))
                    if (consumer.lastPaidDate.isNotBlank()) DetailItem("Last Paid Date", consumer.lastPaidDate)
                    if (consumer.duesSince.isNotBlank()) DetailItem("Dues Since", consumer.duesSince)
                    if (consumer.billingMonth.isNotBlank()) DetailItem("Billing Month", consumer.billingMonth)
                    if (consumer.areaCode.isNotBlank()) DetailItem("Area Code", consumer.areaCode)
                    if (consumer.fiCode.isNotBlank()) DetailItem("FI Code", consumer.fiCode)
                    if (consumer.phase.isNotBlank()) DetailItem("Phase", consumer.phase)
                    if (consumer.deptName.isNotBlank()) DetailItem("Dept Name", consumer.deptName)
                    if (consumer.govtNonGovt.isNotBlank()) DetailItem("Govt / Non-Govt", consumer.govtNonGovt)
                    if (consumer.smartNonSmart.isNotBlank()) DetailItem("Smart / Non-Smart", consumer.smartNonSmart)

                    // Any additional dynamic imported columns
                    val standardFieldNames = setOf(
                        "kno", "accno", "consumer name", "father's name", "address", "feeder name",
                        "mobile no", "tariffcode", "consumer status", "meter no", "meter status",
                        "outstanding amount", "last received amount", "last_paid", "dues since",
                        "billing month", "areacode", "fi_code", "phase", "dept name",
                        "govt/non govt", "smart/non smart", "_row_id"
                    )

                    consumer.dynamicFields.forEach { (key, value) ->
                        if (value.isNotBlank() && !standardFieldNames.any { key.equals(it, ignoreCase = true) }) {
                            DetailItem(key, value)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        )
    }
    HorizontalDivider(color = Color(0xFFF1F5F9))
}
