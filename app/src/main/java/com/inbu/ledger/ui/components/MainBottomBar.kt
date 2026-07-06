package com.inbu.ledger.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

enum class MainSection(
    val label: String,
    val icon: ImageVector,
) {
    Home("홈", Icons.Filled.Home),
    Work("작업", Icons.Filled.Edit),
    Search("조회", Icons.Filled.Search),
    Manage("관리", Icons.Filled.Settings),
}
@Composable
fun MainBottomBar(
    selectedSection: MainSection,
    onSectionSelected: (MainSection) -> Unit,
) {
    NavigationBar(containerColor = Color.White) {
        MainSection.entries.forEach { section ->
            NavigationBarItem(
                selected = section == selectedSection,
                onClick = { onSectionSelected(section) },
                icon = {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = section.label,
                    )
                },
                label = {
                    Text(
                        text = section.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
    }
}
