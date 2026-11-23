package com.samdori93.yeoksadam.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.designsystem.component.MonoMedallion
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.domain.model.Quiz
import com.samdori93.yeoksadam.core.domain.model.QuizRecord
import com.samdori93.yeoksadam.core.domain.model.UserAccount
import com.samdori93.yeoksadam.core.domain.model.UserStats
import com.samdori93.yeoksadam.core.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 내 계정 — 회원가입·로그인, 서버에 저장된 기록 요약, 내 질문 퀴즈. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repo: AccountRepository,
) : ViewModel() {
    val account: StateFlow<UserAccount?> = repo.account

    private val _stats = MutableStateFlow<UserStats?>(null)
    val stats: StateFlow<UserStats?> = _stats.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 퀴즈
    private val _quiz = MutableStateFlow<Quiz?>(null)
    val quiz: StateFlow<Quiz?> = _quiz.asStateFlow()
    private val _history = MutableStateFlow<List<QuizRecord>>(emptyList())
    val history: StateFlow<List<QuizRecord>> = _history.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _stats.value = repo.stats() }
    }

    fun auth(signup: Boolean, id: String, password: String, name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            val r = if (signup) repo.signup(id, password, name) else repo.login(id, password)
            _busy.value = false
            r.onSuccess {
                onDone()
                refresh()
            }.onFailure { _error.value = it.message }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _stats.value = null
            _quiz.value = null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun loadQuiz() {
        viewModelScope.launch {
            _error.value = null
            repo.currentQuiz().onSuccess { (q, h) ->
                _quiz.value = q
                _history.value = h
            }.onFailure { _error.value = it.message }
        }
    }

    fun newQuiz() {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            repo.newQuiz(5).onSuccess { _quiz.value = it }.onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun answer(index: Int, choice: Int) {
        viewModelScope.launch {
            repo.answer(index, choice).onSuccess {
                _quiz.value = it
                if (it.finished) {
                    refresh()
                    loadQuiz()
                }
            }.onFailure { _error.value = it.message }
        }
    }
}

@Composable
fun AccountCard(discoveryCount: Int, figureCount: Int, modifier: Modifier = Modifier, viewModel: AccountViewModel = hiltViewModel()) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var showAuth by rememberSaveable { mutableStateOf(false) }
    var showQuiz by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(account) { if (account != null) viewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(DancheongColors.HanjiCard, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoMedallion(text = account?.name?.take(1) ?: "客", size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(account?.name ?: "손님", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DancheongColors.Meok)
                Text(account?.let { "@${it.id} · 역사 탐험가" } ?: "로그인하면 대화가 서버에 저장돼요", fontSize = 12.sp, color = DancheongColors.MeokSoft)
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Star, null, tint = DancheongColors.Hwangto, modifier = Modifier.size(13.dp))
                    Text("발견 $discoveryCount · 만난 인물 ${figureCount}명", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DancheongColors.CheongnokDeep)
                }
            }
        }
        if (account == null) {
            Text(
                "회원가입·로그인하면 인물과 나눈 질문과 답이 내 계정에 저장되고, RAG 자료에 없던 답은 내 자료로 쌓여 다음 대화에 쓰여요. 내가 물었던 내용으로 퀴즈도 풀 수 있어요.",
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = DancheongColors.MeokSoft,
            )
            PrimaryButton("로그인 · 회원가입") { showAuth = true }
        } else {
            stats?.let { s ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    StatBox("저장된 질문", "${s.questions}", Modifier.weight(1f))
                    StatBox("대화한 인물", "${s.figures}", Modifier.weight(1f))
                    StatBox("내 RAG 자료", "${s.learned}", Modifier.weight(1f))
                    StatBox("퀴즈 최고", if (s.quizzes > 0) "${s.bestScore}점" else "-", Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("📝 내 질문 퀴즈", Modifier.weight(1f)) { showQuiz = true }
                OutlineButton("로그아웃") { viewModel.logout() }
            }
        }
    }

    if (showAuth) AuthDialog(viewModel, onClose = { showAuth = false })
    if (showQuiz) QuizDialog(viewModel, onClose = { showQuiz = false })
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(DancheongColors.Hanji, RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Jujak)
        Text(label, fontSize = 10.5.sp, color = DancheongColors.MeokSoft)
    }
}

@Composable
private fun PrimaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(if (enabled) DancheongColors.Jujak else DancheongColors.MeokSoft.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        color = DancheongColors.Meok,
        modifier = Modifier
            .border(1.dp, DancheongColors.Meok.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    )
}

@Composable
private fun FullDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DancheongColors.Hanji)
                .statusBarsPadding()
                .imePadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✕", fontSize = 18.sp, color = DancheongColors.Meok, modifier = Modifier.clickable(onClick = onClose).padding(4.dp))
                Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Meok)
                Spacer(Modifier.width(26.dp))
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun AuthDialog(viewModel: AccountViewModel, onClose: () -> Unit) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var signup by rememberSaveable { mutableStateOf(false) }
    var id by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.clearError() }

    FullDialog(if (signup) "회원가입" else "로그인", onClose) {
        Row(Modifier.background(DancheongColors.HanjiDim, RoundedCornerShape(12.dp)).padding(4.dp)) {
            listOf(false to "로그인", true to "회원가입").forEach { (v, label) ->
                Text(
                    label,
                    color = if (signup == v) DancheongColors.Jujak else DancheongColors.MeokSoft,
                    fontWeight = if (signup == v) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(if (signup == v) DancheongColors.HanjiCard else Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable {
                            signup = v
                            viewModel.clearError()
                        }
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                )
            }
        }
        InputField("아이디", id, "영문 소문자·숫자·밑줄 3~20자") { id = it }
        InputField("비밀번호", password, "6자 이상", password = true) { password = it }
        if (signup) InputField("이름", name, "앱에 보일 이름 (예: 희수)") { name = it }
        error?.let { Text(it, fontSize = 12.5.sp, color = DancheongColors.Jujak) }
        PrimaryButton(if (busy) "잠시만요…" else if (signup) "가입하고 시작하기" else "로그인", enabled = !busy && id.isNotBlank() && password.isNotBlank() && (!signup || name.isNotBlank())) {
            viewModel.auth(signup, id, password, name, onDone = onClose)
        }
        Text(
            "계정은 역사담 서버에 사용자별 파일로 저장돼요. 비밀번호는 해시로만 저장하고, 폰에는 로그인 토큰만 남겨요.",
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = DancheongColors.MeokSoft,
        )
    }
}

@Composable
private fun QuizDialog(viewModel: AccountViewModel, onClose: () -> Unit) {
    val quiz by viewModel.quiz.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadQuiz() }

    FullDialog("내 질문 퀴즈", onClose) {
        Text(
            "인물과 나눈 대화에서 내가 물었던 내용으로 4지선다 문제를 만들어요. 보기를 누르면 바로 채점하고 해설을 보여 줘요.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = DancheongColors.MeokSoft,
        )
        val q = quiz
        if (q == null || q.finished) {
            q?.let { Text("지난 퀴즈 ${it.score} / ${it.items.size} 정답", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Jujak) }
            PrimaryButton(if (busy) "퀴즈를 만드는 중… (10~20초)" else "새 퀴즈 만들기 (5문제)", enabled = !busy) { viewModel.newQuiz() }
        }
        error?.let { Text(it, fontSize = 12.5.sp, color = DancheongColors.Jujak) }
        q?.let { cur ->
            Text("${cur.answered} / ${cur.items.size} 문제 · 맞힌 문제 ${cur.score}", fontSize = 12.sp, color = DancheongColors.CheongnokDeep, fontWeight = FontWeight.SemiBold)
            cur.items.forEachIndexed { i, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DancheongColors.HanjiCard, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${i + 1}. ${item.question}", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DancheongColors.Meok)
                    if (item.figureName.isNotBlank()) Text("${withGwa(item.figureName)}의 대화에서", fontSize = 11.sp, color = DancheongColors.MeokSoft)
                    item.choices.forEachIndexed { c, choice ->
                        val answered = item.chosen != null
                        val isAnswer = item.answer == c
                        val isChosen = item.chosen == c
                        val bg = when {
                            answered && isAnswer -> DancheongColors.CheongnokDeep.copy(alpha = 0.15f)
                            answered && isChosen -> DancheongColors.Jujak.copy(alpha = 0.12f)
                            else -> DancheongColors.Hanji
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg, RoundedCornerShape(10.dp))
                                .border(1.dp, DancheongColors.Meok.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .clickable(enabled = !answered) { viewModel.answer(i, c) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text("${"①②③④"[c]} $choice", fontSize = 13.5.sp, color = DancheongColors.Meok, modifier = Modifier.weight(1f))
                            if (answered && isAnswer) Text("정답", fontSize = 12.sp, color = DancheongColors.CheongnokDeep, fontWeight = FontWeight.Bold)
                            else if (answered && isChosen) Text("오답", fontSize = 12.sp, color = DancheongColors.Jujak, fontWeight = FontWeight.Bold)
                        }
                    }
                    item.explanation?.let { Text("💡 $it", fontSize = 12.sp, lineHeight = 17.sp, color = DancheongColors.MeokSoft) }
                }
            }
        }
        if (history.isNotEmpty()) {
            Text("지난 기록", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DancheongColors.Meok)
            history.takeLast(5).reversed().forEach { h ->
                Text("${h.at.take(10)} · ${h.score} / ${h.total}", fontSize = 12.sp, color = DancheongColors.MeokSoft)
            }
        }
    }
}

@Composable
private fun InputField(label: String, value: String, placeholder: String, password: Boolean = false, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = DancheongColors.MeokSoft)
        Box(
            Modifier
                .fillMaxWidth()
                .background(DancheongColors.HanjiCard, RoundedCornerShape(12.dp))
                .border(1.dp, DancheongColors.Meok.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = DancheongColors.Meok),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, fontSize = 15.sp, color = DancheongColors.MeokSoft)
                    inner()
                },
            )
        }
    }
}

/** 받침에 맞는 「과/와」 (정조와 · 세종대왕과) */
private fun withGwa(word: String): String {
    val c = word.trimEnd().lastOrNull() ?: return word
    val hasBatchim = c in '가'..'힣' && (c - '가') % 28 != 0
    return word + if (hasBatchim) "과" else "와"
}
