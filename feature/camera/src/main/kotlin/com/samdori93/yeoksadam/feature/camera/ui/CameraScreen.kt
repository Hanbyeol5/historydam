package com.samdori93.yeoksadam.feature.camera.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.ui.sample.SampleData
import kotlinx.coroutines.delay

private enum class CamPhase { PREVIEW, RECOGNIZING, RESULT }

/** 유물 인식 카메라 (목업 3번) — 일반 카메라 → 촬영 → 유물 인식 → 인물 선택 → 해설(음성). */
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onOpenFigure: (String) -> Unit,
    hasCameraPermission: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf(CamPhase.PREVIEW) }
    val relic = SampleData.recognizedRelic
    var selectedFigure by remember { mutableStateOf(relic.figures.first()) }

    val context = LocalContext.current
    val tts = remember { CameraTts(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    // 인식 단계: 잠시 후 결과로
    LaunchedEffect(phase) {
        if (phase == CamPhase.RECOGNIZING) {
            delay(1400)
            phase = CamPhase.RESULT
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2620), Color(0xFF14110D)))),
    ) {
        if (hasCameraPermission) {
            CameraPreview(modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("카메라 권한을 허용해 주세요", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        // 상단 바
        Row(
            modifier = Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CamIcon(Icons.Outlined.Home, onBack)
            Spacer(Modifier.weight(1f))
            CamIcon(Icons.Filled.Cameraswitch) {}
        }

        // 가운데 뷰파인더(리티클) — 촬영 전/인식 중
        if (phase != CamPhase.RESULT) {
            Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
                Reticle()
                if (phase == CamPhase.RECOGNIZING) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DancheongColors.Hwangto, strokeWidth = 3.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("유물을 분석하는 중…", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // 하단 영역
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (phase) {
                CamPhase.PREVIEW -> {
                    Text(
                        "역사적인 유물·문화재를 비추고 촬영하세요",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    Shutter(enabled = hasCameraPermission) { phase = CamPhase.RECOGNIZING }
                }
                CamPhase.RECOGNIZING -> {
                    Spacer(Modifier.height(86.dp))
                }
                CamPhase.RESULT -> {
                    ResultPanel(
                        relic = relic,
                        selected = selectedFigure,
                        onSelectFigure = {
                            selectedFigure = it
                            tts.stop()
                        },
                        onSpeak = { tts.speak(selectedFigure.narration) },
                        onOpenFigure = { onOpenFigure(selectedFigure.id) },
                        onRetake = {
                            tts.stop()
                            phase = CamPhase.PREVIEW
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(
    relic: SampleData.RelicResult,
    selected: SampleData.RelicFigure,
    onSelectFigure: (SampleData.RelicFigure) -> Unit,
    onSpeak: () -> Unit,
    onOpenFigure: () -> Unit,
    onRetake: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 인식 태그
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text("${relic.name} · ${relic.meta}", color = Color.White, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))

        // 관련 인물 칩
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            relic.figures.forEach { fig ->
                FigChip(name = fig.name, selected = fig.id == selected.id) { onSelectFigure(fig) }
            }
        }
        Spacer(Modifier.height(12.dp))

        // 해설 카드
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DancheongColors.HanjiCard.copy(alpha = 0.97f), RoundedCornerShape(18.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedallionPortrait(portraitUrl = null, name = selected.name, size = 34.dp)
                Spacer(Modifier.width(8.dp))
                Text(selected.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DancheongColors.Meok)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(34.dp).background(DancheongColors.Jujak, CircleShape).clickable(onClick = onSpeak),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.VolumeUp, "음성으로 듣기", tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(selected.narration, fontSize = 13.sp, lineHeight = 19.sp, color = DancheongColors.Meok)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("다시 촬영", Icons.Filled.Refresh, solid = false, modifier = Modifier.weight(1f), onClick = onRetake)
                SmallButton("인물과 대화", null, solid = true, modifier = Modifier.weight(1f), onClick = onOpenFigure)
            }
        }
    }
}

@Composable
private fun SmallButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    solid: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val styled = if (solid) {
        modifier.background(DancheongColors.Jujak, shape)
    } else {
        modifier.border(1.dp, DancheongColors.Jujak, shape)
    }
    Row(
        modifier = styled.height(40.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (solid) Color.White else DancheongColors.Jujak, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, color = if (solid) Color.White else DancheongColors.Jujak, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(Color.White.copy(alpha = if (enabled) 0.25f else 0.1f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(58.dp).background(if (enabled) Color.White else Color.White.copy(alpha = 0.4f), CircleShape))
    }
}

@Composable
private fun CamIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(38.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(13.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FigChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(if (selected) DancheongColors.Jujak else Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(end = 12.dp, start = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MedallionPortrait(portraitUrl = null, name = name, size = 28.dp)
        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Reticle() {
    Box(modifier = Modifier.size(220.dp)) {
        val c = DancheongColors.Hwangto
        listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { a ->
            Box(modifier = Modifier.align(a).size(30.dp).border(3.dp, c, RoundedCornerShape(6.dp)))
        }
    }
}

@Preview(heightDp = 800)
@Composable
private fun CameraPreviewMock() {
    YeoksadamTheme {
        CameraScreen({}, {}, hasCameraPermission = false)
    }
}
