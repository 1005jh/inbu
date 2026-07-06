package com.inbu.ledger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ManagementCategory {
    Sites,
    Workers,
}

@Composable
fun ManagementCategoryTabs(
    selected: ManagementCategory,
    onSitesClick: () -> Unit,
    onWorkersClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CategoryButton(
            text = "현장",
            selected = selected == ManagementCategory.Sites,
            onClick = onSitesClick,
            modifier = Modifier.weight(1f),
        )
        CategoryButton(
            text = "인부",
            selected = selected == ManagementCategory.Workers,
            onClick = onWorkersClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CategoryButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            shape = RoundedCornerShape(15.dp),
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}
