package com.samdori93.yeoksadam.core.ui.sample

/**
 * 백엔드 연동 전, 목업 화면 렌더를 위한 샘플 데이터.
 * core:data 의 FakeFigureCatalog 와 별개로 표현계층 미리보기/시안용.
 */
data class SampleFigure(
    val id: String,
    val name: String,
    val title: String, // 호/직함
    val site: String,
    val distanceLabel: String,
    val discovered: Boolean = false,
    val initial: Char = name.first(),
)

object SampleData {

    val chae = SampleFigure("fig_chae", "채제공", "번암", "수원화성", "30m", discovered = true)

    val figures: List<SampleFigure> = listOf(
        SampleFigure("fig_kang", "강세황", "표암 · 문인화가", "—", "미발견"),
        SampleFigure("fig_kim", "김홍도", "단원 · 화가", "—", "미발견"),
        SampleFigure("fig_yoon", "윤두서", "공재 · 자화상", "—", "미발견"),
        SampleFigure("fig_jeongyy", "정약용", "다산초당 · 실학자", "강진", "미발견"),
        chae,
    )

    fun figure(id: String): SampleFigure =
        figures.firstOrNull { it.id == id } ?: chae

    /** 챗봇 샘플 대화 (목업 5번). true=인물(them), false=나(me). */
    val chatTranscript: List<Pair<Boolean, String>> = listOf(
        true to "어서 오시게. 화성 행궁에는 들러보셨는가?",
        false to "대감께서 수원 화성 축조를 총괄하셨다지요?",
        true to "그렇네. 정조 임금의 깊은 효심과 백성을 위한 뜻을 성벽 돌 하나하나에 새겼지.",
        false to "그 큰 공사를 어찌 그리 빨리 마치셨습니까?",
        true to "정약용이 만든 거중기로 무거운 돌을 손쉽게 들어 올리니, 백성의 수고가 크게 덜렸느니라.",
    )

    /** 음성 실시간 자막 샘플 (목업 6번). */
    val voiceTranscript: List<Pair<Boolean, String>> = listOf(
        false to "대감, 백성을 위한 정치란 무엇입니까?",
        true to "두려움 없이 바른말을 올리고, 백성의 곤궁을 내 일처럼 여기는 것이네.",
    )

    /** 유물 인식 해설 (목업 3번). */
    const val RELIC_NAME = "백자 달항아리"
    const val RELIC_META = "18세기 · 인식 92%"
    const val RELIC_NARRATION =
        "조선의 백자 달항아리일세. 꾸밈없는 둥근 자태에 선비의 청렴한 기품이 그대로 담겨 있지 않은가."

    /** 지도 핀 — 유적지에 맞는 인물 (실제 좌표). */
    data class MapPin(
        val figureId: String,
        val figureName: String,
        val siteId: String,
        val siteName: String,
        val lat: Double,
        val lng: Double,
        val distanceLabel: String,
    )

    val mapPins: List<MapPin> = listOf(
        MapPin("fig_sejong", "세종", "site_gbg", "경복궁", 37.579617, 126.977041, "약 1.2km"),
        MapPin("fig_jeongjo", "정조", "site_cdg", "창덕궁", 37.582604, 126.991987, "약 1.8km"),
        MapPin("fig_gojong", "고종", "site_dsg", "덕수궁", 37.565804, 126.975144, "약 900m"),
        MapPin("fig_sinsaimdang", "신사임당", "site_jm", "종묘", 37.574202, 126.994359, "약 2.1km"),
    )

    /** 카메라 유물 인식 결과 — 인식된 유물 + 해설 가능한 관련 인물들. */
    data class RelicFigure(val id: String, val name: String, val narration: String)

    data class RelicResult(val name: String, val meta: String, val figures: List<RelicFigure>)

    val recognizedRelic = RelicResult(
        name = "백자 달항아리",
        meta = "18세기 · 인식 92%",
        figures = listOf(
            RelicFigure(
                "fig_chae", "채제공",
                "조선의 백자 달항아리일세. 꾸밈없는 둥근 자태에 선비의 청렴한 기품이 그대로 담겨 있지 않은가.",
            ),
            RelicFigure(
                "fig_jeongyy", "정약용",
                "이 항아리의 부드러운 곡선은 두 개의 발을 따로 빚어 이어 붙인 것이라네. 실용과 아름다움이 함께 깃들었지.",
            ),
            RelicFigure(
                "fig_jeongjo", "정조",
                "백자의 검소하고 단정한 빛깔은 조선이 추구한 절제의 미를 그대로 보여주는구나.",
            ),
        ),
    )
}
