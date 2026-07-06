package com.inbu.ledger.ui.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inbu.ledger.ui.components.MainBottomBar
import com.inbu.ledger.ui.components.MainSection
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import com.inbu.ledger.ui.work.WorkRecordMockData
import com.inbu.ledger.ui.work.WorkRecordSummary
import com.inbu.ledger.ui.workers.WorkerMockData
import com.inbu.ledger.ui.workers.WorkerSummary
import java.text.NumberFormat
import java.util.Locale

private enum class PaymentFilter(val label: String) {
    Unpaid("미지급 있음"),
    All("전체 인부"),
}

@Composable
fun PaymentsScreen(
    records: List<WorkRecordSummary>,
    workers: List<WorkerSummary>,
    payments: List<WorkerPayment>,
    onOpenWorker: (Long) -> Unit = {},
    onSectionSelected: (MainSection) -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf(PaymentFilter.Unpaid) }
    val workerBalances = workers.mapNotNull { worker ->
        val gross = records.grossFor(worker.id)
        if (gross <= 0) return@mapNotNull null
        val paid = payments.paidFor(worker.id)
        WorkerBalance(worker = worker, gross = gross, paid = paid.coerceAtMost(gross))
    }
    val visibleBalances = workerBalances.filter { filter == PaymentFilter.All || it.unpaid > 0 }
    val totalUnpaid = workerBalances.sumOf { it.unpaid }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainBottomBar(
                selectedSection = MainSection.Search,
                onSectionSelected = onSectionSelected,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("지급 관리", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "오래된 작업부터 순서대로 일당을 지급합니다.",
                    modifier = Modifier.padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("전체 미지급", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "${totalUnpaid.toWon()}원",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "인부별 지급 현황",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("${visibleBalances.size}명", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (visibleBalances.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("미지급 일당이 없어요.", modifier = Modifier.padding(22.dp))
                    }
                }
            } else {
                items(visibleBalances, key = { it.worker.id }) { balance ->
                    WorkerPaymentCard(balance = balance, onClick = { onOpenWorker(balance.worker.id) })
                }
            }
        }
    }
}

private data class WorkerBalance(
    val worker: WorkerSummary,
    val gross: Long,
    val paid: Long,
) {
    val unpaid: Long get() = (gross - paid).coerceAtLeast(0L)
    val status: PaymentStatus
        get() = when {
            unpaid == 0L -> PaymentStatus.Paid
            paid > 0L -> PaymentStatus.Partial
            else -> PaymentStatus.Unpaid
        }
}

@Composable
private fun WorkerPaymentCard(balance: WorkerBalance, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(balance.worker.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = balance.worker.memo.ifBlank { "역할 미입력" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PaymentStatusBadge(balance.status)
            }
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("총 일당", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("${balance.gross.toWon()}원", fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("남은 금액", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(
                        text = "${balance.unpaid.toWon()}원",
                        color = if (balance.unpaid > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus) {
    val color = when (status) {
        PaymentStatus.Paid -> MaterialTheme.colorScheme.primary
        PaymentStatus.Partial -> Color(0xFF8A6100)
        PaymentStatus.Unpaid -> MaterialTheme.colorScheme.error
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.13f)) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

fun List<WorkRecordSummary>.grossFor(workerId: Long): Long = sumOf { record ->
    record.workers.filter { it.workerId == workerId }.sumOf { (it.dailyWage * it.workUnits).toLong() }
}

fun List<WorkerPayment>.paidFor(workerId: Long): Long =
    filter { it.workerId == workerId }.sumOf { it.amount }

fun List<WorkerPayment>.paidFor(recordId: Long, workerId: Long): Long = sumOf { payment ->
    payment.allocations
        .filter { it.recordId == recordId && it.workerId == workerId }
        .sumOf { it.amount }
}

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PaymentsScreenPreview() {
    InbuLedgerTheme {
        PaymentsScreen(
            records = WorkRecordMockData.records,
            workers = WorkerMockData.workers,
            payments = PaymentMockData.payments,
        )
    }
}
