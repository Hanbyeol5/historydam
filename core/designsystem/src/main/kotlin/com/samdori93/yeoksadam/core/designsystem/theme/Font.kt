package com.samdori93.yeoksadam.core.designsystem.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.samdori93.yeoksadam.core.designsystem.R

/**
 * 나눔명조(NanumMyeongjo, OFL) — 인물명·제목 등 명조 표기용.
 * 본문(sans)은 시스템 기본(한국어 Noto Sans KR)을 사용한다.
 */
val NanumMyeongjo = FontFamily(
    Font(R.font.nanum_myeongjo_regular, FontWeight.Normal),
    Font(R.font.nanum_myeongjo_bold, FontWeight.Bold),
    Font(R.font.nanum_myeongjo_extrabold, FontWeight.ExtraBold),
)
