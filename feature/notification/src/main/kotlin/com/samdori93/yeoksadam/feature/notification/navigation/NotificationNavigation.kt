package com.samdori93.yeoksadam.feature.notification.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.samdori93.yeoksadam.feature.notification.ui.NotificationScreen
import kotlinx.serialization.Serializable

/** 위치 기반 알림 목록 라우트 (지오펜싱/FCM, CLAUDE.md §10). */
@Serializable
data object Notification

fun NavController.navigateToNotification(navOptions: NavOptions? = null) =
    navigate(Notification, navOptions)

fun NavGraphBuilder.notificationScreen(
    modifier: Modifier = Modifier,
) {
    composable<Notification> {
        NotificationScreen(modifier = modifier)
    }
}
