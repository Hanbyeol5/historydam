package com.samdori93.yeoksadam.core.data.rag

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.sqrt

/**
 * 질문 의도 분류 — historydam backend 의 vectorizer.pkl(TfidfVectorizer 기본값) + intent_model.pkl(LogisticRegression).
 * 모델 값은 assets/intent_model.json (scripts/export_intent_model.py 로 뽑음). 계산은 scikit-learn 과 같다:
 *   소문자 → 토큰(글자·숫자·_ 가 2자 이상 이어진 것) → 단어 수 × idf → L2 정규화 → coef·x + intercept > 0 이면 역사 질의.
 */
class IntentClassifier(private val model: Model) {

    @Serializable
    data class Model(
        val vocabulary: Map<String, Int>,
        val idf: List<Double>,
        val coef: List<Double>,
        val intercept: Double,
    )

    /** LogisticRegression.decision_function */
    fun score(text: String): Double {
        val counts = HashMap<Int, Int>()
        tokens(text.lowercase()).forEach { t -> model.vocabulary[t]?.let { counts[it] = (counts[it] ?: 0) + 1 } }
        val weights = counts.mapValues { (i, c) -> c * model.idf[i] }
        val norm = sqrt(weights.values.sumOf { it * it })
        if (norm == 0.0) return model.intercept
        return weights.entries.sumOf { (i, w) -> w / norm * model.coef[i] } + model.intercept
    }

    /** true: 역사 사료 질의(라벨 1, 공공 API 검색) · false: 단순 인사/일반 대화(라벨 0) */
    fun isHistoryQuestion(text: String): Boolean = score(text) > 0

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String) = IntentClassifier(json.decodeFromString(Model.serializer(), text))

        /** scikit-learn 기본 token_pattern `(?u)\b\w\w+\b` — 단어 글자가 2자 이상 이어진 덩어리 */
        internal fun tokens(text: String): List<String> {
            val out = ArrayList<String>()
            val sb = StringBuilder()
            fun flush() {
                if (sb.length >= 2) out += sb.toString()
                sb.clear()
            }
            text.forEach { c -> if (c.isLetterOrDigit() || c == '_') sb.append(c) else flush() }
            flush()
            return out
        }
    }
}
