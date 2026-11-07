package com.samdori93.yeoksadam.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.samdori93.yeoksadam.core.backend.source.GeminiVisionDataSource
import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.common.error.AppError
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.HeritageVisionResult
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage
import com.samdori93.yeoksadam.core.domain.repository.VisionRepository
import com.samdori93.yeoksadam.core.network.worker.VisionBody
import com.samdori93.yeoksadam.core.network.worker.VisionCandidate
import com.samdori93.yeoksadam.core.network.worker.WorkerApi
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max

/**
 * 사진 속 문화재 판별.
 *  1) Gemini 비전 (앱의 GEMINI_API_KEY) — 기본
 *  2) 실패하면 역사담 서버 /vision (웹앱과 같은 서버)
 * 둘 다 안 되면 Failure → 유스케이스가 가까운 국가유산으로 대체하고 그 사실을 표시한다.
 * 사진은 긴 변 1280px JPEG 로 줄여 보낸다.
 */
class VisionRepositoryImpl @Inject constructor(
    private val worker: WorkerApi,
    private val gemini: GeminiVisionDataSource,
    private val dispatchers: DispatcherProvider,
) : VisionRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun recognize(imageJpeg: ByteArray, candidates: List<RecognizedHeritage>): Result<HeritageVisionResult> {
        val small = withContext(dispatchers.default) { shrink(imageJpeg) }
        if (gemini.configured) {
            val viaGemini = withContext(dispatchers.io) { runCatching { parse(gemini.generate(small, prompt(candidates)), candidates) } }
            viaGemini.getOrNull()?.let { return Result.Success(it) }
        }
        return viaWorker(small, candidates)
    }

    private suspend fun viaWorker(small: ByteArray, candidates: List<RecognizedHeritage>): Result<HeritageVisionResult> =
        try {
            val r = worker.vision(
                VisionBody(
                    image = Base64.encodeToString(small, Base64.NO_WRAP),
                    candidates = candidates.take(MAX_CANDIDATES).map { VisionCandidate(it.id, it.name, it.kind, it.address) },
                ),
            )
            Result.Success(
                HeritageVisionResult(
                    name = r.name,
                    kind = r.kind,
                    era = r.era,
                    description = r.description,
                    matchedHeritageId = r.matchedId,
                    confidence = r.confidence,
                    isHeritage = r.isHeritage,
                    category = r.category,
                ),
            )
        } catch (e: HttpException) {
            Result.Failure(AppError.Server(e.code(), "사진을 판별하지 못했습니다."))
        } catch (e: IOException) {
            Result.Failure(AppError.Network(e))
        }

    private fun prompt(candidates: List<RecognizedHeritage>): String {
        val candidateBlock = if (candidates.isEmpty()) {
            "(주변 후보 없음)"
        } else {
            candidates.take(MAX_CANDIDATES).joinToString("\n") { "- id=${it.id} | ${it.name} | ${it.kind} | ${it.address}" }
        }
        return """
            너는 한국 문화재·유물 감정 전문가다. 사용자가 촬영한 사진 1장을 보고, 그 안의 문화재/유물/건축물/고지도/서화가 무엇인지 판별하라.

            아래는 사용자의 현재 위치 주변 국가유산 후보다(참고용 힌트일 뿐):
            $candidateBlock

            규칙:
            - matchedId: 사진 속 대상이 위 후보 목록의 그 국가유산 "자체"(같은 건물·유적)일 때만 그 id 를 넣어라.
            - 유물·고지도·서화·책·초상화·동상 등은 대개 주변 유적지와 다른 대상이다. 이런 경우 matchedId 를 null 로 두고, 주변 유적지에 억지로 맞추지 말고 네 지식으로 "그 대상 자체"를 판별하라. (예: 대동여지도, 훈민정음 해례본, 백자 달항아리)
            - 책·화면·인쇄물에 실린 문화재 사진이나 복제품이어도, 그 대상이 무엇인지 판별하라.
            - isHeritage: 대상이 문화재/유물/유적/전통 건축물/고지도/고문헌이거나 "역사 인물의 초상화·동상"이면 true. 실제 사람이나 현대 사물이면 false.
            - category: "figure"(인물의 초상화·동상 등), "site"(건물·궁궐·성곽·탑·유적), "relic"(도자기·회화·지도·문헌·공예품 등 그 외 유물).
            - description 은 한국어로 2~3문장, 사실 위주(과장·창작 금지).
            - 확신이 낮으면 confidence(0~100) 를 낮게 매겨라.

            반드시 아래 JSON 형식으로만 답하라(다른 텍스트 금지):
            {"matchedId": string|null, "name": string, "kind": string, "era": string, "description": string, "confidence": number, "isHeritage": boolean, "category": string}
        """.trimIndent()
    }

    private fun parse(text: String, candidates: List<RecognizedHeritage>): HeritageVisionResult {
        val body = text.substringAfter('{', "").substringBeforeLast('}', "")
        val obj = json.parseToJsonElement("{$body}").jsonObject
        fun field(key: String): String? = obj[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }

        val matchedId = field("matchedId")?.takeIf { id -> candidates.any { it.id == id } }
        val name = field("name") ?: throw IOException("판별 결과에 이름이 없습니다.")
        return HeritageVisionResult(
            name = name,
            kind = field("kind").orEmpty(),
            era = field("era").orEmpty(),
            description = field("description").orEmpty(),
            matchedHeritageId = matchedId,
            confidence = (field("confidence")?.toDoubleOrNull()?.toInt() ?: 0).coerceIn(0, 100),
            isHeritage = field("isHeritage")?.toBooleanStrictOrNull() ?: (matchedId != null),
            category = field("category")?.lowercase().orEmpty(),
        )
    }

    private fun shrink(jpeg: ByteArray): ByteArray {
        val src = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return jpeg
        val scale = MAX_SIDE.toFloat() / max(src.width, src.height)
        val bmp = if (scale < 1f) Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true) else src
        return ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
            out.toByteArray()
        }
    }

    private companion object {
        const val MAX_SIDE = 1280
        const val MAX_CANDIDATES = 12
    }
}
