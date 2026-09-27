package com.example.typingarticle.core.model

/**
 * 打字游标
 *
 * @param sectionIdx 当前段落下标
 * @param sentenceIdx 当前句子下标
 * @param wordIdx 当前词元下标
 * @param charIdx 当前词元内字符位置（k）
 */
data class Cursor(
    val sectionIdx: Int = 0,
    val sentenceIdx: Int = 0,
    val wordIdx: Int = 0,
    val charIdx: Int = 0
)
