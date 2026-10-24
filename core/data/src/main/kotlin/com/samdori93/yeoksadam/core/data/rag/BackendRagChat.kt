package com.samdori93.yeoksadam.core.data.rag

import android.content.Context
import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.common.result.getOrNull
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.Citation
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.Role
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.domain.repository.FigureRepository
import com.samdori93.yeoksadam.core.network.rag.GeminiTextDataSource
import com.samdori93.yeoksadam.core.network.rag.PublicArchiveDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 백엔드 RAG — historydam backend(main.py `POST /v1/chat`)를 서버 없이 앱 안에서 그대로 돌린다.
 *  1) 질문 의도 분류 ([IntentClassifier]) — 단순 대화면 자료 검색을 건너뛴다
 *  2) 역사 질의면 공공기관 자료 7곳을 동시에 검색 (인물 검색어 + 질문 핵심어 2개, 국가유산청은 인물의 관련 유적)
 *  3) 인물 설정·참고 자료·질문으로 프롬프트를 만들어 Gemini 에 묻는다
 * 백엔드에 설정이 있는 세종대왕·이순신·장영실은 그 설정을, 나머지 인물은 앱의 인물 자료(직함·생몰년·말씨)로 만든 설정을 쓴다.
 */
@Singleton
class BackendRagChat @Inject constructor(
    @ApplicationContext private val context: Context,
    private val archives: PublicArchiveDataSource,
    private val gemini: GeminiTextDataSource,
    private val figures: FigureRepository,
    private val catalog: CatalogRepository,
    private val dispatchers: DispatcherProvider,
) {
    private val classifier by lazy {
        IntentClassifier.fromJson(context.assets.open(MODEL_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    /** @param boundary 지식 경계 필터(설정) — 켜져 있으면 백엔드의 원칙 4(세상을 떠난 뒤의 일은 모른다)를 넣는다 */
    suspend fun ask(figureId: String, question: String, boundary: Boolean): ChatMessage = withContext(dispatchers.io) {
        archives.prefetch() // 검색어를 받지 않는 지식이음 목록 3개를 뒤에서 미리 받아 둔다
        val figure = figureOf(figureId)
        val persona = personaOf(figureId, figure)
        val keyword = optimizedQuery(persona.keyword, question)
        val contexts = if (classifier.isHistoryQuestion(question)) search(keyword, sitesOf(figure)) else emptyList()
        val answer = gemini.generate(prompt(persona, contexts, question, boundary))
        ChatMessage(role = Role.FIGURE, text = answer, citations = contexts.map(::citationOf))
    }

    /** 백엔드의 5곳 중 한국고고학사전(검색 불가)은 국가유산청 국가유산 정보(인물의 관련 유적)로 바꾸고, 지식이음 조상화·고문헌을 더했다 (공공기관 자료 7곳) */
    private suspend fun search(keyword: String, sites: List<PublicArchiveDataSource.SiteRef>): List<String> = coroutineScope {
        listOf<() -> List<String>>(
            { archives.museum(keyword) },
            { archives.archives(keyword) },
            { archives.kdp(keyword) },
            { archives.heritage(sites) },
            { archives.diary(keyword) },
            { archives.portraits(keyword) },
            { archives.oldBooks(keyword) },
        ).map { fetch -> async { fetch() } }.awaitAll().flatten()
    }

    private suspend fun figureOf(figureId: String): Figure? =
        figures.getFigure(figureId).getOrNull() ?: figures.getFigure(figureId.removePrefix("fig_")).getOrNull()

    /** 인물의 관련 유적 (인물 자료에 적힌 순서 — 대표 유적이 먼저) */
    private suspend fun sitesOf(figure: Figure?): List<PublicArchiveDataSource.SiteRef> =
        figure?.relatedSiteIds.orEmpty().take(MAX_SITES).map { id ->
            PublicArchiveDataSource.SiteRef(id = id, name = catalog.site(id)?.name.orEmpty())
        }

    private fun personaOf(figureId: String, figure: Figure?): Persona {
        val key = normalize(figureId)
        BACKEND_FIGURES[key]?.let { return it }
        if (figure == null) {
            return Persona(
                name = figureId,
                era = "역사 속",
                maxYear = 2026,
                keyword = figureId,
                persona = "역사적 인물 '$figureId'이다. 품격 있는 어조로 답하라.",
            )
        }
        return personaFrom(figure)
    }

    internal data class Persona(
        val name: String,
        val era: String,
        val maxYear: Int,
        val keyword: String,
        val persona: String,
    )

    internal companion object {
        const val MODEL_ASSET = "intent_model.json"
        const val MAX_SITES = 3

        /** 백엔드 FIGURE_INFO (main.py) 그대로 */
        val BACKEND_FIGURES = mapOf(
            "sejong" to Persona(
                name = "세종대왕", era = "조선", maxYear = 1450, keyword = "세종",
                persona = "조선의 제4대 왕이다. 백성을 아끼는 자애롭고 위엄 있는 어조(~하노라, ~이니라)를 쓴다.",
            ),
            "yisunsin" to Persona(
                name = "이순신", era = "조선", maxYear = 1598, keyword = "이순신",
                persona = "조선의 수군절도사이다. 장수로서 결연하고 강직한 어조(~하였소, ~하겠네)를 쓴다.",
            ),
            "jangyeongsil" to Persona(
                name = "장영실", era = "조선", maxYear = 1450, keyword = "장영실",
                persona = "조선의 천민 출신 과학자이다. 신분은 낮았으나 기술에 대한 열정이 넘치며, 겸손하고 공손한 어조(~하옵니다, ~였습니다)를 쓴다.",
            ),
        )

        /** fig_sejong · jang-yeongsil · yi_sun_sin → sejong · jangyeongsil · yisunsin */
        fun normalize(id: String) = id.removePrefix("fig_").replace("-", "").replace("_", "")

        /** 백엔드에 없는 인물 — 앱 인물 자료로 백엔드와 같은 모양의 설정을 만든다 */
        fun personaFrom(f: Figure): Persona {
            val died = f.died ?: 2026
            val tone = when (f.style) {
                "king" -> "임금으로서 백성을 아끼는 위엄 있고 자애로운 어조(~하노라, ~이니라)를 쓴다."
                "general" -> "장수로서 결연하고 강직한 어조(~하였소, ~하겠네)를 쓴다."
                "lady" -> "단아하고 온화하며 기품 있는 어조(~하옵니다, ~이지요)를 쓴다."
                else -> "선비로서 차분하고 품위 있는 어조(~하오, ~이외다)를 쓴다."
            }
            // 예: 「정조(1752–1800)이다. 조선 제22대 국왕. 개혁 군주로 … 어조를 쓴다.」
            val years = f.years.takeIf { it.isNotBlank() && it.any(Char::isDigit) }?.let { "($it)" }.orEmpty()
            val title = f.title.takeIf { it.isNotBlank() }?.let { "$it." }.orEmpty()
            return Persona(
                name = f.name,
                era = if (died <= 1392) "고려" else "조선",
                maxYear = died,
                // 「태조 이성계」→ 이성계, 「세종대왕」→ 세종 (백엔드 검색어와 같은 방식)
                keyword = f.name.split(' ').last().removeSuffix("대왕"),
                persona = "${f.name}$years 이다. $title ${f.bio.trim()} $tone".replace(Regex("\\s+"), " ").replace(" 이다.", "이다.").trim(),
            )
        }

        /** 백엔드 get_optimized_query — 질문에서 문장부호를 빼고 2자 이상 낱말 2개를 인물 검색어에 붙인다 */
        fun optimizedQuery(keyword: String, question: String): String {
            val cleaned = question.filter { it.isLetterOrDigit() || it == '_' || it.isWhitespace() }
            val words = cleaned.split(Regex("\\s+")).filter { it.length > 1 && it != keyword }.take(2)
            return if (words.isEmpty()) keyword else "$keyword ${words.joinToString(" ")}"
        }

        /** 「[출처: 국가기록원 API] 내용」 → 출처 · 내용 */
        fun citationOf(context: String): Citation {
            val m = Regex("^\\[출처: (.+?)]\\s*(.*)$", RegexOption.DOT_MATCHES_ALL).find(context)
            return if (m == null) {
                Citation(source = "참고 자료", excerpt = context.take(EXCERPT))
            } else {
                Citation(source = m.groupValues[1], excerpt = m.groupValues[2].take(EXCERPT))
            }
        }

        private const val EXCERPT = 90

        /** 백엔드 main.py 의 프롬프트 그대로 (원칙 4 는 지식 경계 필터가 켜졌을 때만) */
        fun prompt(p: Persona, contexts: List<String>, question: String, boundary: Boolean): String {
            val contextText = if (contexts.isNotEmpty()) {
                contexts.joinToString("\n")
            } else {
                "'${p.name}'와 관련된 직접적인 공공 API 기록을 찾지 못했으나, 인물의 성향에 맞춰 지혜롭게 답하라."
            }
            val rule4 = if (!boundary) "" else "\n4. 너는 네가 세상을 떠난 ${p.maxYear}년 이후에 일어난 미래의 역사나 현대의 일에 대해서는 전혀 알지 못한다. " +
                "참고 지식에 없거나 네가 살던 시기 이후의 미래 일을 물어보면 인물의 성격에 맞춰 \"내가 살아있을 적에는 들어보지 못한 후세의 일이로다\"라는 식으로 페르소나를 유지하며 모른다고 답하라."
            return """
너는 ${p.era}시대를 살다 간 역사적 인물 '${p.name}'이다.
[상세 신분 및 말투 지침]
${p.persona}

사용자의 질문에 답할 때, 다음 원칙을 반드시 지켜라:
1. 현대인의 말투나 인공지능스러운 말투("안녕하세요", "AI입니다" 등)를 절대 쓰지 말고, 위 [상세 신분 및 말투 지침]에 정의된 인물 고유의 어조를 철저히 지켜라.
2. 아래 제공되는 [실시간 참고 역사 자료]를 적극적으로 활용하여 역사적 사실에 기반해 깊이 있게 답변하라.
3. 자료에 구체적인 내용이 없더라도 인물의 업적과 성향에 맞추어 지혜롭게 답변하되, 역사적 사실을 왜곡하지 말라.$rule4

[실시간 참고 역사 자료]
$contextText

[사용자 질문]
$question
"""
        }
    }
}
