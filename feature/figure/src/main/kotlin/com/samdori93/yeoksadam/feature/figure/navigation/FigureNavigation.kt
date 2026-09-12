package com.samdori93.yeoksadam.feature.figure.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.feature.figure.ui.AllFiguresScreen
import com.samdori93.yeoksadam.feature.figure.ui.FigureSheetScreen
import kotlinx.serialization.Serializable

/** 인물 선택 시트 / 상세 라우트 (figureId 전달). */
@Serializable
data class FigureSheet(val figureId: String)

/** 모든 인물(가나다·주변 필터) 라우트. */
@Serializable
data object AllFigures

fun NavController.navigateToFigureSheet(figureId: String, navOptions: NavOptions? = null) =
    navigate(FigureSheet(figureId), navOptions)

fun NavController.navigateToAllFigures(navOptions: NavOptions? = null) =
    navigate(AllFigures, navOptions)

fun NavGraphBuilder.figureScreens(
    onBack: () -> Unit,
    onStartConversation: (String) -> Unit,
    onFigureClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<FigureSheet> { entry ->
        val args = entry.toRoute<FigureSheet>()
        FigureSheetScreen(
            figureId = args.figureId,
            onBack = onBack,
            onRelatedSites = { /* TODO: 관련 유적지 화면 */ },
            onStartConversation = onStartConversation,
            modifier = modifier,
        )
    }
    composable<AllFigures> {
        AllFiguresScreen(
            onBack = onBack,
            onFigureClick = onFigureClick,
            modifier = modifier,
        )
    }
}
