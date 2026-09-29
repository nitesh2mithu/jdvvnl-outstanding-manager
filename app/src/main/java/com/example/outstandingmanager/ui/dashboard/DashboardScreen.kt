package com.example.outstandingmanager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.outstandingmanager.model.AppStatus
import com.example.outstandingmanager.model.DashboardSummary
import com.example.outstandingmanager.model.Dataset
import com.example.outstandingmanager.model.FilterCriteria
import com.example.outstandingmanager.ui.common.CurrencyFormatter
import com.example.outstandingmanager.ui.common.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dataset: Dataset?,
    summary: DashboardSummary,
    onNavigateToOutstandingWithFilter: (FilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard", fontWeight = FontWeight.Bold)
                        if (dataset != null) {
                            Text(
                                text = dataset.sourceFileName,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Section 1: Primary KPIs
            Text(
                text = "OVERVIEW",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Total Consumers",
                    value = "${summary.totalConsumers}",
                    icon = Icons.Default.Groups,
                    iconTint = Color(0xFF0284C7),
                    onClick = { onNavigateToOutstandingWithFilter(FilterCriteria()) },
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "Total Outstanding",
                    value = CurrencyFormatter.formatIndianCurrency(summary.totalOutstanding),
                    icon = Icons.Default.CurrencyRupee,
                    iconTint = Color(0xFFDC2626),
                    onClick = { onNavigateToOutstandingWithFilter(FilterCriteria()) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Average Dues",
                    value = CurrencyFormatter.formatIndianCurrency(summary.averageOutstanding),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconTint = Color(0xFFD97706),
                    onClick = { onNavigateToOutstandingWithFilter(FilterCriteria()) },
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "Highest Due",
                    value = CurrencyFormatter.formatIndianCurrency(summary.highestOutstanding),
                    icon = Icons.Default.Leaderboard,
                    iconTint = Color(0xFF7C3AED),
                    onClick = { onNavigateToOutstandingWithFilter(FilterCriteria()) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Amount Buckets
            Text(
                text = "OUTSTANDING AMOUNT BUCKETS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    BucketItem(
                        label = "> ₹5,000",
                        count = summary.bucket5kCount,
                        onClick = {
                            onNavigateToOutstandingWithFilter(FilterCriteria(presetAmount = 5000.0))
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    BucketItem(
                        label = "> ₹10,000",
                        count = summary.bucket10kCount,
                        onClick = {
                            onNavigateToOutstandingWithFilter(FilterCriteria(presetAmount = 10000.0))
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    BucketItem(
                        label = "> ₹25,000",
                        count = summary.bucket25kCount,
                        onClick = {
                            onNavigateToOutstandingWithFilter(FilterCriteria(presetAmount = 25000.0))
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    BucketItem(
                        label = "> ₹50,000",
                        count = summary.bucket50kCount,
                        onClick = {
                            onNavigateToOutstandingWithFilter(FilterCriteria(presetAmount = 5000.0 * 10))
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    BucketItem(
                        label = "> ₹1,00,000 (1 Lakh)",
                        count = summary.bucket1LakhCount,
                        onClick = {
                            onNavigateToOutstandingWithFilter(FilterCriteria(presetAmount = 100000.0))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Status Summary
            Text(
                text = "STATUS BREAKDOWN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val statusList = listOf(
                        AppStatus.NO_STATUS,
                        AppStatus.VISITED,
                        AppStatus.PAYMENT_PROMISED,
                        AppStatus.PART_PAYMENT,
                        AppStatus.PAID,
                        AppStatus.DISCONNECTED,
                        AppStatus.DISPUTE,
                        AppStatus.OTHERS
                    )

                    statusList.forEachIndexed { idx, st ->
                        val count = summary.statusCounts[st.displayName] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val filterValue = if (st == AppStatus.NO_STATUS) AppStatus.NO_STATUS.displayName else st.displayName
                                    onNavigateToOutstandingWithFilter(FilterCriteria(appStatus = filterValue))
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(st.displayName)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$count",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (count > 0) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (idx < statusList.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "build by nitesh2mithu",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun BucketItem(
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$count consumers",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (count > 0) Color(0xFF0284C7) else Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
