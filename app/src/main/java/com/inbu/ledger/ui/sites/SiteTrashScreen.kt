package com.inbu.ledger.ui.sites

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
import com.inbu.ledger.ui.theme.InbuLedgerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteTrashScreen(
    sites: List<SiteSummary>,
    protectedSiteIds: Set<Long> = emptySet(),
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
                title = { Text("현장 휴지통", fontWeight = FontWeight.Bold) },
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

            if (sites.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Text("현장 휴지통이 비어 있어요.", modifier = Modifier.padding(22.dp))
                    }
                }
            } else {
                items(sites, key = { it.id }) { site ->
                    val hasRecords = site.id in protectedSiteIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(site.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(site.memo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onRestore(site.id) },
                                    modifier = Modifier.weight(1f),
                                ) { Text("복구", fontWeight = FontWeight.Bold) }
                                OutlinedButton(
                                    onClick = { deleteTargetId = site.id },
                                    modifier = Modifier.weight(1f),
                                    enabled = !hasRecords,
                                ) {
                                    Text(
                                        text = if (hasRecords) "기록 연결됨" else "영구 삭제",
                                        color = if (hasRecords) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (hasRecords) {
                                Text(
                                    text = "연결된 작업 기록이 있어 영구 삭제할 수 없어요.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTargetId?.let { siteId ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("현장을 완전히 삭제할까요?") },
            text = { Text("삭제한 뒤에는 이 현장을 복구할 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTargetId = null
                    onDeletePermanently(siteId)
                }) { Text("영구 삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text("취소") }
            },
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SiteTrashScreenPreview() {
    InbuLedgerTheme {
        SiteTrashScreen(sites = SiteMockData.sites.take(2))
    }
}
