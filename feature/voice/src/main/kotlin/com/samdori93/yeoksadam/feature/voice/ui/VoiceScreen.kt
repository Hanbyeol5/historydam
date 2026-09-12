package com.samdori93.yeoksadam.feature.voice.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.ui.sample.SampleData
import com.samdori93.yeoksadam.feature.voice.viewmodel.VoiceLine
import com.samdori93.yeoksadam.feature.voice.viewmodel.VoiceStatus
import com.samdori93.yeoksadam.feature.voice.viewmodel.VoiceUiState

/** 초상화 음성 대화 (목업 6번) — 온디바이스 STT/TTS + 실시간 자막. */
@Composable
fun VoiceScreen(
    figureId: String,
    state: VoiceUiState,
    onMic: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val figure = SampleData.figure(figureId)
    val active = state.status == VoiceStatus.LISTENING || state.status == VoiceStatus.SPEAKING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DancheongColors.Hanji)
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(Icons.Outlined.Home, "홈", tint = DancheongColors.Meok, modifier = Modifier.size(24.dp).clickable(onClick = onBack))
            Icon(Icons.Filled.KeyboardArrowDown, "닫기", tint = DancheongColors.Meok, modifier = Modifier.size(24.dp).clickable(onClick = onBack))
        }

        Spacer(Modifier.height(20.dp))

        // 초상 + 펄스 링
        val pulse = rememberInfiniteTransition(label = "pulse")
        val ringScale by pulse.animateFloat(
            initialValue = 0.92f,
            targetValue = if (active) 1.12f else 0.95f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "ring",
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
            Box(
                Modifier.size(220.dp).scale(ringScale)
                    .background(DancheongColors.Jujak.copy(alpha = if (active) 0.10f else 0.05f), CircleShape),
            )
            Box(
                Modifier.size(186.dp).scale((ringScale + 1f) / 2f)
                    .background(DancheongColors.Jujak.copy(alpha = if (active) 0.14f else 0.07f), CircleShape),
            )
            MedallionPortrait(portraitUrl = null, name = figure.name, sealMark = figure.initial.toString(), size = 156.dp, selected = true)
        }

        Spacer(Modifier.height(16.dp))
        Text(figure.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DancheongColors.Meok)
        Spacer(Modifier.height(4.dp))
        StatusLine(state.status)

        Spacer(Modifier.height(20.dp))

        // 실시간 자막
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DancheongColors.HanjiCard, RoundedCornerShape(18.dp))
                .border(1.dp, DancheongColors.Meok.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("실시간 대화", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DancheongColors.Hwangto)
            val lines = state.transcript.ifEmpty {
                SampleData.voiceTranscript.map { VoiceLine(mine = !it.first, text = it.second) }
            }
            lines.takeLast(4).forEach { line ->
                Text(
                    text = line.text,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = if (!line.mine) DancheongColors.Meok else DancheongColors.MeokSoft,
                    fontWeight = if (!line.mine) FontWeight.Medium else FontWeight.Normal,
                )
            }
            if (state.partial.isNotBlank()) {
                Text("“${state.partial}”", fontSize = 13.sp, color = DancheongColors.Cheongnok)
            }
        }

        Spacer(Modifier.weight(1f))

        // wave + mic
        Row(
            modifier = Modifier.padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WaveBars(listOf(10, 22, 14, 26, 12), active)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(if (state.status == VoiceStatus.LISTENING) ringScale else 1f)
                    .background(
                        if (state.status == VoiceStatus.LISTENING) DancheongColors.JujakDeep else DancheongColors.Jujak,
                        CircleShape,
                    )
                    .clickable(onClick = onMic),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (state.status == VoiceStatus.LISTENING) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = "마이크",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            WaveBars(listOf(18, 10, 24, 14, 8), active)
        }
    }
}

@Composable
private fun StatusLine(status: VoiceStatus) {
    val (label, dot) = when (status) {
        VoiceStatus.LISTENING -> "듣는 중…" to DancheongColors.JujakDeep
        VoiceStatus.THINKING -> "생각하는 중…" to DancheongColors.Hwangto
        VoiceStatus.SPEAKING -> "말하는 중…" to DancheongColors.Jujak
        VoiceStatus.IDLE -> "마이크를 눌러 말해보세요" to DancheongColors.Cheongnok
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).background(dot, CircleShape))
        Text(label, fontSize = 12.sp, color = DancheongColors.MeokSoft)
    }
}

@Composable
private fun WaveBars(heights: List<Int>, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        heights.forEach { h ->
            Box(
                Modifier
                    .width(4.dp)
                    .height((if (active) h else h / 2).dp)
                    .background(DancheongColors.Cheongnok, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Preview(heightDp = 800)
@Composable
private fun VoicePreview() {
    YeoksadamTheme {
        VoiceScreen("fig_chae", VoiceUiState(status = VoiceStatus.SPEAKING), {}, {})
    }
}
