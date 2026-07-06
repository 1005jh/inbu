package com.inbu.ledger.ui.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.inbu.ledger.ui.sites.SiteMockData
import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import com.inbu.ledger.ui.work.WorkRecordMockData
import com.inbu.ledger.ui.work.WorkRecordSummary
import com.inbu.ledger.ui.workers.WorkerMockData
import com.inbu.ledger.ui.workers.WorkerSummary
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class WorkerWorkPayment(
    val record: WorkRecordSummary,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerPaymentScreen(
    worker: WorkerSummary,
    records: List<WorkRecordSummary>,
    sites: List<SiteSummary>,
    payments: List<WorkerPayment>,
    initialSiteId: Long? = null,
    initialCutoffEpochDay: Long? = null,
    onBack: () -> Unit = {},
    onSettle: (siteId: Long, cutoffEpochDay: Long) -> Unit = { _, _ -> },
    onCancelPayment: (Long) -> Unit = {},
) {
    val workerWorks = records.mapNotNull { record ->
        val amount = record.workers.firstOrNull { it.workerId == worker.id } ?: return@mapNotNull null
        val gross = (amount.dailyWage * amount.workUnits).toLong()
        WorkerWorkPayment(
            record = record,
            gross = gross,
            paid = payments.paidFor(record.id, worker.id).coerceAtMost(gross),
        )
    }.sortedWith(compareBy<WorkerWorkPayment> { it.record.dateEpochDay }.thenBy { it.record.id })
    val availableSiteIds = workerWorks.map { it.record.siteId }.distinct()
    val availableSites = sites.filter { it.id in availableSiteIds }
    var selectedSiteId by rememberSaveable(worker.id) {
        mutableStateOf(initialSiteId?.takeIf { it in availableSiteIds })
    }
    var cutoffEpochDay by rememberSaveable(worker.id) {
        mutableStateOf(initialCutoffEpochDay ?: LocalDate.now().toEpochDay())
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var cancelPaymentId by rememberSaveable { mutableStateOf<Long?>(null) }
    val gross = workerWorks.sumOf { it.gross }
    val paid = workerWorks.sumOf { it.paid }
    val unpaid = (gross - paid).coerceAtLeast(0L)
    val settlementWorks = workerWorks.filter {
        it.record.siteId == selectedSiteId && it.record.dateEpochDay <= cutoffEpochDay
    }
    val settlementUnpaid = settlementWorks.sumOf { it.unpaid }
    val settlementUnits = settlementWorks.sumOf { work ->
        work.record.workers.first { it.workerId == worker.id }.workUnits
    }
    val visibleWorks = selectedSiteId?.let { siteId ->
        workerWorks.filter { it.record.siteId == siteId }
    } ?: workerWorks
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.ofEpochDay(cutoffEpochDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("${worker.name} 작업 내역", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = selectedSiteId != null && settlementUnpaid > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = if (selectedSiteId == null) "정산할 현장을 선택하세요" else "기준일까지 지급 완료",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        BalanceRow("총 일당", gross)
                        BalanceRow("지급 완료", paid)
                        BalanceRow("남은 금액", unpaid, emphasized = true)
                    }
                }
            }

            item {
                Text("1. 정산할 현장", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "다른 현장의 작업에는 지급 상태가 반영되지 않습니다.",
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availableSites, key = { it.id }) { site ->
                        FilterChip(
                            selected = selectedSiteId == site.id,
                            onClick = { selectedSiteId = site.id },
                            label = { Text(site.name, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }

            item {
                Text("2. 정산 기준일", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = selectedSiteId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Text(
                        text = LocalDate.ofEpochDay(cutoffEpochDay).toKoreanDate(),
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (selectedSiteId != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("정산 대상", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("${settlementWorks.size}일 · ${settlementUnits.toWorkUnit()}공수")
                            Text(
                                text = "지급할 금액 ${settlementUnpaid.toWon()}원",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (selectedSiteId == null) "전체 작업 내역" else "선택 현장 작업 내역",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(visibleWorks, key = { it.record.id }) { work ->
                val siteName = sites.firstOrNull { it.id == work.record.siteId }?.name ?: "삭제된 현장"
                val isInSettlement = selectedSiteId == work.record.siteId && work.record.dateEpochDay <= cutoffEpochDay
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInSettlement) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                        } else {
                            Color.White
                        },
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(siteName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(
                                    LocalDate.ofEpochDay(work.record.dateEpochDay).toKoreanDate(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            PaymentStatusBadge(work.status)
                        }
                        Row {
                            Text("일당 ${work.gross.toWon()}원", modifier = Modifier.weight(1f))
                            Text("남음 ${work.unpaid.toWon()}원", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val workerPayments = payments.filter { it.workerId == worker.id }.sortedByDescending { it.paidDateEpochDay }
            if (workerPayments.isNotEmpty()) {
                item {
                    Text("지급 내역", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(workerPayments, key = { it.id }) { payment ->
                    val siteName = sites.firstOrNull { it.id == payment.siteId }?.name ?: "삭제된 현장"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Column(modifier = Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$siteName · ${payment.amount.toWon()}원",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                )
                                OutlinedButton(
                                    onClick = { cancelPaymentId = payment.id },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text("지급 취소", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                }
                            }
                            Text(
                                "${LocalDate.ofEpochDay(payment.settledThroughEpochDay).toKoreanDate()}까지 정산",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "지급일 ${LocalDate.ofEpochDay(payment.paidDateEpochDay).toKoreanDate()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        cutoffEpochDay = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("선택") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (showConfirmDialog && selectedSiteId != null) {
        val siteName = sites.firstOrNull { it.id == selectedSiteId }?.name ?: "선택 현장"
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("${settlementUnpaid.toWon()}원을 지급할까요?") },
            text = {
                Text("${siteName}에서 ${LocalDate.ofEpochDay(cutoffEpochDay).toKoreanDate()}까지 일한 내역만 지급 완료됩니다.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onSettle(requireNotNull(selectedSiteId), cutoffEpochDay)
                }) { Text("지급 완료") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("취소") }
            },
        )
    }

    cancelPaymentId?.let { paymentId ->
        val payment = payments.firstOrNull { it.id == paymentId }
        if (payment != null) {
            AlertDialog(
                onDismissRequest = { cancelPaymentId = null },
                title = { Text("지급 처리를 취소할까요?") },
                text = { Text("${payment.amount.toWon()}원이 다시 미지급으로 돌아갑니다.") },
                confirmButton = {
                    TextButton(onClick = {
                        cancelPaymentId = null
                        onCancelPayment(paymentId)
                    }) { Text("지급 취소", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { cancelPaymentId = null }) { Text("닫기") }
                },
            )
        }
    }
}

@Composable
private fun BalanceRow(label: String, amount: Long, emphasized: Boolean = false) {
    Row {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(
            text = "${amount.toWon()}원",
            fontSize = if (emphasized) 20.sp else 16.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

private fun LocalDate.toKoreanDate(): String =
    format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN))

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

private fun Double.toWorkUnit(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkerPaymentScreenPreview() {
    InbuLedgerTheme {
        WorkerPaymentScreen(
            worker = WorkerMockData.workers[1],
            records = WorkRecordMockData.records,
            sites = SiteMockData.sites,
            payments = PaymentMockData.payments,
        )
    }
}
