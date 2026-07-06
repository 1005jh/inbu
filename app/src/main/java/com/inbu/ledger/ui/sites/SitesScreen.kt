package com.inbu.ledger.ui.sites

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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

private enum class SiteFilter(val label: String) {
    All("전체"),
    Active("진행 중"),
    Completed("종료"),
}

@Composable
fun SitesScreen(
    sites: List<SiteSummary> = SiteMockData.sites,
    onAddSite: () -> Unit = {},
    onOpenSite: (Long) -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onManageWorkers: () -> Unit = {},
    onSectionSelected: (MainSection) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(SiteFilter.All) }
    val visibleSites = sites.filter { site ->
        val matchesQuery = query.isBlank() || site.name.contains(query, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            SiteFilter.All -> true
            SiteFilter.Active -> site.status == SiteStatus.Active
            SiteFilter.Completed -> site.status == SiteStatus.Completed
        }
        matchesQuery && matchesFilter
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
                        text = "현장 관리",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "작업할 현장을 추가하고 상태를 확인하세요.",
                        modifier = Modifier.padding(top = 5.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                ManagementCategoryTabs(
                    selected = ManagementCategory.Sites,
                    onSitesClick = {},
                    onWorkersClick = onManageWorkers,
                )
            }

            item {
                Button(
                    onClick = onAddSite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "+  새 현장 추가",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenTrash,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(
                        text = "현장 휴지통",
                        modifier = Modifier.padding(start = 7.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("현장명 검색") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SiteFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
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

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "등록된 현장",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${visibleSites.size}개",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (visibleSites.isEmpty()) {
                item { EmptySitesCard() }
            } else {
                items(visibleSites, key = { it.id }) { site ->
                    SiteCard(
                        site = site,
                        onClick = { onOpenSite(site.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SiteCard(
    site: SiteSummary,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = site.memo,
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                    )
                }
                SiteStatusBadge(site.status)
            }

            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                SiteMetric(
                    label = "총 공수",
                    value = "${site.totalWorkUnits.toWorkUnit()}공수",
                )
                SiteMetric(
                    label = "미지급 금액",
                    value = "${site.unpaidAmount.toWon()}원",
                    valueColor = if (site.unpaidAmount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun SiteStatusBadge(status: SiteStatus) {
    val colors = when (status) {
        SiteStatus.Active -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        SiteStatus.Completed -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = colors.first,
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            color = colors.second,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SiteMetric(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 5.dp),
            color = valueColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptySitesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Text(
            text = "검색 조건에 맞는 현장이 없어요.",
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
private fun SitesScreenPreview() {
    InbuLedgerTheme {
        SitesScreen()
    }
}
