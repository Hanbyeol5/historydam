package com.samdori93.yeoksadam.core.network.heritage

/**
 * 국가유산청 국가유산정보 OpenAPI 응답(XML) 매핑용 DTO.
 * (JSON 이 아니라 XML 이므로 kotlinx.serialization 대신 XmlPullParser 로 파싱한다.)
 */
data class HeritageItemDto(
    val kind: String, // ccmaName (국보/사적 등)
    val nameKo: String, // ccbaMnm1
    val nameHanja: String, // ccbaMnm2
    val ctcdName: String, // ccbaCtcdNm (시도)
    val siName: String, // ccsiName (시군구)
    val kdcd: String, // ccbaKdcd (종목코드)
    val asno: String, // ccbaAsno (관리번호)
    val ctcd: String, // ccbaCtcd (시도코드)
    val lat: Double?, // latitude
    val lng: Double?, // longitude
)

data class HeritageDetailDto(
    val kind: String, // ccmaName
    val nameKo: String, // ccbaMnm1
    val nameHanja: String, // ccbaMnm2
    val era: String, // ccceName (시대)
    val category: String, // gcodeName 등 분류
    val address: String, // ccbaLcad (소재지)
    val content: String, // content (설명)
    val imageUrl: String?, // imageUrl (https 로 정규화)
    val lat: Double?,
    val lng: Double?,
)
