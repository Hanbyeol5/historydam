package com.samdori93.yeoksadam.feature.camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.Discovery
import com.samdori93.yeoksadam.core.domain.model.DiscoveryType
import com.samdori93.yeoksadam.core.domain.model.PhotoRecognition
import com.samdori93.yeoksadam.core.domain.usecase.GetHeritageDetailUseCase
import com.samdori93.yeoksadam.core.domain.usecase.RecognizeHeritagePhotoUseCase
import com.samdori93.yeoksadam.core.domain.usecase.RecordDiscoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 유물/건물 인식 카메라 ViewModel.
 * 셔터로 촬영 → AI 비전 판별(+국가유산청 보강) → 후보로 보정 가능.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val recognizePhoto: RecognizeHeritagePhotoUseCase,
    private val getHeritageDetail: GetHeritageDetailUseCase,
    private val recordDiscovery: RecordDiscoveryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onEvent(event: CameraUiEvent) {
        when (event) {
            is CameraUiEvent.Recognize -> recognize(event.imageJpeg)
            is CameraUiEvent.Select -> selectHeritage(event.id)
            CameraUiEvent.Retake -> _uiState.value = CameraUiState()
        }
    }

    private fun recognize(imageJpeg: ByteArray) {
        _uiState.update { it.copy(phase = CamPhase.RECOGNIZING, errorMessage = null) }
        viewModelScope.launch {
            when (val result = recognizePhoto(imageJpeg)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            phase = CamPhase.RESULT,
                            identification = data.identification,
                            candidates = data.candidates,
                            official = data.official,
                            selectedId = data.identification.matchedHeritageId,
                            errorMessage = null,
                        )
                    }
                    // 실제 문화재로 확인된 경우에만 역사의 전당(도감)에 기록
                    if (data.identification.isHeritage) {
                        recordDiscovery(toDiscovery(data))
                    }
                }

                is Result.Failure -> _uiState.update {
                    it.copy(
                        phase = CamPhase.PREVIEW,
                        errorMessage = result.error.message ?: "인식에 실패했습니다. 네트워크·API 키를 확인해 주세요.",
                    )
                }
            }
        }
    }

    private fun selectHeritage(id: String) {
        _uiState.update { it.copy(selectedId = id, loadingDetail = true) }
        viewModelScope.launch {
            when (val result = getHeritageDetail(id)) {
                is Result.Success -> _uiState.update {
                    if (it.selectedId == id) it.copy(official = result.data, loadingDetail = false) else it
                }

                is Result.Failure -> _uiState.update {
                    if (it.selectedId == id) {
                        it.copy(loadingDetail = false, errorMessage = result.error.message ?: "상세를 불러오지 못했습니다.")
                    } else {
                        it
                    }
                }
            }
        }
    }

    /** 인식 결과를 도감 항목으로 변환(공식 국가유산청 데이터 우선). */
    private fun toDiscovery(data: PhotoRecognition): Discovery {
        val id = data.identification
        val official = data.official
        val name = official?.name ?: id.name
        val kind = official?.kind ?: id.kind
        val era = official?.era ?: id.era
        val description = official?.description?.ifBlank { null } ?: id.description
        return Discovery(
            type = typeOf(id.category, name, kind),
            refId = official?.id ?: "vision:$name",
            name = name,
            subtitle = listOfNotNull(kind.ifBlank { null }, era.ifBlank { null }).joinToString(" · "),
            description = description,
            imageUrl = official?.imageUrl,
            discoveredAt = System.currentTimeMillis(),
        )
    }

    /** AI 카테고리(인물/유적지/유물) 우선, 없으면 키워드로 유적지/유물 분류. */
    private fun typeOf(category: String, name: String, kind: String): DiscoveryType =
        when (category) {
            "figure" -> DiscoveryType.FIGURE
            "site" -> DiscoveryType.SITE
            "relic" -> DiscoveryType.RELIC
            else -> classify(name, kind)
        }

    /** 건물·유적성 키워드면 유적지(SITE), 아니면 유물(RELIC). */
    private fun classify(name: String, kind: String): DiscoveryType {
        val text = "$name $kind"
        return if (SITE_KEYWORDS.any { text.contains(it) }) DiscoveryType.SITE else DiscoveryType.RELIC
    }

    private companion object {
        val SITE_KEYWORDS = listOf(
            "사적", "명승", "유적", "성곽", "성", "궁", "문", "전", "탑", "사지",
            "능", "묘", "서원", "향교", "건조물", "요지", "고분", "누각", "정자",
        )
    }
}
