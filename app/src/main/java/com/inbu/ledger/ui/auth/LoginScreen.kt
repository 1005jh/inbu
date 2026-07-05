package com.inbu.ledger.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inbu.ledger.ui.theme.InbuLedgerTheme

private val KakaoYellow = Color(0xFFFEE500)
private val KakaoBrown = Color(0xFF191919)

@Composable
fun LoginScreen(
    isLoading: Boolean,
    message: String?,
    onKakaoLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        LedgerMark()
        Spacer(Modifier.height(28.dp))

        Text(
            text = "인부장부",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "현장 기록부터 일당 지급까지\n쉽고 정확하게 관리하세요.",
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
        )

        Spacer(Modifier.height(56.dp))

        Button(
            onClick = onKakaoLogin,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KakaoYellow,
                contentColor = KakaoBrown,
                disabledContainerColor = KakaoYellow.copy(alpha = 0.65f),
                disabledContentColor = KakaoBrown.copy(alpha = 0.65f),
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = KakaoBrown,
                    strokeWidth = 2.dp,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KakaoSymbol()
                    Text(
                        text = "카카오로 시작하기",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (message != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Text(
            text = "카카오 계정 정보는 로그인 확인에만 사용됩니다.",
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun LedgerMark() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(28.dp),
            )
            .semantics { contentDescription = "인부장부 로고" },
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun KakaoSymbol() {
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = "" },
    ) {
        drawRoundRect(
            color = KakaoBrown,
            topLeft = Offset(1.dp.toPx(), 2.dp.toPx()),
            size = Size(22.dp.toPx(), 17.dp.toPx()),
            cornerRadius = CornerRadius(9.dp.toPx()),
        )
        val tail = Path().apply {
            moveTo(7.dp.toPx(), 17.dp.toPx())
            lineTo(5.dp.toPx(), 23.dp.toPx())
            lineTo(12.dp.toPx(), 18.dp.toPx())
            close()
        }
        drawPath(tail, KakaoBrown)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    InbuLedgerTheme {
        LoginScreen(
            isLoading = false,
            message = null,
            onKakaoLogin = {},
        )
    }
}
