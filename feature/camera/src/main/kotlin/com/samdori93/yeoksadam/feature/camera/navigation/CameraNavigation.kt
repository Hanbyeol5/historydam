package com.samdori93.yeoksadam.feature.camera.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.samdori93.yeoksadam.feature.camera.ui.CameraRoute
import kotlinx.serialization.Serializable

/** 유물 인식 카메라 라우트 (CLAUDE.md §6 #3). */
@Serializable
data object CameraGraph

fun NavController.navigateToCamera(navOptions: NavOptions? = null) =
    navigate(CameraGraph, navOptions)

fun NavGraphBuilder.cameraScreen(
    onBack: () -> Unit,
    onOpenFigure: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<CameraGraph> {
        CameraRoute(
            onBack = onBack,
            onOpenFigure = onOpenFigure,
            modifier = modifier,
        )
    }
}
