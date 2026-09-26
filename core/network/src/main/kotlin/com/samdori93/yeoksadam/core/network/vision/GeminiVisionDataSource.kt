package com.samdori93.yeoksadam.core.network.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.samdori93.yeoksadam.core.network.BuildConfig
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Gemini `generateContent`(멀티모달) 호출.
 * 사진(JPEG) + 프롬프트를 전송하고, 모델이 생성한 텍스트(JSON 문자열)를 반환한다.
 *
 * 키는 BuildConfig.GEMINI_API_KEY(local.properties). 온디바이스 직접 호출은 데모용이며,
 * 실서비스는 백엔드 프록시(`/v1/vision/recognize`) 경유가 정석이다.
 */
class GeminiVisionDataSource @Inject constructor(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun generate(imageJpeg: ByteArray, prompt: String): String {
        val key = BuildConfig.GEMINI_API_KEY
        require(key.isNotBlank()) {
            "GEMINI_API_KEY 가 없습니다. local.properties 에 GEMINI_API_KEY=... 를 추가하세요."
        }

        val base64Image = Base64.encodeToString(downscaleJpeg(imageJpeg), Base64.NO_WRAP)
        val payload = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                        addJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            }
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            }
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA)
        var lastError: IOException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val request = Request.Builder()
                .url("$ENDPOINT$MODEL:generateContent?key=$key")
                .post(requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> return extractText(body)
                    // 503(과부하)/429(레이트리밋)는 잠시 후 재시도
                    response.code == 503 || response.code == 429 ->
                        lastError = IOException("Gemini 일시적 오류 ${response.code}")
                    else -> throw IOException("Gemini API 오류 ${response.code}: ${body.take(300)}")
                }
            }
            if (attempt < MAX_ATTEMPTS - 1) Thread.sleep(RETRY_DELAY_MS)
        }
        throw lastError ?: IOException("Gemini 호출에 실패했습니다.")
    }

    /** 응답 candidates[0].content.parts[0].text 를 추출. */
    private fun extractText(responseBody: String): String {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val candidates = root["candidates"]?.jsonArray
        require(!candidates.isNullOrEmpty()) { "Gemini 응답에 결과가 없습니다: ${responseBody.take(200)}" }
        val parts = candidates[0].jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray
        return parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: error("Gemini 응답 파싱 실패")
    }

    /** 전송량·속도를 위해 최대 변 1024px, JPEG 85% 로 축소. 실패 시 원본 사용. */
    private fun downscaleJpeg(original: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(original, 0, original.size) ?: return original
        val longest = maxOf(bitmap.width, bitmap.height)
        val scaled = if (longest > MAX_DIMEN) {
            val ratio = MAX_DIMEN.toFloat() / longest
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    private companion object {
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val MODEL = "gemini-3-flash-preview"
        const val MAX_DIMEN = 1024
        const val JPEG_QUALITY = 85
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1_500L
        val JSON_MEDIA = "application/json".toMediaType()
    }
}
