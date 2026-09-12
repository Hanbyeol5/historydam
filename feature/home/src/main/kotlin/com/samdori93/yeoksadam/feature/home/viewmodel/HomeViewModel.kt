package com.samdori93.yeoksadam.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.domain.usecase.GetNearbyFiguresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNearbyFigures: GetNearbyFiguresUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeNearby()
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.PrevFigure -> _uiState.update {
                it.copy(selectedIndex = (it.selectedIndex - 1).coerceAtLeast(0))
            }
            HomeUiEvent.NextFigure -> _uiState.update {
                it.copy(selectedIndex = (it.selectedIndex + 1).coerceAtMost(it.nearby.lastIndex.coerceAtLeast(0)))
            }
            is HomeUiEvent.SelectFigure -> _uiState.update {
                it.copy(selectedIndex = event.index.coerceIn(0, it.nearby.lastIndex.coerceAtLeast(0)))
            }
            is HomeUiEvent.OpenFigureSheet -> emitEffect(HomeUiEffect.NavigateToFigureSheet(event.figureId))
            HomeUiEvent.OpenAllFigures -> emitEffect(HomeUiEffect.NavigateToAllFigures)
            HomeUiEvent.OpenNotifications -> emitEffect(HomeUiEffect.NavigateToNotifications)
            HomeUiEvent.Retry -> observeNearby()
        }
    }

    private fun observeNearby() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getNearbyFigures()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "주변 인물을 불러오지 못했습니다.")
                    }
                }
                .onEach { list ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            nearby = list,
                            selectedIndex = state.selectedIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0)),
                            errorMessage = null,
                        )
                    }
                }
                .collect()
        }
    }

    private fun emitEffect(effect: HomeUiEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
