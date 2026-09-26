package com.samdori93.yeoksadam.core.domain.model

/**
 * 멀티모달 AI(Gemini)가 사진을 보고 판별한 결과.
 * 주변 국가유산 후보와 일치하면 [matchedHeritageId] 로 국가유산청 상세를 보강한다.
 */
data class HeritageVisionResult(
    val name: String, // AI가 추정한 명칭
    val kind: String, // 종류(예: 석탑/전각/도자기)
    val era: String, // 시대(추정)
    val description: String, // AI 해설(2~3문장)
    val matchedHeritageId: String?, // 주변 후보 중 일치한 국가유산 id (없으면 null)
    val confidence: Int, // 0~100
    val isHeritage: Boolean = false, // 실제 문화재/유물/유적 여부(도감 기록 조건)
    val category: String = "", // "figure"(초상화·인물상) / "site"(건물·유적) / "relic"(유물)
)

/**
 * 사진 인식 1회의 종합 결과.
 * - [identification]: AI가 사진에서 판별한 내용(항상 존재)
 * - [candidates]: 현재 위치 주변 국가유산(사용자 보정용)
 * - [official]: 일치한 국가유산청 상세(있으면 권위 있는 해설·이미지)
 */
data class PhotoRecognition(
    val identification: HeritageVisionResult,
    val candidates: List<RecognizedHeritage>,
    val official: RecognizedHeritage?,
)
