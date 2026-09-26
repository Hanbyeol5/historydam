package com.samdori93.yeoksadam.core.domain.model

/** 도감 수집 종류 (CLAUDE.md §7). */
enum class DiscoveryType { SITE, FIGURE, RELIC }

/**
 * 사용자가 발견/수집한 항목 (역사의 전당 도감).
 * 카메라로 인식한 유물/유적지, 만난 인물이 여기 기록된다.
 */
data class Discovery(
    val type: DiscoveryType,
    val refId: String, // 국가유산 id / 인물 id
    val name: String,
    val subtitle: String = "", // 종목·시대·소재지 등
    val description: String = "", // 상세 해설(도감에서 다시 보기용)
    val imageUrl: String? = null,
    val discoveredAt: Long = 0L, // epoch millis
)
