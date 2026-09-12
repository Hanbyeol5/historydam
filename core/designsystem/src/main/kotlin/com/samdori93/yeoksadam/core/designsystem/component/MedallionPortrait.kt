package com.samdori93.yeoksadam.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme

/**
 * 단청 금테를 두른 원형 인물 초상 메달 (목업 `.med`).
 * 한지 라디얼 그라데이션 바탕 + 금테(이중 링) + 우하단 낙관(seal-mark).
 *
 * @param portraitUrl 초상 이미지 URL. 비어있으면 인물명 첫 글자를 명조 글리프로 표시.
 * @param name 인물명 (플레이스홀더/접근성)
 * @param sealMark 우하단 낙관에 새길 글자(보통 성씨 한자 또는 이름 첫 글자). null 이면 숨김.
 * @param selected 선택(포커스) 상태면 금테를 더 굵고 밝게.
 */
@Composable
fun MedallionPortrait(
    portraitUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 170.dp,
    sealMark: String? = null,
    selected: Boolean = false,
) {
    val ringColor = if (selected) DancheongColors.HwangtoLight else DancheongColors.Hwangto
    val ringWidth = if (selected) 6.dp else 4.dp

    val hanjiBrush = Brush.radialGradient(
        colors = listOf(DancheongColors.MedalTop, DancheongColors.MedalMid, DancheongColors.MedalBottom),
        center = Offset(0.32f * 1000f, 0.24f * 1000f),
        radius = 1000f,
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(if (selected) 16.dp else 10.dp, CircleShape, clip = false)
            // 바깥 금테
            .background(ringColor, CircleShape)
            .padding(ringWidth)
            // 한지 카드 링
            .background(DancheongColors.HanjiCard, CircleShape)
            .padding(3.dp)
            // 한지 그라데이션 바탕
            .background(hanjiBrush, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!portraitUrl.isNullOrBlank()) {
            AsyncImage(
                model = portraitUrl,
                contentDescription = "$name 초상",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.take(1),
                fontFamily = NanumMyeongjo,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
                color = DancheongColors.CheongnokDeep,
            )
        }

        // 우하단 낙관(seal)
        if (!sealMark.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(size * 0.08f)
                    .size(size * 0.24f)
                    .shadow(2.dp, RoundedCornerShape(5.dp), clip = false)
                    .background(DancheongColors.Jujak, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sealMark.take(1),
                    fontFamily = NanumMyeongjo,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.12f).sp,
                    color = Color.White,
                )
            }
        }
    }
}

/** 미발견/잠김 메달 — 한지 바탕 + 점선 테두리 + '?' (목업 `.med.pend`). */
@Composable
fun LockedMedallion(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    label: String = "?",
) {
    Box(
        modifier = modifier
            .size(size)
            .background(DancheongColors.HanjiDim, CircleShape)
            .border(2.dp, DancheongColors.Hwangto.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = NanumMyeongjo,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.32f).sp,
            color = DancheongColors.MeokSoft.copy(alpha = 0.6f),
        )
    }
}

/** 단색(청자) 아바타 메달 — 프로필 등 (목업 `.med.mono`). */
@Composable
fun MonoMedallion(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(DancheongColors.Cheongnok, DancheongColors.CheongnokDeep)),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.take(1),
            fontFamily = NanumMyeongjo,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
            color = DancheongColors.Hanji,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EAD9)
@Composable
private fun MedallionPortraitPreview() {
    YeoksadamTheme {
        Box(Modifier.padding(20.dp)) {
            MedallionPortrait(
                portraitUrl = null,
                name = "채제공",
                sealMark = "채",
                selected = true,
            )
        }
    }
}
