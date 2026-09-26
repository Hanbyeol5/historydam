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

// Discovery / DiscoveryType 는 Discovery.kt 로 이동(이미지·명칭 포함 확장판).

/** 위치(위경도) 값 객체. */
data class LatLng(
    val lat: Double,
    val lng: Double,
)
