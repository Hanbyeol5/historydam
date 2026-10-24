package com.samdori93.yeoksadam.core.network.rag

import com.samdori93.yeoksadam.core.network.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 공공 API 5곳 검색 — historydam backend(main.py)의 fetch_museum · fetch_archives · fetch_kdp ·
 * fetch_dictionary · fetch_diary 를 앱으로 옮긴 것 (지식이음 2곳은 실제 응답 형식에 맞게 고침). 각 함수는 「[출처: …] 내용」 문자열을 최대 2건 돌려주고,
 * 오류·빈 결과는 빈 목록이다 (백엔드와 같음). 네트워크를 쓰므로 IO 디스패처에서 부른다.
 *
 * 키는 local.properties 의 PUBLIC_API_KEY(공공데이터포털) · KDP_API_KEY (커밋 금지).
 */
@Singleton
class PublicArchiveDataSource @Inject constructor(
    client: OkHttpClient,
) {
    /** 백엔드와 같이 요청마다 5초 */
    private val http = client.newBuilder().callTimeout(TIMEOUT_S, TimeUnit.SECONDS).build()

    /** 백엔드의 unquote(PUBLIC_API_KEY) — 포털의 인코딩된 키를 넣어도 한 번 풀어서 보낸다 (+ 는 그대로) */
    private val publicKey: String = percentDecode(BuildConfig.PUBLIC_API_KEY)
    private val kdpKey: String = BuildConfig.KDP_API_KEY

    /**
     * 1. 국립중앙박물관 소장품 — 응답은 <data><item key="nameKr" value="…"/>…</data> 형식
     * (백엔드는 item 의 글자를 읽어 늘 비어 있었다 — 속성 key/value 로 읽는다)
     */
    fun museum(keyword: String): List<String> = guarded {
        if (publicKey.isBlank()) return@guarded emptyList()
        val doc = get(MUSEUM_URL, "serviceKey" to publicKey, "pageNo" to "1", "numOfRows" to "5", "nameKr" to keyword)
            ?: return@guarded emptyList()
        doc.items("data").mapNotNull { d ->
            val f = d.childElements().filter { it.tagName == "item" }.associate { it.getAttribute("key") to clean(it.getAttribute("value")) }
            val name = f["nameKr"].orEmpty().ifBlank { f["name"].orEmpty() }
            val texts = listOf(name, f["museumName2"].orEmpty(), f["indexWord"].orEmpty()).filter { it.isNotBlank() }
            if (name.isBlank()) null else "[출처: 국립중앙박물관 API] ${texts.joinToString(" ")}"
        }.take(MAX_PER_SOURCE)
    }

    /** 2. 국가기록원 나라기록물 */
    fun archives(keyword: String): List<String> = guarded {
        if (publicKey.isBlank()) return@guarded emptyList()
        val doc = get(ARCHIVES_URL, "serviceKey" to publicKey, "pageNo" to "1", "display" to "3", "query" to keyword)
            ?: return@guarded emptyList()
        if (doc.items("searchError").isNotEmpty()) return@guarded emptyList()
        doc.items("item").map { item ->
            val title = clean(item.child("title")?.ownText()).ifEmpty { "기록물" }
            val content = clean(item.child("content")?.ownText())
            "[출처: 국가기록원 API] $title: $content"
        }.take(MAX_PER_SOURCE)
    }

    /** 3. 한국학자료통합플랫폼(KDP) */
    fun kdp(keyword: String): List<String> = guarded {
        if (kdpKey.isBlank()) return@guarded emptyList()
        val doc = get(KDP_URL, "key" to kdpKey, "keyword" to keyword, "page" to "1", "size" to "3")
            ?: return@guarded emptyList()
        val items = doc.items("item").ifEmpty { doc.items("result") }.ifEmpty { doc.items("doc") }
        items.mapNotNull { item ->
            // 제목 · 초록 · 소장 기관 (백엔드는 앞 3개 칸 = 제목·기관·DB 이름이라 내용이 빠졌다)
            val texts = listOf("title", "abstract", "institution").map { clean(item.child(it)?.textContent) }.filter { it.isNotBlank() }
                .ifEmpty { item.childElements().map { clean(it.ownText()) }.filter { it.isNotBlank() }.take(3) }
            if (texts.isEmpty()) null else "[출처: 한국학자료통합플랫폼(KDP) API] ${texts.joinToString(" ")}"
        }.take(MAX_PER_SOURCE)
    }

    /**
     * 4. 국가유산 지식이음 — 한국고고학사전 (idx=39).
     * 이 API 는 검색어를 받지 않고 2만 건이 넘어 전부 받을 수 없으므로, 백엔드처럼 앞 20건에서 검색어가 든 것만 쓴다.
     * (백엔드는 firstIndex·<item>·NAME 으로 읽어 늘 0건이었다 — 실제 응답은 firstindex·<data>·md_dataN)
     */
    fun dictionary(keyword: String): List<String> = guarded {
        val doc = get(NRICH_URL, "idx" to "39", "firstindex" to "1", "recordcountperpage" to "20")
            ?: return@guarded emptyList()
        match(nrichRecords(doc), keyword, "한국고고학사전 API")
    }

    /**
     * 5. 국가유산 지식이음 — 조선시대 개인일기 (idx=205, 1,431건 · 약 2MB · 받는 데 30초 안팎).
     * 검색어를 받지 않는 API 라 처음 부를 때 뒤에서 전부 받아 두고(앱이 켜져 있는 동안), 받기 전에는 빈 목록을 돌려
     * 답을 기다리게 하지 않는다. 받아 둔 뒤의 질문부터 일기에서 찾는다.
     */
    fun diary(keyword: String): List<String> {
        val all = diaries
        if (all == null) {
            prefetchDiaries()
            return emptyList()
        }
        return runCatching { match(all, keyword, "조선시대 개인일기 API") }.getOrDefault(emptyList())
    }

    /** 일기 전체를 뒤에서 받아 둔다 (이미 받았거나 받는 중이면 아무것도 안 함, 실패하면 다음에 다시) */
    fun prefetchDiaries() {
        if (diaries != null || !loadingDiaries.compareAndSet(false, true)) return
        Thread {
            try {
                get(NRICH_URL, "idx" to "205", "firstindex" to "1", "recordcountperpage" to "5000", timeoutS = DIARY_TIMEOUT_S)
                    ?.let { diaries = nrichRecords(it) }
            } catch (_: Exception) {
                // 다음 질문 때 다시 시도
            } finally {
                loadingDiaries.set(false)
            }
        }.apply { isDaemon = true; name = "nrich-diary" }.start()
    }

    @Volatile
    private var diaries: List<NrichRecord>? = null
    private val loadingDiaries = java.util.concurrent.atomic.AtomicBoolean(false)

    /** 지식이음 레코드 — md_data3 이 이름(일기명·유적명), 가장 긴 글이 설명 */
    private data class NrichRecord(val title: String, val text: String)

    private fun nrichRecords(doc: Element): List<NrichRecord> = doc.items("data").map { d ->
        val fields = d.childElements().associate { it.tagName to clean(it.textContent) }
            .filterValues { it.isNotBlank() && it != "null" && !it.startsWith("http") }
        val title = fields["md_data3"].orEmpty()
        NrichRecord(title, fields.values.filter { it != title }.maxByOrNull { it.length }.orEmpty())
    }

    /** 인물 검색어(첫 낱말)가 들어간 것 중 질문 핵심어가 많이 겹치는 순으로 2건 */
    private fun match(records: List<NrichRecord>, keyword: String, source: String): List<String> {
        val words = keyword.split(' ').filter { it.isNotBlank() }
        val head = words.firstOrNull() ?: return emptyList()
        return records
            .filter { head in it.title || head in it.text }
            .sortedByDescending { r -> words.drop(1).count { w -> w in r.title || w in r.text } }
            .take(MAX_PER_SOURCE)
            .map { "[출처: $source] ${it.title}: ${it.text.take(100)}" }
    }

    /** 응답이 200 이 아니면 null (KDP 는 백엔드도 상태코드를 본다. 나머지는 XML 파싱 실패로 같은 결과) */
    private fun get(base: String, vararg params: Pair<String, String>, timeoutS: Long = TIMEOUT_S): Element? {
        val url = base.toHttpUrl().newBuilder().apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        // 지식이음 일기 전체는 첫 바이트까지 수십 초 걸린다 — 읽기 대기도 함께 늘린다
        val client = if (timeoutS == TIMEOUT_S) {
            http
        } else {
            http.newBuilder().callTimeout(timeoutS, TimeUnit.SECONDS).readTimeout(timeoutS, TimeUnit.SECONDS).build()
        }
        client.newCall(Request.Builder().url(url).build()).execute().use { res ->
            if (!res.isSuccessful) return null
            return parseXml(res.body?.bytes() ?: return null)
        }
    }

    private inline fun guarded(block: () -> List<String>): List<String> = runCatching(block).getOrDefault(emptyList())

    private fun parseXml(bytes: ByteArray): Element {
        val factory = DocumentBuilderFactory.newInstance()
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        return factory.newDocumentBuilder().parse(InputSource(ByteArrayInputStream(bytes))).documentElement
    }

    /** ElementTree 의 findall(".//name") */
    private fun Element.items(name: String): List<Element> {
        val nodes = getElementsByTagName(name)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    /** ElementTree 의 find("name") (바로 아래 자식) */
    private fun Element.child(name: String): Element? = childElements().firstOrNull { it.tagName == name }

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

    /** ElementTree 의 element.text — 첫 자식 요소 앞까지의 글자 */
    private fun Element.ownText(): String = buildString {
        for (i in 0 until childNodes.length) {
            val n = childNodes.item(i)
            if (n.nodeType == Node.ELEMENT_NODE) break
            if (n.nodeType == Node.TEXT_NODE || n.nodeType == Node.CDATA_SECTION_NODE) append(n.nodeValue)
        }
    }

    /** 백엔드 clean_text — HTML 태그(<em> 등) 제거 */
    private fun clean(text: String?): String = text.orEmpty().replace(TAG, "").trim()

    private companion object {
        /** 파이썬 urllib.parse.unquote — %XX 만 풀고 + 는 그대로 */
        fun percentDecode(s: String): String {
            if ('%' !in s) return s
            val out = java.io.ByteArrayOutputStream()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '%' && i + 2 <= s.lastIndex && s.substring(i + 1, i + 3).toIntOrNull(16) != null) {
                    out.write(s.substring(i + 1, i + 3).toInt(16))
                    i += 3
                } else {
                    out.write(c.toString().toByteArray(Charsets.UTF_8))
                    i++
                }
            }
            return out.toString(Charsets.UTF_8.name())
        }

        const val MUSEUM_URL = "https://apis.data.go.kr/1371027/openapi/list"
        const val ARCHIVES_URL = "https://apis.data.go.kr/1741050/openapi/searcharc"
        const val KDP_URL = "https://kdp.aks.ac.kr/api/search"
        const val NRICH_URL = "http://portal.nrich.go.kr/kor/openapi.do"
        const val TIMEOUT_S = 5L
        const val DIARY_TIMEOUT_S = 90L
        const val MAX_PER_SOURCE = 2
        val TAG = Regex("<.*?>")
    }
}
