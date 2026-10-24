package com.samdori93.yeoksadam.feature.map.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.OverlayImage
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import kotlin.math.cos
import kotlin.math.pow

/** 인물 핑 크기 — 원형 초상(지름 = 폭) 아래로 꼬리가 붙는다. */
internal val FigurePinWidth = 46.dp
internal val FigurePinHeight = 56.dp

/** 같은 유적에 모인 인물 핑을 나란히 펼칠 때 핑 사이 간격(dp) — 이름 캡션이 겹치지 않을 만큼 */
private const val SPREAD_DP = 66.0
private const val ROW_DP = 76.0
private const val PER_ROW = 4

/** 핑 하나의 자리 — 유적에서 화면상 [col] 칸 옆, [row] 줄 위. */
internal data class FigurePinSlot(val col: Float, val row: Int, val showSite: Boolean)

/**
 * 인물 id → 핑 자리. 혼자인 유적은 제자리,
 * 여럿이 모인 유적은 한 줄에 최대 [PER_ROW] 명씩 가운데 정렬로 나란히, 넘치면 윗줄로.
 */
internal fun figurePinSlots(figures: List<NearbyFigure>): Map<String, FigurePinSlot> =
    figures.groupBy { it.site.id }.values.flatMap { group ->
        group.mapIndexed { i, nf ->
            val row = i / PER_ROW
            val inRow = minOf(PER_ROW, group.size - row * PER_ROW)
            val col = i % PER_ROW - (inRow - 1) / 2f
            nf.figure.id to FigurePinSlot(col, row, showSite = group.size == 1 || (row == 0 && i == (inRow - 1) / 2))
        }
    }.toMap()

/**
 * 자리만큼 옮긴 좌표. 지도 확대 수준([zoom])에 맞춰 화면에서 늘 같은 간격이 되도록
 * dp 를 미터로 바꿔 더한다 (네이버 지도: 확대 0 에서 1dp ≈ 78,272m × cos 위도 — 512dp 타일).
 */
internal fun FigurePinSlot.position(lat: Double, lng: Double, zoom: Double): LatLng {
    if (col == 0f && row == 0) return LatLng(lat, lng)
    val metersPerDp = 78_271.517 * cos(Math.toRadians(lat)) / 2.0.pow(zoom)
    val east = col * SPREAD_DP * metersPerDp
    val north = row * ROW_DP * metersPerDp
    return LatLng(
        lat + north / 111_320.0,
        lng + east / (111_320.0 * cos(Math.toRadians(lat))),
    )
}

private const val PIN_W_PX = 138 // 46dp × 3 (xxhdpi 기준으로 그려 두고 마커가 dp 로 줄인다)
private const val PIN_H_PX = 168

/**
 * 인물 초상이 든 지도 핑 아이콘. 초상을 받기 전·실패 시에는 이름 첫 글자 메달로 그린다.
 * [selected] 면 테두리를 주작색으로.
 */
@Composable
internal fun rememberFigurePinIcon(portraitUrl: String, name: String, selected: Boolean): OverlayImage {
    val context = LocalContext.current
    val portrait by produceState<Bitmap?>(initialValue = null, portraitUrl) {
        value = loadPortrait(context, portraitUrl)
    }
    return remember(portrait, name, selected) {
        OverlayImage.fromBitmap(drawFigurePin(portrait, name, selected))
    }
}

private suspend fun loadPortrait(context: Context, url: String): Bitmap? {
    if (url.isBlank()) return null
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(PIN_W_PX)
        .allowHardware(false) // Canvas 에 다시 그려야 하므로 소프트웨어 비트맵
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
    return (result.drawable as? BitmapDrawable)?.bitmap
}

private fun drawFigurePin(portrait: Bitmap?, name: String, selected: Boolean): Bitmap {
    val bmp = Bitmap.createBitmap(PIN_W_PX, PIN_H_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val ringColor = (if (selected) DancheongColors.Jujak else DancheongColors.Hwangto).toArgb()
    val shadow = 6f
    val cx = PIN_W_PX / 2f
    val outerR = cx - shadow
    val cy = outerR + shadow / 2

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ringColor
        setShadowLayer(shadow, 0f, 2f, 0x55000000)
    }
    // 꼬리 (원 아래 삼각형) + 바깥 테
    val tail = Path().apply {
        moveTo(cx - outerR * 0.42f, cy + outerR * 0.78f)
        lineTo(cx, PIN_H_PX - shadow)
        lineTo(cx + outerR * 0.42f, cy + outerR * 0.78f)
        close()
    }
    canvas.drawPath(tail, ringPaint)
    canvas.drawCircle(cx, cy, outerR, ringPaint)

    // 한지 안쪽 링
    val innerR = outerR - if (selected) 9f else 7f
    canvas.drawCircle(cx, cy, innerR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DancheongColors.HanjiCard.toArgb() })

    val faceR = innerR - 4f
    if (portrait != null) {
        // 원 안에 꽉 차게 (center-crop, 얼굴이 위쪽이라 살짝 위로 맞춤)
        val scale = (faceR * 2) / minOf(portrait.width, portrait.height)
        val dx = cx - portrait.width * scale / 2
        val dy = cy - faceR - (portrait.height * scale - faceR * 2) * 0.15f
        val shader = BitmapShader(portrait, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setScale(scale, scale); postTranslate(dx, dy) })
        }
        canvas.drawCircle(cx, cy, faceR, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
    } else {
        canvas.drawCircle(cx, cy, faceR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DancheongColors.MedalMid.toArgb() })
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DancheongColors.CheongnokDeep.toArgb()
            textSize = faceR * 1.05f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(name.take(1), cx, baseline, textPaint)
    }
    return bmp
}
