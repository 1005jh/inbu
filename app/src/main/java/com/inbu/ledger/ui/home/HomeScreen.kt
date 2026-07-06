package com.inbu.ledger.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import com.inbu.ledger.ui.components.MainBottomBar
import com.inbu.ledger.ui.components.MainSection
import java.text.NumberFormat
import java.util.Locale

private const val AllSites = "전체 현장"

@Composable
fun HomeScreen(
    uiState: HomeUiState = HomeMockData.state,
    onRecordWork: () -> Unit = {},
    onOpenRecord: (Long) -> Unit = {},
    onSectionSelected: (MainSection) -> Unit = {},
) {
    var selectedSite by rememberSaveable { mutableStateOf(AllSites) }
    val sites = listOf(AllSites) + uiState.records.map { it.siteName }.distinct()
    val visibleRecords = uiState.records.filter {
        selectedSite == AllSites || it.siteName == selectedSite
    }
    val todayRecords = visibleRecords.filter { it.isToday }
    val todayWorkers = todayRecords.sumOf { it.workerCount }
    val todayWorkUnits = todayRecords.sumOf { it.workUnits }
    val unpaidAmount = visibleRecords.sumOf { it.unpaidAmount }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainBottomBar(
                selectedSection = MainSection.Home,
                onSectionSelected = onSectionSelected,
            )
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
                HomeHeader(todayLabel = uiState.todayLabel)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "현장 선택",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sites) { site ->
                            FilterChip(
                                selected = selectedSite == site,
                                onClick = { selectedSite = site },
                                label = {
                                    Text(
                                        text = site,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onRecordWork,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "+  오늘 작업 기록하기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryCard(
                        label = "오늘 투입",
                        value = "${todayWorkers}명",
                        supportingText = "${todayWorkUnits.toWorkUnit()}공수 · ${todayRecords.size}개 현장",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCard(
                        label = "전체 미지급",
                        value = "${unpaidAmount.toWon()}원",
                        supportingText = "확인이 필요한 금액",
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "최근 작업 기록",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${visibleRecords.size}건",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (visibleRecords.isEmpty()) {
                item { EmptyRecordsCard() }
            } else {
                items(visibleRecords, key = { it.id }) { record ->
                    WorkRecordCard(
                        record = record,
                        onClick = { onOpenRecord(record.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(todayLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "안녕하세요",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "오늘도 안전하게 기록해요.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = todayLabel,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                modifier = Modifier.padding(top = 8.dp),
                color = valueColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = 7.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WorkRecordCard(
    record: WorkRecordSummary,
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
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = record.dateShortLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(
                        text = record.siteName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${record.dateLabel} · ${record.workerCount}명 · ${record.workUnits.toWorkUnit()}공수",
                        modifier = Modifier.padding(top = 5.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                    )
                }
                PaymentBadge(record.paymentState)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "총 일당",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${record.totalWage.toWon()}원",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (record.unpaidAmount > 0) {
                Row(
                    modifier = Modifier.padding(top = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "남은 금액",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${record.unpaidAmount.toWon()}원",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentBadge(state: PaymentState) {
    val colors = when (state) {
        PaymentState.Unpaid -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
        PaymentState.PartiallyPaid -> Color(0xFFFFE8B3) to Color(0xFF604500)
        PaymentState.Paid -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        shape = CircleShape,
        color = colors.first,
    ) {
        Text(
            text = state.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = colors.second,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyRecordsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Text(
            text = "선택한 현장의 작업 기록이 없어요.",
            modifier = Modifier.padding(22.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Long.toWon(): String = NumberFormat
    .getNumberInstance(Locale.KOREA)
    .format(this)

private fun Double.toWorkUnit(): String = if (this % 1.0 == 0.0) {
    toInt().toString()
} else {
    toString()
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    InbuLedgerTheme {
        HomeScreen()
    }
}
