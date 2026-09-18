package com.samdori93.yeoksadam.core.network.heritage

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * 국가유산청 OpenAPI 의 평면적인 XML 응답을 파싱한다.
 * - `<item>` 블록마다 `태그 → 텍스트` 맵을 만든다(목록/이미지 = 다수, 상세 = 1개).
 * - `<item>` 바깥의 최상위 태그(latitude/longitude/totalCnt 등)는 [Parsed.root] 로 모은다.
 * CDATA 는 기본 pull parser 에서 TEXT 이벤트로 전달된다.
 */
internal object HeritageXmlParser {

    data class Parsed(val root: Map<String, String>, val items: List<Map<String, String>>)

    fun parse(xml: String): Parsed {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }
        val root = mutableMapOf<String, String>()
        val items = mutableListOf<Map<String, String>>()
        var current: MutableMap<String, String>? = null
        var tag: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG ->
                    if (parser.name == ITEM) current = mutableMapOf() else tag = parser.name

                XmlPullParser.TEXT -> {
                    val key = tag
                    val text = parser.text?.trim().orEmpty()
                    if (key != null && text.isNotEmpty()) {
                        val target = current ?: root
                        // 같은 태그의 첫 유효 텍스트만 취한다(공백/개행 노이즈 방지).
                        if (target[key].isNullOrEmpty()) target[key] = text
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == ITEM) {
                        current?.let { items.add(it) }
                        current = null
                    }
                    tag = null
                }
            }
            event = parser.next()
        }
        return Parsed(root, items)
    }

    private const val ITEM = "item"
}
