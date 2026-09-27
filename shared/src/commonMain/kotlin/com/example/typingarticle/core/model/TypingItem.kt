package com.example.typingarticle.core.model

/**
 * 引擎执行单元（一题抽象）
 *
 * - 文章跟打中：一个 Item 对应一句话中的所有 Token 序列
 * - 单词默写中：一个 Item 对应单个英文单词 Token（且 nextSpace=false）
 */
data class TypingItem(
    val id: String,
    val title: String? = null,
    val text: String,
    val translate: String? = null,
    val tokens: List<Token>,
    val audioSrc: String? = null,
    val lrcStart: Double? = null,
    val lrcEnd: Double? = null,
    val phonetic: String? = null,
    val extraData: Map<String, String> = emptyMap()
)

/**
 * 题源抽象接口（文章/词库）
 */
interface ItemSource {
    val totalItems: Int
    fun getItem(index: Int): TypingItem?
    fun hasNext(currentIndex: Int): Boolean = currentIndex + 1 < totalItems
    fun hasPrev(currentIndex: Int): Boolean = currentIndex > 0
}
