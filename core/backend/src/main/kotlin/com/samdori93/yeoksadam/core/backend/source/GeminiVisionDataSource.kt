package com.samdori93.yeoksadam.core.backend.source

import android.util.Base64
import com.samdori93.yeoksadam.core.backend.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Gemini 멀티모달(사진 + 프롬프트) — 사진 속 문화재 판별용. 답은 JSON 문자열.
 * 키는 BuildConfig.GEMINI_API_KEY (local.properties, 커밋 금지). 네트워크를 쓰므로 IO 디스패처에서 부른다.
 */
class GeminiVisionDataSource @Inject constructor(
    client: OkHttpClient,
) {
    private val http = client.newBuilder().readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    val configured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /** @param imageJpeg 이미 줄인 JPEG */
    fun generate(imageJpeg: ByteArray, prompt: String): String {
        check(configured) { "Gemini 키가 없어요." }
        val payload = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                        addJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", "image/jpeg")
                                put("data", Base64.encodeToString(imageJpeg, Base64.NO_WRAP))
                            }
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            }
        }.toString().toRequestBody(JSON_MEDIA)

        var lastError: IOException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val request = Request.Builder()
                .url("$ENDPOINT$MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
                .post(payload)
                .build()
            http.newCall(request).execute().use { res ->
                val body = res.body?.string().orEmpty()
                when {
                    res.isSuccessful -> return extractText(body)
                    res.code == 503 || res.code == 429 -> lastError = IOException("Gemini 일시적 오류 ${res.code}")
                    else -> throw IOException("Gemini 비전 오류 ${res.code}")
                }
            }
            if (attempt < MAX_ATTEMPTS - 1) Thread.sleep(RETRY_DELAY_MS)
        }
        throw lastError ?: IOException("Gemini 호출에 실패했습니다.")
    }

    private fun extractText(body: String): String {
        val parts = json.parseToJsonElement(body).jsonObject["candidates"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
        return parts?.map { it.jsonObject }
            ?.filter { it["thought"]?.jsonPrimitive?.content != "true" }
            ?.mapNotNull { it["text"]?.jsonPrimitive?.content }
            ?.joinToString("")
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException("Gemini 응답에 결과가 없습니다.")
    }

    private companion object {
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val MODEL = "gemini-3-flash-preview"
        const val READ_TIMEOUT_S = 60L
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 1_200L
        val JSON_MEDIA = "application/json".toMediaType()
    }
}
