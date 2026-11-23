package com.samdori93.yeoksadam.core.backend

/**
 * 지식 경계 — 역사 인물은 세상을 떠난 뒤의 일을 모른다.
 * 질문이 사후의 일(연도·세기·사건·근대 문물·사후에 태어난 인물)을 물으면 LLM 에 묻지 않고 정해진 답을 한다.
 * 역사담 Worker 의 worker/src/ragText.ts afterDeath() 와 같은 규칙.
 */
object KnowledgeBoundary {
    private val EVENTS: List<Pair<Regex, Int>> = listOf(
        Regex("임진왜란") to 1592,
        Regex("정유재란") to 1597,
        Regex("정묘호란") to 1627,
        Regex("병자호란") to 1636,
        Regex("갑신정변") to 1884,
        Regex("동학\\s*농민|갑오개혁|청일\\s*전쟁") to 1894,
        Regex("을사(늑약|조약)") to 1905,
        Regex("경술국치|한일\\s*병합|일제\\s*강점|일제\\s*시대|일본\\s*강점") to 1910,
        Regex("(3|삼)[·.\\s]?1\\s*운동") to 1919,
        Regex("광복|해방\\s*후|8[·.]15") to 1945,
        Regex("한국\\s*전쟁|6[·.]25\\s*전쟁|6[·.]25") to 1950,
        Regex("(4|사)[·.]19") to 1960,
        Regex("서울\\s*올림픽") to 1988,
        Regex("월드컵") to 2002,
    )
    private val MODERN = Regex(
        "(대한민국|대통령|국회의원|인터넷|스마트\\s*폰|휴대\\s*폰|컴퓨터|텔레비전|티비|자동차|비행기|기차|지하철|핵무기|원자\\s*폭탄|유튜브|인공\\s*지능|AI|코로나|아이돌|케이팝|K-?pop|민주주의|공화국|헌법)",
        RegexOption.IGNORE_CASE,
    )
    private val YEAR = Regex("(1\\d{3}|20\\d{2})\\s*년")
    private val CENTURY = Regex("(\\d{1,2})\\s*세기")

    /**
     * @param later 다른 인물 [이름, 태어난 해]
     * @return 걸린 근거 (예: "1950년", "임진왜란"), 아니면 null
     */
    fun afterDeath(question: String, died: Int, later: List<Pair<String, Int>> = emptyList()): String? {
        val q = question.replace(Regex("\\s+"), " ")
        YEAR.findAll(q).firstOrNull { it.groupValues[1].toInt() > died }?.let { return "${it.groupValues[1]}년" }
        CENTURY.findAll(q).firstOrNull { (it.groupValues[1].toInt() - 1) * 100 + 1 > died }?.let { return "${it.groupValues[1]}세기" }
        EVENTS.firstOrNull { (re, year) -> year > died && re.containsMatchIn(q) }?.let { (re, _) -> return re.find(q)!!.value }
        if (died < 1876) MODERN.find(q)?.let { return it.value }
        later.firstOrNull { (name, born) -> born > died && name.length >= 2 && q.contains(name) }?.let { return it.first }
        return null
    }

    /** 지식 경계에 걸린 질문에 대한 정해진 답 (인물 말씨) */
    fun reply(style: String?, died: Int): String =
        if (style == "lady") {
            "제가 세상을 떠난 ${died}년 뒤의 일이라 저는 알지 못한답니다. 제가 살던 때의 이야기를 물어 주세요."
        } else {
            "내가 세상을 떠난 ${died}년 뒤의 일이라 나는 알지 못하네. 내가 살던 때의 이야기라면 기꺼이 들려주겠네."
        }
}
