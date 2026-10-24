package com.samdori93.yeoksadam.core.network.rag

import com.samdori93.yeoksadam.core.network.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Gemini 텍스트 생성 — historydam backend 의 `model.generate_content_async(prompt)`.
 * 키는 BuildConfig.GEMINI_API_KEY(local.properties, 커밋 금지). 네트워크를 쓰므로 IO 디스패처에서 부른다.
 *
 * 모델은 앱의 사진 인식과 같은 [MODEL] (3~4초). 백엔드의 gemini-3.8-flash 는 시험에서 한 번 답에 108초가 걸려
 * 대화용으로 쓰지 않고, [MODEL] 이 없어졌을 때(404)만 [FALLBACK_MODEL] 로 쓴다.
 */
class GeminiTextDataSource @Inject constructor(
    client: OkHttpClient,
) {
    private val http = client.newBuilder().readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    val configured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /** @throws IllegalStateException 키가 없거나 Gemini 가 오류를 돌려줄 때 (화면에 그대로 보여 줄 문장) */
    fun generate(prompt: String): String {
        check(configured) { "Gemini 키가 없어요. local.properties 에 GEMINI_API_KEY 를 넣고 다시 빌드해 주세요." }
        val payload = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject { putJsonArray("parts") { addJsonObject { put("text", prompt) } } }
            }
        }.toString().toRequestBody(JSON_MEDIA)

        var model = MODEL
        var lastError: IOException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val request = Request.Builder()
                .url("$ENDPOINT$model:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
                .post(payload)
                .build()
            http.newCall(request).execute().use { res ->
                val body = res.body?.string().orEmpty()
                when {
                    res.isSuccessful -> return extractText(body)
                    res.code == 404 && model != FALLBACK_MODEL -> model = FALLBACK_MODEL
                    res.code == 503 || res.code == 429 -> lastError = IOException("Gemini 일시적 오류 ${res.code}")
                    else -> error("Gemini 가 답하지 못했어요. (${res.code})")
                }
            }
            if (attempt < MAX_ATTEMPTS - 1) Thread.sleep(RETRY_DELAY_MS)
        }
        throw lastError ?: IOException("Gemini 호출에 실패했습니다.")
    }

    /** candidates[0].content.parts[*].text */
    private fun extractText(body: String): String {
        val candidates = json.parseToJsonElement(body).jsonObject["candidates"]?.jsonArray
        val parts = candidates?.firstOrNull()?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
        // 생각 과정(thought: true) 조각은 빼고 답만
        val text = parts?.map { it.jsonObject }
            ?.filter { it["thought"]?.jsonPrimitive?.content != "true" }
            ?.mapNotNull { it["text"]?.jsonPrimitive?.content }
            ?.joinToString("").orEmpty()
        check(text.isNotBlank()) { "Gemini 가 빈 답을 돌려줬어요." }
        return text.trim()
    }

    private companion object {
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val MODEL = "gemini-3-flash-preview"
        const val FALLBACK_MODEL = "gemini-3.8-flash"
        const val READ_TIMEOUT_S = 120L
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1_500L
        val JSON_MEDIA = "application/json".toMediaType()
    }
}
