package com.samdori93.yeoksadam.feature.map.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.samdori93.yeoksadam.feature.map.ui.MapScreen
import kotlinx.serialization.Serializable

/** 지도 라우트 — 카카오맵 + 인물 핑 → AR 진입(CLAUDE.md §6 #4). */
@Serializable
data object MapGraph

fun NavController.navigateToMap(navOptions: NavOptions? = null) =
    navigate(MapGraph, navOptions)

fun NavGraphBuilder.mapScreen(
    onEnterAr: (figureId: String, siteId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<MapGraph> {
        MapScreen(onEnterAr = onEnterAr, modifier = modifier)
    }
}
