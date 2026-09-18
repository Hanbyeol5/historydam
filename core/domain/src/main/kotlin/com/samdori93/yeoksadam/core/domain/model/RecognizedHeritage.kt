package com.samdori93.yeoksadam.core.domain.model

/**
 * 국가유산청 국가유산정보 OpenAPI로 조회한 국가유산(문화재) 1건.
 * 카메라의 유물/건물 인식 결과 및 지도/AR 후보로 사용한다.
 *
 * 목록 단계에서는 [description]/[era]/[imageUrl] 이 비어 있고,
 * 상세 조회(GetHeritageDetailUseCase) 후 채워진다.
 */
data class RecognizedHeritage(
    val id: String, // "종목코드_관리번호_시도코드" 합성 키 (상세 조회용)
    val name: String, // 국문 명칭 (ccbaMnm1)
    val hanja: String, // 한자 명칭 (ccbaMnm2)
    val kind: String, // 지정 종목 (ccmaName) 예: 국보 / 사적
    val era: String, // 시대 (ccceName)
    val category: String, // 분류 (gcodeName 등)
    val address: String, // 소재지
    val description: String, // 설명 (content)
    val imageUrl: String?, // 대표 이미지 (https)
    val lat: Double,
    val lng: Double,
    val distanceM: Float, // 현재 위치로부터 거리(m)
)
