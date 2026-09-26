package com.samdori93.yeoksadam.core.network.heritage

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/**
 * 국가유산청 국가유산정보 OpenAPI 호출(인증키 불필요, XML 응답).
 * 블로킹 호출이므로 상위 Repository 가 IO 디스패처에서 호출한다.
 *
 * 엔드포인트(https://www.khs.go.kr/cha/):
 * - 종목별 목록: SearchKindOpenapiList.do?ccbaKdcd=&ccbaCtcd=&pageUnit=
 * - 상세:        SearchKindOpenapiDt.do?ccbaKdcd=&ccbaAsno=&ccbaCtcd=
 */
class HeritageRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
) {
    /** 지정 종목([kdcd]) 전체(또는 [ctcd] 시도) 목록. 좌표 포함. */
    fun fetchList(kdcd: String, ctcd: String? = null, pageUnit: Int = DEFAULT_PAGE_UNIT): List<HeritageItemDto> {
        val url = buildString {
            append(BASE).append("SearchKindOpenapiList.do?ccbaKdcd=").append(kdcd)
            append("&pageUnit=").append(pageUnit)
            if (!ctcd.isNullOrEmpty()) append("&ccbaCtcd=").append(ctcd)
        }
        return HeritageXmlParser.parse(get(url)).items.map { it.toItemDto() }
    }

    /** 단일 국가유산 상세(설명·시대·이미지). */
    fun fetchDetail(kdcd: String, asno: String, ctcd: String): HeritageDetailDto? {
        val url = "${BASE}SearchKindOpenapiDt.do?ccbaKdcd=$kdcd&ccbaAsno=$asno&ccbaCtcd=$ctcd"
        val parsed = HeritageXmlParser.parse(get(url))
        val item = parsed.items.firstOrNull() ?: return null
        return item.toDetailDto(parsed.root)
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("국가유산청 API 오류: HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private companion object {
        const val BASE = "https://www.khs.go.kr/cha/"
        const val DEFAULT_PAGE_UNIT = 900
        const val USER_AGENT = "Yeoksadam-Android/1.0 (heritage-openapi)"
    }
}

private fun Map<String, String>.toItemDto() = HeritageItemDto(
    kind = this["ccmaName"].orEmpty(),
    nameKo = this["ccbaMnm1"].orEmpty(),
    nameHanja = this["ccbaMnm2"].orEmpty(),
    ctcdName = this["ccbaCtcdNm"].orEmpty(),
    siName = this["ccsiName"].orEmpty(),
    kdcd = this["ccbaKdcd"].orEmpty(),
    asno = this["ccbaAsno"].orEmpty(),
    ctcd = this["ccbaCtcd"].orEmpty(),
    lat = this["latitude"]?.toDoubleOrNull(),
    lng = this["longitude"]?.toDoubleOrNull(),
)

private fun Map<String, String>.toDetailDto(root: Map<String, String>) = HeritageDetailDto(
    kind = this["ccmaName"].orEmpty(),
    nameKo = this["ccbaMnm1"].orEmpty(),
    nameHanja = this["ccbaMnm2"].orEmpty(),
    era = this["ccceName"].orEmpty(),
    category = listOfNotNull(this["gcodeName"], this["bcodeName"], this["mcodeName"])
        .filter { it.isNotBlank() }
        .joinToString(" · "),
    address = this["ccbaLcad"].orEmpty(),
    content = this["content"].orEmpty(),
    imageUrl = this["imageUrl"]?.takeIf { it.isNotBlank() }?.replaceFirst("http://", "https://"),
    lat = (this["latitude"] ?: root["latitude"])?.toDoubleOrNull(),
    lng = (this["longitude"] ?: root["longitude"])?.toDoubleOrNull(),
)
