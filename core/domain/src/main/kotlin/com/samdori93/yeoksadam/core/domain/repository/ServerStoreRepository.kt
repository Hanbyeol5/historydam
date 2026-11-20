package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.domain.model.BackendPersona
import com.samdori93.yeoksadam.core.domain.model.FigureModel
import com.samdori93.yeoksadam.core.domain.model.LocalStoreFile
import com.samdori93.yeoksadam.core.domain.model.StoreContext
import com.samdori93.yeoksadam.core.domain.model.StoreKind
import com.samdori93.yeoksadam.core.domain.model.StoredFile
import com.samdori93.yeoksadam.core.domain.model.UploadResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 서버 저장소 — 3D 인물 모델·RAG 자료를 서버에 올리고(관리자), 모든 폰이 서버에서 불러온다.
 * 3D 모델은 파일 이름 = 인물 이름(예: 이순신.glb)으로 찾는다.
 */
interface ServerStoreRepository {
    /** 폰에 받아 둔 3D 모델 (인물 id → 모델). 앱을 다시 켜도 남아 있어 오프라인에서도 쓴다 */
    val models: StateFlow<Map<String, FigureModel>>

    /** 서버 목록에 이 인물의 모델이 있으면 내려받는다 (이미 같은 것이 있으면 그대로) */
    suspend fun syncModel(figureId: String)

    /** 외부 RAG(백엔드) 인물 설정 — 편집기에서 고친 인물만 (실패하면 빈 값) */
    suspend fun personas(): Map<String, BackendPersona>

    /** 서버에 올린 RAG 자료 중 이 인물(과 그 유적)에 연결된 조각 (실패하면 빈 값) */
    suspend fun contextFor(figureId: String): List<StoreContext>

    // ---------- 관리자: 폴더 업로드 ----------
    val uploadKey: Flow<String>
    suspend fun setUploadKey(key: String)

    /** 종류별로 지정한 폰 폴더(문서 트리 URI) */
    fun folder(kind: StoreKind): Flow<String?>
    suspend fun setFolder(kind: StoreKind, treeUri: String)

    /** 서버 목록 */
    suspend fun serverFiles(kind: StoreKind): List<StoredFile>

    /** 지정한 폴더의 파일을 읽어 서버 목록과 비교 */
    suspend fun scanFolder(kind: StoreKind): List<LocalStoreFile>

    /** 파일 하나를 서버에 저장 (3D 는 [heightM] 키 함께) */
    suspend fun upload(kind: StoreKind, file: LocalStoreFile, heightM: Float? = null): UploadResult
}
