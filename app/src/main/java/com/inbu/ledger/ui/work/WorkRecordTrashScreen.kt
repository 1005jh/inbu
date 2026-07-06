package com.inbu.ledger.ui.work

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.inbu.ledger.ui.sites.SiteMockData
import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkRecordTrashScreen(
    records: List<WorkRecordSummary>,
    sites: List<SiteSummary>,
    protectedRecordIds: Set<Long> = emptySet(),
    onBack: () -> Unit = {},
    onRestore: (Long) -> Unit = {},
    onDeletePermanently: (Long) -> Unit = {},
) {
    var deleteTargetId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("작업 기록 휴지통", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "복구하거나 한 번 더 삭제하면 완전히 사라집니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (records.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Text(
                            text = "휴지통이 비어 있어요.",
                            modifier = Modifier.padding(22.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(records, key = { it.id }) { record ->
                    val siteName = sites.firstOrNull { it.id == record.siteId }?.name ?: "삭제된 현장"
                    val isPaymentLinked = record.id in protectedRecordIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(siteName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                LocalDate.ofEpochDay(record.dateEpochDay)
                                    .format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${record.workers.size}명 · ${record.workers.sumOf { it.workUnits }.toWorkUnit()}공수 · ${record.totalCost().toWon()}원",
                            )
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onRestore(record.id) },
                                    modifier = Modifier.weight(1f),
                                ) { Text("복구", fontWeight = FontWeight.Bold) }
                                OutlinedButton(
                                    onClick = { deleteTargetId = record.id },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isPaymentLinked,
                                ) {
                                    Text(
                                        text = if (isPaymentLinked) "지급 연결됨" else "영구 삭제",
                                        color = if (isPaymentLinked) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (isPaymentLinked) {
                                Text(
                                    text = "연결된 지급 내역을 먼저 취소해야 영구 삭제할 수 있어요.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTargetId?.let { recordId ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("완전히 삭제할까요?") },
            text = { Text("삭제한 뒤에는 이 작업 기록을 복구할 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTargetId = null
                    onDeletePermanently(recordId)
                }) { Text("영구 삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text("취소") }
            },
        )
    }
}

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

private fun Double.toWorkUnit(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkRecordTrashScreenPreview() {
    InbuLedgerTheme {
        WorkRecordTrashScreen(
            records = WorkRecordMockData.records.take(2),
            sites = SiteMockData.sites,
        )
    }
}
