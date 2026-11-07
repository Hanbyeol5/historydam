package com.samdori93.yeoksadam.core.domain.usecase

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.HeritageSite
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.model.NearbySite
import com.samdori93.yeoksadam.core.domain.model.SiteDetail
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindRelatedFiguresUseCaseTest {

    private val hwaseong = "13-0000030000000-31"
    private val figures = listOf(
        Figure("jeongjo", "정조", "조선 제22대 국왕", "", relatedSiteIds = listOf(hwaseong), siteNotes = mapOf(hwaseong to "화성 축성을 명함")),
        Figure("jeong-yakyong", "정약용", "다산", "", relatedSiteIds = listOf(hwaseong), siteNotes = mapOf(hwaseong to "화성 설계·거중기 고안")),
        Figure("sejong", "세종대왕", "조선 제4대 국왕", "", hanja = "世宗"),
        Figure("yi-i", "이이", "율곡", ""),
    )
    private val useCase = FindRelatedFiguresUseCase(FakeCatalog(figures))

    @Test
    fun `인식한 유적의 관련 인물은 유적 메모와 함께`() = runTest {
        val r = useCase(siteId = hwaseong, texts = emptyList())
        assertEquals(listOf("jeongjo", "jeong-yakyong"), r.map { it.figure.id })
        assertEquals("화성 설계·거중기 고안", r[1].reason)
    }

    @Test
    fun `해설 속 이름 · 호 · 대왕 뺀 이름으로 찾는다`() = runTest {
        val r = useCase(siteId = null, texts = listOf("세종 때 만든 측우기다. 율곡의 글씨가 남아 있다."))
        assertEquals(listOf("sejong", "yi-i"), r.map { it.figure.id })
    }

    @Test
    fun `다른 낱말 속 글자는 무시한다`() = runTest {
        val r = useCase(siteId = null, texts = listOf("우리 종이 한지 공정조사편, 높이이며 너비는 2m"))
        assertEquals(emptyList<String>(), r.map { it.figure.id })
    }

    private class FakeCatalog(private val list: List<Figure>) : CatalogRepository {
        override suspend fun figures(): Result<List<Figure>> = Result.Success(list)
        override suspend fun sites(): Result<List<HeritageSite>> = Result.Success(emptyList())
        override suspend fun figure(id: String): Figure? = list.firstOrNull { it.id == id }
        override suspend fun site(id: String): HeritageSite? = null
        override suspend fun siteDetail(id: String): Result<SiteDetail> = error("unused")
        override suspend fun nearbySites(origin: LatLng, radiusM: Int): List<NearbySite> = emptyList()
        override suspend fun nearestSiteOf(figure: Figure, origin: LatLng): NearbyFigure? = null
    }
}
