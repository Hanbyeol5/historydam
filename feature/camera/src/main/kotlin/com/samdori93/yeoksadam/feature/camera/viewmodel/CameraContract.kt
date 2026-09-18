package com.samdori93.yeoksadam.feature.camera.viewmodel

import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage

/** 카메라 인식 단계. */
enum class CamPhase { PREVIEW, RECOGNIZING, RESULT }

/** 유물/건물 인식 화면 상태(MVI, 불변). */
data class CameraUiState(
    val phase: CamPhase = CamPhase.PREVIEW,
    val candidates: List<RecognizedHeritage> = emptyList(),
    val selectedId: String? = null,
    val detail: RecognizedHeritage? = null,
    val loadingDetail: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedCandidate: RecognizedHeritage?
        get() = candidates.firstOrNull { it.id == selectedId }
}

/** 사용자 입력 이벤트. */
sealed interface CameraUiEvent {
    /** 셔터 → 현재 위치 기준 인식. */
    data object Shutter : CameraUiEvent

    /** 후보 국가유산 선택 → 상세 조회. */
    data class Select(val id: String) : CameraUiEvent

    /** 다시 촬영(초기화). */
    data object Retake : CameraUiEvent
}
