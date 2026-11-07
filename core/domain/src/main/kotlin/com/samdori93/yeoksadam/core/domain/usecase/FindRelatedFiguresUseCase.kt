package com.samdori93.yeoksadam.core.domain.usecase

import com.samdori93.yeoksadam.core.common.result.getOrNull
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import javax.inject.Inject

/** 사진으로 인식한 유적·유물과 관련된 인물과, 어떻게 관련됐는지 */
data class RelatedFigure(val figure: Figure, val reason: String)

/**
 * 인식 결과 → 관련 인물.
 *  1) 인식한 유적이 인물의 관련 유적이면 그 인물 (관계 설명: 인물 자료의 유적 메모, 예 「화성 설계·거중기 고안」)
 *  2) 이름·해설에 인물 이름이나 호가 나오면 그 인물 (예 「정조 때 …」, 「다산 정약용의 …」)
 * 다른 낱말 속에 섞인 글자(「공정조사」의 「정조」)는 걸리지 않도록 앞 글자가 한글 음절이면 제외한다.
 */
class FindRelatedFiguresUseCase @Inject constructor(
    private val catalog: CatalogRepository,
) {
    suspend operator fun invoke(siteId: String?, texts: List<String>): List<RelatedFigure> {
        val figures = catalog.figures().getOrNull().orEmpty()
        val bySite = siteId?.let { id ->
            figures.filter { id in it.relatedSiteIds }
                .map { RelatedFigure(it, it.siteNotes[id]?.takeIf(String::isNotBlank) ?: "이 유적과 관련된 인물") }
        }.orEmpty()
        val text = texts.filter { it.isNotBlank() }.joinToString("\n")
        val byText = figures
            .filter { f -> bySite.none { it.figure.id == f.id } }
            .mapNotNull { f -> keysOf(f).firstOrNull { mentions(text, it) }?.let { key -> RelatedFigure(f, "해설에 「$key」이(가) 나와요") } }
        return (bySite + byText).take(MAX_FIGURES)
    }

    /** 이름(「세종대왕」→ 세종도), 「태조 이성계」의 각 낱말, 호(다산·율곡 등 짧은 직함), 한자 이름 */
    private fun keysOf(f: Figure): List<String> = buildList {
        add(f.name)
        add(f.name.removeSuffix("대왕"))
        addAll(f.name.split(' '))
        f.title.split('·').map(String::trim).filter { it.length in 2..3 && ' ' !in it }.forEach(::add)
        f.hanja.takeIf { it.length >= 2 }?.let(::add)
    }.filter { it.length >= 2 }.distinct()

    /** [key] 가 낱말의 시작으로 나오는지 (뒤에 조사는 붙어도 됨) */
    private fun mentions(text: String, key: String): Boolean {
        var i = text.indexOf(key)
        while (i >= 0) {
            val before = if (i == 0) null else text[i - 1]
            if (before == null || before !in '가'..'힣') return true
            i = text.indexOf(key, i + 1)
        }
        return false
    }

    private companion object {
        const val MAX_FIGURES = 4
    }
}
