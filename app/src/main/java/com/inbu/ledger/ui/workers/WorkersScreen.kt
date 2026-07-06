package com.inbu.ledger.ui.workers

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.inbu.ledger.ui.components.ManagementCategory
import com.inbu.ledger.ui.components.ManagementCategoryTabs
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WorkersScreen(
    workers: List<WorkerSummary> = WorkerMockData.workers,
    onAddWorker: () -> Unit = {},
    onOpenWorker: (Long) -> Unit = {},
    onManageSites: () -> Unit = {},
    onSectionSelected: (MainSection) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleWorkers = workers.filter { worker ->
        query.isBlank() || worker.name.contains(query, ignoreCase = true) ||
            worker.memo.contains(query, ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainBottomBar(
                selectedSection = MainSection.Manage,
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
                Column {
                    Text(
                        text = "인부 관리",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "이름과 일당을 등록해 작업 기록에서 바로 선택하세요.",
                        modifier = Modifier.padding(top = 5.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                ManagementCategoryTabs(
                    selected = ManagementCategory.Workers,
                    onSitesClick = onManageSites,
                    onWorkersClick = {},
                )
            }

            item {
                Button(
                    onClick = onAddWorker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("+  새 인부 추가", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("이름 또는 역할 검색") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "등록된 인부",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${visibleWorkers.size}명",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (visibleWorkers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Text(
                            text = "검색 조건에 맞는 인부가 없어요.",
                            modifier = Modifier.padding(22.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(visibleWorkers, key = { it.id }) { worker ->
                    WorkerCard(
                        worker = worker,
                        onClick = { onOpenWorker(worker.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerCard(
    worker: WorkerSummary,
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
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = worker.name.take(1),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 13.dp),
                ) {
                    Text(worker.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = worker.memo.ifBlank { "역할 미입력" },
                        modifier = Modifier.padding(top = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (worker.isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = if (worker.isActive) "근무 중" else "비활성",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                WorkerMetric("기본 일당", "${worker.dailyWage.toWon()}원")
                WorkerMetric(
                    label = "미지급 금액",
                    value = "${worker.unpaidAmount.toWon()}원",
                    valueColor = if (worker.unpaidAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun WorkerMetric(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(
            text = value,
            modifier = Modifier.padding(top = 5.dp),
            color = valueColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkersScreenPreview() {
    InbuLedgerTheme { WorkersScreen() }
}
