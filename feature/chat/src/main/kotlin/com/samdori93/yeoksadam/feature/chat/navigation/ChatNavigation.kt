package com.samdori93.yeoksadam.feature.chat.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.feature.chat.ui.ChatListRoute
import com.samdori93.yeoksadam.feature.chat.ui.ChatRoute
import kotlinx.serialization.Serializable

/** Q&A 인물 목록 화면 라우트 */
@Serializable
object ChatList

/** 페르소나 챗봇(텍스트) 라우트. figureId 필수, sessionId 는 음성과 공유(CLAUDE.md §11). */
@Serializable
data class Chat(val figureId: String, val sessionId: String? = null)

/** Q&A 인물 목록으로 이동 */
fun NavController.navigateToChatList(navOptions: NavOptions? = null) =
    navigate(ChatList, navOptions)

/** 개별 인물 대화방으로 이동 */
fun NavController.navigateToChat(figureId: String, navOptions: NavOptions? = null) =
    navigate(Chat(figureId), navOptions)

fun NavGraphBuilder.chatScreen(
    onFigureClick: (String) -> Unit,
    onBack: () -> Unit,
    onSwitchToVoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 1. Q&A 인물 선택 목록 화면
    composable<ChatList> {
        ChatListRoute(
            onFigureClick = onFigureClick,
            modifier = modifier,
        )
    }

    // 2. 개별 인물 대화방 화면 (기존 유지)
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