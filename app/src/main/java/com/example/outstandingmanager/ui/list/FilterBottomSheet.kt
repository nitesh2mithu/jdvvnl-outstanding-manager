package com.example.outstandingmanager.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.outstandingmanager.model.AppStatus
import com.example.outstandingmanager.model.FilterCriteria
import com.example.outstandingmanager.model.RemarkFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    sheetState: SheetState,
    initialCriteria: FilterCriteria,
    availableFeeders: List<String>,
    availableTariffCodes: List<String>,
    availableAreaCodes: List<String>,
    matchingCount: Int,
    onApply: (FilterCriteria) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var presetAmount by remember { mutableStateOf(initialCriteria.presetAmount) }
    var minAmountText by remember { mutableStateOf(initialCriteria.minAmount?.toInt()?.toString() ?: "") }
    var maxAmountText by remember { mutableStateOf(initialCriteria.maxAmount?.toInt()?.toString() ?: "") }
    var selectedFeeder by remember { mutableStateOf(initialCriteria.feeder) }
    var selectedAppStatus by remember { mutableStateOf(initialCriteria.appStatus) }
    var selectedTariff by remember { mutableStateOf(initialCriteria.tariffCode) }
    var selectedArea by remember { mutableStateOf(initialCriteria.areaCode) }
    var remarkFilter by remember { mutableStateOf(initialCriteria.remarkFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Consumers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                OutlinedButton(
                    onClick = {
                        presetAmount = null
                        minAmountText = ""
                        maxAmountText = ""
                        selectedFeeder = null
                        selectedAppStatus = null
                        selectedTariff = null
                        selectedArea = null
                        remarkFilter = RemarkFilter.ALL
                        onReset()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear All", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Presets
            Text("OUTSTANDING AMOUNT PRESETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(5000.0, 10000.0, 25000.0, 50000.0, 100000.0)
                presets.forEach { amt ->
                    val label = if (amt >= 100000) "₹1 Lakh" else "₹${amt.toInt()}"
                    FilterChip(
                        selected = presetAmount == amt,
                        onClick = {
                            presetAmount = if (presetAmount == amt) null else amt
                            minAmountText = "" // clear custom min when preset chosen
                        },
                        label = { Text("> $label", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Min / Max Amount
            Text("CUSTOM AMOUNT RANGE (₹)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minAmountText,
                    onValueChange = {
                        minAmountText = it
                        if (it.isNotBlank()) presetAmount = null
                    },
                    label = { Text("Min Amount", fontSize = 12.sp) },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { maxAmountText = it },
                    label = { Text("Max Amount", fontSize = 12.sp) },
                    placeholder = { Text("No limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Status
            Text("APP STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = listOf(
                    AppStatus.NO_STATUS.displayName,
                    AppStatus.VISITED.displayName,
                    AppStatus.PAYMENT_PROMISED.displayName,
                    AppStatus.PART_PAYMENT.displayName,
                    AppStatus.PAID.displayName,
                    AppStatus.DISCONNECTED.displayName,
                    AppStatus.DISPUTE.displayName,
                    AppStatus.OTHERS.displayName
                )
                statuses.forEach { st ->
                    FilterChip(
                        selected = selectedAppStatus == st,
                        onClick = {
                            selectedAppStatus = if (selectedAppStatus == st) null else st
                        },
                        label = { Text(st, fontSize = 12.sp) }
                    )
                }
            }

            // Feeder Filter
            if (availableFeeders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("FEEDER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableFeeders.forEach { f ->
                        FilterChip(
                            selected = selectedFeeder == f,
                            onClick = { selectedFeeder = if (selectedFeeder == f) null else f },
                            label = { Text(f, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Remark Filter
            Spacer(modifier = Modifier.height(16.dp))
            Text("REMARK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RemarkFilter.entries.forEach { rf ->
                    FilterChip(
                        selected = remarkFilter == rf,
                        onClick = { remarkFilter = rf },
                        label = { Text(rf.displayName, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(16.dp))

            // Apply Button
            Button(
                onClick = {
                    val criteria = FilterCriteria(
                        query = initialCriteria.query,
                        presetAmount = presetAmount,
                        minAmount = minAmountText.toDoubleOrNull(),
                        maxAmount = maxAmountText.toDoubleOrNull(),
                        feeder = selectedFeeder,
                        appStatus = selectedAppStatus,
                        tariffCode = selectedTariff,
                        areaCode = selectedArea,
                        remarkFilter = remarkFilter
                    )
                    onApply(criteria)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "APPLY FILTERS",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
