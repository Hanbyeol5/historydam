package com.samdori93.yeoksadam.feature.camera.viewmodel

import com.samdori93.yeoksadam.core.domain.model.HeritageVisionResult
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage

/** 카메라 인식 단계. */
enum class CamPhase { PREVIEW, RECOGNIZING, RESULT }

/** 유물/건물 인식 화면 상태(MVI, 불변). */
data class CameraUiState(
    val phase: CamPhase = CamPhase.PREVIEW,
    val identification: HeritageVisionResult? = null, // AI가 사진에서 판별한 결과
    val candidates: List<RecognizedHeritage> = emptyList(), // 주변 국가유산(사용자 보정용)
    val official: RecognizedHeritage? = null, // 일치/선택한 국가유산청 상세
    val selectedId: String? = null,
    val loadingDetail: Boolean = false,
    val errorMessage: String? = null,
) {
    /** 표시 명칭: 공식(국가유산청) 우선, 없으면 AI 판별명. */
    val displayName: String
        get() = official?.name ?: identification?.name ?: "국가유산"

    /** 표시 해설: 공식 설명 우선, 없으면 AI 해설. */
    val description: String?
        get() = official?.description?.takeIf { it.isNotBlank() } ?: identification?.description
}

/** 사용자 입력 이벤트. */
sealed interface CameraUiEvent {
    /** 셔터로 촬영한 사진(JPEG)으로 인식 요청. */
    class Recognize(val imageJpeg: ByteArray) : CameraUiEvent

    /** 주변 후보를 눌러 특정 국가유산으로 보정 → 상세 조회. */
    data class Select(val id: String) : CameraUiEvent

    /** 다시 촬영(초기화). */
    data object Retake : CameraUiEvent
}
