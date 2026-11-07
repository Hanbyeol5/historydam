package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.backend.source.GeminiTextDataSource
import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.domain.repository.RelatedPeopleRepository
import com.samdori93.yeoksadam.core.domain.repository.RelatedPerson
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** 문화유산 → 관련 역사 인물 (Gemini). 답은 JSON 배열로 받고, 형식이 틀리거나 오류면 빈 목록. */
class RelatedPeopleRepositoryImpl @Inject constructor(
    private val gemini: GeminiTextDataSource,
    private val dispatchers: DispatcherProvider,
) : RelatedPeopleRepository {

    @Serializable
    private data class Item(val name: String = "", val years: String = "", val relation: String = "", val summary: String = "")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun suggest(subject: String, description: String): List<RelatedPerson> = withContext(dispatchers.io) {
        if (!gemini.configured || subject.isBlank()) return@withContext emptyList()
        runCatching {
            val text = gemini.generate(prompt(subject, description.take(DESC_MAX)))
            val body = text.substringAfter('[', "").substringBeforeLast(']', "")
            if (body.isBlank()) return@runCatching emptyList()
            json.decodeFromString<List<Item>>("[$body]")
                .filter { it.name.isNotBlank() && it.relation.isNotBlank() }
                .take(MAX)
                .map { RelatedPerson(it.name.trim(), it.years.trim(), it.relation.trim(), it.summary.trim()) }
        }.getOrDefault(emptyList())
    }

    private fun prompt(subject: String, description: String) = """
너는 한국사 해설사다. 아래 문화유산과 '직접' 관련된 역사 인물을 최대 $MAX 명 알려 줘라.
직접 관련 = 건립·제작·설계를 명하거나 맡은 사람, 소유·사용한 사람, 이곳에서 일어난 중요한 사건의 주인공, 문화유산에 이름이 남은 사람.
- 사료나 공식 해설로 확인되는 인물만. 확실하지 않으면 넣지 말 것 (지어내지 말 것)
- 관련 인물이 없으면 빈 배열 []
- 한국어로, 아래 JSON 배열만 출력 (다른 글·코드블록 표시 없이)
[{"name":"인물 이름","years":"생몰년 (예: 1762–1836, 모르면 빈 문자열)","relation":"이 문화유산과의 관계 한 줄","summary":"인물 소개 한 문장"}]

[문화유산]
$subject

[해설]
$description
""".trimIndent()

    private companion object {
        const val MAX = 3
        const val DESC_MAX = 1200
    }
}
