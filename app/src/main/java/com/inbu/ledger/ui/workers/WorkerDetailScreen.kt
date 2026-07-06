package com.inbu.ledger.ui.workers

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import com.inbu.ledger.ui.sites.SiteMockData
import com.inbu.ledger.ui.sites.SiteStatus
import com.inbu.ledger.ui.sites.SiteSummary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailScreen(
    worker: WorkerSummary,
    sites: List<SiteSummary> = SiteMockData.sites,
    onBack: () -> Unit = {},
    onSave: (WorkerSummary) -> Unit = {},
    onActiveChange: (Boolean) -> Unit = {},
) {
    var isEditing by rememberSaveable(worker.id) { mutableStateOf(false) }
    var name by rememberSaveable(worker.id) { mutableStateOf(worker.name) }
    var wageDigits by rememberSaveable(worker.id) { mutableStateOf(worker.dailyWage.toString()) }
    var phone by rememberSaveable(worker.id) { mutableStateOf(worker.phone) }
    var memo by rememberSaveable(worker.id) { mutableStateOf(worker.memo) }
    var siteWageDigits by remember(worker.id) {
        mutableStateOf(worker.siteDailyWages.mapValues { it.value.toString() })
    }
    var showStatusDialog by rememberSaveable { mutableStateOf(false) }
    val wage = wageDigits.toLongOrNull() ?: 0L

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "인부 정보 수정" else "인부 상세",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) {
                            name = worker.name
                            wageDigits = worker.dailyWage.toString()
                            phone = worker.phone
                            memo = worker.memo
                            siteWageDigits = worker.siteDailyWages.mapValues { it.value.toString() }
                            isEditing = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "수정")
                        }
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (isEditing) {
                item {
                    WorkerEditForm(
                        name = name,
                        onNameChange = { name = it.take(20) },
                        wageDigits = wageDigits,
                        onWageChange = { input ->
                            wageDigits = input.filter(Char::isDigit).take(9).trimStart('0')
                        },
                        phone = phone,
                        onPhoneChange = { input ->
                            phone = input.filter { it.isDigit() || it == '-' }.take(13)
                        },
                        memo = memo,
                        onMemoChange = { memo = it.take(50) },
                        sites = sites.filter { it.status == SiteStatus.Active },
                        siteWageDigits = siteWageDigits,
                        onSiteWageChange = { siteId, input ->
                            val digits = input.filter(Char::isDigit).take(9).trimStart('0')
                            siteWageDigits = if (digits.isBlank()) {
                                siteWageDigits - siteId
                            } else {
                                siteWageDigits + (siteId to digits)
                            }
                        },
                    )
                }
                item {
                    Button(
                        onClick = {
                            onSave(
                                worker.copy(
                                    name = name.trim(),
                                    dailyWage = wage,
                                    phone = phone.trim(),
                                    memo = memo.trim(),
                                    siteDailyWages = siteWageDigits.mapNotNull { (siteId, digits) ->
                                        digits.toLongOrNull()?.takeIf { it > 0 }?.let { siteId to it }
                                    }.toMap(),
                                ),
                            )
                            isEditing = false
                        },
                        enabled = name.isNotBlank() && wage > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("수정 내용 저장", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                item { WorkerHeader(worker) }
                item { WorkerPaySummary(worker) }
                item { SiteWageCard(worker = worker, sites = sites) }
                item {
                    InfoCard(
                        rows = listOf(
                            "전화번호" to worker.phone.ifBlank { "등록되지 않음" },
                            "역할·메모" to worker.memo.ifBlank { "등록되지 않음" },
                        ),
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showStatusDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            1.dp,
                            if (worker.isActive) MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = if (worker.isActive) "근무 인부에서 제외" else "근무 인부로 다시 등록",
                            color = if (worker.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                item {
                    Text(
                        text = if (worker.isActive) {
                            "제외해도 이전 작업과 지급 내역은 그대로 보관됩니다."
                        } else {
                            "다시 등록하면 작업 기록에서 선택할 수 있어요."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = {
                Text(if (worker.isActive) "근무 인부에서 제외할까요?" else "다시 근무 인부로 등록할까요?")
            },
            text = {
                Text(
                    if (worker.isActive) {
                        "이전 기록은 삭제되지 않으며 새 작업을 기록할 때만 목록에서 숨겨져요."
                    } else {
                        "새 작업을 기록할 때 ${worker.name}님을 다시 선택할 수 있어요."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStatusDialog = false
                    onActiveChange(!worker.isActive)
                }) {
                    Text(if (worker.isActive) "제외하기" else "다시 등록")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun WorkerHeader(worker: WorkerSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = worker.name.take(1),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column {
            Text(worker.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = if (worker.isActive) "근무 중" else "비활성",
                modifier = Modifier.padding(top = 5.dp),
                color = if (worker.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WorkerPaySummary(worker: WorkerSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryValue("기본 일당", "${worker.dailyWage.toWon()}원", Modifier.weight(1f))
                SummaryValue("총 공수", "${worker.totalWorkUnits.toWorkUnit()}공수", Modifier.weight(1f))
            }
            SummaryValue("미지급 금액", "${worker.unpaidAmount.toWon()}원")
        }
    }
}

@Composable
private fun SiteWageCard(worker: WorkerSummary, sites: List<SiteSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("현장별 일당", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                sites.filter { it.status == SiteStatus.Active }.forEach { site ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(site.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (site.id in worker.siteDailyWages) "현장별 설정" else "기본 일당 적용",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        }
                        Text(
                            text = "${worker.dailyWageFor(site.id).toWon()}원",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                    }
                }
            }
        }
        Text(
            text = "별도 설정이 없는 현장에서는 기본 일당이 적용됩니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
        Text(
            text = value,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            rows.forEach { (label, value) ->
                Column {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(value, modifier = Modifier.padding(top = 4.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun WorkerEditForm(
    name: String,
    onNameChange: (String) -> Unit,
    wageDigits: String,
    onWageChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    memo: String,
    onMemoChange: (String) -> Unit,
    sites: List<SiteSummary>,
    siteWageDigits: Map<Long, String>,
    onSiteWageChange: (Long, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("이름 (필수)") },
            supportingText = { Text("${name.length}/20자") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = wageDigits.toWonInput(),
            onValueChange = onWageChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("기본 일당 (필수)") },
            suffix = { Text("원") },
            supportingText = { Text("현장별 일당이 없을 때 적용") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("현장별 일당", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "비워두면 기본 일당이 적용됩니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            sites.forEach { site ->
                OutlinedTextField(
                    value = siteWageDigits[site.id].orEmpty().toWonInput(),
                    onValueChange = { onSiteWageChange(site.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(site.name) },
                    placeholder = { Text("기본 일당 적용") },
                    suffix = { Text("원") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("전화번호") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = memo,
            onValueChange = onMemoChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("역할 또는 메모") },
            supportingText = { Text("${memo.length}/50자") },
            minLines = 2,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

private fun String.toWonInput(): String = toLongOrNull()?.toWon().orEmpty()

private fun Double.toWorkUnit(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkerDetailScreenPreview() {
    InbuLedgerTheme { WorkerDetailScreen(worker = WorkerMockData.workers.first()) }
}
