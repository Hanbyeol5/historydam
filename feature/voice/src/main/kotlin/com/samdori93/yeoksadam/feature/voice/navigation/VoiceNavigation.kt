package com.samdori93.yeoksadam.feature.voice.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.feature.voice.ui.VoiceRoute
import kotlinx.serialization.Serializable

/**
 * 음성 대화 라우트 (STT/TTS + 실시간 자막, CLAUDE.md §11).
 * [portrait] = 챗봇에서 전환한 초상화 대화(카메라 없음), 아니면 카메라 위 AR 대면 대화.
 */
@Serializable
data class Voice(val figureId: String, val sessionId: String? = null, val portrait: Boolean = false)

fun NavController.navigateToVoice(figureId: String, portrait: Boolean = false, navOptions: NavOptions? = null) =
    navigate(Voice(figureId, portrait = portrait), navOptions)

fun NavGraphBuilder.voiceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<Voice> { entry ->
        VoiceRoute(
            portrait = entry.toRoute<Voice>().portrait,
            onBack = onBack,
            modifier = modifier,
        )
    }
}
