package com.samdori93.yeoksadam.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 홈 하단 5탭 (CLAUDE.md §6 #1: 카메라·지도·홈·Q&A·메뉴).
 * 실제 라우트 문자열은 :app 의 NavHost 가 소유한다(feature 간 직접 의존 금지).
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    CAMERA("camera", "카메라", Icons.Outlined.CameraAlt),
    MAP("map", "지도", Icons.Outlined.Map),
    HOME("home", "홈", Icons.Outlined.Home),
    CHAT("chat", "Q&A", Icons.Outlined.QuestionAnswer),
    MENU("profile", "메뉴", Icons.Outlined.Menu),
}
