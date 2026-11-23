package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.domain.model.Quiz
import com.samdori93.yeoksadam.core.domain.model.QuizRecord
import com.samdori93.yeoksadam.core.domain.model.StoreContext
import com.samdori93.yeoksadam.core.domain.model.UserAccount
import com.samdori93.yeoksadam.core.domain.model.UserStats
import kotlinx.coroutines.flow.StateFlow

/**
 * 회원가입 · 로그인과 사용자별 서버 저장 (역사담 Worker /auth · /me).
 * 로그인하면 질문·답이 서버의 사용자 파일에 저장되고, RAG 자료 없이 답한 내용은 그 사용자의 RAG 자료가 된다.
 * 실패는 kotlin.Result 의 예외 메시지(한국어)로 돌려준다.
 */
interface AccountRepository {
    /** 로그인한 사용자 (없으면 null) */
    val account: StateFlow<UserAccount?>

    suspend fun signup(id: String, password: String, name: String): kotlin.Result<UserAccount>
    suspend fun login(id: String, password: String): kotlin.Result<UserAccount>
    suspend fun logout()

    suspend fun stats(): UserStats?

    /** 질문·답 하나를 서버에 기록 (로그인했을 때만, 실패해도 대화에는 영향 없음) */
    fun log(figureId: String, figureName: String?, mode: String, question: String, answer: String, grounded: Boolean, died: Int?)

    /** 이 사용자가 이 인물에게 물어 배운 자료 (백엔드 RAG 참고 자료) */
    suspend fun learnedContext(figureId: String): List<StoreContext>

    // ---------- 퀴즈 ----------
    suspend fun currentQuiz(): kotlin.Result<Pair<Quiz?, List<QuizRecord>>>
    suspend fun newQuiz(count: Int = 5): kotlin.Result<Quiz>
    suspend fun answer(index: Int, choice: Int): kotlin.Result<Quiz>
}
