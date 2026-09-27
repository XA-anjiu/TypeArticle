package com.example.typingarticle.core.model

/**
 * 句子模型
 *
 * @param idx 段落或文章内的句子下标
 * @param text 句子原文
 * @param translate 句子翻译（可选）
 * @param lrcStart 音频起始点（秒，可选）
 * @param lrcEnd 音频结束点（秒，可选）
 * @param tokens 切分后的词元列表
 */
data class Sentence(
    val idx: Int,
    val text: String,
    val translate: String? = null,
    val lrcStart: Double? = null,
    val lrcEnd: Double? = null,
    val tokens: List<Token> = emptyList()
)
