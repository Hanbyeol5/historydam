package com.samdori93.yeoksadam.core.network.worker

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * 역사담 대화·음성·인식 서버 (Cloudflare Worker, BuildConfig.WORKER_BASE_URL).
 * 웹앱과 같은 서버를 쓰며, 앱은 X-App-Key 헤더로 들어간다([AppKeyInterceptor]).
 */
interface WorkerApi {
    /** 기본 대화 — 인물에 연결된 유적 설명을 근거로 */
    @POST("chat")
    suspend fun chat(@Body body: TalkBody): TalkResponse

    /** RAG 대화 — 질문마다 유적 자료를 검색해 근거로, 출처 표시 */
    @POST("rag")
    suspend fun rag(@Body body: TalkBody): TalkResponse

    /** 인물 목소리 (Azure 신경망 음성, audio/mpeg) */
    @Streaming
    @POST("tts")
    suspend fun tts(@Body body: TtsBody): ResponseBody

    /** 사진 속 문화재 판별 (Claude 비전) */
    @POST("vision")
    suspend fun vision(@Body body: VisionBody): VisionResponse

    /** 외부 RAG 서버 (historydam backend: POST /v1/chat {figureId, question}) */
    @POST
    suspend fun external(@Url url: String, @Body body: ExternalBody): ExternalResponse

    // ---------- 서버 저장소 (3D 모델 · RAG 자료 · 외부 RAG 인물 설정) ----------
    @GET("store/models.json")
    suspend fun storeModels(): StoreModelsDto

    @GET("store/docs.json")
    suspend fun storeDocs(): StoreDocsDto

    @GET("store/personas.json")
    suspend fun storePersonas(): StorePersonasDto

    /** 인물에 연결된 업로드 자료 조각 (백엔드 RAG 참고 자료) */
    @GET("store/context/{figureId}")
    suspend fun storeContext(@Path("figureId") figureId: String): StoreContextDto

    /** 3D 모델 내려받기 — [fileName] 은 「이순신.glb」 처럼 확장자 포함 */
    @Streaming
    @GET("model/{fileName}")
    suspend fun model(@Path("fileName") fileName: String): ResponseBody

    @PUT("model/{fileName}")
    suspend fun putModel(
        @Path("fileName") fileName: String,
        @Header("X-Upload-Key") uploadKey: String,
        @Header("X-Content-SHA256") sha256: String,
        @Header("X-Height-M") heightM: String?,
        @Body body: RequestBody,
    ): StorePutDto

    @PUT("doc/{fileName}")
    suspend fun putDoc(
        @Path("fileName") fileName: String,
        @Header("X-Upload-Key") uploadKey: String,
        @Body body: RequestBody,
    ): StorePutDto
}

@Serializable
data class StoreModelDto(
    val name: String,
    val key: String,
    val id: String? = null,
    val size: Long = 0,
    val sha256: String = "",
    val heightM: Float = 1.65f,
    val uploadedAt: String = "",
)

@Serializable
data class StoreModelsDto(val models: List<StoreModelDto> = emptyList())

@Serializable
data class StoreRefDto(val id: String, val name: String)

@Serializable
data class StoreDocDto(
    val name: String,
    val key: String,
    val size: Long = 0,
    val sha256: String = "",
    val uploadedAt: String = "",
    val target: StoreRefDto? = null,
    val link: StoreRefDto? = null,
)

@Serializable
data class StoreDocsDto(val docs: List<StoreDocDto> = emptyList())

@Serializable
data class StorePersonaDto(
    val name: String,
    val era: String = "",
    val maxYear: Int = 2026,
    val keyword: String,
    val persona: String,
)

@Serializable
data class StorePersonasDto(val figures: Map<String, StorePersonaDto> = emptyMap())

@Serializable
data class StoreChunkDto(val source: String = "", val text: String = "")

@Serializable
data class StoreContextDto(val chunks: List<StoreChunkDto> = emptyList())

@Serializable
data class StorePutDto(val warning: String? = null, val error: String? = null)

@Serializable
data class TalkLine(val mine: Boolean, val text: String, val system: Boolean? = null)

@Serializable
data class TalkBody(
    val figureId: String,
    val siteId: String? = null,
    val lines: List<TalkLine>,
    val boundary: Boolean? = null,
)

@Serializable
data class SourceDto(val id: String? = null, val name: String = "", val quote: String = "")

@Serializable
data class TalkResponse(
    val text: String = "",
    val sources: List<SourceDto> = emptyList(),
    val retrieved: List<String> = emptyList(),
    val error: String? = null,
)

@Serializable
data class TtsBody(val figureId: String, val text: String)

@Serializable
data class VisionCandidate(val id: String, val name: String, val kind: String, val city: String? = null)

@Serializable
data class VisionBody(val image: String, val candidates: List<VisionCandidate>)

@Serializable
data class VisionResponse(
    val matchedId: String? = null,
    val name: String = "",
    val kind: String = "",
    val era: String = "",
    val description: String = "",
    val confidence: Int = 0,
    val isHeritage: Boolean = false,
    val category: String = "relic",
)

@Serializable
data class ExternalBody(val figureId: String, val question: String, val figureName: String? = null)

@Serializable
data class ExternalResponse(
    val answer: String = "",
    @SerialName("referenced_data") val referencedData: List<String> = emptyList(),
)
