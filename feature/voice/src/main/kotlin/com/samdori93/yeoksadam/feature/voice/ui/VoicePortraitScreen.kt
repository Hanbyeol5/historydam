package com.samdori93.yeoksadam.feature.voice.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.feature.voice.viewmodel.VoiceStatus
import com.samdori93.yeoksadam.feature.voice.viewmodel.VoiceUiState

/**
 * 초상화 음성 대화 — 챗봇의 「초상화 대화로 전환」에서 들어온다.
 * 카메라 없이 가운데에 인물 초상 메달을 두고, 대화는 아래에 자막으로 흐른다.
 * (인물 시트·지도·AR 의 「대화하기」는 카메라 위 AR 대면 대화 [VoiceScreen])
 */
@Composable
fun VoicePortraitScreen(
    state: VoiceUiState,
    onMic: () -> Unit,
    onType: (String) -> Unit,
    onMute: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val figure = state.figure?.figure
    Column(modifier = modifier.fillMaxSize().background(Color(0xFF1A140F))) {
        VoiceTopBar(state, onBack, onMute)
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val size = minOf(maxWidth * 0.62f, maxHeight * 0.78f, 260.dp)
            if (size > 48.dp) {
                FigurePortrait(
                    figure = figure,
                    size = size,
                    active = state.status == VoiceStatus.SPEAKING || state.status == VoiceStatus.LISTENING,
                )
            }
        }
        VoiceBottomPanel(
            state = state,
            figureName = figure?.name ?: "",
            onMic = onMic,
            onType = onType,
            onReset = onReset,
        ) {
            // 마이크를 가운데에 두기 위한 자리
            Spacer(Modifier.size(44.dp))
        }
    }
}

/** 인물 초상 메달 — 말하거나 듣는 동안 바깥 링이 숨 쉬듯 커졌다 작아진다. */
@Composable
private fun FigurePortrait(figure: Figure?, size: Dp, active: Boolean) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = if (active) 1.1f else 0.96f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ring",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size * 1.3f)) {
        Box(
            Modifier.size(size * 1.28f).scale(ring)
                .background(DancheongColors.Jujak.copy(alpha = if (active) 0.16f else 0.07f), CircleShape),
        )
        Box(
            Modifier.size(size * 1.14f).scale((ring + 1f) / 2f)
                .background(DancheongColors.Jujak.copy(alpha = if (active) 0.22f else 0.1f), CircleShape),
        )
        MedallionPortrait(
            portraitUrl = figure?.portraitUrl?.ifBlank { null },
            name = figure?.name ?: "",
            size = size,
            sealMark = figure?.seal?.ifBlank { null },
            selected = active,
        )
    }
}
