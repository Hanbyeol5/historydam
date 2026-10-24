package com.samdori93.yeoksadam.core.backend

import com.samdori93.yeoksadam.core.domain.model.Figure
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 백엔드(파이썬)와 같은 결과를 내는지 — 기준값은 scripts/export_intent_model.py 가 만든 intent_cases.json */
class BackendRagChatTest {

    @Serializable
    private data class Case(val text: String, val label: Int, val score: Double)

    private val classifier = IntentClassifier.fromJson(File("src/main/assets/intent_model.json").readText())

    @Test
    fun `의도 분류 점수가 scikit-learn 과 같다`() {
        val cases = Json.decodeFromString<List<Case>>(
            javaClass.classLoader!!.getResource("intent_cases.json")!!.readText(),
        )
        assertTrue(cases.isNotEmpty())
        cases.forEach { c ->
            assertEquals(c.text, c.score, classifier.score(c.text), 1e-9)
            assertEquals(c.text, c.label == 1, classifier.isHistoryQuestion(c.text))
        }
    }

    @Test
    fun `토큰은 2자 이상 단어 글자 덩어리`() {
        assertEquals(listOf("hello", "world", "세종", "업적"), IntentClassifier.tokens("hello, world! 세종 업적 a 왜"))
    }

    @Test
    fun `검색어 최적화는 백엔드와 같다`() {
        assertEquals("세종 훈민정음은 언제", BackendRagChat.optimizedQuery("세종", "훈민정음은 언제 만들었나요?"))
        assertEquals("세종", BackendRagChat.optimizedQuery("세종", "왜?"))
        assertEquals("이순신 거북선은", BackendRagChat.optimizedQuery("이순신", "이순신 거북선은?"))
    }

    @Test
    fun `인물 ID 는 백엔드 설정과 맞춰진다`() {
        assertEquals("sejong", BackendRagChat.normalize("fig_sejong"))
        assertEquals("jangyeongsil", BackendRagChat.normalize("jang-yeongsil"))
        assertTrue(BackendRagChat.BACKEND_FIGURES.containsKey(BackendRagChat.normalize("jang-yeongsil")))
    }

    @Test
    fun `백엔드에 없는 인물은 앱 자료로 설정을 만든다`() {
        val p = BackendRagChat.personaFrom(
            Figure(
                id = "jeongjo", name = "정조", title = "조선 제22대 국왕", portraitUrl = "",
                years = "1752–1800", bio = "개혁 군주로 규장각을 세웠다.", style = "king", died = 1800,
            ),
        )
        assertEquals("조선", p.era)
        assertEquals(1800, p.maxYear)
        assertEquals("정조", p.keyword)
        assertTrue(p.persona, p.persona.startsWith("정조(1752–1800)이다. 조선 제22대 국왕. 개혁 군주로"))
        assertEquals("이성계", BackendRagChat.personaFrom(Figure("taejo", "태조 이성계", "", "", style = "king", died = 1408)).keyword)
        assertEquals("고려", BackendRagChat.personaFrom(Figure("jm", "정몽주", "포은", "", died = 1392)).era)
    }

    @Test
    fun `지식 경계 필터가 꺼지면 원칙 4 를 빼고, 출처를 나눈다`() {
        val p = BackendRagChat.BACKEND_FIGURES.getValue("sejong")
        assertTrue(BackendRagChat.prompt(p, emptyList(), "q", boundary = true).contains("1450년 이후"))
        assertFalse(BackendRagChat.prompt(p, emptyList(), "q", boundary = false).contains("1450년 이후"))
        val c = BackendRagChat.citationOf("[출처: 국가기록원 API] 훈민정음: 내용")
        assertEquals("국가기록원 API", c.source)
        assertEquals("훈민정음: 내용", c.excerpt)
    }
}
