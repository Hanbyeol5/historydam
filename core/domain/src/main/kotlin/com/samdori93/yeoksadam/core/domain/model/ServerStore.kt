package com.samdori93.yeoksadam.core.domain.model

/**
 * 서버 저장소 (역사담 Worker /store · /model · /doc, arHeritage docs/SERVER-STORAGE.md)
 * — 3D 인물 모델(.glb) · RAG 자료(.md) · 외부 RAG(백엔드) 인물 설정.
 */

/** 서버에서 내려받아 폰에 저장한 3D 인물 모델 */
data class FigureModel(
    val figureId: String,
    val name: String,
    /** 폰에 저장된 glb 파일 절대 경로 */
    val filePath: String,
    /** 실제 키(m) */
    val heightM: Float,
)

/** 업로드 대상 종류 */
enum class StoreKind(val ext: String, val label: String) {
    MODEL("glb", "3D 모델"),
    DOC("md", "RAG 자료"),
}

/** 서버 목록의 파일 하나 */
data class StoredFile(
    val name: String,
    /** 이름 비교 열쇠 (NFC + 공백 제거) */
    val key: String,
    val size: Long,
    val sha256: String,
    val uploadedAt: String,
    /** 연결된 인물·유적 이름 (없으면 null — 목록에 없는 이름) */
    val linkedName: String?,
)

/** 폴더 파일과 서버 상태 비교 */
enum class SyncState(val label: String) {
    NEW("새 파일"),
    CHANGED("바뀜"),
    SAME("서버와 같음"),
}

/** 지정한 폰 폴더의 파일 하나 */
data class LocalStoreFile(
    val uri: String,
    /** 확장자를 뺀 이름 (예: 이순신) */
    val name: String,
    val size: Long,
    val sha256: String,
    val state: SyncState,
    /** 인물·유적 목록에 있는 이름인지 (3D 는 인물 이름만) */
    val known: Boolean,
)

data class UploadResult(val name: String, val warning: String? = null)

/** 외부 RAG(백엔드 RAG) 인물 설정 — RAG 자료 편집기의 「외부 RAG 인물 설정」 탭에서 편집 */
data class BackendPersona(
    val name: String,
    val era: String,
    val maxYear: Int,
    val keyword: String,
    val persona: String,
)

/** 서버에 올린 RAG 자료 중 인물에 연결된 조각 (백엔드 RAG 참고 자료) */
data class StoreContext(val source: String, val text: String)
