package com.inbu.ledger.ui.work

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inbu.ledger.ui.sites.SiteMockData
import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import com.inbu.ledger.ui.workers.WorkerMockData
import com.inbu.ledger.ui.workers.WorkerSummary
import com.inbu.ledger.ui.payment.PaymentStatus
import com.inbu.ledger.ui.payment.PaymentStatusBadge
import com.inbu.ledger.ui.payment.WorkerPayment
import com.inbu.ledger.ui.payment.paidFor
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkRecordDetailScreen(
    record: WorkRecordSummary,
    sites: List<SiteSummary>,
    workers: List<WorkerSummary>,
    payments: List<WorkerPayment> = emptyList(),
    isPaymentLinked: Boolean = false,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onMoveToTrash: () -> Unit = {},
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val siteName = sites.firstOrNull { it.id == record.siteId }?.name ?: "삭제된 현장"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("작업 기록 상세", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, enabled = !isPaymentLinked) {
                        Icon(Icons.Filled.Edit, contentDescription = "수정")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(siteName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = LocalDate.ofEpochDay(record.dateEpochDay)
                        .format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN)),
                    modifier = Modifier.padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Text("일한 인부", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(record.workers, key = { it.workerId }) { amount ->
                val workerName = workers.firstOrNull { it.id == amount.workerId }?.name ?: "삭제된 인부"
                val gross = (amount.dailyWage * amount.workUnits).toLong()
                val paid = payments.paidFor(record.id, amount.workerId).coerceAtMost(gross)
                val paymentStatus = when {
                    paid == 0L -> PaymentStatus.Unpaid
                    paid < gross -> PaymentStatus.Partial
                    else -> PaymentStatus.Paid
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(workerName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                text = "적용 일당 ${amount.dailyWage.toWon()}원",
                                modifier = Modifier.padding(top = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column {
                            Text("${amount.workUnits.toWorkUnit()}공수", fontWeight = FontWeight.Bold)
                            Text(
                                text = "${gross.toWon()}원",
                                modifier = Modifier.padding(top = 3.dp),
                                fontWeight = FontWeight.Bold,
                            )
                            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                            PaymentStatusBadge(paymentStatus)
                        }
                    }
                }
            }

            item {
                CostDetailCard(record)
            }

            if (record.memo.isNotBlank()) {
                item {
                    Text("작업 메모", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Text(record.memo, modifier = Modifier.padding(16.dp))
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isPaymentLinked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = if (isPaymentLinked) "지급 취소 후 삭제 가능" else "휴지통으로 이동",
                        modifier = Modifier.padding(start = 7.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (isPaymentLinked) {
                item {
                    Text(
                        text = "이 기록에 연결된 지급 내역을 먼저 취소해야 삭제할 수 있어요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("작업 기록을 휴지통으로 옮길까요?") },
            text = { Text("조회와 합계에서 제외되지만 휴지통에서 복구할 수 있어요.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onMoveToTrash()
                }) { Text("휴지통으로 이동", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun CostDetailCard(record: WorkRecordSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("금액 합계", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            CostRow("일당", record.laborCost())
            CostRow("유류비", record.fuelCost)
            CostRow("식비", record.mealCost)
            CostRow("총비용", record.totalCost(), emphasized = true)
        }
    }
}

@Composable
private fun CostRow(label: String, amount: Long, emphasized: Boolean = false) {
    Row {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text("${amount.toWon()}원", fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold)
    }
}

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

private fun Double.toWorkUnit(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkRecordDetailScreenPreview() {
    InbuLedgerTheme {
        WorkRecordDetailScreen(
            record = WorkRecordMockData.records.first(),
            sites = SiteMockData.sites,
            workers = WorkerMockData.workers,
        )
    }
}
