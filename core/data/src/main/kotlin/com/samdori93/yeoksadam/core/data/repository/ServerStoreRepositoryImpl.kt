package com.samdori93.yeoksadam.core.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.common.result.getOrNull
import com.samdori93.yeoksadam.core.datastore.AdminStore
import com.samdori93.yeoksadam.core.domain.model.BackendPersona
import com.samdori93.yeoksadam.core.domain.model.FigureModel
import com.samdori93.yeoksadam.core.domain.model.LocalStoreFile
import com.samdori93.yeoksadam.core.domain.model.StoreContext
import com.samdori93.yeoksadam.core.domain.model.StoreKind
import com.samdori93.yeoksadam.core.domain.model.StoredFile
import com.samdori93.yeoksadam.core.domain.model.SyncState
import com.samdori93.yeoksadam.core.domain.model.UploadResult
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.domain.repository.ServerStoreRepository
import com.samdori93.yeoksadam.core.network.worker.StoreModelDto
import com.samdori93.yeoksadam.core.network.worker.WorkerApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import retrofit2.HttpException
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 저장소 (역사담 Worker) 구현.
 *  - 3D 모델: 서버 목록(models.json)에서 인물 id 로 찾아 filesDir/models/<id>.glb 로 받아 둔다 (sha256 이 같으면 다시 받지 않음)
 *  - 관리자 업로드: 저장소 접근 프레임워크로 지정한 폴더를 읽어 서버 목록과 sha256 으로 비교하고, 파일 하나씩 PUT
 */
@Singleton
class ServerStoreRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: WorkerApi,
    private val admin: AdminStore,
    private val catalog: CatalogRepository,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : ServerStoreRepository {

    @Serializable
    private data class LocalModelMeta(val figureId: String, val name: String, val sha256: String, val heightM: Float)

    private val modelDir = File(context.filesDir, "models").apply { mkdirs() }
    private val _models = MutableStateFlow(loadLocalModels())
    override val models: StateFlow<Map<String, FigureModel>> = _models.asStateFlow()

    private val listLock = Mutex()
    private var modelList: Pair<Long, List<StoreModelDto>>? = null
    private val downloadLock = Mutex()
    private var personaCache: Pair<Long, Map<String, BackendPersona>>? = null

    private fun loadLocalModels(): Map<String, FigureModel> =
        modelDir.listFiles { f -> f.name.endsWith(".json") }.orEmpty().mapNotNull { metaFile ->
            runCatching {
                val m = json.decodeFromString<LocalModelMeta>(metaFile.readText())
                val glb = File(modelDir, "${m.figureId}.glb")
                if (glb.exists()) m.figureId to FigureModel(m.figureId, m.name, glb.absolutePath, m.heightM) else null
            }.getOrNull()
        }.toMap()

    private suspend fun remoteModels(force: Boolean = false): List<StoreModelDto> = listLock.withLock {
        val cached = modelList
        if (!force && cached != null && System.currentTimeMillis() - cached.first < LIST_TTL_MS) return cached.second
        val list = api.storeModels().models
        modelList = System.currentTimeMillis() to list
        list
    }

    override suspend fun syncModel(figureId: String) {
        withContext(dispatchers.io) {
            val id = figureId.removePrefix("fig_")
            val remote = runCatching { remoteModels() }.getOrNull() ?: return@withContext // 오프라인: 받아 둔 것으로
            val entry = remote.firstOrNull { it.id == id }
            if (entry == null) {
                // 서버에서 지운 모델은 폰에서도 지운다
                if (_models.value.containsKey(id)) removeLocal(id)
                return@withContext
            }
            val local = _models.value[id]
            val meta = File(modelDir, "$id.json")
            val localSha = runCatching { json.decodeFromString<LocalModelMeta>(meta.readText()).sha256 }.getOrNull()
            if (local != null && localSha == entry.sha256) {
                if (local.heightM != entry.heightM) _models.update { it + (id to local.copy(heightM = entry.heightM)) }
                return@withContext
            }
            downloadLock.withLock {
                val tmp = File(modelDir, "$id.glb.part")
                runCatching {
                    api.model("${entry.name}.glb").byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    val glb = File(modelDir, "$id.glb")
                    if (glb.exists()) glb.delete()
                    check(tmp.renameTo(glb))
                    meta.writeText(json.encodeToString(LocalModelMeta.serializer(), LocalModelMeta(id, entry.name, entry.sha256, entry.heightM)))
                    _models.update { it + (id to FigureModel(id, entry.name, glb.absolutePath, entry.heightM)) }
                }.onFailure { tmp.delete() }
            }
        }
    }

    private fun removeLocal(id: String) {
        File(modelDir, "$id.glb").delete()
        File(modelDir, "$id.json").delete()
        _models.update { it - id }
    }

    override suspend fun personas(): Map<String, BackendPersona> = withContext(dispatchers.io) {
        personaCache?.takeIf { System.currentTimeMillis() - it.first < PERSONA_TTL_MS }?.let { return@withContext it.second }
        val fresh = runCatching {
            api.storePersonas().figures.mapValues { (_, p) -> BackendPersona(p.name, p.era, p.maxYear, p.keyword, p.persona) }
        }.getOrNull()
        if (fresh != null) personaCache = System.currentTimeMillis() to fresh
        fresh ?: personaCache?.second.orEmpty()
    }

    override suspend fun contextFor(figureId: String): List<StoreContext> = withContext(dispatchers.io) {
        runCatching { api.storeContext(figureId.removePrefix("fig_")).chunks.map { StoreContext(it.source, it.text) } }.getOrDefault(emptyList())
    }

    // ---------- 관리자 ----------
    override fun folder(kind: StoreKind): Flow<String?> = admin.folder(kind.name)

    override suspend fun setFolder(kind: StoreKind, treeUri: String) {
        // 앱을 다시 켜도 같은 폴더를 읽을 수 있게 권한을 영구 보관
        runCatching {
            context.contentResolver.takePersistableUriPermission(Uri.parse(treeUri), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        admin.setFolder(kind.name, treeUri)
    }

    override suspend fun serverFiles(kind: StoreKind): List<StoredFile> = withContext(dispatchers.io) {
        when (kind) {
            StoreKind.MODEL -> remoteModels(force = true).map { m ->
                StoredFile(m.name, m.key, m.size, m.sha256, m.uploadedAt, linkedName = m.id?.let { m.name })
            }
            StoreKind.DOC -> api.storeDocs().docs.map { d ->
                StoredFile(d.name, d.key, d.size, d.sha256, d.uploadedAt, linkedName = (d.target ?: d.link)?.name)
            }
        }
    }

    override suspend fun scanFolder(kind: StoreKind): List<LocalStoreFile> = withContext(dispatchers.io) {
        val tree = admin.folder(kind.name).first() ?: return@withContext emptyList()
        val server = serverFiles(kind).associateBy { it.key }
        val names = knownNames(kind)
        listFolder(Uri.parse(tree))
            .filter { it.second.endsWith(".${kind.ext}", ignoreCase = true) }
            .map { (uri, fileName, size) ->
                val name = fileName.substring(0, fileName.length - kind.ext.length - 1)
                val sha = sha256(uri)
                val remote = server[nameKey(name)]
                LocalStoreFile(
                    uri = uri.toString(),
                    name = Normalizer.normalize(name, Normalizer.Form.NFC),
                    size = size,
                    sha256 = sha,
                    state = when {
                        remote == null -> SyncState.NEW
                        remote.sha256.equals(sha, ignoreCase = true) -> SyncState.SAME
                        else -> SyncState.CHANGED
                    },
                    known = nameKey(name) in names,
                )
            }
            .sortedWith(compareBy({ it.state == SyncState.SAME }, { it.name }))
    }

    override suspend fun upload(kind: StoreKind, file: LocalStoreFile, heightM: Float?): UploadResult = withContext(dispatchers.io) {
        val uri = Uri.parse(file.uri)
        val body = object : RequestBody() {
            override fun contentType() = (if (kind == StoreKind.MODEL) "model/gltf-binary" else "text/markdown; charset=utf-8").toMediaType()
            override fun contentLength() = file.size
            override fun writeTo(sink: BufferedSink) {
                context.contentResolver.openInputStream(uri)!!.source().use { sink.writeAll(it) }
            }
        }
        val fileName = "${file.name}.${kind.ext}"
        val res = try {
            when (kind) {
                StoreKind.MODEL -> api.putModel(fileName, file.sha256, heightM?.toString(), body)
                StoreKind.DOC -> api.putDoc(fileName, body)
            }
        } catch (e: HttpException) {
            throw IllegalStateException(errorMessage(e))
        }
        if (kind == StoreKind.MODEL) modelList = null
        UploadResult(file.name, res.warning)
    }

    private fun errorMessage(e: HttpException): String {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        val msg = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.get(1)
        return msg ?: when (e.code()) {
            413 -> "파일이 너무 큽니다."
            else -> "서버 오류 ${e.code()}"
        }
    }

    /** 인물(3D·RAG) · 유적(RAG) 이름 열쇠 */
    private suspend fun knownNames(kind: StoreKind): Set<String> {
        val figures = catalog.figures().getOrNull().orEmpty().map { nameKey(it.name) }
        if (kind == StoreKind.MODEL) return figures.toSet()
        val sites = catalog.sites().getOrNull().orEmpty().map { nameKey(it.name) }
        return (figures + sites).toSet()
    }

    /** 문서 트리의 파일들 (uri, 파일 이름, 크기) — 하위 폴더는 보지 않는다 */
    private fun listFolder(tree: Uri): List<Triple<Uri, String, Long>> {
        val docId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val out = mutableListOf<Triple<Uri, String, Long>>()
        context.contentResolver.query(children, cols, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(3) == DocumentsContract.Document.MIME_TYPE_DIR) continue
                out += Triple(DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)), c.getString(1) ?: continue, c.getLong(2))
            }
        }
        return out
    }

    private fun sha256(uri: Uri): String {
        val md = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val LIST_TTL_MS = 10 * 60_000L
        const val PERSONA_TTL_MS = 5 * 60_000L

        /** 서버와 같은 이름 열쇠: NFC(자모 분리 형태 통일) + 공백 제거 */
        fun nameKey(name: String): String =
            Normalizer.normalize(name, Normalizer.Form.NFC).replace(Regex("\\.(glb|md)$", RegexOption.IGNORE_CASE), "").replace(Regex("\\s+"), "")
    }
}
