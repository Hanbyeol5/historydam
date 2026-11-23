package com.samdori93.yeoksadam.core.network.worker

import com.samdori93.yeoksadam.core.network.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/** 로그인 세션 토큰 — AccountRepository 가 로그인·로그아웃·앱 시작 때 채운다 */
object AuthToken {
    @Volatile
    var value: String? = null
}

/**
 * 역사담 서버(Worker)로 가는 요청에만 앱 식별 헤더를 붙인다.
 * 웹앱은 Origin 으로, 앱은 X-App-Key 로 확인한다 (Worker 의 APP_KEYS 와 같은 값).
 * 로그인했으면 Authorization: Bearer <토큰> 도 붙여 서버가 사용자별로 저장·검색하게 한다.
 */
class AppKeyInterceptor : Interceptor {
    private val workerHost = BuildConfig.WORKER_BASE_URL.toHttpUrl().host

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        if (req.url.host != workerHost) return chain.proceed(req)
        val b = req.newBuilder().header("X-App-Key", BuildConfig.APP_KEY)
        AuthToken.value?.let { if (req.header("Authorization") == null) b.header("Authorization", "Bearer $it") }
        return chain.proceed(b.build())
    }
}
