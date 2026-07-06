package com.inbu.ledger.ui.sites

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inbu.ledger.ui.theme.InbuLedgerTheme

@Composable
fun AddSiteScreen(
    onBack: () -> Unit,
    onSave: (name: String, memo: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    val canSave = name.isNotBlank()

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = { onSave(name.trim(), memo.trim()) },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "현장 저장하기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로 가기",
                    )
                }
                Text(
                    text = "새 현장 추가",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "현장명만 입력해도 등록할 수 있어요.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "새 현장은 ‘진행 중’ 상태로 등록됩니다.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "현장명",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 마곡 오피스텔") },
                    supportingText = { Text("필수 · ${name.length}/30자") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "메모",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it.take(100) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("주소나 공사 내용을 적어주세요.\n예: 서울 강서구 · 골조 공사")
                    },
                    supportingText = { Text("선택 · ${memo.length}/100자") },
                    minLines = 4,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AddSiteScreenPreview() {
    InbuLedgerTheme {
        AddSiteScreen(
            onBack = {},
            onSave = { _, _ -> },
        )
    }
}
