package com.samdori93.yeoksadam.feature.home.viewmodel

import com.samdori93.yeoksadam.core.domain.model.NearbyFigure

/**
 * 홈 화면 MVI 계약 (CLAUDE.md §4). 목업 1번 화면 동선.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val nearby: List<NearbyFigure> = emptyList(),
    val selectedIndex: Int = 0,
    val errorMessage: String? = null,
) {
    val selected: NearbyFigure? get() = nearby.getOrNull(selectedIndex)
    val isEmpty: Boolean get() = !isLoading && nearby.isEmpty() && errorMessage == null
    val hasPrev: Boolean get() = selectedIndex > 0
    val hasNext: Boolean get() = selectedIndex < nearby.lastIndex
}

sealed interface HomeUiEvent {
    data object PrevFigure : HomeUiEvent
    data object NextFigure : HomeUiEvent
    data class SelectFigure(val index: Int) : HomeUiEvent
    data class OpenFigureSheet(val figureId: String) : HomeUiEvent
    data object OpenAllFigures : HomeUiEvent
    data object OpenNotifications : HomeUiEvent
    data object Retry : HomeUiEvent
}

sealed interface HomeUiEffect {
    data class NavigateToFigureSheet(val figureId: String) : HomeUiEffect
    data object NavigateToAllFigures : HomeUiEffect
    data object NavigateToNotifications : HomeUiEffect
}
