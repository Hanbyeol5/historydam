package com.samdori93.yeoksadam.feature.voice.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samdori93.yeoksadam.feature.voice.viewmodel.VoiceViewModel

@Composable
fun VoiceRoute(
    portrait: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    var hasCamera by remember { mutableStateOf(granted(Manifest.permission.CAMERA)) }
    var hasMic by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasMic = ok
        if (ok) viewModel.onMic() else viewModel.onPermissionDenied()
    }
    val onMic = { if (hasMic) viewModel.onMic() else micLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    if (portrait) {
        // 챗봇에서 전환한 초상화 대화 — 카메라 없음
        VoicePortraitScreen(
            state = state,
            onMic = onMic,
            onType = viewModel::onType,
            onMute = viewModel::onMute,
            onReset = viewModel::onReset,
            onBack = onBack,
            modifier = modifier,
        )
    } else {
        // 인물 시트·지도·AR 에서 들어온 AR 대면 대화 — 후면 카메라 위 인물
        LaunchedEffect(Unit) { if (!hasCamera) cameraLauncher.launch(Manifest.permission.CAMERA) }
        VoiceScreen(
            state = state,
            hasCamera = hasCamera,
            onMic = onMic,
            onType = viewModel::onType,
            onMute = viewModel::onMute,
            onReset = viewModel::onReset,
            onBack = onBack,
            modifier = modifier,
        )
    }
}
