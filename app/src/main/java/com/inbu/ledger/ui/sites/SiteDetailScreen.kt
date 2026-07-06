package com.inbu.ledger.ui.sites

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    site: SiteSummary,
    onBack: () -> Unit = {},
    onSave: (SiteSummary) -> Unit = {},
    onMoveToTrash: () -> Unit = {},
) {
    var isEditing by rememberSaveable(site.id) { mutableStateOf(false) }
    var name by rememberSaveable(site.id) { mutableStateOf(site.name) }
    var memo by rememberSaveable(site.id) { mutableStateOf(site.memo) }
    var status by rememberSaveable(site.id) { mutableStateOf(site.status) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "현장 수정" else "현장 상세",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) {
                            name = site.name
                            memo = site.memo
                            status = site.status
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isEditing) {
                item {
                    EditForm(
                        name = name,
                        onNameChange = { if (it.length <= 30) name = it },
                        memo = memo,
                        onMemoChange = { if (it.length <= 100) memo = it },
                        status = status,
                        onStatusChange = { status = it },
                    )
                }
                item {
                    Button(
                        onClick = {
                            onSave(
                                site.copy(
                                    name = name.trim(),
                                    memo = memo.trim().ifBlank { "추가 정보 없음" },
                                    status = status,
                                ),
                            )
                            isEditing = false
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("수정 내용 저장", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                item { SiteHeader(site) }
                item { SummaryCard(site) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("현장 메모", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        ) {
                            Text(
                                text = site.memo,
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text(
                            text = "휴지통으로 이동",
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                item {
                    Text(
                        text = "삭제되지 않으며 휴지통에서 복구할 수 있어요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("현장을 휴지통으로 옮길까요?") },
            text = { Text("${site.name} 현장은 목록에서 숨겨지지만 휴지통에서 복구할 수 있어요.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onMoveToTrash()
                }) {
                    Text("휴지통으로 이동", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun SiteHeader(site: SiteSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(site.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            text = site.status.label,
            color = if (site.status == SiteStatus.Active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SummaryCard(site: SiteSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            SummaryMetric("총 공수", "${site.totalWorkUnits.toWorkUnit()}공수", Modifier.weight(1f))
            SummaryMetric("미지급 금액", "${site.unpaidAmount.toWon()}원", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f), fontSize = 14.sp)
        Text(
            value,
            modifier = Modifier.padding(top = 5.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun EditForm(
    name: String,
    onNameChange: (String) -> Unit,
    memo: String,
    onMemoChange: (String) -> Unit,
    status: SiteStatus,
    onStatusChange: (SiteStatus) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("현장명 (필수)") },
            supportingText = { Text("${name.length}/30자") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = memo,
            onValueChange = onMemoChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("메모") },
            supportingText = { Text("${memo.length}/100자") },
            minLines = 3,
            shape = RoundedCornerShape(16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("현장 상태", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SiteStatus.entries.forEach { option ->
                    FilterChip(
                        selected = status == option,
                        onClick = { onStatusChange(option) },
                        label = { Text(option.label, fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
        }
    }
}

private fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)

private fun Double.toWorkUnit(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SiteDetailScreenPreview() {
    InbuLedgerTheme {
        SiteDetailScreen(site = SiteMockData.sites.first())
    }
}
