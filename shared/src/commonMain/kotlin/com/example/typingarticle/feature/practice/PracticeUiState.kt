package com.example.typingarticle.feature.practice

import androidx.compose.runtime.Immutable
import com.example.typingarticle.core.model.Cursor
import com.example.typingarticle.core.model.PracticeStats
import com.example.typingarticle.core.model.TokenType

/**
 * 侧栏文章列表项
 */
@Immutable
data class ArticleItemUi(
    val idx: Int,
    val title: String,
    val titleTranslate: String? = null
)

/**
 * 单词 Token 呈现分段模型
 *
 * 遵循 Section 13.2 规范：
 * 提供渲染所需的分段：已正确串 / 错误串 / 剩余串 / 是否完成 / 是否当前 + nextSpace 与 isWaitingSpace
 */
@Immutable
data class TokenRender(
    val idx: Int,
    val text: String,
    val type: TokenType,
    val nextSpace: Boolean,
    val isWaitingSpace: Boolean = false,
    val isCurrent: Boolean = false,
    val isCompleted: Boolean = false,
    val correctPrefix: String = "",
    val typedRaw: String = "",
    val errorChar: String? = null,
    val remainingText: String = text
)

/**
 * 句子呈现模型
 */
@Immutable
data class SentenceRender(
    val idx: Int,
    val text: String,
    val translate: String? = null,
    val isCurrent: Boolean = false,
    val isCompleted: Boolean = false,
    val tokens: List<TokenRender> = emptyList()
)

/**
 * 段落呈现模型
 */
@Immutable
data class SectionRender(
    val idx: Int,
    val sentences: List<SentenceRender> = emptyList()
)

/**
 * 文章跟打界面完整契约状态（前端只消费此模型，不可要求修改内核）
 */
@Immutable
data class PracticeUiState(
    val articleId: String = "",
    val bookId: String = "",
    val bookName: String = "",
    val title: String = "",
    val titleTranslate: String? = null,
    val articleIdx: Int = 0,
    val totalArticles: Int = 0,
    val articleList: List<ArticleItemUi> = emptyList(),
    val sections: List<SectionRender> = emptyList(),
    val cursor: Cursor = Cursor(),
    val isSpace: Boolean = false,
    val isEnd: Boolean = false,
    val dictation: Boolean = false,
    val translate: Boolean = true,
    val stats: PracticeStats = PracticeStats(),
    val progressPercent: Float = 0f,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

