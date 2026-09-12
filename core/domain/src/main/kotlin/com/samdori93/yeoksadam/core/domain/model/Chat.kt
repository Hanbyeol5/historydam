package com.samdori93.yeoksadam.core.domain.model

/** 인물 대화 메시지. */
data class ChatMessage(
    val role: Role,
    val text: String,
    val citations: List<Citation> = emptyList(),
)

enum class Role { USER, FIGURE }

/** RAG 응답의 사료 근거. */
data class Citation(
    val source: String,
    val excerpt: String,
)

/** 도감 수집 기록. */
data class Discovery(
    val type: DiscoveryType,
    val refId: String,
    val discoveredAt: Long,
)

enum class DiscoveryType { SITE, FIGURE, RELIC }

/** 위치(위경도) 값 객체. */
data class LatLng(
    val lat: Double,
    val lng: Double,
)
