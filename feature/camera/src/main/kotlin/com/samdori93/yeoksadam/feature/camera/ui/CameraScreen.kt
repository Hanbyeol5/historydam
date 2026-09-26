package com.samdori93.yeoksadam.feature.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.domain.model.HeritageVisionResult
import com.samdori93.yeoksadam.feature.camera.viewmodel.CamPhase
import com.samdori93.yeoksadam.feature.camera.viewmodel.CameraUiEvent
import com.samdori93.yeoksadam.feature.camera.viewmodel.CameraUiState

/**
 * 유물/건물 인식 카메라 (목업 3번).
 * 일반 카메라 → 촬영 → AI(Gemini) 비전 판별 → 국가유산청 데이터로 해설(음성).
 */
@Composable
fun CameraScreen(
    uiState: CameraUiState,
    onEvent: (CameraUiEvent) -> Unit,
    onBack: () -> Unit,
    hasCameraPermission: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tts = remember { CameraTts(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }
    val captureController = remember { CameraCaptureController() }
    var captureError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2620), Color(0xFF14110D)))),
    ) {
        if (hasCameraPermission) {
            CameraPreview(controller = captureController, modifier = Modifier.fillMaxSize())
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

        // 뷰파인더(촬영 전/인식 중)
        if (uiState.phase != CamPhase.RESULT) {
            Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
                Reticle()
                if (uiState.phase == CamPhase.RECOGNIZING) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DancheongColors.Hwangto, strokeWidth = 3.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("주변 국가유산을 찾는 중…", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // 하단 영역
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            (uiState.errorMessage ?: captureError)?.takeIf { uiState.phase != CamPhase.RESULT }?.let { msg ->
                Text(
                    msg,
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(DancheongColors.Jujak.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(12.dp))
            }

            when (uiState.phase) {
                CamPhase.PREVIEW -> {
                    Text(
                        "유적·문화재를 비추고 촬영하면\n주변 국가유산을 찾아드려요",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    Shutter(enabled = hasCameraPermission) {
                        captureError = null
                        captureController.capture(
                            context = context,
                            onJpeg = { onEvent(CameraUiEvent.Recognize(it)) },
                            onFailure = { captureError = it.message ?: "촬영에 실패했습니다." },
                        )
                    }
                }

                CamPhase.RECOGNIZING -> Spacer(Modifier.height(86.dp))

                CamPhase.RESULT -> ResultPanel(
                    uiState = uiState,
                    onSelect = { id ->
                        tts.stop()
                        onEvent(CameraUiEvent.Select(id))
                    },
                    onSpeak = { uiState.description?.takeIf { it.isNotBlank() }?.let(tts::speak) },
                    onRetake = {
                        tts.stop()
                        onEvent(CameraUiEvent.Retake)
                    },
                )
            }
        }
    }
}

@Composable
private fun ResultPanel(
    uiState: CameraUiState,
    onSelect: (String) -> Unit,
    onSpeak: () -> Unit,
    onRetake: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 인식 태그 (출처 + 명칭 + 확신도)
        val source = if (uiState.official != null) "국가유산청" else "AI 판별"
        val confidence = uiState.identification?.confidence?.takeIf { it > 0 }?.let { " · ${it}%" }.orEmpty()
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text("$source · ${uiState.displayName}$confidence", color = Color.White, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))

        // 주변 국가유산 후보(보정용) — 건물/유적(site)일 때만. 유물·인물엔 무관하므로 숨김
        val cat = uiState.identification?.category.orEmpty()
        if (uiState.candidates.isNotEmpty() && cat == "site") {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.candidates.forEach { item ->
                    HeritageChip(
                        name = item.name,
                        selected = item.id == uiState.selectedId,
                        onClick = { onSelect(item.id) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 해설 카드
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DancheongColors.HanjiCard.copy(alpha = 0.98f), RoundedCornerShape(18.dp))
                .padding(14.dp),
        ) {
            uiState.official?.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = uiState.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(DancheongColors.HanjiDim, RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(10.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                MedallionPortrait(portraitUrl = null, name = uiState.displayName, size = 34.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        uiState.displayName,
                        fontFamily = NanumMyeongjo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DancheongColors.Meok,
                    )
                    val sub = listOfNotNull(
                        uiState.identification?.kind?.takeIf { it.isNotBlank() },
                        (uiState.official?.era ?: uiState.identification?.era)?.takeIf { it.isNotBlank() },
                        uiState.official?.address?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(sub, fontSize = 11.sp, color = DancheongColors.MeokSoft)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(DancheongColors.Jujak, CircleShape)
                        .clickable(onClick = onSpeak),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, "음성으로 듣기", tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            when {
                uiState.loadingDetail -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = DancheongColors.Jujak, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("공식 정보를 불러오는 중…", fontSize = 12.sp, color = DancheongColors.MeokSoft)
                }

                uiState.description != null -> Text(
                    uiState.description!!,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = DancheongColors.Meok,
                    modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState()),
                )

                else -> Text("설명 정보가 없습니다.", fontSize = 12.sp, color = DancheongColors.MeokSoft)
            }

            Spacer(Modifier.height(12.dp))
            SmallButton("다시 촬영", Icons.Filled.Refresh, modifier = Modifier.fillMaxWidth(), onClick = onRetake)
        }
    }
}

@Composable
private fun SmallButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .border(1.dp, DancheongColors.Jujak, RoundedCornerShape(20.dp))
            .height(40.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = DancheongColors.Jujak, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = DancheongColors.Jujak, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
private fun HeritageChip(name: String, selected: Boolean, onClick: () -> Unit) {
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
private fun CameraScreenPreview() {
    YeoksadamTheme {
        CameraScreen(
            uiState = CameraUiState(
                phase = CamPhase.RESULT,
                identification = HeritageVisionResult(
                    name = "백자 달항아리",
                    kind = "도자기",
                    era = "18세기 조선",
                    description = "둥근 보름달을 닮은 조선 백자로, 꾸밈없는 형태에 절제된 아름다움이 담겨 있다.",
                    matchedHeritageId = null,
                    confidence = 88,
                ),
            ),
            onEvent = {},
            onBack = {},
            hasCameraPermission = false,
        )
    }
}
