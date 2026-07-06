package com.inbu.ledger.ui.workers

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AddWorkerScreen(
    onBack: () -> Unit,
    onSave: (name: String, dailyWage: Long, phone: String, memo: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var dailyWageDigits by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    val dailyWage = dailyWageDigits.toLongOrNull() ?: 0L
    val canSave = name.isNotBlank() && dailyWage > 0

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
                    onClick = { onSave(name.trim(), dailyWage, phone.trim(), memo.trim()) },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("인부 저장하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                }
                Text("새 인부 추가", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "이름과 일당은 꼭 입력해 주세요.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "작업 공수에 따라 받을 금액이 자동 계산됩니다.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            LabeledField("이름") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 김철수") },
                    supportingText = { Text("필수 · ${name.length}/20자") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            LabeledField("일당") {
                OutlinedTextField(
                    value = dailyWageDigits.toWonInput(),
                    onValueChange = { input ->
                        dailyWageDigits = input.filter(Char::isDigit).take(9).trimStart('0')
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 180,000") },
                    suffix = { Text("원") },
                    supportingText = { Text("필수 · 하루 1공수 기준") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            LabeledField("전화번호") {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { char -> char.isDigit() || char == '-' }.take(13) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 010-1234-5678") },
                    supportingText = { Text("선택") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            LabeledField("역할 또는 메모") {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it.take(50) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 목수, 반장") },
                    supportingText = { Text("선택 · ${memo.length}/50자") },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

private fun String.toWonInput(): String {
    val amount = toLongOrNull() ?: return ""
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AddWorkerScreenPreview() {
    InbuLedgerTheme {
        AddWorkerScreen(onBack = {}, onSave = { _, _, _, _ -> })
    }
}
