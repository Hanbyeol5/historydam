package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.common.error.AppError
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.HeritageVisionResult
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage
import com.samdori93.yeoksadam.core.domain.repository.VisionRepository
import com.samdori93.yeoksadam.core.network.vision.GeminiVisionDataSource
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import javax.inject.Inject

/**
 * Gemini 멀티모달 비전으로 사진 속 문화재를 판별하는 [VisionRepository] 구현.
 * 프롬프트에 주변 국가유산 후보를 힌트로 넣고, 모델이 돌려준 JSON 을 파싱한다.
 */
class VisionRepositoryImpl @Inject constructor(
    private val gemini: GeminiVisionDataSource,
    private val dispatchers: DispatcherProvider,
) : VisionRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun recognize(
        imageJpeg: ByteArray,
        candidates: List<RecognizedHeritage>,
    ): Result<HeritageVisionResult> = withContext(dispatchers.io) {
        runCatching {
            val text = gemini.generate(imageJpeg, buildPrompt(candidates))
            parse(text, candidates)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Result.Failure(if (e is IOException) AppError.Network(e) else AppError.Unknown(e))
            },
        )
    }

    private fun buildPrompt(candidates: List<RecognizedHeritage>): String {
        val candidateBlock = if (candidates.isEmpty()) {
            "(주변 후보 없음)"
        } else {
            candidates.joinToString("\n") { "- id=${it.id} | ${it.name} | ${it.kind} | ${it.address}" }
        }
        return """
            너는 한국 문화재·유물 감정 전문가다. 사용자가 촬영한 사진 1장을 보고, 그 안의 문화재/유물/건축물이 무엇인지 판별하라.

            아래는 사용자의 현재 위치 주변 국가유산 후보다(참고용 힌트):
            $candidateBlock

            규칙:
            - matchedId: 사진 속 대상이 위 후보 목록의 그 국가유산 "자체"(같은 건물·유적)일 때만 그 id 를 넣어라.
            - 유물·초상화·동상·조각상 등은 대개 주변 유적지와 다른 대상이다. 이런 경우 matchedId 를 null 로 두고 주변 유적지에 억지로 맞추지 말고, 네 지식으로 "그 대상 자체"를 판별하라.
            - isHeritage: 대상이 문화재/유물/유적/전통 건축물이거나 "역사 인물의 초상화·동상·조각상"이면 true. 살아있는 실제 사람이나 현대 사물(노트북·휴대폰·음식 등)이면 false.
            - category: 대상을 하나로 분류 — "figure"(인물의 초상화·동상·흉상 등 사람을 묘사한 문화재), "site"(건물·궁궐·성곽·탑·유적), "relic"(도자기·회화·공예품 등 그 외 유물).
            - description 은 한국어로 2~3문장, 사실 위주(과장·창작 금지). figure 면 그 인물에 대한 설명 위주로.
            - 확신이 낮으면 confidence 를 낮게 매겨라.

            반드시 아래 JSON 형식으로만 답하라(다른 텍스트 금지):
            {"matchedId": string|null, "name": string, "kind": string, "era": string, "description": string, "confidence": number, "isHeritage": boolean, "category": string}
        """.trimIndent()
    }

    private fun parse(text: String, candidates: List<RecognizedHeritage>): HeritageVisionResult {
        val obj = json.parseToJsonElement(text).jsonObject
        fun field(key: String): String? =
            obj[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }

        val matchedId = field("matchedId")?.takeIf { id -> candidates.any { it.id == id } }
        val confidence = field("confidence")?.toDoubleOrNull()?.toInt() ?: 0
        val isHeritage = field("isHeritage")?.toBooleanStrictOrNull() ?: (matchedId != null)
        return HeritageVisionResult(
            name = field("name") ?: "미확인 유물",
            kind = field("kind").orEmpty(),
            era = field("era").orEmpty(),
            description = field("description") ?: "설명을 생성하지 못했습니다.",
            matchedHeritageId = matchedId,
            confidence = confidence.coerceIn(0, 100),
            isHeritage = isHeritage,
            category = field("category")?.lowercase().orEmpty(),
        )
    }
}
