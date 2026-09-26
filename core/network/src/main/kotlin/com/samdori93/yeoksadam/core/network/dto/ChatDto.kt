package com.samdori93.yeoksadam.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequestDto(
    @SerialName("figureId") val figureId: String,
    @SerialName("message") val message: String
)

@Serializable
data class CitationDto(
    @SerialName("source") val source: String,
    @SerialName("excerpt") val excerpt: String
)

@Serializable
data class ChatResponseDto(
    @SerialName("id") val id: String = "",
    @SerialName("text") val text: String,
    @SerialName("citations") val citations: List<CitationDto> = emptyList()
)