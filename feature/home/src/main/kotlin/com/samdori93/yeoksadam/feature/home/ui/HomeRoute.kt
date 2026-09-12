package com.samdori93.yeoksadam.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samdori93.yeoksadam.core.ui.util.ObserveAsEvents
import com.samdori93.yeoksadam.feature.home.viewmodel.HomeUiEffect
import com.samdori93.yeoksadam.feature.home.viewmodel.HomeViewModel

/**
 * 홈 진입점 — 상태 구독 + UiEffect 를 네비게이션 콜백으로 연결(stateful).
 * 네비게이션은 :app NavHost contract 경유 — feature 간 직접 의존 금지(CLAUDE.md §3).
 */
@Composable
fun HomeRoute(
    onNavigateToFigureSheet: (String) -> Unit,
    onNavigateToAllFigures: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            is HomeUiEffect.NavigateToFigureSheet -> onNavigateToFigureSheet(effect.figureId)
            HomeUiEffect.NavigateToAllFigures -> onNavigateToAllFigures()
            HomeUiEffect.NavigateToNotifications -> onNavigateToNotifications()
        }
    }

    HomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
