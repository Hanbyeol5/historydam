package com.samdori93.yeoksadam.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 단청(丹靑) 오방색 + 한지(韓紙) 팔레트.
 * 값은 앱 목업(역사담_앱목업)의 CSS 변수와 1:1 일치.
 */
object DancheongColors {
    // 적색 — 주조색(주작/진사 cinnabar)
    val Jujak = Color(0xFFB23A32) // --cinnabar
    val JujakBright = Color(0xFFC24A3F) // --cinnabar-2
    val JujakDeep = Color(0xFF8E2C25)
    val JujakLight = Color(0xFFD96A60)

    // 청록(청자 celadon) — 보조색
    val Cheongnok = Color(0xFF52837A) // --celadon
    val CheongnokDeep = Color(0xFF345A52) // --celadon-deep
    val CheongnokLight = Color(0xFF7BA59B)

    // 군청(단청 dancheong blue)
    val Gamcheong = Color(0xFF28586B) // --dancheong

    // 황(금 gold) — 강조/테두리
    val Hwangto = Color(0xFFBB9148) // --gold
    val HwangtoLight = Color(0xFFD4AC63) // --gold-2

    // 한지(paper) — 배경/표면
    val Hanji = Color(0xFFF4EAD9) // --paper
    val HanjiDim = Color(0xFFEFE2CD) // --paper-2
    val HanjiCard = Color(0xFFFBF5EA) // --paper-card
    val HanjiDark = Color(0xFF2B2018)

    // 메달 그라데이션(hanji radial)
    val MedalTop = Color(0xFFF7EEDD)
    val MedalMid = Color(0xFFE7D4B6)
    val MedalBottom = Color(0xFFD8C39D)

    // 먹(墨 ink)
    val Meok = Color(0xFF221C17) // --ink
    val MeokSoft = Color(0xFF4A4036) // --ink-soft

    // 백(白)
    val Baek = Color(0xFFFBF5EA)

    // 별칭(하위호환)
    val Hwangja get() = Hwangto
}
