package com.samdori93.yeoksadam.feature.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.domain.model.LocalStoreFile
import com.samdori93.yeoksadam.core.domain.model.StoreKind
import com.samdori93.yeoksadam.core.domain.model.SyncState
import com.samdori93.yeoksadam.core.domain.repository.ServerStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 관리자: 서버 저장소 업로드 — 폰의 폴더를 지정하고, 새로 만들거나 바뀐 3D 모델(.glb)·RAG 자료(.md)를 하나씩 서버에 저장한다.
 * 파일 이름 = 인물·유적 이름 (예: 이순신.glb, 수원 화성.md). 다른 폰은 다음에 그 인물을 만날 때 서버에서 받아 온다.
 */
@HiltViewModel
class ServerStoreViewModel @Inject constructor(
    private val repo: ServerStoreRepository,
) : ViewModel() {

    data class Section(
        val folder: String? = null,
        val files: List<LocalStoreFile> = emptyList(),
        val loading: Boolean = false,
        val busy: Set<String> = emptySet(),
    )

    private val _sections = MutableStateFlow(StoreKind.entries.associateWith { Section() })
    val sections: StateFlow<Map<StoreKind, Section>> = _sections.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        SHOWN_KINDS.forEach { kind ->
            viewModelScope.launch {
                repo.folder(kind).distinctUntilChanged().collect { folder ->
                    edit(kind) { it.copy(folder = folder) }
                    if (folder != null) scan(kind)
                }
            }
        }
    }

    private fun edit(kind: StoreKind, f: (Section) -> Section) = _sections.update { it + (kind to f(it.getValue(kind))) }

    fun setFolder(kind: StoreKind, uri: Uri) {
        viewModelScope.launch { repo.setFolder(kind, uri.toString()) }
    }

    fun scan(kind: StoreKind) {
        viewModelScope.launch {
            edit(kind) { it.copy(loading = true) }
            runCatching { repo.scanFolder(kind) }
                .onSuccess { files -> edit(kind) { it.copy(files = files) } }
                .onFailure { _message.value = "${kind.label} 폴더를 읽지 못했어요: ${it.message ?: it::class.simpleName}" }
            edit(kind) { it.copy(loading = false) }
        }
    }

    fun upload(kind: StoreKind, files: List<LocalStoreFile>, heightM: Float?) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val notes = mutableListOf<String>()
            var ok = 0
            for (file in files) {
                edit(kind) { it.copy(busy = it.busy + file.uri) }
                runCatching { repo.upload(kind, file, heightM) }
                    .onSuccess { r ->
                        ok++
                        r.warning?.let { w -> notes += "${r.name}: $w" }
                    }
                    .onFailure { notes += "${file.name}: ${it.message ?: "실패"}" }
                edit(kind) { it.copy(busy = it.busy - file.uri) }
            }
            _message.value = buildString {
                append("${kind.label} ${ok}개를 서버에 저장했어요.")
                if (kind == StoreKind.DOC && ok > 0) append(" 1분 안에 인물 대화에 쓰여요.")
                notes.forEach { append("\n· ").append(it) }
            }
            scan(kind)
        }
    }

    fun dismiss() {
        _message.value = null
    }
}

/** 폰에서 올리는 것은 3D 모델만 — RAG 자료는 웹의 RAG 자료 편집기에서 */
private val SHOWN_KINDS = listOf(StoreKind.MODEL)

@Composable
fun ServerStoreCard(modifier: Modifier = Modifier, viewModel: ServerStoreViewModel = hiltViewModel()) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var height by rememberSaveable { mutableStateOf("1.65") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(DancheongColors.HanjiCard, RoundedCornerShape(16.dp))
            .border(1.dp, DancheongColors.Meok.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🗄 서버 저장소 (관리자) · 3D 모델", fontSize = 13.sp, color = DancheongColors.MeokSoft)
            Spacer(Modifier.weight(1f))
            Text(if (open) "▲" else "▼", fontSize = 11.sp, color = DancheongColors.MeokSoft)
        }
        if (!open) return@Column

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 14.dp)) {
            Text(
                "폰 폴더의 3D 모델을 서버에 올려요. 파일 이름은 인물 이름 그대로 (예: 이순신.glb). " +
                    "다른 폰은 다음에 그 인물을 만날 때 서버에서 받아 와요. RAG 자료는 웹의 RAG 자료 편집기에서 고쳐요.",
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = DancheongColors.MeokSoft,
            )
            SHOWN_KINDS.forEach { kind ->
                FolderSection(
                    kind = kind,
                    section = sections.getValue(kind),
                    height = height,
                    onHeight = { height = it },
                    onFolder = { viewModel.setFolder(kind, it) },
                    onRefresh = { viewModel.scan(kind) },
                    onUpload = { files -> viewModel.upload(kind, files, height.toFloatOrNull()?.takeIf { kind == StoreKind.MODEL }) },
                )
            }
            message?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DancheongColors.Hanji, RoundedCornerShape(10.dp))
                        .clickable { viewModel.dismiss() }
                        .padding(10.dp),
                ) {
                    Text(msg, fontSize = 12.sp, lineHeight = 17.sp, color = DancheongColors.Meok, modifier = Modifier.weight(1f))
                    Text("✕", fontSize = 12.sp, color = DancheongColors.MeokSoft)
                }
            }
        }
    }
}

@Composable
private fun FolderSection(
    kind: StoreKind,
    section: ServerStoreViewModel.Section,
    height: String,
    onHeight: (String) -> Unit,
    onFolder: (Uri) -> Unit,
    onRefresh: () -> Unit,
    onUpload: (List<LocalStoreFile>) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(onFolder) }
    val pending = section.files.filter { it.state != SyncState.SAME }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DancheongColors.Hanji, RoundedCornerShape(12.dp))
            .border(1.dp, DancheongColors.Meok.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${kind.label} (.${kind.ext})", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DancheongColors.Meok)
            Spacer(Modifier.weight(1f))
            if (section.folder != null) Chip("새로고침", onClick = onRefresh)
            Spacer(Modifier.width(6.dp))
            Chip(if (section.folder == null) "폴더 지정" else "폴더 바꾸기", onClick = { picker.launch(null) })
        }
        Text(
            section.folder?.let { "폴더: ${folderLabel(it)}" } ?: "폴더를 지정하면 그 안의 .${kind.ext} 파일을 서버와 비교해요.",
            fontSize = 11.5.sp,
            color = DancheongColors.MeokSoft,
        )
        if (kind == StoreKind.MODEL && section.folder != null) {
            Field(label = "인물 키(m)", value = height, placeholder = "1.65", number = true, onChange = onHeight)
        }
        when {
            section.loading -> Text("파일을 읽는 중…", fontSize = 12.sp, color = DancheongColors.MeokSoft)
            section.folder != null && section.files.isEmpty() -> Text("이 폴더에 .${kind.ext} 파일이 없어요.", fontSize = 12.sp, color = DancheongColors.MeokSoft)
        }
        section.files.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("${f.name}.${kind.ext}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DancheongColors.Meok)
                    Text(
                        buildString {
                            append(sizeLabel(f.size)).append(" · ").append(f.state.label)
                            if (!f.known) append(if (kind == StoreKind.MODEL) " · ⚠ 인물 목록에 없는 이름" else " · ⚠ 인물·유적 목록에 없는 이름(독립 자료)")
                            if (kind == StoreKind.MODEL && f.size > 20L * 1024 * 1024) append(" · 20MB 초과, 압축 권장")
                        },
                        fontSize = 11.sp,
                        color = when {
                            !f.known -> DancheongColors.Jujak
                            f.state == SyncState.SAME -> DancheongColors.CheongnokDeep
                            else -> DancheongColors.MeokSoft
                        },
                    )
                }
                when {
                    f.uri in section.busy -> Text("저장 중…", fontSize = 12.sp, color = DancheongColors.MeokSoft)
                    f.state != SyncState.SAME -> Chip("서버에 저장", primary = true) { onUpload(listOf(f)) }
                }
            }
        }
        if (pending.size > 1) {
            Chip("바뀐 것 모두 저장 (${pending.size})", primary = true, enabled = section.busy.isEmpty()) { onUpload(pending) }
        }
    }
}

@Composable
private fun Chip(label: String, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val color = if (primary) DancheongColors.Jujak else DancheongColors.Meok
    Text(
        label,
        fontSize = 12.sp,
        color = if (!enabled) DancheongColors.MeokSoft.copy(alpha = 0.5f) else if (primary) Color.White else color,
        modifier = Modifier
            .background(if (primary && enabled) color else Color.Transparent, RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun Field(label: String, value: String, placeholder: String, password: Boolean = false, number: Boolean = false, onChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(DancheongColors.HanjiCard, RoundedCornerShape(10.dp))
            .border(1.dp, DancheongColors.Meok.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Text(label, fontSize = 12.sp, color = DancheongColors.MeokSoft)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = DancheongColors.Meok),
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else if (password) KeyboardType.Password else KeyboardType.Text),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, fontSize = 13.sp, color = DancheongColors.MeokSoft)
                inner()
            },
        )
    }
}

/** content://…/tree/primary%3ADownload%2F역사담3D → 내부 저장소/Download/역사담3D */
private fun folderLabel(treeUri: String): String {
    val doc = Uri.decode(Uri.parse(treeUri).lastPathSegment ?: treeUri)
    return doc.replaceFirst("primary:", "내부 저장소/")
}

private fun sizeLabel(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "${bytes / 1024}KB"
    else -> "${bytes}B"
}
