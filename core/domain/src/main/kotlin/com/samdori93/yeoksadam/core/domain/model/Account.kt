package com.samdori93.yeoksadam.core.domain.model

/** 로그인한 사용자 (서버의 users/<아이디>.json) */
data class UserAccount(val id: String, val name: String)

/** 서버에 저장된 이 사용자의 기록 요약 */
data class UserStats(
    val figures: Int = 0,
    val questions: Int = 0,
    /** RAG 자료 없이 LLM 이 답해 이 사용자의 RAG 에 더해진 질문·답 수 */
    val learned: Int = 0,
    val quizzes: Int = 0,
    val bestScore: Int = 0,
)

/** 내가 물었던 내용으로 만든 퀴즈 */
data class Quiz(
    val id: String,
    val items: List<QuizItem>,
    val score: Int,
    val answered: Int,
) {
    val finished: Boolean get() = answered >= items.size && items.isNotEmpty()
}

data class QuizItem(
    val question: String,
    val choices: List<String>,
    val figureName: String,
    /** 답했으면 고른 번호 · 정답 · 해설 */
    val chosen: Int? = null,
    val answer: Int? = null,
    val explanation: String? = null,
) {
    val correct: Boolean? get() = if (chosen == null || answer == null) null else chosen == answer
}

data class QuizRecord(val at: String, val score: Int, val total: Int)
