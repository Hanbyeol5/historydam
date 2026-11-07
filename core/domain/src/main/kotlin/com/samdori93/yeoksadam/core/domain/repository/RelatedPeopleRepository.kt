package com.samdori93.yeoksadam.core.domain.repository

/** AI 가 알려 준, 문화유산과 직접 관련된 역사 인물 (앱 인물 16명 밖의 인물 포함) */
data class RelatedPerson(
    val name: String,
    val years: String,
    /** 이 문화유산과의 관계 (예: 「건립을 명함」) */
    val relation: String,
    /** 인물 소개 한두 문장 */
    val summary: String,
)

interface RelatedPeopleRepository {
    /**
     * [subject](문화유산 이름)와 [description](해설)을 보고 직접 관련된 역사 인물을 최대 3명.
     * 확실한 인물이 없거나 AI 를 쓸 수 없으면 빈 목록.
     */
    suspend fun suggest(subject: String, description: String): List<RelatedPerson>
}
