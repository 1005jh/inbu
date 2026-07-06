package com.inbu.ledger.ui.work

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inbu.ledger.ui.components.MainBottomBar
import com.inbu.ledger.ui.components.MainSection
import com.inbu.ledger.ui.sites.SiteMockData
import com.inbu.ledger.ui.sites.SiteStatus
import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import com.inbu.ledger.ui.workers.WorkerMockData
import com.inbu.ledger.ui.workers.WorkerSummary
import com.inbu.ledger.ui.workers.dailyWageFor
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkRecordScreen(
    sites: List<SiteSummary>,
    workers: List<WorkerSummary>,
    records: List<WorkRecordSummary>,
    initialRecord: WorkRecordSummary? = null,
    onSave: (
        dateEpochDay: Long,
        siteId: Long,
        workers: List<WorkerWorkAmount>,
        memo: String,
        fuelCost: Long,
        mealCost: Long,
    ) -> Unit,
    onBack: (() -> Unit)? = null,
    onSectionSelected: (MainSection) -> Unit = {},
) {
    var selectedDateEpochDay by rememberSaveable(initialRecord?.id) {
        mutableStateOf(initialRecord?.dateEpochDay ?: LocalDate.now().toEpochDay())
    }
    var selectedSiteId by rememberSaveable(initialRecord?.id) {
        mutableStateOf(initialRecord?.siteId)
    }
    var selectedUnits by remember(initialRecord?.id) {
        mutableStateOf(initialRecord?.workers?.associate { it.workerId to it.workUnits }.orEmpty())
    }
    var memo by rememberSaveable(initialRecord?.id) { mutableStateOf(initialRecord?.memo.orEmpty()) }
    var fuelCostDigits by rememberSaveable(initialRecord?.id) {
        mutableStateOf(initialRecord?.fuelCost?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var mealCostDigits by rememberSaveable(initialRecord?.id) {
        mutableStateOf(initialRecord?.mealCost?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.ofEpochDay(selectedDateEpochDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )
    val activeSites = sites.filter {
        it.status == SiteStatus.Active || it.id == initialRecord?.siteId
    }
    val initialWorkerIds = initialRecord?.workers?.mapTo(mutableSetOf()) { it.workerId }.orEmpty()
    val activeWorkers = workers.filter { it.isActive || it.id in initialWorkerIds }
    val duplicateWorkerIds = records
        .filter {
            it.id != initialRecord?.id &&
                it.dateEpochDay == selectedDateEpochDay &&
                it.siteId == selectedSiteId
        }
        .flatMap { it.workers }
        .mapTo(mutableSetOf()) { it.workerId }
    val selectedWorkers = selectedUnits.map { (workerId, units) ->
        val worker = workers.first { it.id == workerId }
        val savedWage = initialRecord
            ?.takeIf { it.siteId == selectedSiteId }
            ?.workers
            ?.firstOrNull { it.workerId == workerId }
            ?.dailyWage
        WorkerWorkAmount(
            workerId = workerId,
            workUnits = units,
            dailyWage = savedWage ?: worker.dailyWageFor(selectedSiteId ?: 0L),
        )
    }
    val totalUnits = selectedUnits.values.sum()
    val expectedPay = selectedWorkers.sumOf { (it.dailyWage * it.workUnits).toLong() }
    val fuelCost = fuelCostDigits.toLongOrNull() ?: 0L
    val mealCost = mealCostDigits.toLongOrNull() ?: 0L
    val canSave = selectedSiteId != null && selectedUnits.isNotEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Button(
                        onClick = {
                            onSave(
                                selectedDateEpochDay,
                                requireNotNull(selectedSiteId),
                                selectedWorkers,
                                memo.trim(),
                                fuelCost,
                                mealCost,
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            text = if (initialRecord == null) "작업 기록 저장" else "수정 내용 저장",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (initialRecord == null) {
                    MainBottomBar(
                        selectedSection = MainSection.Work,
                        onSectionSelected = onSectionSelected,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    }
                    Column {
                        Text(
                            text = if (initialRecord == null) "오늘 작업 기록" else "작업 기록 수정",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (initialRecord == null) {
                                "현장과 일한 인부를 선택해 주세요."
                            } else {
                                "저장하면 조회 결과와 금액이 함께 변경됩니다."
                            },
                            modifier = Modifier.padding(top = 5.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionTitle("작업 날짜", required = true)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Text(
                        text = LocalDate.ofEpochDay(selectedDateEpochDay).toKoreanDate(),
                        modifier = Modifier.padding(start = 10.dp),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                SectionTitle("현장", required = true)
                LazyRow(
                    modifier = Modifier.padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(activeSites, key = { it.id }) { site ->
                        FilterChip(
                            selected = selectedSiteId == site.id,
                            onClick = {
                                selectedSiteId = site.id
                                selectedUnits = emptyMap()
                            },
                            label = {
                                Text(site.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            },
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("일한 인부", required = true, modifier = Modifier.weight(1f))
                    Text(
                        text = "${selectedUnits.size}명 선택",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (selectedSiteId == null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 9.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "현장을 먼저 선택하면 인부 목록이 열려요.",
                            modifier = Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (selectedSiteId != null) {
                items(activeWorkers, key = { it.id }) { worker ->
                    val isDuplicate = worker.id in duplicateWorkerIds
                    WorkerSelectionCard(
                        worker = worker,
                        dailyWage = worker.dailyWageFor(requireNotNull(selectedSiteId)),
                        hasSiteWage = worker.siteDailyWages.containsKey(requireNotNull(selectedSiteId)),
                        selectedUnits = selectedUnits[worker.id],
                        isDuplicate = isDuplicate,
                        onSelectionChange = { selected ->
                            selectedUnits = if (selected) {
                                selectedUnits + (worker.id to 1.0)
                            } else {
                                selectedUnits - worker.id
                            }
                        },
                        onUnitsChange = { units ->
                            selectedUnits = selectedUnits + (worker.id to units)
                        },
                    )
                }
            }

            item {
                SectionTitle("추가 비용", required = false)
                Text(
                    text = "해당 날짜와 현장에서 사용한 비용을 입력하세요.",
                    modifier = Modifier.padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Row(
                    modifier = Modifier.padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ExpenseField(
                        label = "유류비",
                        digits = fuelCostDigits,
                        onValueChange = { fuelCostDigits = it },
                        modifier = Modifier.weight(1f),
                    )
                    ExpenseField(
                        label = "식비",
                        digits = mealCostDigits,
                        onValueChange = { mealCostDigits = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SectionTitle("작업 메모", required = false)
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it.take(100) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    placeholder = { Text("예: 2층 골조 및 자재 정리") },
                    supportingText = { Text("선택 · ${memo.length}/100자") },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            if (selectedUnits.isNotEmpty()) {
                item {
                    WorkSummaryCard(
                        workerCount = selectedUnits.size,
                        totalUnits = totalUnits,
                        expectedPay = expectedPay,
                        fuelCost = fuelCost,
                        mealCost = mealCost,
                    )
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
                        selectedDateEpochDay = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toEpochDay()
                        selectedUnits = emptyMap()
                    }
                    showDatePicker = false
                }) { Text("선택") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    required: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = if (required) "필수" else "선택",
            modifier = Modifier.padding(start = 8.dp),
            color = if (required) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WorkerSelectionCard(
    worker: WorkerSummary,
    dailyWage: Long,
    hasSiteWage: Boolean,
    selectedUnits: Double?,
    isDuplicate: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onUnitsChange: (Double) -> Unit,
) {
    val isSelected = selectedUnits != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDuplicate) { onSelectionChange(!isSelected) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDuplicate -> MaterialTheme.colorScheme.surfaceVariant
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
                else -> Color.White
            },
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange(it) },
                    enabled = !isDuplicate,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(worker.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = buildString {
                            append(worker.memo.ifBlank { "역할 미입력" })
                            append(" · ")
                            append(if (hasSiteWage) "현장 일당 " else "기본 일당 ")
                            append(dailyWage.toWon())
                            append("원")
                        },
                        modifier = Modifier.padding(top = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                if (isDuplicate) {
                    Text(
                        text = "이미 기록됨",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }

            if (isSelected) {
                Row(
                    modifier = Modifier.padding(start = 48.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        1.5 to "1.5공수",
                        1.0 to "1공수",
                        0.5 to "0.5공수",
                    ).forEach { (units, label) ->
                        FilterChip(
                            selected = selectedUnits == units,
                            onClick = { onUnitsChange(units) },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseField(
    label: String,
    digits: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = digits.toWonInput(),
        onValueChange = { input ->
            onValueChange(input.filter(Char::isDigit).take(9).trimStart('0'))
        },
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text("원") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun WorkSummaryCard(
    workerCount: Int,
    totalUnits: Double,
    expectedPay: Long,
    fuelCost: Long,
    mealCost: Long,
) {
    val extraCost = fuelCost + mealCost
    val totalCost = expectedPay + extraCost
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("입력 내용 확인", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("${workerCount}명 · ${totalUnits.toWorkUnit()}공수")
            Text(
                text = "예상 일당 합계 ${expectedPay.toWon()}원",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            if (extraCost > 0) {
                Text("추가 비용 ${extraCost.toWon()}원 (유류비 ${fuelCost.toWon()}원 · 식비 ${mealCost.toWon()}원)")
            }
            Text(
                text = "오늘 총비용 ${totalCost.toWon()}원",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun LocalDate.toKoreanDate(): String = format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN))

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

private fun String.toWonInput(): String = toLongOrNull()?.toWon().orEmpty()

private fun Double.toWorkUnit(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AddWorkRecordScreenPreview() {
    InbuLedgerTheme {
        AddWorkRecordScreen(
            sites = SiteMockData.sites,
            workers = WorkerMockData.workers,
            records = WorkRecordMockData.records,
            onSave = { _, _, _, _, _, _ -> },
        )
    }
}
