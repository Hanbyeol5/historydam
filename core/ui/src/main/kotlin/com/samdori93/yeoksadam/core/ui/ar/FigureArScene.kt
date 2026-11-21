package com.samdori93.yeoksadam.core.ui.ar

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.samdori93.yeoksadam.core.domain.model.FigureModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D 인물 모델 — 서버 저장소(역사담 Worker /model, 파일 이름 = 인물 이름)에서 받아 폰에 둔 glb 를 먼저 쓰고,
 * 없으면 앱에 넣어 둔 assets/models/<figureId>.glb (개발용). 모델이 있는 인물만 3D, 없으면 각 화면의 2D 표현.
 * 라이선스 메모는 /assets/figures/<id>/license.json.
 */
object Figure3dModels {
    /** 앱에 넣어 둘 수 있는 모델 (파일이 assets 에 실제로 있을 때만 쓴다) */
    private val bundled = mapOf(
        // 조선 후기 성인 남성 평균 키 약 161cm + 관모 → 모델 전체(관모 포함) 1.70m
        "jeong-yakyong" to Model(asset = "models/jeong-yakyong.glb", heightM = 1.70f),
    )

    /**
     * @param asset assets 경로, [file] 이 있으면 폰에 받아 둔 파일 경로를 쓴다
     * @param heightM 발끝부터 머리 장식 꼭대기까지 실제 키(m)
     */
    data class Model(val asset: String, val heightM: Float, val file: String? = null)

    fun of(context: Context, figureId: String?, downloaded: Map<String, FigureModel>): Model? {
        val id = figureId?.removePrefix("fig_") ?: return null
        downloaded[id]?.let { return Model(asset = "", heightM = it.heightM, file = it.filePath) }
        val b = bundled[id] ?: return null
        val present = runCatching { context.assets.list(b.asset.substringBeforeLast('/'))?.contains(b.asset.substringAfterLast('/')) }.getOrNull()
        return b.takeIf { present == true }
    }
}

/** 서버 3D 모델 공급 — MainActivity 가 ServerStoreRepository 로 채운다 */
class FigureModelSource(
    val models: StateFlow<Map<String, FigureModel>>,
    val sync: suspend (figureId: String) -> Unit,
)

val LocalFigureModelSource = staticCompositionLocalOf<FigureModelSource?> { null }

/** 인물의 3D 모델 (없으면 null). 처음 만나면 서버 목록을 확인해 받아 오고, 다 받으면 3D 로 바뀐다 */
@Composable
fun rememberFigure3dModel(figureId: String?): Figure3dModels.Model? {
    val source = LocalFigureModelSource.current
    val context = LocalContext.current
    val empty = remember { MutableStateFlow<Map<String, FigureModel>>(emptyMap()) }
    val downloaded by (source?.models ?: empty).collectAsState()
    LaunchedEffect(figureId, source) { if (figureId != null) source?.sync(figureId) }
    return remember(figureId, downloaded) { Figure3dModels.of(context, figureId, downloaded) }
}

/** 인물을 어디에 세울지 */
sealed interface FigurePlacement {
    /** 대면 대화: 화면 아래 바닥을 찾으면 그 위, 못 찾으면 약 2.2m 앞 */
    data object InFront : FigurePlacement

    /**
     * 위치 기반 AR: 지금 화면 정면에서 [deltaDeg](오른쪽 +) 만큼 돌린 방향으로 [distanceM] 떨어진 곳.
     * 값을 아직 모르면(위치·방위 대기) null — 알게 되면 그때 세운다.
     */
    data class Toward(val aim: () -> Pair<Float, Float>?) : FigurePlacement
}

/** ARCore 를 쓸 수 있는 기기인지 (null = 아직 확인 중). 설치 안 된 지원 기기는 SceneView 가 설치를 안내한다. */
@Composable
fun rememberArCoreSupported(): State<Boolean?> {
    val context = LocalContext.current
    return produceState<Boolean?>(initialValue = null, context) {
        repeat(AVAILABILITY_POLLS) {
            val a = ArCoreApk.getInstance().checkAvailability(context)
            if (!a.isTransient) {
                value = a.isSupported
                return@produceState
            }
            delay(AVAILABILITY_POLL_MS)
        }
        value = false
    }
}

/**
 * ARCore 로 실제 공간에 사람 크기 3D 인물을 세운다 (카메라 화면도 이 장면이 그린다).
 * 실제 키([Figure3dModels.Model.heightM])로 맞추므로 다가가면 커지고 멀어지면 작아진다 — ARCore 가 폰 이동을 추적.
 * 인물은 세워질 때 사용자를 바라보고 그 자리에 고정된다 — 주위를 돌면 옆모습 · 뒷모습이 보인다.
 *
 * @param recall 값이 바뀌면 인물을 지운 뒤 [placement] 대로 다시 세운다
 * @param onAim 화면 정면 기준 인물 방향(도, 오른쪽 +)과 거리(m)
 * @param onFailed ARCore 세션을 못 열었을 때 (2D 로 대체)
 * @param onTracking 공간 추적이 안 될 때 사용자에게 보여 줄 안내 (추적 중이면 null). 추적이 끊기면 인물도 그려지지 않는다
 */
@Composable
fun FigureArScene(
    model: Figure3dModels.Model,
    placement: FigurePlacement,
    recall: Int,
    onAim: (angleDeg: Float, distanceM: Float) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
    onTracking: (message: String?) -> Unit = {},
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val childNodes = rememberNodes()
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val currentPlacement by rememberUpdatedState(placement)
    val placer = remember(model) { FigurePlacer(model, modelLoader, childNodes) }

    LaunchedEffect(recall) { if (recall > 0) placer.reset() }

    ARScene(
        modifier = modifier.onSizeChanged { viewSize = it },
        engine = engine,
        modelLoader = modelLoader,
        childNodes = childNodes,
        planeRenderer = false,
        sessionConfiguration = { session, config ->
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.focusMode = Config.FocusMode.AUTO
            config.depthMode =
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
        },
        onSessionFailed = { onFailed() },
        onSessionUpdated = { session, frame ->
            placer.trackingMessage(frame)?.let { onTracking(it.ifEmpty { null }) }
            placer.update(session, frame, viewSize, currentPlacement)?.let { (angle, dist) -> onAim(angle, dist) }
        },
    )
}

/** 인물을 세우고(앵커) 발을 실제 바닥에 맞춘다 */
private class FigurePlacer(
    private val model: Figure3dModels.Model,
    private val modelLoader: ModelLoader,
    private val nodes: MutableList<Node>,
) {
    private var anchorNode: AnchorNode? = null
    private var modelNode: ModelNode? = null
    private var trackedFrames = 0

    /** 바닥을 못 찾고 눈높이로 어림해 세웠는지 — 그렇다면 바닥을 찾는 대로 발 높이를 맞춘다 */
    private var floorEstimated = false

    /** centerOrigin 이 발을 원점에 맞추려고 잡은 모델 위치 — 바닥 보정은 여기에 더한다 */
    private var basePosition = Position()

    private var lastTracking: String? = null

    /** 추적 안내가 바뀌었을 때만 돌려준다 (""= 추적 중) */
    fun trackingMessage(frame: Frame): String? {
        val camera = frame.camera
        val msg = if (camera.trackingState == TrackingState.TRACKING) {
            ""
        } else {
            when (camera.trackingFailureReason) {
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "조금 더 밝은 곳에서 비춰 주세요"
                TrackingFailureReason.EXCESSIVE_MOTION -> "폰을 천천히 움직여 주세요"
                TrackingFailureReason.INSUFFICIENT_FEATURES -> "무늬가 보이는 바닥·주변을 1~2m 떨어져 비춰 주세요"
                TrackingFailureReason.CAMERA_UNAVAILABLE -> "카메라를 쓸 수 없어요"
                else -> "폰을 들고 주변을 천천히 비추면 공간을 인식해요"
            }
        }
        if (msg == lastTracking) return null
        lastTracking = msg
        return msg
    }

    fun reset() {
        anchorNode?.let {
            nodes.remove(it)
            it.detachAnchor()
            it.destroy()
        }
        anchorNode = null
        modelNode = null
        trackedFrames = 0
        floorEstimated = false
    }

    /** @return (화면 정면 기준 각도 · 오른쪽 +, 거리 m) */
    fun update(session: Session, frame: Frame, view: IntSize, placement: FigurePlacement): Pair<Float, Float>? {
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) return null
        val cam = camera.pose
        if (anchorNode == null) place(session, frame, cam, view, placement) ?: return null

        val target = anchorNode?.anchor?.pose ?: return null
        if (floorEstimated) {
            floorY(session, cam)?.let { y -> modelNode?.position = basePosition + Position(y = y - target.ty()) }
        }
        val dx = target.tx() - cam.tx()
        val dz = target.tz() - cam.tz()

        val (fx, fz) = flatForward(cam)
        // 정면 → 인물 방향의 시계 방향 각도 (오른쪽 +)
        val cross = fx * dz - fz * dx
        val dot = fx * dx + fz * dz
        val angle = Math.toDegrees(atan2(cross, dot).toDouble()).toFloat()
        return angle to sqrt(dx * dx + dz * dz)
    }

    private fun place(session: Session, frame: Frame, cam: Pose, view: IntSize, placement: FigurePlacement): Unit? {
        trackedFrames++
        val at = when (placement) {
            is FigurePlacement.Toward -> {
                val (delta, dist) = placement.aim() ?: return null
                toward(cam, delta, dist)
            }
            FigurePlacement.InFront -> {
                val floor = if (view.width > 0) {
                    frame.hitTest(view.width / 2f, view.height * FLOOR_HIT_Y).firstOrNull { hit ->
                        val plane = hit.trackable as? Plane
                        plane != null && plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                            plane.isPoseInPolygon(hit.hitPose) && hit.distance in MIN_DIST_M..MAX_DIST_M
                    }?.hitPose
                } else {
                    null
                }
                floor?.translation ?: if (trackedFrames > FALLBACK_AFTER_FRAMES) toward(cam, 0f, FALLBACK_DIST_M) else return null
            }
        }
        val onFloorHit = placement is FigurePlacement.InFront && trackedFrames <= FALLBACK_AFTER_FRAMES
        if (!onFloorHit) floorY(session, cam)?.let { at[1] = it } ?: run { floorEstimated = true }

        val node = ModelNode(
            modelInstance = model.file?.let { path -> modelLoader.createModelInstance(readDirect(File(path))) }
                ?: modelLoader.createModelInstance(model.asset),
            autoAnimate = true,
            scaleToUnits = model.heightM,
            centerOrigin = Position(x = 0f, y = -1f, z = 0f), // 발이 원점(바닥)에 오도록
        ).apply {
            // 세우는 순간 모델 정면(+Z)이 사용자를 보게 한 번만 돌린다 — 이후 고정이라 뒤로 돌아가면 뒷모습
            rotation = Rotation(y = Math.toDegrees(atan2(cam.tx() - at[0], cam.tz() - at[2]).toDouble()).toFloat())
        }
        val anchor = AnchorNode(modelLoader.engine, session.createAnchor(Pose.makeTranslation(at[0], at[1], at[2])))
        basePosition = node.position
        anchor.addChildNode(node)
        nodes += anchor
        anchorNode = anchor
        modelNode = node
        return Unit
    }

    /**
     * 추적 중인 위쪽 수평면 가운데 발밑 높이(폰 아래 [FLOOR_MIN_BELOW_M]~[FLOOR_MAX_BELOW_M])에서 가장 낮은 면 = 바닥.
     * 탁자 · 의자 면보다 바닥을 고르려고 가장 낮은 것. 못 찾으면 null
     */
    private fun floorY(session: Session, cam: Pose): Float? = session.getAllTrackables(Plane::class.java)
        .asSequence()
        .filter {
            it.trackingState == TrackingState.TRACKING && it.subsumedBy == null &&
                it.type == Plane.Type.HORIZONTAL_UPWARD_FACING
        }
        .map { it.centerPose.ty() }
        .filter { cam.ty() - it in FLOOR_MIN_BELOW_M..FLOOR_MAX_BELOW_M }
        .minOrNull()

    /** 화면 정면을 수평으로 편 방향에서 [deltaDeg](오른쪽 +) 돌린 쪽으로 [dist]m, 높이는 발밑(폰 높이 - 1.45m) */
    private fun toward(cam: Pose, deltaDeg: Float, dist: Float): FloatArray {
        val (fx, fz) = flatForward(cam)
        val r = Math.toRadians(deltaDeg.toDouble())
        // ARCore(y 위, 오른손 좌표)에서 수평 벡터를 시계 방향으로 돌리기
        val x = (fx * cos(r) - fz * sin(r)).toFloat()
        val z = (fz * cos(r) + fx * sin(r)).toFloat()
        return floatArrayOf(cam.tx() + x * dist, cam.ty() - EYE_HEIGHT_M, cam.tz() + z * dist)
    }

    /**
     * 카메라가 보는 방향(-Z)을 수평으로 편 단위 벡터 (x, z).
     * 바닥이나 천장을 거의 수직으로 볼 때는 -Z 가 수평 성분이 없어 흔들리므로, 폰 위쪽(+Y) 방향을 쓴다.
     */
    private fun flatForward(cam: Pose): Pair<Float, Float> {
        val z = cam.zAxis
        var fx = -z[0]
        var fz = -z[2]
        var len = sqrt(fx * fx + fz * fz)
        if (len < STEEP_LEN) {
            val y = cam.yAxis
            val sign = if (z[1] > 0f) -1f else 1f // 바닥을 볼 때(-Z 가 아래)는 폰 위쪽이 앞
            fx = y[0] * sign
            fz = y[2] * sign
            len = sqrt(fx * fx + fz * fz)
        }
        if (len < 1e-3f) return 0f to -1f
        return fx / len to fz / len
    }

    private companion object {
        const val FLOOR_HIT_Y = 0.72f
        const val MIN_DIST_M = 0.8f
        const val MAX_DIST_M = 6f
        const val FALLBACK_AFTER_FRAMES = 75 // 약 2.5초
        const val FALLBACK_DIST_M = 2.2f
        const val EYE_HEIGHT_M = 1.45f
        const val FLOOR_MIN_BELOW_M = 0.5f
        const val FLOOR_MAX_BELOW_M = 2.2f
        const val STEEP_LEN = 0.35f
    }
}

private const val AVAILABILITY_POLLS = 20
private const val AVAILABILITY_POLL_MS = 200L

/** 폰에 받아 둔 glb → Filament 가 읽는 direct ByteBuffer */
private fun readDirect(file: File): ByteBuffer {
    val bytes = file.readBytes()
    return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).apply { rewind() }
}
