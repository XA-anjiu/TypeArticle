package com.example.typingarticle.core.model

/**
 * 打字引擎快照状态
 *
 * @param cursor 当前游标位置 (s, t, w, k)
 * @param isSpace 是否处于等待空格态
 * @param isEnd 是否全篇打完
 * @param input 当前词已输入正确的字符串
 * @param rawInput 当前词的原始输入缓冲（含打错的字符），供前端渲染“打错标红”
 * @param currentToken 当前正在打的词元
 * @param currentSentence 当前所在句子
 * @param stats 实时打字统计
 */
data class EngineSnapshot(
    val cursor: Cursor = Cursor(),
    val isSpace: Boolean = false,
    val isEnd: Boolean = false,
    val input: String = "",
    val rawInput: String = "",
    val currentToken: Token? = null,
    val currentSentence: Sentence? = null,
    val stats: PracticeStats = PracticeStats()
)

/**
 * 打字引擎触发的事件流
 */
sealed class EngineEvent {
    data class CorrectChar(val char: Char) : EngineEvent()
    data class WrongChar(val inputChar: Char, val expectedChar: Char) : EngineEvent()
    data class NextWord(val token: Token) : EngineEvent()
    data class NextSentence(val sentenceIdx: Int, val sectionIdx: Int) : EngineEvent()
    data class Complete(val stats: PracticeStats) : EngineEvent()
    data object SpaceWait : EngineEvent()
    data object Backspace : EngineEvent()
}
