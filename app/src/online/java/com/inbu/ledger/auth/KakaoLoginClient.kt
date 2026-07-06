package com.inbu.ledger.auth

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

sealed interface KakaoLoginResult {
    data class Success(val accessToken: String) : KakaoLoginResult
    data object Cancelled : KakaoLoginResult
    data class Failure(val cause: Throwable) : KakaoLoginResult
}

class KakaoLoginClient {
    fun login(context: Context, onResult: (KakaoLoginResult) -> Unit) {
        val accountLoginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            onResult(token.toLoginResult(error))
        }
        if (!UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoAccount(context = context, callback = accountLoginCallback)
            return
        }
        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
            when {
                token != null -> onResult(KakaoLoginResult.Success(token.accessToken))
                error is ClientError && error.reason == ClientErrorCause.Cancelled -> onResult(KakaoLoginResult.Cancelled)
                else -> UserApiClient.instance.loginWithKakaoAccount(context = context, callback = accountLoginCallback)
            }
        }
    }
}

private fun OAuthToken?.toLoginResult(error: Throwable?): KakaoLoginResult = when {
    this != null -> KakaoLoginResult.Success(accessToken)
    error is ClientError && error.reason == ClientErrorCause.Cancelled -> KakaoLoginResult.Cancelled
    error != null -> KakaoLoginResult.Failure(error)
    else -> KakaoLoginResult.Failure(IllegalStateException("카카오 로그인 결과가 없습니다."))
}
