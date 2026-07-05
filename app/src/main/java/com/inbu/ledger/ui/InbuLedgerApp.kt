package com.inbu.ledger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.inbu.ledger.BuildConfig
import com.inbu.ledger.auth.KakaoLoginClient
import com.inbu.ledger.auth.KakaoLoginResult
import com.inbu.ledger.ui.auth.LoginScreen
import com.inbu.ledger.ui.home.HomeScreen

private enum class AppScreen {
    Login,
    Home,
}

@Composable
fun InbuLedgerApp() {
    val context = LocalContext.current
    val loginClient = remember { KakaoLoginClient() }
    var appScreen by rememberSaveable { mutableStateOf(AppScreen.Login) }
    var isLoggingIn by rememberSaveable { mutableStateOf(false) }
    var loginMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val isKakaoConfigured = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()

    when (appScreen) {
        AppScreen.Login -> LoginScreen(
            isLoading = isLoggingIn,
            message = loginMessage,
            onKakaoLogin = {
                if (!isKakaoConfigured) {
                    loginMessage = "카카오 앱 키를 연결한 뒤 로그인할 수 있어요."
                    return@LoginScreen
                }

                loginMessage = null
                isLoggingIn = true
                loginClient.login(context) { result ->
                    isLoggingIn = false
                    when (result) {
                        is KakaoLoginResult.Success -> {
                            // 서버 구현 단계에서 accessToken을 서버 세션으로 교환합니다.
                            appScreen = AppScreen.Home
                        }
                        KakaoLoginResult.Cancelled -> Unit
                        is KakaoLoginResult.Failure -> {
                            loginMessage = "로그인하지 못했어요. 잠시 후 다시 시도해 주세요."
                        }
                    }
                }
            },
        )
        AppScreen.Home -> HomeScreen()
    }
}

