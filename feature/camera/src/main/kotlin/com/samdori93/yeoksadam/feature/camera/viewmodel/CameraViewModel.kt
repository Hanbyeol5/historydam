package com.samdori93.yeoksadam.feature.camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.usecase.GetHeritageDetailUseCase
import com.samdori93.yeoksadam.core.domain.usecase.RecognizeNearbyHeritageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 유물/건물 인식 카메라 ViewModel.
 * 셔터 → 위치 기반 인식(국가유산청 API) → 후보 선택 → 상세 해설.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val recognizeNearby: RecognizeNearbyHeritageUseCase,
    private val getHeritageDetail: GetHeritageDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onEvent(event: CameraUiEvent) {
        when (event) {
            CameraUiEvent.Shutter -> recognize()
            is CameraUiEvent.Select -> selectHeritage(event.id)
            CameraUiEvent.Retake -> _uiState.value = CameraUiState()
        }
    }

    private fun recognize() {
        _uiState.update { it.copy(phase = CamPhase.RECOGNIZING, errorMessage = null) }
        viewModelScope.launch {
            when (val result = recognizeNearby()) {
                is Result.Success -> {
                    val list = result.data
                    if (list.isEmpty()) {
                        _uiState.update {
                            it.copy(phase = CamPhase.PREVIEW, errorMessage = "주변에서 국가유산을 찾지 못했습니다.")
                        }
                    } else {
                        _uiState.update {
                            it.copy(phase = CamPhase.RESULT, candidates = list, errorMessage = null)
                        }
                        selectHeritage(list.first().id)
                    }
                }

                is Result.Failure -> _uiState.update {
                    it.copy(
                        phase = CamPhase.PREVIEW,
                        errorMessage = result.error.message ?: "인식에 실패했습니다. 네트워크를 확인해 주세요.",
                    )
                }
            }
        }
    }

    private fun selectHeritage(id: String) {
        _uiState.update { it.copy(selectedId = id, loadingDetail = true, detail = null) }
        viewModelScope.launch {
            when (val result = getHeritageDetail(id)) {
                is Result.Success -> _uiState.update {
                    // 그 사이 다른 후보를 눌렀다면 무시.
                    if (it.selectedId == id) it.copy(detail = result.data, loadingDetail = false) else it
                }

                is Result.Failure -> _uiState.update {
                    if (it.selectedId == id) {
                        it.copy(loadingDetail = false, errorMessage = result.error.message ?: "상세 정보를 불러오지 못했습니다.")
                    } else {
                        it
                    }
                }
            }
        }
    }
}
