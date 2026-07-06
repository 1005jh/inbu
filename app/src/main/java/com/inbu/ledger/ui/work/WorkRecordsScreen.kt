package com.inbu.ledger.ui.work

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.inbu.ledger.ui.components.MainBottomBar
import com.inbu.ledger.ui.components.MainSection
import com.inbu.ledger.ui.sites.SiteMockData
import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import com.inbu.ledger.ui.workers.WorkerMockData
import com.inbu.ledger.ui.workers.WorkerSummary
import com.inbu.ledger.ui.payment.PaymentStatus
import com.inbu.ledger.ui.payment.PaymentStatusBadge
import com.inbu.ledger.ui.payment.WorkerPayment
import com.inbu.ledger.ui.payment.paidFor
import com.inbu.ledger.ui.payment.PaymentMockData
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DateTarget { Start, End }
private enum class QueryMode(val label: String) {
    Records("작업별"),
    Workers("인부별"),
}

private data class WorkerHistorySummary(
    val worker: WorkerSummary,
    val workDays: Int,
    val workUnits: Double,
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
private fun EmptyQueryCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(22.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WorkerHistoryCard(history: WorkerHistorySummary, onClick: () -> Unit) {
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
                    Text(history.worker.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        history.worker.memo.ifBlank { "역할 미입력" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PaymentStatusBadge(history.status)
            }
            Text("근무 ${history.workDays}일 · ${history.workUnits.toWorkUnit()}공수")
            Row {
                Text("일당 ${history.gross.toWon()}원", modifier = Modifier.weight(1f))
                Text(
                    text = "미지급 ${history.unpaid.toWon()}원",
                    color = if (history.unpaid > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkRecordsScreen(
    records: List<WorkRecordSummary>,
    sites: List<SiteSummary>,
    workers: List<WorkerSummary>,
    payments: List<WorkerPayment> = emptyList(),
    onOpenRecord: (Long) -> Unit = {},
    onOpenWorkerHistory: (workerId: Long, startEpochDay: Long?, endEpochDay: Long?, siteId: Long?) -> Unit =
        { _, _, _, _ -> },
    onOpenPayments: () -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onSectionSelected: (MainSection) -> Unit = {},
) {
    val today = LocalDate.now()
    var startEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var endEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedSiteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateTarget by rememberSaveable { mutableStateOf<DateTarget?>(null) }
    var queryMode by rememberSaveable { mutableStateOf(QueryMode.Records) }
    var workerQuery by rememberSaveable { mutableStateOf("") }
    val queryableSiteIds = sites.mapTo(mutableSetOf()) { it.id }
    val visibleRecords = records
        .filter { it.siteId in queryableSiteIds }
        .filter { record -> startEpochDay?.let { record.dateEpochDay >= it } ?: true }
        .filter { record -> endEpochDay?.let { record.dateEpochDay <= it } ?: true }
        .filter { selectedSiteId == null || it.siteId == selectedSiteId }
        .sortedByDescending { it.dateEpochDay }
    val totalUnits = visibleRecords.sumOf { record -> record.workers.sumOf { it.workUnits } }
    val laborCost = visibleRecords.sumOf { it.laborCost() }
    val extraCost = visibleRecords.sumOf { it.fuelCost + it.mealCost }
    val workerHistories = workers.mapNotNull { worker ->
        val workItems = visibleRecords.mapNotNull { record ->
            record.workers.firstOrNull { it.workerId == worker.id }?.let { record to it }
        }
        if (workItems.isEmpty()) return@mapNotNull null
        val gross = workItems.sumOf { (_, amount) -> (amount.dailyWage * amount.workUnits).toLong() }
        val paid = workItems.sumOf { (record, amount) ->
            payments.paidFor(record.id, worker.id).coerceAtMost((amount.dailyWage * amount.workUnits).toLong())
        }
        WorkerHistorySummary(
            worker = worker,
            workDays = workItems.map { it.first.dateEpochDay }.distinct().size,
            workUnits = workItems.sumOf { it.second.workUnits },
            gross = gross,
            paid = paid,
        )
    }.filter {
        workerQuery.isBlank() || it.worker.name.contains(workerQuery, ignoreCase = true) ||
            it.worker.memo.contains(workerQuery, ignoreCase = true)
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("작업 조회", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "기간과 현장별 장부를 확인하세요.",
                            modifier = Modifier.padding(top = 5.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenPayments,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text("지급 관리", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenTrash,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("휴지통", modifier = Modifier.padding(start = 5.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QueryMode.entries.forEach { mode ->
                        FilterChip(
                            selected = queryMode == mode,
                            onClick = { queryMode = mode },
                            label = { Text(mode.label, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "조회 기간",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    FilterChip(
                        selected = startEpochDay == null && endEpochDay == null && selectedSiteId == null,
                        onClick = {
                            startEpochDay = null
                            endEpochDay = null
                            selectedSiteId = null
                        },
                        label = { Text("전체 조회", fontWeight = FontWeight.Bold) },
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateRangeButton(
                        label = "시작",
                        epochDay = startEpochDay,
                        onClick = { dateTarget = DateTarget.Start },
                        modifier = Modifier.weight(1f),
                    )
                    DateRangeButton(
                        label = "종료",
                        epochDay = endEpochDay,
                        onClick = { dateTarget = DateTarget.End },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Text("현장", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                LazyRow(
                    modifier = Modifier.padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedSiteId == null,
                            onClick = { selectedSiteId = null },
                            label = { Text("전체 현장", fontWeight = FontWeight.SemiBold) },
                        )
                    }
                    items(sites, key = { it.id }) { site ->
                        FilterChip(
                            selected = selectedSiteId == site.id,
                            onClick = { selectedSiteId = site.id },
                            label = { Text(site.name, fontWeight = FontWeight.SemiBold) },
                        )
                    }
                }
            }

            if (queryMode == QueryMode.Records) {
                item {
                    QuerySummaryCard(
                        recordCount = visibleRecords.size,
                        totalUnits = totalUnits,
                        laborCost = laborCost,
                        extraCost = extraCost,
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "작업 목록",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("${visibleRecords.size}건", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (visibleRecords.isEmpty()) {
                    item { EmptyQueryCard("선택한 조건에 작업 기록이 없어요.") }
                } else {
                    items(visibleRecords, key = { it.id }) { record ->
                        WorkRecordCard(
                            record = record,
                            siteName = sites.firstOrNull { it.id == record.siteId }?.name ?: "삭제된 현장",
                            workerNames = record.workers.mapNotNull { amount ->
                                workers.firstOrNull { it.id == amount.workerId }?.name
                            },
                            onClick = { onOpenRecord(record.id) },
                        )
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = workerQuery,
                        onValueChange = { workerQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("인부 이름 또는 역할 검색") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "인부별 작업 내역",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("${workerHistories.size}명", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (workerHistories.isEmpty()) {
                    item { EmptyQueryCard("선택한 조건에 일한 인부가 없어요.") }
                } else {
                    items(workerHistories, key = { it.worker.id }) { history ->
                        WorkerHistoryCard(
                            history = history,
                            onClick = {
                                onOpenWorkerHistory(
                                    history.worker.id,
                                    startEpochDay,
                                    endEpochDay,
                                    selectedSiteId,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    dateTarget?.let { target ->
        val initialEpochDay = when (target) {
            DateTarget.Start -> startEpochDay ?: records.minOfOrNull { it.dateEpochDay } ?: today.toEpochDay()
            DateTarget.End -> endEpochDay ?: today.toEpochDay()
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(initialEpochDay)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        if (target == DateTarget.Start) {
                            startEpochDay = selected
                            if (endEpochDay != null && requireNotNull(endEpochDay) < selected) endEpochDay = selected
                        } else {
                            endEpochDay = selected
                            if (startEpochDay != null && requireNotNull(startEpochDay) > selected) startEpochDay = selected
                        }
                    }
                    dateTarget = null
                }) { Text("선택") }
            },
            dismissButton = {
                TextButton(onClick = { dateTarget = null }) { Text("취소") }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun DateRangeButton(label: String, epochDay: Long?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = epochDay?.let { LocalDate.ofEpochDay(it).toShortDate() } ?: "선택 안 함",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun QuerySummaryCard(recordCount: Int, totalUnits: Double, laborCost: Long, extraCost: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("조회 합계", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("${recordCount}건 · ${totalUnits.toWorkUnit()}공수")
            Text("일당 ${laborCost.toWon()}원", fontWeight = FontWeight.Bold)
            Text("추가 비용 ${extraCost.toWon()}원 · 총비용 ${(laborCost + extraCost).toWon()}원")
        }
    }
}

@Composable
private fun WorkRecordCard(
    record: WorkRecordSummary,
    siteName: String,
    workerNames: List<String>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(siteName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        LocalDate.ofEpochDay(record.dateEpochDay).toLongDate(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("${record.totalCost().toWon()}원", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Text(
                text = "${record.workers.size}명 · ${record.workers.sumOf { it.workUnits }.toWorkUnit()}공수 · ${workerNames.joinToString(", ")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (record.memo.isNotBlank()) Text(record.memo, fontSize = 14.sp)
        }
    }
}

fun WorkRecordSummary.laborCost(): Long = workers.sumOf { (it.dailyWage * it.workUnits).toLong() }

fun WorkRecordSummary.totalCost(): Long = laborCost() + fuelCost + mealCost

private fun LocalDate.toShortDate(): String = format(DateTimeFormatter.ofPattern("yy년 M월 d일", Locale.KOREAN))

private fun LocalDate.toLongDate(): String = format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

private fun Double.toWorkUnit(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkRecordsScreenPreview() {
    InbuLedgerTheme {
        WorkRecordsScreen(
            records = WorkRecordMockData.records,
            sites = SiteMockData.sites,
            workers = WorkerMockData.workers,
            payments = PaymentMockData.payments,
        )
    }
}
