package com.samdori93.yeoksadam.feature.home.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.samdori93.yeoksadam.feature.home.ui.HomeRoute
import kotlinx.serialization.Serializable

/** 홈 화면 type-safe 라우트 키. */
@Serializable
data object Home

fun NavController.navigateToHome(navOptions: NavOptions? = null) =
    navigate(Home, navOptions)

/**
 * 홈 화면 그래프. 네비게이션 콜백은 :app 이 주입한다(feature 간 직접 의존 금지).
 */
fun NavGraphBuilder.homeScreen(
    onNavigateToFigureSheet: (String) -> Unit,
    onNavigateToAllFigures: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<Home> {
        HomeRoute(
            onNavigateToFigureSheet = onNavigateToFigureSheet,
            onNavigateToAllFigures = onNavigateToAllFigures,
            onNavigateToNotifications = onNavigateToNotifications,
            modifier = modifier,
        )
    }
}
