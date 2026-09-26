package com.samdori93.yeoksadam.feature.camera.ui

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat

/**
 * [CameraPreview] 가 만든 [LifecycleCameraController] 로 실제 사진(JPEG)을 캡처한다.
 * 바인딩은 컨트롤러가 라이프사이클에 맞춰 자동 관리한다.
 */
class CameraCaptureController {
    var controller: LifecycleCameraController? = null

    fun capture(
        context: Context,
        onJpeg: (ByteArray) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val cam = controller
        if (cam == null) {
            onFailure(IllegalStateException("카메라가 아직 준비되지 않았습니다."))
            return
        }
        cam.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                        onJpeg(bytes)
                    } catch (t: Throwable) {
                        onFailure(t)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) = onFailure(exception)
            },
        )
    }
}
