package com.samdori93.yeoksadam.feature.ar.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.core.ui.component.PlaceholderScreen
import kotlinx.serialization.Serializable

/** AR 탐색·대면 라우트 (CLAUDE.md §9). */
@Serializable
data class ArSearch(val figureId: String, val siteId: String)

fun NavController.navigateToAr(figureId: String, siteId: String, navOptions: NavOptions? = null) =
    navigate(ArSearch(figureId, siteId), navOptions)

fun NavGraphBuilder.arScreen(
    onStartConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<ArSearch> { entry ->
        val args = entry.toRoute<ArSearch>()
        PlaceholderScreen(
            title = "AR 탐색",
            description = "figureId=${args.figureId}\nGeospatial 앵커 · 나침반/레이더 · 초상 등장 (구현 예정)",
            modifier = modifier,
        )
    }
}
