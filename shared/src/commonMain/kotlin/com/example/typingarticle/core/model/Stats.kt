package com.example.typingarticle.core.model

/**
 * 文章跟打实时统计
 *
 * @param startedAt 开始时间戳（毫秒）
 * @param spendMs 累计用时（毫秒）
 * @param totalWords 学习词数（仅 Word 且未被忽略的 token）
 * @param wrongCount 错误次数（同词连续错合并为 1 次）
 * @param completedWords 已完成词数
 */
data class PracticeStats(
    val startedAt: Long = System.currentTimeMillis(),
    val spendMs: Long = 0L,
    val totalWords: Int = 0,
    val wrongCount: Int = 0,
    val completedWords: Int = 0
) {
    val accuracy: Float
        get() {
            val total = completedWords + wrongCount
            return if (total > 0) (completedWords.toFloat() / total.toFloat()) * 100f else 100f
        }
}

/**
 * 单词默写实时统计
 *
 * @param totalTested 已出题数
 * @param wrongCount 错词数（去重）
 * @param spendMs 累计用时（毫秒）
 * @param wrongWords 错词列表
 */
data class WordStats(
    val totalTested: Int = 0,
    val wrongCount: Int = 0,
    val spendMs: Long = 0L,
    val wrongWords: List<String> = emptyList()
) {
    val accuracy: Float
        get() {
            return if (totalTested > 0) {
                ((totalTested - wrongCount).coerceAtLeast(0).toFloat() / totalTested.toFloat()) * 100f
            } else 100f
        }
}
