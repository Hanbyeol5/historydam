package com.samdori93.yeoksadam.feature.camera.ui

import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * CameraX 후면 카메라 프리뷰 + 사진 캡처.
 * [LifecycleCameraController] 가 프리뷰·ImageCapture 바인딩을 라이프사이클에 맞춰 자동 관리한다.
 */
@Composable
fun CameraPreview(
    controller: CameraCaptureController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val camController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    DisposableEffect(lifecycleOwner) {
        camController.bindToLifecycle(lifecycleOwner)
        controller.controller = camController
        onDispose {
            controller.controller = null
            camController.unbind()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                this.controller = camController
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
    )
}
