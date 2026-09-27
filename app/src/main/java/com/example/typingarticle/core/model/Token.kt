package com.example.typingarticle.core.model

/**
 * 最小切分词元
 *
 * @param idx 句子内的词元序号
 * @param text 词元原文
 * @param type 词元类型：Number / Word / Symbol
 * @param nextSpace 本词元结束位置到下一词元开始位置之间是否含空白。
 *                  若为 true，用户打完该词后必须敲击空格键方可进入下一词。
 */
data class Token(
    val idx: Int,
    val text: String,
    val type: TokenType,
    val nextSpace: Boolean
)
