package com.samdori93.yeoksadam.core.data.local

import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.HeritageSite

/**
 * 백엔드 연동 전까지 사용하는 시드 데이터.
 * ⚠️ 초상 URL 은 저작권 정책(CLAUDE.md §12)상 비워둔다 — 라이선스 확인된 에셋만 채울 것.
 * 실제 연동 시 [com.samdori93.yeoksadam.core.network.api.YeoksadamApi] + Room 캐시로 교체.
 */
object FakeFigureCatalog {

    val sites: List<HeritageSite> = listOf(
        HeritageSite("site_gbg", "경복궁", 37.579617, 126.977041, "조선의 법궁(法宮).", 150f),
        HeritageSite("site_cdg", "창덕궁", 37.582604, 126.991987, "유네스코 세계유산, 후원으로 유명.", 150f),
        HeritageSite("site_jm", "종묘", 37.574202, 126.994359, "조선 왕실의 사당.", 120f),
        HeritageSite("site_dsg", "덕수궁", 37.565804, 126.975144, "대한제국의 황궁.", 120f),
        HeritageSite("site_hseong", "수원화성", 37.288323, 127.014053, "정조가 축조한 계획 성곽.", 300f),
    )

    val figures: List<Figure> = listOf(
        Figure(
            id = "fig_jeongjo",
            name = "정조",
            title = "조선 제22대 왕",
            portraitUrl = "",
            relatedSiteIds = listOf("site_hseong", "site_cdg"),
            voiceId = "voice_jeongjo",
        ),
        Figure(
            id = "fig_chaejegong",
            name = "채제공",
            title = "번암(樊巖)",
            portraitUrl = "",
            relatedSiteIds = listOf("site_hseong"),
            voiceId = "voice_chaejegong",
        ),
        Figure(
            id = "fig_sejong",
            name = "세종",
            title = "조선 제4대 왕",
            portraitUrl = "",
            relatedSiteIds = listOf("site_gbg"),
            voiceId = "voice_sejong",
        ),
        Figure(
            id = "fig_jeongyakyong",
            name = "정약용",
            title = "다산(茶山)",
            portraitUrl = "",
            relatedSiteIds = listOf("site_hseong"),
            voiceId = "voice_dasan",
        ),
        Figure(
            id = "fig_sinsaimdang",
            name = "신사임당",
            title = "사임당(師任堂)",
            portraitUrl = "",
            relatedSiteIds = listOf("site_gbg"),
            voiceId = "voice_saimdang",
        ),
        Figure(
            id = "fig_gojong",
            name = "고종",
            title = "대한제국 초대 황제",
            portraitUrl = "",
            relatedSiteIds = listOf("site_dsg"),
            voiceId = "voice_gojong",
        ),
    )

    fun siteById(id: String): HeritageSite? = sites.firstOrNull { it.id == id }

    /** 인물의 대표 유적지(첫 relatedSite). */
    fun primarySiteOf(figure: Figure): HeritageSite? =
        figure.relatedSiteIds.firstNotNullOfOrNull { siteById(it) }
}
