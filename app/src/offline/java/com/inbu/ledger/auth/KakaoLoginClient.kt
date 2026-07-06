package com.inbu.ledger.auth

import android.content.Context

sealed interface KakaoLoginResult {
    data class Success(val accessToken: String) : KakaoLoginResult
    data object Cancelled : KakaoLoginResult
    data class Failure(val cause: Throwable) : KakaoLoginResult
}

class KakaoLoginClient {
    fun login(context: Context, onResult: (KakaoLoginResult) -> Unit) {
        onResult(KakaoLoginResult.Failure(IllegalStateException("기기 저장 버전에는 로그인이 없습니다.")))
    }
}
