package com.example.typingarticle.feature.word

import com.example.typingarticle.core.model.WordStats
import com.example.typingarticle.feature.practice.TokenRender

/**
 * 单词默写界面契约状态（前端只消费此模型）
 *
 * 遵循 Section 13.3 规范：
 * - 词库元信息（名称 / 题号 / 总题数）
 * - 提示区：释义、音标、显示开关、听写模式标记（听写模式下只放发音不显示释义）
 * - 当前词渲染分段：TokenRender（已正确串 / 错误串 / 剩余串 / 是否完成 / 是否当前）
 * - 实时统计：对 / 错 / 进度 / 错词数
 */
data class WordUiState(
    val bookId: String = "",
    val bookName: String = "",
    val currentIdx: Int = 0,
    val totalWords: Int = 0,
    val word: String = "",
    val phonetic: String? = null,
    val trans: String? = null,
    val showMeaning: Boolean = true,
    val showPhonetic: Boolean = true,
    val isListeningMode: Boolean = false,
    /** 默写：开启则隐藏单词（凭记忆拼），关闭则显示单词（跟打） */
    val dictation: Boolean = false,
    val tokenRender: TokenRender? = null,
    val isEnd: Boolean = false,
    val stats: WordStats = WordStats(),
    val isWrongOnlyMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
