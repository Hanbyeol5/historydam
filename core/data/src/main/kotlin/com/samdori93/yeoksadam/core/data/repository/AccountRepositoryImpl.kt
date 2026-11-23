package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.datastore.AuthStore
import com.samdori93.yeoksadam.core.domain.model.Quiz
import com.samdori93.yeoksadam.core.domain.model.QuizItem
import com.samdori93.yeoksadam.core.domain.model.QuizRecord
import com.samdori93.yeoksadam.core.domain.model.StoreContext
import com.samdori93.yeoksadam.core.domain.model.UserAccount
import com.samdori93.yeoksadam.core.domain.model.UserStats
import com.samdori93.yeoksadam.core.domain.repository.AccountRepository
import com.samdori93.yeoksadam.core.network.worker.AuthDto
import com.samdori93.yeoksadam.core.network.worker.AuthToken
import com.samdori93.yeoksadam.core.network.worker.LogBody
import com.samdori93.yeoksadam.core.network.worker.LoginBody
import com.samdori93.yeoksadam.core.network.worker.NewQuizBody
import com.samdori93.yeoksadam.core.network.worker.QuizAnswerBody
import com.samdori93.yeoksadam.core.network.worker.QuizDto
import com.samdori93.yeoksadam.core.network.worker.SignupBody
import com.samdori93.yeoksadam.core.network.worker.WorkerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 회원 · 사용자별 서버 저장 (역사담 Worker). 서버는 사용자마다 JSON 파일(users/<아이디>/…)로 저장한다.
 * 토큰은 DataStore 에 두고, 앱이 켜지면 [AuthToken] 에 실어 모든 Worker 요청에 붙인다.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: WorkerApi,
    private val store: AuthStore,
) : AccountRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _account = MutableStateFlow<UserAccount?>(null)
    override val account: StateFlow<UserAccount?> = _account.asStateFlow()

    init {
        scope.launch {
            store.session.first()?.let { s ->
                AuthToken.value = s.token
                _account.value = UserAccount(s.id, s.name)
            }
        }
    }

    private suspend fun <T> call(block: suspend () -> T): kotlin.Result<T> = withContext(Dispatchers.IO) {
        try {
            kotlin.Result.success(block())
        } catch (e: HttpException) {
            if (e.code() == 401 && AuthToken.value != null && !e.isAuthPath()) expire()
            kotlin.Result.failure(IllegalStateException(errorMessage(e)))
        } catch (e: IOException) {
            kotlin.Result.failure(IllegalStateException("네트워크 연결을 확인해 주세요."))
        }
    }

    private fun HttpException.isAuthPath() = response()?.raw()?.request?.url?.encodedPath?.startsWith("/auth/") == true

    private fun errorMessage(e: HttpException): String {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        return Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.get(1) ?: "서버 오류 ${e.code()}"
    }

    /** 세션이 만료됐으면 로그아웃 상태로 */
    private suspend fun expire() {
        AuthToken.value = null
        _account.value = null
        store.clear()
    }

    private suspend fun signedIn(r: AuthDto): UserAccount {
        store.save(AuthStore.Session(r.token, r.user.id, r.user.name))
        AuthToken.value = r.token
        return UserAccount(r.user.id, r.user.name).also { _account.value = it }
    }

    override suspend fun signup(id: String, password: String, name: String) =
        call { signedIn(api.signup(SignupBody(id.trim().lowercase(), password, name.trim()))) }

    override suspend fun login(id: String, password: String) =
        call { signedIn(api.login(LoginBody(id.trim().lowercase(), password))) }

    override suspend fun logout() {
        runCatching { withContext(Dispatchers.IO) { api.logout() } }
        expire()
    }

    override suspend fun stats(): UserStats? {
        if (AuthToken.value == null) return null
        return call { api.me().stats }.getOrNull()?.let { UserStats(it.figures, it.questions, it.learned, it.quizzes, it.bestScore) }
    }

    override fun log(figureId: String, figureName: String?, mode: String, question: String, answer: String, grounded: Boolean, died: Int?) {
        if (AuthToken.value == null) return
        scope.launch {
            call { api.log(LogBody(figureId.removePrefix("fig_"), figureName, mode, question.take(500), answer.take(2000), grounded, died)) }
        }
    }

    override suspend fun learnedContext(figureId: String): List<StoreContext> {
        if (AuthToken.value == null) return emptyList()
        return call { api.meContext(figureId.removePrefix("fig_")).chunks.map { StoreContext(it.source, it.text) } }.getOrDefault(emptyList())
    }

    override suspend fun currentQuiz() = call {
        val r = api.quiz()
        r.current?.toDomain() to r.history.map { QuizRecord(it.at, it.score, it.total) }
    }

    override suspend fun newQuiz(count: Int) = call { api.newQuiz(NewQuizBody(count)).current!!.toDomain() }

    override suspend fun answer(index: Int, choice: Int) = call { api.answerQuiz(QuizAnswerBody(index, choice)).quiz.toDomain() }

    private fun QuizDto.toDomain() = Quiz(
        id = id,
        items = items.map { QuizItem(it.question, it.choices, it.figureName, it.chosen, it.answer, it.explanation) },
        score = score,
        answered = answered,
    )
}
