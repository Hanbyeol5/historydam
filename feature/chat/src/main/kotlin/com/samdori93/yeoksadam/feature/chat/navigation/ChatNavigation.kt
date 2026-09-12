package com.samdori93.yeoksadam.feature.chat.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.feature.chat.ui.ChatRoute
import kotlinx.serialization.Serializable

/** 페르소나 챗봇(텍스트) 라우트. figureId 필수, sessionId 는 음성과 공유(CLAUDE.md §11). */
@Serializable
data class Chat(val figureId: String, val sessionId: String? = null)

fun NavController.navigateToChat(figureId: String, navOptions: NavOptions? = null) =
    navigate(Chat(figureId), navOptions)

fun NavGraphBuilder.chatScreen(
    onBack: () -> Unit,
    onSwitchToVoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<Chat> { entry ->
        val args = entry.toRoute<Chat>()
        ChatRoute(
            figureId = args.figureId,
            onBack = onBack,
            onSwitchToVoice = onSwitchToVoice,
            modifier = modifier,
        )
    }
}
