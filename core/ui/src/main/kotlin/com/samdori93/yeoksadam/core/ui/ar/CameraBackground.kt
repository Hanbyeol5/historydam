package com.samdori93.yeoksadam.core.ui.ar

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 후면 카메라 영상을 화면 가득 (AR 유적 라벨·인물 대면 대화의 배경).
 * 카메라 권한은 호출하는 화면이 먼저 받는다.
 */
@Composable
fun CameraBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        var provider: ProcessCameraProvider? = null
        var disposed = false
        future.addListener({
            if (disposed) return@addListener
            provider = future.get().also { p ->
                // 이 화면의 Preview 만 이 화면 수명에 묶는다. unbindAll 은 쓰지 않는다:
                // 화면이 바뀔 때 새 화면이 먼저 그려지고 옛 화면이 나중에 정리되므로,
                // 전부 끊으면 새 화면(AR ↔ 대면 대화)의 카메라까지 끊겨 영상이 멈춘다.
                runCatching { p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview) }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            provider?.unbind(preview)
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
